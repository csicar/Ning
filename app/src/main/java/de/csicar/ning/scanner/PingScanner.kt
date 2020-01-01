package de.csicar.ning.scanner

import android.net.wifi.WifiInfo
import android.util.Log
import de.csicar.ning.Device
import de.csicar.ning.Network
import de.csicar.ning.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

fun getLocalIpAddress(scanId: Long): List<Network> {
    return try {
        NetworkInterface.getNetworkInterfaces().asSequence().flatMap { networkInterface ->
            networkInterface.interfaceAddresses.asSequence().map {
                val addr = it.address
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    Network.from(addr, it.networkPrefixLength, scanId, networkInterface.displayName)
                } else {
                    null
                }
            }.filterNotNull()
        }.toList()
    } catch (ex: SocketException) {
        ex.printStackTrace()
        listOf()
    }
}

suspend fun pingIpAddresses(viewModel: ScanViewModel, network: Network) =
    withContext(Dispatchers.IO) {
        network.enumerateAddresses().asSequence().chunked(10).forEach { ipAddresses ->
            launch {
                ipAddresses.forEach { ipAddress ->
                    val isReachable = ipAddress.isReachable(500)
                    if (isReachable) {
                        Log.d("asd", "IP: $ipAddress is reachable.")

                        viewModel.deviceDao.insert(
                            Device(
                                0, network.networkId,
                                ipAddress, null, viewModel.getHwFromArp(ipAddress)
                            )
                        )

                        viewModel.fetchArpTable()
                    }
                    withContext(Dispatchers.Main) {
                        viewModel.scanProgress.value = (viewModel.scanProgress.value
                            ?: ScanViewModel.ScanProgress.ScanNotStarted) + 1.0 / network.networkSize
                    }
                }
            }
        }
    }