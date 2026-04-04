package de.csicar.ning

import android.app.Application
import android.content.Intent
import android.os.Build
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

    val scanProgress: StateFlow<ScanRepository.ScanProgress> = ScanService.scanProgress
    private val currentNetworkId: StateFlow<NetworkId?> = ScanService.currentNetworkId
    val currentScanId: StateFlow<ScanId?> = ScanService.currentScanId

    val devices: StateFlow<List<DeviceWithName>> = currentNetworkId
        .filterNotNull()
        .flatMapLatest { deviceDao.getAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNetworks: StateFlow<List<Network>> = currentScanId
        .filterNotNull()
        .flatMapLatest { networkDao.getAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchAvailableInterfaces() = InterfaceScanner.getNetworkInterfaces()

    fun getAllScans(): Flow<List<Scan>> = scanDao.getAll()

    fun getNetworksForScan(scanId: ScanId): Flow<List<Network>> = networkDao.getAll(scanId)

    fun getDevicesForNetwork(networkId: NetworkId): Flow<List<DeviceWithName>> = deviceDao.getAll(networkId)

    fun getDevice(id: DeviceId): Flow<DeviceWithName?> = deviceDao.getById(id)

    fun getPortsForDevice(id: DeviceId): Flow<List<Port>> = portDao.getAllForDevice(id)

    fun scanPorts(device: Device) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PortScanner(device.ip).scanPorts().forEach {
                    launch {
                        val result = it.await()
                        if (result.isOpen) {
                            portDao.upsert(
                                Port(
                                    PortId(0),
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

    fun startScan(interfaceName: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, ScanService::class.java).apply {
            putExtra(ScanService.EXTRA_INTERFACE_NAME, interfaceName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
