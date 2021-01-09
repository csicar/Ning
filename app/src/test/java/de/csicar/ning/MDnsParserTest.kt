package de.csicar.ning

import de.csicar.ning.scanner.LowLevelMDnsScanner
import org.junit.Test

import org.junit.Assert.*

/**
 * ExampleU local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MDnsParserTest {
    fun paketFromDump(dump: String) =
        dump.trim().split("\n").map { line ->
            line.trim().substring(7, 54).trim().split(" ").map {
                it.toInt(16).toByte()
            }
        }.flatten().toByteArray()

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
                0090   41 10 6d 61 63 3d 38 30 37 64 33 61 36 65 34 39   A.mac=807d3a6e49
                00a0   63 36 20 61 64 64 72 65 73 73 3d 65 73 70 68 6f   c6 address=espho
                00b0   6d 65 5f 77 6f 68 6e 7a 69 6d 6d 65 72 2e 6c 6f   me_wohnzimmer.lo
                00c0   63 61 6c 0e 76 65 72 73 69 6f 6e 3d 31 2e 31 35   cal.version=1.15
                00d0   2e 33 c0 6b 00 01 80 01 00 00 00 78 00 04 c0 a8   .3.k.......x....
                00e0   00 96                                             ..
                """
        val scanner = LowLevelMDnsScanner { res -> Unit }
        val result = scanner.parse(paketFromDump(paket))
        assertEquals(
            listOf(
                LowLevelMDnsScanner.DnsAnswers(
                    name = "_esphomelib._tcp.local",
                    domainName = "esphome_wohnzimmer._esphomelib._tcp.local",
                    txt = ""
                )
            ),
            result
        )

    }

    @Test
    fun addition_isCorrect() {

        val examplePaket = ubyteArrayOf(
            0x00U,
            0x00U,
            0x84U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x05U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x05U,
            0x5fU,
            0x63U,
            0x6fU,
            0x61U,
            0x70U,
            0x04U,
            0x5fU,
            0x75U,
            0x64U,
            0x70U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x00U,
            0x0cU,
            0x00U,
            0x01U,
            0x00U,
            0x00U,
            0x11U,
            0x94U,
            0x00U,
            0x22U,
            0x0fU,
            0x67U,
            0x77U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x5fU,
            0x63U,
            0x6fU,
            0x61U,
            0x70U,
            0x04U,
            0x5fU,
            0x75U,
            0x64U,
            0x70U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x0fU,
            0x67U,
            0x77U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x5fU,
            0x63U,
            0x6fU,
            0x61U,
            0x70U,
            0x04U,
            0x5fU,
            0x75U,
            0x64U,
            0x70U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x00U,
            0x10U,
            0x00U,
            0x01U,
            0x00U,
            0x00U,
            0x11U,
            0x94U,
            0x00U,
            0x10U,
            0x0fU,
            0x76U,
            0x65U,
            0x72U,
            0x73U,
            0x69U,
            0x6fU,
            0x6eU,
            0x3dU,
            0x31U,
            0x2eU,
            0x31U,
            0x32U,
            0x2eU,
            0x33U,
            0x34U,
            0x0fU,
            0x67U,
            0x77U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x5fU,
            0x63U,
            0x6fU,
            0x61U,
            0x70U,
            0x04U,
            0x5fU,
            0x75U,
            0x64U,
            0x70U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x00U,
            0x21U,
            0x00U,
            0x01U,
            0x00U,
            0x00U,
            0x00U,
            0x78U,
            0x00U,
            0x2aU,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x16U,
            0x34U,
            0x1cU,
            0x54U,
            0x52U,
            0x41U,
            0x44U,
            0x46U,
            0x52U,
            0x49U,
            0x2dU,
            0x47U,
            0x61U,
            0x74U,
            0x65U,
            0x77U,
            0x61U,
            0x79U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x1cU,
            0x54U,
            0x52U,
            0x41U,
            0x44U,
            0x46U,
            0x52U,
            0x49U,
            0x2dU,
            0x47U,
            0x61U,
            0x74U,
            0x65U,
            0x77U,
            0x61U,
            0x79U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x00U,
            0x01U,
            0x00U,
            0x01U,
            0x00U,
            0x00U,
            0x00U,
            0x78U,
            0x00U,
            0x04U,
            0xc0U,
            0xa8U,
            0x00U,
            0xbcU,
            0x1cU,
            0x54U,
            0x52U,
            0x41U,
            0x44U,
            0x46U,
            0x52U,
            0x49U,
            0x2dU,
            0x47U,
            0x61U,
            0x74U,
            0x65U,
            0x77U,
            0x61U,
            0x79U,
            0x2dU,
            0x34U,
            0x34U,
            0x39U,
            0x31U,
            0x36U,
            0x30U,
            0x32U,
            0x39U,
            0x31U,
            0x66U,
            0x64U,
            0x39U,
            0x05U,
            0x6cU,
            0x6fU,
            0x63U,
            0x61U,
            0x6cU,
            0x00U,
            0x00U,
            0x1cU,
            0x00U,
            0x01U,
            0x00U,
            0x00U,
            0x00U,
            0x78U,
            0x00U,
            0x10U,
            0xfeU,
            0x80U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x46U,
            0x91U,
            0x60U,
            0xffU,
            0xfeU,
            0x29U,
            0x1fU,
            0xd9U
        )
        val scanner = LowLevelMDnsScanner { res -> Unit }
        val result = scanner.parse(examplePaket.toByteArray())
        assertEquals(result, result)
    }
}
