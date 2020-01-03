package de.csicar.ning.scanner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.util.regex.Pattern

data class ArpEntry(val ip: InetAddress, val hwAddress: MacAddress) {
    companion object {
        fun from(ip: String, mac: String) = ArpEntry(InetAddress.getByName(ip), MacAddress(mac))
    }
}

data class MacAddress(val address: String) {
    val isBroadcast get() = address == "00:00:00:00:00:00"
}

private val whiteSpacePattern = Pattern.compile("\\s+")

private fun InputStream.readStreamAsTable(): Sequence<List<String>> {
    return this.bufferedReader().use { it.readText() }.lineSequence().map { it.split(whiteSpacePattern) }
}


object ArpScanner {

    suspend fun getFromAllSources() = withContext(Dispatchers.Default) {
        listOf(async { getArpTableFromFile() }, async { getArpTableFromIpCommand() })
            .awaitAll()
            .asSequence()
            .flatten()
            .filter { !it.hwAddress.isBroadcast }
            .associateBy { it.ip }

    }

    private suspend fun getArpTableFromFile() = withContext(Dispatchers.IO) {
        File("/proc/net/arp").inputStream().readStreamAsTable()
            .drop(1)
            .filter { it.size == 6 }
            .map {
                ArpEntry.from(it[0], it[3])
            }
    }

    private suspend fun getArpTableFromIpCommand() = withContext(Dispatchers.IO) {
        val execution = Runtime.getRuntime().exec("ip neigh")
        execution.waitFor()
        execution.inputStream.readStreamAsTable()
            .filter { it.size >= 5 }
            .map {
                ArpEntry.from(it[0], it[4])
            }
            .onEach { Log.d("asd", "found entry in 'ip neight': $it") }
    }
}



