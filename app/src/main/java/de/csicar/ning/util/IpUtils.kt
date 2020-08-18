package de.csicar.ning.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.csicar.ning.R
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

class AppPreferences {
    val context: Context
    private val resources: Resources
    private val preferences: SharedPreferences

    constructor(context: Context, resources: Resources) {
        this.context = context
        this.resources = resources
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    constructor(view: View) : this(view.context, view.resources)

    constructor(fragment: Fragment) : this(fragment.requireContext(), fragment.resources)


    val hideMacDetails
        get(): Boolean {
            return preferences.getBoolean(resources.getString(R.string.hideMacDetails), false)
        }
}