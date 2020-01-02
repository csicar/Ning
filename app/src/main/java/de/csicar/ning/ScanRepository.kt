package de.csicar.ning

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import de.csicar.ning.scanner.*
import kotlinx.coroutines.*
import java.net.Inet4Address

class ScanRepository(
    private val networkDao: NetworkDao,
    private val scanDao: ScanDao,
    private val deviceDao: DeviceDao,
    private val application: Application
) {
    private val scanId by lazy {
        MutableLiveData<Long>()
    }

    suspend fun startScan(
        interfaceName: String,
        scanProgress: MutableLiveData<ScanProgress>,
        currentNetwork: MutableLiveData<Long>
    ) =
        withContext(Dispatchers.IO) {
            val newScanId = scanDao.insert(Scan(0, 0))


            val networkData =
                InterfaceScanner.getNetworkInterfaces().find { it.interfaceName == interfaceName }!!
            val networkId = networkDao.insert(
                Network.from(
                    networkData.address,
                    networkData.prefix,
                    newScanId,
                    networkData.interfaceName
                )
            )
            withContext(Dispatchers.Main) {
                scanProgress.value = ScanProgress.ScanNotStarted
                scanId.value = newScanId
                currentNetwork.value = networkId
            }
            val network = networkDao.getByIdNow(networkId)
            listOf(launch {
                PingScanner(network) { newResult ->
                    if (newResult.isReachable) {
                        Log.d("asd", "isReachable ${newResult.ipAddress}")
                        deviceDao.insertIfNew(networkId, newResult.ipAddress)
                    }
                    scanProgress.postValue(scanProgress.value + newResult.progressIncrease)
                }.pingIpAddresses()
            }, launch {
                NsdScanner(application) { newResult ->
                    deviceDao.upsertName(networkId, newResult.ipAddress, newResult.name)
                }.scan()
            }, launch {
                delay(1000)
                ArpScanner.getArpTableFromFile().forEach {
                    val ip = it.key
                    if (ip is Inet4Address) {
                        deviceDao.upsertHwAddress(scanId.value ?: return@forEach, ip, it.value.hwAddress)
                    }
                }
            }, launch {
                val userDevice = LocalMacScanner.asDevice(network) ?: return@launch
                deviceDao.upsert(userDevice)
            }).joinAll()
            //this@withContext.launch { fetchHwAddressesFromArpTable() }
            withContext(Dispatchers.Main) {
                scanProgress.value = ScanProgress.ScanFinished
            }
            network
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
