package de.csicar.ning

import android.app.Application
import androidx.lifecycle.*
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
    val networkScanRepository = ScanRepository(networkDao, scanDao, deviceDao, application)
    val scanProgress by lazy { MutableLiveData<ScanRepository.ScanProgress>() }
    private val currentNetworkId = MutableLiveData<Long>()

    val devices = Transformations.switchMap(currentNetworkId) {
        deviceDao.getAll(it)
    }


    suspend fun startScan(interfaceName: String): Network {
        val network = networkScanRepository.startScan(interfaceName, scanProgress, currentNetworkId)
        return network
    }
}

