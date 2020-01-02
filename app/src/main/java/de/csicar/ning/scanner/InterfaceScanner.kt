package de.csicar.ning.scanner

import de.csicar.ning.Network
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

object InterfaceScanner {
    fun getNetworkInterfaces(): List<NetworkResult> {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence().flatMap { networkInterface ->
                networkInterface.interfaceAddresses.asSequence().map {
                    val addr = it.address
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        NetworkResult(addr, it.networkPrefixLength, networkInterface.displayName, networkInterface.displayName)
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

    data class NetworkResult(
        val address: Inet4Address,
        val prefix: Short,
        val interfaceName: String,
        val displayName: String
    )
}