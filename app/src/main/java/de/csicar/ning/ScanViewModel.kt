package de.csicar.ning

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import de.csicar.ning.scanner.ArpEntry
import de.csicar.ning.scanner.MacAddress
import de.csicar.ning.scanner.getArpTableFromFile
import de.csicar.ning.scanner.pingIpAddresses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.sql.Timestamp

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    val db = Room
        .databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java, "ning-db"
        )
        .createFromAsset("mac_devices.db")
        .fallbackToDestructiveMigration()
        .build()
    val deviceDao = db.deviceDao()
    val networkDao = db.networkDao()
    val portDao = db.portDao()
    val scanDao = db.scanDao()

    val scanId by lazy {
        MutableLiveData<Long>()
    }

    val currentScanId get() = scanId.value!!

    //val currentScan get() = scanDao.getById(scanId)

    val arpTable by lazy {
        MutableLiveData<List<ArpEntry>>().also {
            fetchArpTable()
        }
    }

    suspend fun startScan() = withContext(Dispatchers.Main) {
        scanId.value = scanDao.insert(Scan(0, 0))
        pingIpAddresses(this@ScanViewModel)
    }

    fun fetchArpTable() {
        viewModelScope.launch {
            getArpTableFromFile().forEach {
                if (it.ip is Inet4Address) {
                    withContext(Dispatchers.IO) {
                        val device = deviceDao.getByAddress(it.ip, currentScanId)
                        if (device != null) {
                            deviceDao.update(
                                Device(
                                    device.deviceId,
                                    device.networkId,
                                    device.ip,
                                    it.hwAddress
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun getHwFromArp(ipAddress: Inet4Address): MacAddress? =
        arpTable.value?.find { it.ip == ipAddress }?.hwAddress
}

