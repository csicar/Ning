package de.csicar.ning.scanner

import android.util.Log
import de.csicar.ning.Device
import de.csicar.ning.Network
import kotlinx.coroutines.*
import java.net.Inet4Address

class PingScanner(
    val network: Network,
    val onUpdate: (ScanResult) -> Unit
) {

    suspend fun pingIpAddresses() :  List<ScanResult> =
        withContext(Dispatchers.IO) {
            network.enumerateAddresses().chunked(10).map { ipAddresses ->
                async {
                    ipAddresses.map { ipAddress ->
                        val isReachable = ipAddress.isReachable(5000)
                        val result = ScanResult(ipAddress, isReachable, 1.0/ network.networkSize)
                        onUpdate(result)
                        result
                    }
                }
            }.toList().awaitAll().flatten()
        }

    data class ScanResult(val ipAddress: Inet4Address, val isReachable: Boolean, val progressIncrease: Double)
}

