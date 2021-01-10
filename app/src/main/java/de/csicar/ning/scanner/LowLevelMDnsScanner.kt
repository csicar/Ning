package de.csicar.ning.scanner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            (byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                    //  ^- Header
                    + 0x05 + "_coap".toByteArray() + 0x04 + "_udp".toByteArray() + 0x05 + "local".toByteArray() + 0x00
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
        val receiveBuffer = ByteArray(1024 * 16)
        while (true) {
            val dp2 = DatagramPacket(receiveBuffer, receiveBuffer.size)
            ds.receive(dp2)
            val paketContent = dp2.data.copyOfRange(0, dp2.length)
            val textContent = String(paketContent, Charset.forName("UTF8"));
            Log.d(TAG, "data: ${paketContent} --  ${textContent}")
            val parsed = parse(paketContent)
            Log.d(TAG, "parsed: $parsed")
        }
        ds.close()

        true

    }

    fun parse(byteArray: ByteArray): List<DnsAnswers> {
        val noAnswerRecords = byteArray.rangeToInt(6, 7)
        val noAuthorityRecords = byteArray.rangeToInt(8, 9)
        val noAdditionalRecords = byteArray.rangeToInt(10, 11)
        println("noAnswerRecords: $noAnswerRecords, noAuthorityRecords: $noAuthorityRecords, noAdditionalRecords: $noAdditionalRecords")
        var index = 12
        val answers = mutableListOf<DnsAnswers>()
        println("\n\n answer records (index: $index)")
        repeat(noAnswerRecords) {
            val (newIndex, answer) = parseAnswer(index, byteArray)
            println("==> answer: $answer")
            index = newIndex
            answers += answer
        }
        println("\n\n authority records (index: $index)")
        repeat(noAuthorityRecords) {
            val (newIndex, answer) = parseAnswer(index, byteArray)
            println("==> answer: $answer")
            index = newIndex
            answers += answer
        }
        println("\n\n additional records (index: $index)")
        repeat(noAdditionalRecords) {
            val (newIndex, answer) = parseAnswer(index, byteArray)
            println("==> answer: $answer")
            index = newIndex
            answers += answer
        }
        return answers
    }

    private fun ByteArray.rangeToInt(start: Int, end: Int): Int {
        var value = 0
        for (i in start..end) {
            value = value * 0xFF + this[i]
        }
        return value
    }

    fun parseString(
        startIndex: Int,
        byteArray: ByteArray,
        references: MutableMap<Int, List<String>> = mutableMapOf(),
        depth: Int = 1
    ): Pair<Int, List<String>> {
        val depthStr = "\t".repeat(depth)
        //println("\n$depthStr parseString at index $startIndex")
        val name: MutableList<String> = mutableListOf()
        var i = startIndex;
        var hitReference = false
        while (i < byteArray.size && byteArray[i] != 0x00.toByte() && !hitReference) {
            val partLength = byteArray[i].toUByte()
            //println("$depthStr  string part at $i length: $partLength ")
            val referenceMask = 0b11000000
            if (partLength.toInt().and(referenceMask) != 0) {
                val referenceIndex =
                    partLength.toInt().and(referenceMask.inv()) * 0xFF + byteArray[i + 1]
                //println("$depthStr  goto reference -> $referenceIndex (0b${referenceIndex.toString(2)})")
                val referenceValue = references.getOrPut(referenceIndex) {
                    if (depth > 30) {
                        listOf("")
                    } else {
                        parseString(referenceIndex, byteArray, references, depth + 1).second
                    }
                }
                name += referenceValue
                //println("$depthStr  referenceValue $referenceValue (from index: $referenceIndex) state: $references")
                hitReference = true
                i += 1
            } else {
                val stringStart = i + 1
                val stringEnd = stringStart + partLength.toInt()
                name += listOf(
                    String(
                        byteArray.copyOfRange(stringStart, stringEnd),
                        Charsets.UTF_8
                    )
                ).also {
                    //println("$depthStr  direct string part: $it (from index: $stringStart)")
                }
                i += partLength.toInt() + 1
            }

        }
        //println("$depthStr  done\n")
        return i to name
    }

    fun parseAnswer(
        startIndex: Int,
        byteArray: ByteArray
    ): Pair<Int, DnsAnswers> {
        val (i, name) = parseString(startIndex, byteArray)
        val type = byteArray.rangeToInt(i + 1, i + 2)
        val classCode = byteArray.rangeToInt(i + 3, i + 4)
        val timeToLive = byteArray.rangeToInt(i + 5, i + 9)
        val dataLength = byteArray.rangeToInt(i + 9, i + 10)
        val dataIndex = i + 11
        println("parseAnswer at $startIndex => type: $type (${parseRecordType(type)}), classCode: $classCode, ttl: $timeToLive, dataIndex: $dataIndex, name: $name")
        println("i: $i")

        val combinedName = name.joinToString(".")
        val dnsAnswer = when (parseRecordType(type)) {
            RecordType.POINTER ->
                DnsAnswers(combinedName, parseString(dataIndex, byteArray).second, "")
            RecordType.SRV ->
                DnsAnswers(combinedName, parseString(dataIndex, byteArray).second, "")
            RecordType.TXT ->
                DnsAnswers(combinedName, listOf(), parseString(dataIndex, byteArray).second.joinToString("."))
            RecordType.A ->
                DnsAnswers(combinedName, listOf(Inet4Address.getByAddress(byteArray.copyOfRange(dataIndex, dataIndex+4)).toString()), "")
            RecordType.AAAA ->
                DnsAnswers(combinedName, listOf(Inet6Address.getByAddress(byteArray.copyOfRange(dataIndex, dataIndex+16)).toString()), "")
            null -> TODO("unknown record type $type")
            else -> TODO("not implemented for: ${parseRecordType(type)}")
        }
        return (i + 11 + dataLength) to dnsAnswer

    }

    enum class RecordType {
        A, NS, MD, MF, CNAME, SOA, MB, MG, MR, NULL, WKS,
        POINTER, HINFO, MINFO, MX, TXT, AAAA, SRV
    }

    // https://www.ietf.org/rtc/rtc1035.html#section-3.2.2
    fun parseRecordType(typeCode: Int) = when (typeCode) {
        1 -> RecordType.A
        2 -> RecordType.NS
        3 -> RecordType.MD
        4 -> RecordType.MF
        5 -> RecordType.CNAME
        6 -> RecordType.SOA
        7 -> RecordType.MB
        8 -> RecordType.MG
        9 -> RecordType.MR
        10 -> RecordType.NULL
        11 -> RecordType.WKS
        12 -> RecordType.POINTER
        13 -> RecordType.HINFO
        14 -> RecordType.MINFO
        15 -> RecordType.MX
        16 -> RecordType.TXT
        28 -> RecordType.AAAA
        33 -> RecordType.SRV
        else -> null
    }

    data class DnsAnswers(val name: String, val domainName: List<String>, val txt: String)

    data class ScanResult(val content: String)
}