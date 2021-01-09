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
        val receiveBuffer = ByteArray(1024)
        while (true) {
            val dp2 = DatagramPacket(receiveBuffer, receiveBuffer.size)
            ds.receive(dp2)
            val textContent = String(dp2.data, Charset.forName("UTF8"));
            Log.d(TAG, "data: ${dp2.data} --  ${textContent}")

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
            val (newIndex, answer) = parseAnswer(
                byteArray.copyOfRange(index, byteArray.size),
                noAnswerRecords
            )
            println("==> answer: $answer")
            index = newIndex
            answers += answer
        }
        println("\n\n authority records (index: $index)")
        repeat(noAuthorityRecords) {
            val (newIndex, answer) = parseAnswer(
                byteArray.copyOfRange(index, byteArray.size),
                noAnswerRecords
            )
            println("==> answer: $answer")
            index = newIndex
            answers += answer
        }
        println("\n\n additional records (index: $index)")
        repeat(noAdditionalRecords) {
            val (newIndex, answer) = parseAnswer(
                byteArray.copyOfRange(index, byteArray.size),
                noAnswerRecords
            )
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

    fun parseString(byteArray: ByteArray): Pair<Int, List<String>> {
        val name: MutableList<String> = mutableListOf()
        var i = 0;
        while (byteArray[i] != 0x00.toByte() && i <= byteArray.size) {
            val partLength = byteArray[i]
            print(partLength)
            val stringStart = i + 1
            val stringEnd = stringStart + partLength
            name += listOf(String(byteArray.copyOfRange(stringStart, stringEnd), Charsets.UTF_8))
            println(name)
            i += partLength + 1
        }
        return i to name
    }

    fun parseAnswer(byteArray: ByteArray, numberOfAnswers: Int): Pair<Int, DnsAnswers> {
        val (i, name) = parseString(byteArray)
        val type = byteArray.rangeToInt(i + 1, i + 2)
        val classCode = byteArray.rangeToInt(i + 3, i + 4)
        val timeToLive = byteArray.rangeToInt(i + 5, i + 9)
        println("type: $type (${parseRecordType(type)}), classCode: $classCode, ttl: $timeToLive")
        val dataLength = byteArray.rangeToInt(i + 9, i + 10)
        println(String(byteArray))
        println(byteArray[i + 11])
        val dataContent = byteArray.copyOfRange(i + 11, i + 11 + dataLength)
        val (_, domainName) = when (parseRecordType(type)) {
            RecordType.POINTER -> parseString(dataContent)
            RecordType.A -> parseString(dataContent)
            null -> null to null
            else -> TODO("not implemented for: ${parseRecordType(type)}")
        }
        return (i + 11 + dataLength) to DnsAnswers(
            name.joinToString("."),
            domainName?.joinToString(".") ?: "",
            ""
        )
    }

    enum class RecordType {
        A, NS, MD, MF, CNAME, SOA, MB, MG, MR, NULL, WKS,
        POINTER, HINFO, MINFO, MX, TXT
    }
    // https://www.ietf.org/rtc/rtc1035.html#section-3.2.2
    fun parseRecordType(typeCode: Int) = when (typeCode) {
        1->RecordType.A
        2->RecordType.NS
        3->RecordType.MD
        4->RecordType.MF
        5->RecordType.CNAME
        6->RecordType.SOA
        7->RecordType.MB
        8->RecordType.MG
        9->RecordType.MR
        10->RecordType.NULL
        11->RecordType.WKS
        12 -> RecordType.POINTER
        13->RecordType.HINFO
        14->RecordType.MINFO
        15->RecordType.MX
        16->RecordType.TXT
        else -> null
    }

    data class DnsAnswers(val name: String, val domainName: String, val txt: String)

    data class ScanResult(val content: String)
}