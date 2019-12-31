package de.csicar.ning.scanner

import android.net.wifi.WifiInfo
import de.csicar.ning.Device
import de.csicar.ning.Network
import de.csicar.ning.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

fun getLocalIpAddress(scanId: Long): List<Network> {
    return try {
        NetworkInterface.getNetworkInterfaces().asSequence().flatMap { networkInterface ->
            if (networkInterface.name != "wlan0")  return@flatMap emptySequence<Network>()
            networkInterface.interfaceAddresses.asSequence().map {
                val addr = it.address
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    Network.from(addr, it.networkPrefixLength, scanId)
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

suspend fun pingIpAddresses(viewModel: ScanViewModel) = withContext(Dispatchers.IO) {
    val scanId = viewModel.scanId.value!!
    val ipVal = getLocalIpAddress(scanId)
    ipVal.map { network ->
        val networkId = viewModel.networkDao.insert(Network(0, network.baseIp, network.mask, scanId))
        network.enumerateAddresses().map {
            it to it.isReachable(500)
        }.forEach {
            if (it.second) {
                val ipAddress = it.first
                viewModel.deviceDao.insert(
                    Device(0, networkId,
                        ipAddress, viewModel.getHwFromArp(ipAddress))
                )
                viewModel.fetchArpTable()

            }
        }
    }
}