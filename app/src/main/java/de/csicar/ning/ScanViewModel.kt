package de.csicar.ning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.csicar.ning.scanner.InterfaceScanner
import de.csicar.ning.scanner.PortScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.createInstance(application)

    val deviceDao = db.deviceDao()
    private val networkDao = db.networkDao()
    val portDao = db.portDao()
    private val scanDao = db.scanDao()
    private val networkScanRepository = ScanRepository(
        networkDao,
        scanDao,
        deviceDao,
        portDao,
        application
    )

    val scanProgress = MutableStateFlow<ScanRepository.ScanProgress>(ScanRepository.ScanProgress.ScanNotStarted)
    private val currentNetworkId = MutableStateFlow<Long?>(null)
    val currentScanId = MutableStateFlow<Long?>(null)

    val devices: StateFlow<List<DeviceWithName>> = currentNetworkId
        .filterNotNull()
        .flatMapLatest { deviceDao.getAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNetworks: StateFlow<List<Network>> = currentScanId
        .filterNotNull()
        .flatMapLatest { networkDao.getAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchAvailableInterfaces() = networkScanRepository.fetchAvailableInterfaces()

    fun getAllScans(): Flow<List<Scan>> = scanDao.getAll()

    fun getNetworksForScan(scanId: Long): Flow<List<Network>> = networkDao.getAll(scanId)

    fun getDevicesForNetwork(networkId: Long): Flow<List<DeviceWithName>> = deviceDao.getAll(networkId)

    fun getDevice(id: Long): Flow<DeviceWithName?> = deviceDao.getById(id)

    fun getPortsForDevice(id: Long): Flow<List<Port>> = portDao.getAllForDevice(id)

    fun scanPorts(device: Device) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PortScanner(device.ip).scanPorts().forEach {
                    launch {
                        val result = it.await()
                        if (result.isOpen) {
                            portDao.upsert(
                                Port(
                                    0,
                                    result.port,
                                    result.protocol,
                                    device.deviceId
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun startScan(interfaceName: String): Network? {
        val network = networkScanRepository.startScan(interfaceName, scanProgress, currentNetworkId) ?: return null
        currentScanId.value = network.scanId
        return network
    }
}
