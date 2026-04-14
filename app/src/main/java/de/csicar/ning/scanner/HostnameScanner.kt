package de.csicar.ning.scanner

import android.util.Log
import de.csicar.ning.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Performs reverse DNS lookups (PTR record queries) for a list of IP addresses.
 * This is a fallback for devices that don't respond to mDNS.
 */
class HostnameScanner(
    private val devices: List<Device>,
) {
    companion object {
        val TAG: String = HostnameScanner::class.java.name
    }

    data class ScanResult(
        val ip: Inet4Address,
        val hostname: String,
    )

    suspend fun resolve(): List<ScanResult> =
        withContext(Dispatchers.IO) {
            devices
                .chunked(10)
                .map { chunk ->
                    async {
                        chunk.mapNotNull { device ->
                            try {
                                val addr = InetAddress.getByAddress(device.ip.address)
                                val hostname = addr.canonicalHostName
                                // canonicalHostName returns the IP string if lookup fails; skip in that case
                                if (hostname != device.ip.hostAddress) {
                                    ScanResult(device.ip, hostname)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Reverse DNS failed for ${device.ip}: ${e.message}")
                                null
                            }
                        }
                    }
                }.awaitAll()
                .flatten()
        }
}
