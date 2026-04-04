package de.csicar.ning.scanner

import android.util.Log
import it.alessangiorgi.ipneigh30.ArpNDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.util.regex.Pattern

data class ArpEntry(val ip: InetAddress, val hwAddress: MacAddress) {
    companion object {
        fun from(ip: String, mac: String) = ArpEntry(InetAddress.getByName(ip), MacAddress(mac))
    }
}

data class MacAddress(val address: String) {
    fun getAddress(hideMacDetail: Boolean): String {
        if (hideMacDetail) {
            return address.take("aa:bb:cc".length) + ":XX:XX:XX"
        }
        return address
    }

    val isBroadcast get() = address == "00:00:00:00:00:00"
}

private val whiteSpacePattern = Pattern.compile("\\s+")

private fun InputStream.readStreamAsTable(): Sequence<List<String>> {
    return this.bufferedReader().use { it.readText() }.lineSequence()
        .map { it.split(whiteSpacePattern) }
}


object ArpScanner {
    
    private val TAG = ArpScanner.javaClass.name

    suspend fun getFromAllSources() = withContext(Dispatchers.Default) {
        listOf(
            async { getArpTableFromFile() },
            async { getArpTableFromIpCommand() },
            async { getArpTableFromNdk() }
        )
            .awaitAll()
            .asSequence()
            .flatten()
            .filter { !it.hwAddress.isBroadcast }
            .associateBy { it.ip }

    }

    private suspend fun getArpTableFromFile(): Sequence<ArpEntry> = withContext(Dispatchers.IO) {
        try {
            File("/proc/net/arp").inputStream().readStreamAsTable()
                .drop(1)
                .filter { it.size == 6 }
                .map {
                    ArpEntry.from(it[0], it[3])
                }
        } catch (exception: FileNotFoundException) {
            Log.e(TAG, "arp file not found $exception")
            emptySequence<ArpEntry>()
        }
    }

    private suspend fun getArpTableFromIpCommand(): Sequence<ArpEntry> =
        withContext(Dispatchers.IO) {
            try {

                val execution = Runtime.getRuntime().exec("ip neigh")
                execution.waitFor()
                execution.inputStream.readStreamAsTable()
                    .filter { it.size >= 5 }
                    .map {
                        ArpEntry.from(it[0], it[4])
                    }
                    .onEach { Log.d(TAG, "found entry in 'ip neight': $it") }
            } catch (exception: IOException) {
                Log.e(TAG, "io error when running ip neigh $exception")
                emptySequence<ArpEntry>()
            }
        }

    private suspend fun getArpTableFromNdk(): Sequence<ArpEntry> =
        withContext(Dispatchers.IO) {
            try {
                val arpOutput = ArpNDK.getARP()
                Log.d(TAG, "NDK ARP output: $arpOutput")

                arpOutput.lineSequence()
                    .map { it.split(whiteSpacePattern) }
                    .filter { it.size >= 5 }
                    .mapNotNull {
                        try {
                            ArpEntry.from(it[0], it[4])
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse NDK ARP entry: ${it.joinToString(" ")}", e)
                            null
                        }
                    }
                    .onEach { Log.d(TAG, "found entry in NDK ARP: $it") }
            } catch (exception: Exception) {
                Log.e(TAG, "error when getting ARP from NDK: $exception")
                emptySequence<ArpEntry>()
            }
        }
}

