package de.csicar.ning

import de.csicar.ning.scanner.LowLevelMDnsScanner
import org.junit.Assert.*
import org.junit.Test

/**
 * ExampleU local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MDnsParserTest {
    fun paketFromDump(dump: String) =
        dump
            .trim()
            .split("\n")
            .map { line ->
                line.trim().substring(7, 54).trim().split(" ").map {
                    it.toInt(16).toByte()
                }
            }.flatten()
            .toByteArray()

    private val scanner = LowLevelMDnsScanner { }

    /** Encode a DNS name from dot-separated labels, terminated by 0x00. */
    private fun dnsName(vararg labels: String): ByteArray {
        val parts = mutableListOf<Byte>()
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.UTF_8)
            parts.add(bytes.size.toByte())
            parts.addAll(bytes.toList())
        }
        parts.add(0x00)
        return parts.toByteArray()
    }

    private fun bytesOf(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    /** Build a minimal DNS response header. */
    private fun dnsHeader(
        questions: Int = 0,
        answers: Int = 0,
        authority: Int = 0,
        additional: Int = 0,
    ): ByteArray =
        bytesOf(
            0x00,
            0x00, // Transaction ID
            0x84,
            0x00, // Flags: standard response
            (questions shr 8) and 0xFF,
            questions and 0xFF,
            (answers shr 8) and 0xFF,
            answers and 0xFF,
            (authority shr 8) and 0xFF,
            authority and 0xFF,
            (additional shr 8) and 0xFF,
            additional and 0xFF,
        )

    /** Build a DNS resource record. */
    private fun dnsRecord(
        name: ByteArray,
        type: Int,
        classCode: Int = 0x0001,
        ttl: Int = 120,
        rdata: ByteArray,
    ): ByteArray =
        name +
            bytesOf((type shr 8) and 0xFF, type and 0xFF) +
            bytesOf((classCode shr 8) and 0xFF, classCode and 0xFF) +
            bytesOf(
                (ttl shr 24) and 0xFF,
                (ttl shr 16) and 0xFF,
                (ttl shr 8) and 0xFF,
                ttl and 0xFF,
            ) +
            bytesOf((rdata.size shr 8) and 0xFF, rdata.size and 0xFF) +
            rdata

    /** Build a DNS question entry. */
    private fun dnsQuestion(
        name: ByteArray,
        type: Int = 0x000c,
        classCode: Int = 0x0001,
    ): ByteArray =
        name +
            bytesOf((type shr 8) and 0xFF, type and 0xFF) +
            bytesOf((classCode shr 8) and 0xFF, classCode and 0xFF)

    @Test
    fun test3() {
        /** Multicast Domain Name System (response)
        Transaction ID: 0x0000
        Flags: 0x8400 Standard query response, No error
        Questions: 0
        Answer RRs: 1
        Authority RRs: 0
        Additional RRs: 3
        Answers
            _hap._tcp.local: type PTR, class IN, HASS Bridge 0KQ6 A72F86._hap._tcp.local
                Name: _hap._tcp.local
                Type: PTR (domain name PoinTeR) (12)
                .000 0000 0000 0001 = Class: IN (0x0001)
                0... .... .... .... = Cache flush: False
                Time to live: 4500 (1 hour, 15 minutes)
                Data length: 26
                Domain Name: HASS Bridge 0KQ6 A72F86._hap._tcp.local
        Additional records
            HASS Bridge 0KQ6 A72F86._hap._tcp.local: type SRV, class IN, cache flush, priority 0, weight 0, port 51828, target HASS Bridge 0KQ6 A72F86._hap._tcp.local
                Service: HASS Bridge 0KQ6 A72F86
                Protocol: _hap
                Name: _tcp.local
                Type: SRV (Server Selection) (33)
                .000 0000 0000 0001 = Class: IN (0x0001)
                1... .... .... .... = Cache flush: True
                Time to live: 120 (2 minutes)
                Data length: 8
                Priority: 0
                Weight: 0
                Port: 51828
                Target: HASS Bridge 0KQ6 A72F86._hap._tcp.local
            HASS Bridge 0KQ6 A72F86._hap._tcp.local: type TXT, class IN, cache flush
                Name: HASS Bridge 0KQ6 A72F86._hap._tcp.local
                Type: TXT (Text strings) (16)
                .000 0000 0000 0001 = Class: IN (0x0001)
                1... .... .... .... = Cache flush: True
                Time to live: 4500 (1 hour, 15 minutes)
                Data length: 85
                TXT Length: 19
                TXT: md=HASS Bridge 0KQ6
                TXT Length: 6
                TXT: pv=1.0
                TXT Length: 20
                TXT: id=1B:E1:13:17:21:16
                TXT Length: 4
                TXT: c#=2
                TXT Length: 4
                TXT: s#=1
                TXT Length: 4
                TXT: ff=0
                TXT Length: 4
                TXT: ci=2
                TXT Length: 4
                TXT: sf=0
                TXT Length: 11
                TXT: sh=qt6RpA==
            HASS Bridge 0KQ6 A72F86._hap._tcp.local: type A, class IN, cache flush, addr 192.168.10.3
                Name: HASS Bridge 0KQ6 A72F86._hap._tcp.local
                Type: A (Host Address) (1)
                .000 0000 0000 0001 = Class: IN (0x0001)
                1... .... .... .... = Cache flush: True
                Time to live: 120 (2 minutes)
                Data length: 4
                Address: 192.168.10.3
        [Unsolicited: True]
        */
        val paket = """
            0000   00 00 84 00 00 00 00 01 00 00 00 03 04 5f 68 61   ............._ha
            0010   70 04 5f 74 63 70 05 6c 6f 63 61 6c 00 00 0c 00   p._tcp.local....
            0020   01 00 00 11 94 00 1a 17 48 41 53 53 20 42 72 69   ........HASS Bri
            0030   64 67 65 20 30 4b 51 36 20 41 37 32 46 38 36 c0   dge 0KQ6 A72F86.
            0040   0c c0 27 00 21 80 01 00 00 00 78 00 08 00 00 00   ..'.!.....x.....
            0050   00 ca 74 c0 27 c0 27 00 10 80 01 00 00 11 94 00   ..t.'.'.........
            0060   55 13 6d 64 3d 48 41 53 53 20 42 72 69 64 67 65   U.md=HASS Bridge
            0070   20 30 4b 51 36 06 70 76 3d 31 2e 30 14 69 64 3d    0KQ6.pv=1.0.id=
            0080   31 42 3a 45 31 3a 31 33 3a 31 37 3a 32 31 3a 31   1B:E1:13:17:21:1
            0090   36 04 63 23 3d 32 04 73 23 3d 31 04 66 66 3d 30   6.c#=2.s#=1.ff=0
            00a0   04 63 69 3d 32 04 73 66 3d 30 0b 73 68 3d 71 74   .ci=2.sf=0.sh=qt
            00b0   36 52 70 41 3d 3d c0 27 00 01 80 01 00 00 00 78   6RpA==.'.......x
            00c0   00 04 c0 a8 0A 03                                 ......
            """
        val result = scanner.parse(paketFromDump(paket))
        assertEquals(
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_hap._tcp.local",
                    domainName = listOf("HASS Bridge 0KQ6 A72F86", "_hap", "_tcp", "local"),
                    txt = "",
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                    domainName = listOf("HASS Bridge 0KQ6 A72F86", "_hap", "_tcp", "local"),
                    txt = "",
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                    domainName = listOf(),
                    txt = "md=HASS Bridge 0KQ6.pv=1.0.id=1B:E1:13:17:21:16.c#=2.s#=1.ff=0.ci=2.sf=0.sh=qt6RpA==.HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                    domainName = listOf("/192.168.10.3"),
                    txt = "",
                ),
            ),
            result,
        )
    }

    @Test
    fun test2() {
        /** Description from Wireshark:
        Multicast Domain Name System (response)
        Transaction ID: 0x0000
        Flags: 0x8400 Standard query response, No error
        Questions: 0
        Answer RRs: 1
        Authority RRs: 0
        Additional RRs: 3
        Answers
        _esphomelib._tcp.local: type PTR, class IN, esphome_wohnzimmer._esphomelib._tcp.local
        Name: _esphomelib._tcp.local
        Type: PTR (domain name PoinTeR) (12)
        .000 0000 0000 0001 = Class: IN (0x0001)
        0... .... .... .... = Cache flush: False
        Time to live: 4500 (1 hour, 15 minutes)
        Data length: 43
        Domain Name: esphome_wohnzimmer._esphomelib._tcp.local
        Additional records
        esphome_wohnzimmer._esphomelib._tcp.local: type SRV, class IN, cache flush, priority 0, weight 0, port 6053, target esphome_wohnzimmer.local
        Service: esphome_wohnzimmer
        Protocol: _esphomelib
        Name: _tcp.local
        Type: SRV (Server Selection) (33)
        .000 0000 0000 0001 = Class: IN (0x0001)
        1... .... .... .... = Cache flush: True
        Time to live: 4500 (1 hour, 15 minutes)
        Data length: 32
        Priority: 0
        Weight: 0
        Port: 6053
        Target: esphome_wohnzimmer.local
        esphome_wohnzimmer._esphomelib._tcp.local: type TXT, class IN, cache flush
        Name: esphome_wohnzimmer._esphomelib._tcp.local
        Type: TXT (Text strings) (16)
        .000 0000 0000 0001 = Class: IN (0x0001)
        1... .... .... .... = Cache flush: True
        Time to live: 4500 (1 hour, 15 minutes)
        Data length: 65
        TXT Length: 16
        TXT: mac=807d3a6e49c6
        TXT Length: 32
        TXT: address=esphome_wohnzimmer.local
        TXT Length: 14
        TXT: version=1.15.3
        esphome_wohnzimmer.local: type A, class IN, cache flush, addr 192.168.0.150
        Name: esphome_wohnzimmer.local
        Type: A (Host Address) (1)
        .000 0000 0000 0001 = Class: IN (0x0001)
        1... .... .... .... = Cache flush: True
        Time to live: 120 (2 minutes)
        Data length: 4
        Address: 192.168.0.150
        [Unsolicited: True]
         */
        val paket = """
                0000   00 00 84 00 00 00 00 01 00 00 00 03 0b 5f 65 73   ............._es
                0010   70 68 6f 6d 65 6c 69 62 04 5f 74 63 70 05 6c 6f   phomelib._tcp.lo
                0020   63 61 6c 00 00 0c 00 01 00 00 11 94 00 2b 12 65   cal..........+.e
                0030   73 70 68 6f 6d 65 5f 77 6f 68 6e 7a 69 6d 6d 65   sphome_wohnzimme
                0040   72 0b 5f 65 73 70 68 6f 6d 65 6c 69 62 04 5f 74   r._esphomelib._t
                0050   63 70 05 6c 6f 63 61 6c 00 c0 2e 00 21 80 01 00   cp.local....!...
                0060   00 11 94 00 20 00 00 00 00 17 a5 12 65 73 70 68   .... .......esph
                0070   6f 6d 65 5f 77 6f 68 6e 7a 69 6d 6d 65 72 05 6c   ome_wohnzimmer.l
                0080   6f 63 61 6c 00 c0 2e 00 10 80 01 00 00 11 94 00   ocal............
                0090   41 10 6d 61 63 3d 36 30 37 64 33 61 36 65 34 32   A.mac=607d3a6e49
                00a0   63 36 20 61 64 64 72 65 73 73 3d 65 73 70 68 6f   c6 address=espho
                00b0   6d 65 5f 77 6f 68 6e 7a 69 6d 6d 65 72 2e 6c 6f   me_wohnzimmer.lo
                00c0   63 61 6c 0e 76 65 72 73 69 6f 6e 3d 31 2e 31 35   cal.version=1.15
                00d0   2e 33 c0 6b 00 01 80 01 00 00 00 78 00 04 c0 a8   .3.k.......x....
                00e0   00 96                                             ..
                """
        val result = scanner.parse(paketFromDump(paket))
        assertEquals(
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_esphomelib._tcp.local",
                    domainName = listOf("esphome_wohnzimmer", "_esphomelib", "_tcp", "local"),
                    txt = "",
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "esphome_wohnzimmer._esphomelib._tcp.local",
                    domainName = listOf("esphome_wohnzimmer", "local"),
                    txt = "",
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "esphome_wohnzimmer._esphomelib._tcp.local",
                    domainName = listOf(),
                    txt = "mac=607d3a6e42c6.address=esphome_wohnzimmer.local.version=1.15.3.esphome_wohnzimmer.local",
                ),
                LowLevelMDnsScanner.DnsAnswer(name = "esphome_wohnzimmer.local", domainName = listOf("/192.168.0.150"), txt = ""),
            ),
            result,
        )
    }

    // ---- Tests for specific parsing fixes ----

    @Test
    fun `rangeToInt handles values above 255 correctly`() {
        // Verify that multi-byte integer parsing works correctly.
        // The TTL field is 4 bytes. We use TTL = 0x00011194 = 70036.
        // With the old bug (* 0xFF), the parsed TTL would be wrong,
        // which would corrupt the data-length offset and break parsing.
        // We verify by checking that a record with TTL=4500 (0x1194) and
        // a subsequent record are both parsed correctly — the second record's
        // offset depends on the first record's dataLength being read correctly.
        val name = dnsName("_test", "_tcp", "local")
        val rdata1 = dnsName("device1", "_test", "_tcp", "local")
        val rdata2 = dnsName("device2", "_test", "_tcp", "local")

        val packet =
            dnsHeader(answers = 2) +
                dnsRecord(name, type = 0x000c, ttl = 4500, rdata = rdata1) +
                dnsRecord(name, type = 0x000c, ttl = 4500, rdata = rdata2)

        val result = scanner.parse(packet)
        // If rangeToInt were broken, the second record would be parsed
        // from the wrong offset and either fail or return garbage.
        assertEquals(2, result.size)
        assertEquals("device1", result[0].domainName[0])
        assertEquals("device2", result[1].domainName[0])
    }

    @Test
    fun `rangeToInt handles bytes with high bit set`() {
        // Build a packet with one A record. The IPv4 address is 192.168.1.1
        // (bytes 0xC0, 0xA8, 0x01, 0x01 — all except last have high bit set or are >= 128).
        // If rangeToInt doesn't handle sign extension, the address will be mangled.
        val name = dnsName("myhost", "local")
        val rdata = bytesOf(0xC0, 0xA8, 0x01, 0x01) // 192.168.1.1

        val packet =
            dnsHeader(answers = 1) +
                dnsRecord(name, type = 0x0001, rdata = rdata)

        val result = scanner.parse(packet)
        assertEquals(1, result.size)
        assertEquals("/192.168.1.1", result[0].domainName[0])
    }

    @Test
    fun `SRV record parses target name skipping priority weight port`() {
        // Build a minimal SRV record: 2 bytes priority + 2 bytes weight + 2 bytes port + target name.
        val recordName = dnsName("MyPrinter", "_ipp", "_tcp", "local")
        val targetName = dnsName("printer", "local")
        val srvRdata =
            bytesOf(
                0x00,
                0x00, // priority = 0
                0x00,
                0x00, // weight = 0
                0x02,
                0x77, // port = 631
            ) + targetName

        val packet =
            dnsHeader(answers = 1) +
                dnsRecord(recordName, type = 33, rdata = srvRdata) // type 33 = SRV

        val result = scanner.parse(packet)
        assertEquals(1, result.size)
        assertEquals("MyPrinter._ipp._tcp.local", result[0].name)
        assertEquals(listOf("printer", "local"), result[0].domainName)
    }

    @Test
    fun `response with echoed question section parses correctly`() {
        // Some mDNS responders echo the question back. The parser must skip the
        // question section before parsing answer records.
        val questionName = dnsName("_hap", "_tcp", "local")
        val question = dnsQuestion(questionName, type = 0x000c) // PTR query

        val ptrRdata = dnsName("MyDevice", "_hap", "_tcp", "local")
        val answerName = dnsName("_hap", "_tcp", "local")
        val answer = dnsRecord(answerName, type = 0x000c, rdata = ptrRdata)

        val packet = dnsHeader(questions = 1, answers = 1) + question + answer

        val result = scanner.parse(packet)
        assertEquals(1, result.size)
        assertEquals("_hap._tcp.local", result[0].name)
        assertEquals(
            listOf("MyDevice", "_hap", "_tcp", "local"),
            result[0].domainName,
        )
    }

    @Test
    fun `multiple answer records are all parsed`() {
        val name1 = dnsName("_ssh", "_tcp", "local")
        val rdata1 = dnsName("server1", "_ssh", "_tcp", "local")
        val name2 = dnsName("_ssh", "_tcp", "local")
        val rdata2 = dnsName("server2", "_ssh", "_tcp", "local")

        val packet =
            dnsHeader(answers = 2) +
                dnsRecord(name1, type = 0x000c, rdata = rdata1) +
                dnsRecord(name2, type = 0x000c, rdata = rdata2)

        val result = scanner.parse(packet)
        assertEquals(2, result.size)
        assertEquals("server1", result[0].domainName[0])
        assertEquals("server2", result[1].domainName[0])
    }

    @Test
    fun `hostname extracts instance name from PTR answer`() {
        val answers =
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_hap._tcp.local",
                    domainName = listOf("My Smart Speaker", "_hap", "_tcp", "local"),
                    txt = "",
                ),
            )
        val scanResult =
            LowLevelMDnsScanner.ScanResult(
                answers,
                java.net.Inet4Address.getByAddress(bytesOf(192, 168, 1, 1)),
                5353,
            )
        assertEquals("My Smart Speaker", scanResult.hostname)
    }

    @Test
    fun `hostname returns null when all labels start with underscore`() {
        val answers =
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_hap._tcp.local",
                    domainName = listOf("_hap", "_tcp"),
                    txt = "",
                ),
            )
        val scanResult =
            LowLevelMDnsScanner.ScanResult(
                answers,
                java.net.Inet4Address.getByAddress(bytesOf(192, 168, 1, 1)),
                5353,
            )
        assertNull(scanResult.hostname)
    }

    @Test
    fun `hostname returns null when no answers have domainNames`() {
        val answers =
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_hap._tcp.local",
                    domainName = listOf(),
                    txt = "some=txt",
                ),
            )
        val scanResult =
            LowLevelMDnsScanner.ScanResult(
                answers,
                java.net.Inet4Address.getByAddress(bytesOf(192, 168, 1, 1)),
                5353,
            )
        assertNull(scanResult.hostname)
    }

    @Test
    fun `empty response parses to empty list`() {
        val packet = dnsHeader(answers = 0)
        val result = scanner.parse(packet)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `AAAA record parses IPv6 address`() {
        val name = dnsName("myhost", "local")
        // ::1 in 16 bytes
        val rdata =
            bytesOf(
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x01,
            )
        val packet =
            dnsHeader(answers = 1) +
                dnsRecord(name, type = 28, rdata = rdata) // type 28 = AAAA

        val result = scanner.parse(packet)
        assertEquals(1, result.size)
        assertEquals("myhost.local", result[0].name)
        // InetAddress.toString() for ::1
        assertTrue(result[0].domainName[0].contains("0:0:0:0:0:0:0:1"))
    }
}
