package de.csicar.ning.scanner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.util.regex.Pattern

data class ArpEntry(val ip: InetAddress, val hwAddress: MacAddress)

data class MacAddress(val address: String) {
    val isBroadcast get() = address == "00:00:00:00:00:00"
}

suspend fun getArpTableFromFile() = withContext(Dispatchers.IO) {
    val arpFile = File("/proc/net/arp").inputStream()
    val arpString = arpFile.bufferedReader().use { it.readText() }
    val whiteSpacePattern = Pattern.compile("\\s+")
    arpString.lines().asSequence()
        .drop(1)
        .map { it.split(whiteSpacePattern) }
        .filter { it.size == 6 }
        .map {
            ArpEntry(InetAddress.getByName(it[0]), MacAddress(it[3]))
        }
        .filter { !it.hwAddress.isBroadcast }
        .toList()
}