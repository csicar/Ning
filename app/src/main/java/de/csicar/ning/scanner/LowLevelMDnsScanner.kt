package de.csicar.ning.scanner

import android.util.Log
import de.csicar.ning.util.toInet4Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException
import java.net.*
import java.nio.charset.Charset

class LowLevelMDnsScanner(private val onUpdate: (ScanResult) -> Unit) {
    companion object {
        val MDNS_IP: InetAddress =
            Inet4Address.getByAddress(byteArrayOf(224.toByte(), 0, 0, 251.toByte()))
        const val MDNS_PORT = 5353
        const val SERVICE_PORT = 11323
        val TAG = LowLevelMDnsScanner::class.java.name
    }

    suspend fun probeMDns() = withContext(Dispatchers.IO) {
        Log.d(TAG, "probe!")
        val bytes =
            (byteArrayOf(0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00)
             //  ^- Header
                + 0x05 +  "_coap".toByteArray() + 0x04 + "_udp".toByteArray() + 0x05 + "local".toByteArray() + 0x00
                    //  # ^- Name
                    + 0x00 + 0x0c
                    //  # ^- termination & type PTR
                    + 0x80.toByte() + 0x01)
                    // # ^- UNICAST & Q Class
        val ds = MulticastSocket(SERVICE_PORT)
        ds.joinGroup(MDNS_IP);
        val dp = DatagramPacket(bytes, bytes.size, MDNS_IP, MDNS_PORT)
        ds.timeToLive = 2
        ds.soTimeout = 1000
        ds.send(dp)
        val receiveBuffer = ByteArray(1024)
        while (true) {
            val dp2 = DatagramPacket(receiveBuffer, receiveBuffer.size)
            ds.receive(dp2)
            val textContent = String(dp2.data, Charset.forName ("UTF8"));
            Log.d(TAG, "data: ${dp2.data} --  ${textContent}")

        }
        ds.close()

        true

    }


    data class ScanResult(val content: String)
}