package de.csicar.ning

import java.net.Inet4Address

fun inet4AddressFromInt(host: String, ip: Int): Inet4Address {
    return Inet4Address.getByAddress(
        host, byteArrayOf(
            (ip ushr 24 and 0xFF).toByte(),
            (ip ushr 16 and 0xFF).toByte(),
            (ip ushr 8 and 0xFF).toByte(),
            (ip and 0xFF).toByte()
        )
    ) as Inet4Address
}

fun Inet4Address.maskWith(maskLength: Short): Inet4Address {
    val mask = (2.shl(maskLength.toInt()) - 1).shl(32 - maskLength)
    val masked = this.hashCode().and(mask)
    return inet4AddressFromInt("", masked)
}