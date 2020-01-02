package de.csicar.ning.scanner

import de.csicar.ning.Device
import de.csicar.ning.Network
import java.net.Inet4Address
import java.net.NetworkInterface


object LocalMacScanner {
    fun asDevice(network: Network): Device? {
        val foundMac = getMacAddresses()
            .filterKeys { network.containsAddress(it) }.entries.firstOrNull()
            ?: return null

        return Device(0, network.networkId, foundMac.key, null, foundMac.value)
    }

    private fun intToMacAddress(value: ByteArray) =
        MacAddress(value.joinToString(":") { String.format("%02X", it) })

    private fun getMacAddresses(): Map<Inet4Address, MacAddress> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .flatMap { nic ->
                    nic.interfaceAddresses
                        .mapNotNull {
                            if (it.address is Inet4Address && nic.hardwareAddress != null) {
                                (it.address as Inet4Address) to intToMacAddress(nic.hardwareAddress)
                            } else {
                                null
                            }
                        }
                }
                .associate { it }
        } catch (ex: Exception) { //handle exception
            ex.printStackTrace()
            emptyMap()
        }
    }
}
