package de.csicar.ning.scanner

import android.util.Log
import de.csicar.ning.Port
import de.csicar.ning.PortDescription
import de.csicar.ning.Protocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException
import java.net.*
import java.util.logging.Logger

class PortScanner(val ip: InetAddress) {
    companion object {
        val TAG = PortScanner::class.java.name
    }

    suspend fun isUdpPortOpen(port: Int) = withContext(Dispatchers.IO) {
        try {
            val bytes = ByteArray(128)
            val ds = DatagramSocket()
            val dp = DatagramPacket(bytes, bytes.size, ip, port)
            ds.soTimeout = 1000
            ds.send(dp)
            val dp2 = DatagramPacket(bytes, bytes.size)
            ds.receive(dp2)
            ds.close()
        } catch (e: InterruptedIOException) {
            return@withContext false
        } catch (e: IOException) {
            return@withContext false
        }
        true

    }

    suspend fun isTcpPortOpen(port: Int) = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            Log.d(TAG, "trying socket: $ip : $port")
            socket = Socket(ip, port)
            return@withContext true
        } catch (ex: ConnectException) {
            Log.d(TAG, "Got connection error: $ex")
            return@withContext false
        } catch (ex: NoRouteToHostException) {
            Log.d(TAG, "No Route to Host: $ex")
            return@withContext  false
        } finally {
            socket?.close()
        }
    }

    suspend fun scanAllPorts() = withContext(Dispatchers.Main) {
        (1 .. 65535).flatMap {
            listOf(
                async {
                    PortResult(it, Protocol.TCP, isTcpPortOpen(it))
                },
                async {
                    PortResult(it, Protocol.UDP, isUdpPortOpen(it))
                }
            )
        }
    }


    suspend fun scanCommonPorts() = withContext(Dispatchers.Main) {
        PortDescription.commonPorts.flatMap {
            listOf(
                async {
                    PortResult(it.port, Protocol.TCP, isTcpPortOpen(it.port))
                },
                async {
                    PortResult(it.port, Protocol.UDP, isUdpPortOpen(it.port))
                }
            )
        }
    }

    data class PortResult(val port: Int, val protocol: Protocol, val isOpen: Boolean)
}
