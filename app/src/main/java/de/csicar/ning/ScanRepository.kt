package de.csicar.ning

import android.app.Application
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import de.csicar.ning.scanner.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.Inet4Address


class ScanRepository(
    private val networkDao: NetworkDao,
    private val scanDao: ScanDao,
    private val deviceDao: DeviceDao,
    private val portDao: PortDao,
    private val application: Application
) {
    companion object {
        val TAG: String = ScanRepository::class.java.name
    }

    private val scanId = MutableStateFlow<Long?>(null)

    fun fetchAvailableInterfaces() = InterfaceScanner.getNetworkInterfaces()

    suspend fun startScan(
        interfaceName: String,
        scanProgress: MutableStateFlow<ScanProgress>,
        currentNetwork: MutableStateFlow<Long?>
    ) =
        withContext(Dispatchers.IO) {
            val newScanId = scanDao.insert(Scan(0, System.currentTimeMillis()))
            val connectionInfo = getWifiConnectionInfo(application)
            val bssid = if (connectionInfo == null) null else MacAddress(connectionInfo.bssid)
            val ssid = cleanSsid(connectionInfo?.ssid)

            val networkData =
                InterfaceScanner.getNetworkInterfaces()
                    .also { Log.d(TAG, "NetworkInterfaces: $it") }
                    .find { it.interfaceName == interfaceName } ?: return@withContext null

            val networkId = networkDao.insert(
                Network.from(
                    networkData.address,
                    networkData.prefix,
                    newScanId,
                    networkData.interfaceName,
                    bssid,
                    ssid
                )
            )
            scanProgress.value = ScanProgress.ScanNotStarted
            scanId.value = newScanId
            currentNetwork.value = networkId

            val network = networkDao.getByIdNow(networkId)
                .also { Log.d(TAG, "new network scan added: $it") }

            // Map from IpAddress to number of occurrences in old scans
            val ipGuesses = deviceDao
                .getDevicesInPreviousScans(network.ssid, network.bssid, network.baseIp)
                .map {
                    it.ip
                }
                .groupBy { it }
                .mapValues { it.value.size }

            listOf(launch {
                PingScanner(network, ipGuesses) { newResult ->
                    if (newResult.isReachable) {
                        Log.d(TAG, "isReachable ${newResult.ipAddress}")
                        deviceDao.insertIfNew(networkId, newResult.ipAddress)
                        launch { updateFromArp() }
                    }
                    scanProgress.value = scanProgress.value + newResult.progressIncrease
                }.pingIpAddresses()
            }, launch {
                LowLevelMDnsScanner { newResult ->
                    Log.d(TAG, "res: $newResult")
                }.probeCommon()
            }, launch {
                updateFromArp()
            }, launch {
                val userDevice = LocalMacScanner.asDevice(network) ?: return@launch
                deviceDao.upsert(userDevice)
            }).joinAll()
            updateFromArp()
            delay(500)
            updateFromArp()
            scanProgress.value = ScanProgress.ScanFinished
            network
        }

    private suspend fun updateFromArp() {
        ArpScanner.getFromAllSources().forEach {
            val ip = it.key
            if (ip is Inet4Address) {
                deviceDao.upsertHwAddress(
                    scanId.value ?: return@forEach,
                    ip,
                    it.value.hwAddress,
                    false
                )
            }
        }
    }

    private fun getWifiConnectionInfo(context: Context): WifiInfo? {
        val mWifiManager = (context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager)
        return mWifiManager?.connectionInfo
    }

    private fun cleanSsid(rawSsid: String?): String? {
        if (rawSsid == null) return null

        // Remove surrounding quotes
        var cleaned = rawSsid.trim().removeSurrounding("\"")

        // Return null if it's the unknown SSID placeholder
        if (cleaned == "<unknown ssid>" || cleaned.isEmpty()) {
            return null
        }

        return cleaned
    }


    sealed class ScanProgress {
        object ScanNotStarted : ScanProgress()
        data class ScanRunning(val progress: Double) : ScanProgress()
        object ScanFinished : ScanProgress()


        operator fun plus(progress: Double) = when (this) {
            is ScanNotStarted -> ScanRunning(
                progress
            )
            is ScanRunning -> ScanRunning(
                this.progress + progress
            )
            is ScanFinished -> ScanFinished
        }

    }
}

operator fun ScanRepository.ScanProgress?.plus(progress: Double): ScanRepository.ScanProgress =
    this?.plus(progress) ?: ScanRepository.ScanProgress.ScanRunning(progress)
