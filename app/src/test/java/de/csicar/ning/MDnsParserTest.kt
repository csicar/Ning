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
        val scanner = LowLevelMDnsScanner { res -> Unit }
        val result = scanner.parse(paketFromDump(paket))
        assertEquals(
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_hap._tcp.local",
                    domainName = listOf("HASS Bridge 0KQ6 A72F86", "_hap", "_tcp", "local"),
                    txt = ""
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                    domainName = listOf(),
                    txt = ""
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "HASS Bridge 0KQ6 A72F86._hap._tcp.local",
                    domainName = listOf(),
                    txt = "md=HASS Bridge 0KQ6.pv=1.0.id=1B:E1:13:17:21:16.c#=2.s#=1.ff=0.ci=2.sf=0.sh=qt6RpA==.HASS Bridge 0KQ6 A72F86._hap._tcp.local"
                ),
                LowLevelMDnsScanner.DnsAnswer(name="HASS Bridge 0KQ6 A72F86._hap._tcp.local", domainName=listOf("/192.168.10.3"), txt="")
            ),
            result
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
        val scanner = LowLevelMDnsScanner { res -> Unit }
        val result = scanner.parse(paketFromDump(paket))
        assertEquals(
            listOf(
                LowLevelMDnsScanner.DnsAnswer(
                    name = "_esphomelib._tcp.local",
                    domainName = listOf("esphome_wohnzimmer", "_esphomelib", "_tcp", "local"),
                    txt = ""
                ),

                LowLevelMDnsScanner.DnsAnswer(
                    name = "esphome_wohnzimmer._esphomelib._tcp.local",
                    domainName = listOf(),
                    txt = ""
                ),
                LowLevelMDnsScanner.DnsAnswer(
                    name = "esphome_wohnzimmer._esphomelib._tcp.local",
                    domainName = listOf(),
                    txt = "mac=607d3a6e42c6.address=esphome_wohnzimmer.local.version=1.15.3.esphome_wohnzimmer.local"
                ),
                LowLevelMDnsScanner.DnsAnswer(name="esphome_wohnzimmer.local", domainName=listOf("/192.168.0.150"), txt="")
            ),
            result
        )

    }
}
