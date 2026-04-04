package de.csicar.ning.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.net.Inet4Address

fun Int.toInet4Address(host: String = "") =
    inet4AddressFromInt(host, this)
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

class AppPreferences(val context: Context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    val hideMacDetails: Boolean
        get() = preferences.getBoolean("hideMacDetails", false)

    fun setHideMacDetails(value: Boolean) {
        preferences.edit().putBoolean("hideMacDetails", value).apply()
    }
}
