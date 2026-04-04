package de.csicar.ning.scanner

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

object InterfaceScanner {
    fun getNetworkInterfaces(): List<NetworkResult> {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence().flatMap { networkInterface ->
                networkInterface.interfaceAddresses.asSequence().map {
                    val address = it.address
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        NetworkResult(address, it.networkPrefixLength, networkInterface.displayName, networkInterface.displayName)
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