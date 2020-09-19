package de.csicar.ning

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import de.csicar.ning.scanner.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.createInstance(application)

    val deviceDao = db.deviceDao()
    private val networkDao = db.networkDao()
    val portDao = db.portDao()
    private val scanDao = db.scanDao()
    private val networkScanRepository = ScanRepository(networkDao, scanDao, deviceDao, application)
    val scanProgress by lazy { MutableLiveData<ScanRepository.ScanProgress>() }
    private val currentNetworkId = MutableLiveData<Long>()
    val currentScanId = MutableLiveData<Long>()

    val devices = Transformations.switchMap(currentNetworkId) {
        deviceDao.getAll(it)
    }

    val currentNetworks = Transformations.switchMap(currentScanId) {
        Log.d("asd", "netowkrsadss $it")
        networkDao.getAll(it)
    }

    fun fetchAvailableInterfaces() = networkScanRepository.fetchAvailableInterfaces()

    suspend fun startScan(interfaceName: String): Network? {
        val network = networkScanRepository.startScan(interfaceName, scanProgress, currentNetworkId) ?: return null
        currentScanId.value = network.scanId
        return network
    }
}

