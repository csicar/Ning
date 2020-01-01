package de.csicar.ning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.csicar.ning.scanner.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.createInstance(application)
    val deviceDao = db.deviceDao()
    val networkDao = db.networkDao()
    val portDao = db.portDao()
    val scanDao = db.scanDao()
    val scanProgress by lazy { MutableLiveData<ScanProgress>() }

    val scanId by lazy {
        MutableLiveData<Long>()
    }

    val networkId by lazy {
        MutableLiveData<Long>()
    }

    val currentScanId get() = scanId.value!!

    val arpTable by lazy {
        MutableLiveData<List<ArpEntry>>().also {
            fetchArpTable()
        }
    }

    suspend fun startScan(interfaceName: String) = withContext(Dispatchers.IO) {
        val newScanId = scanDao.insert(Scan(0, 0))
        withContext(Dispatchers.Main) {
            scanId.value = newScanId
        }
        val networkData = getLocalIpAddress(newScanId).find { it.interfaceName == interfaceName }!!
        val networkId = networkDao.insert(networkData)
        val network = networkDao.getByIdNow(networkId)
        withContext(Dispatchers.Main) {
            scanProgress.value = ScanProgress.ScanRunning(0.0)
        }
        viewModelScope.launch {
            launch {
                pingIpAddresses(this@ScanViewModel, network)
            }
            launch {
                NsdScanner(this@ScanViewModel).scan()
            }
        }
        withContext(Dispatchers.Main) {
            this@ScanViewModel.networkId.value = networkId
        }
        network
    }

    fun fetchArpTable() =
        viewModelScope.launch {
            getArpTableFromFile().forEach { arpEntry ->
                if (arpEntry.ip is Inet4Address) {
                    withContext(Dispatchers.IO) {
                        val device = deviceDao.getByAddress(arpEntry.ip, currentScanId)
                        if (device != null) {
                            deviceDao.updateHwAddress(device.deviceId, arpEntry.hwAddress)
                        }
                    }
                }
            }
        }

    fun getHwFromArp(ipAddress: Inet4Address): MacAddress? =
        arpTable.value?.find { it.ip == ipAddress }?.hwAddress

    sealed class ScanProgress {
        object ScanNotStarted : ScanProgress()
        data class ScanRunning(val progress: Double) : ScanProgress()
        object ScanFinished : ScanProgress()

        operator fun plus(progress: Double) =
            when(this) {
                is ScanNotStarted -> ScanRunning(progress)
                is ScanRunning -> ScanRunning(this.progress + progress)
                is ScanFinished -> ScanFinished
            }
    }
}

