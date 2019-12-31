package de.csicar.ning

import android.util.Log
import androidx.room.*
import de.csicar.ning.scanner.MacAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.Inet4Address
import java.net.Socket
import java.sql.Timestamp

@Entity
data class Scan(@PrimaryKey(autoGenerate = true) val scanId : Long, val startedAt: Long)

@Entity
data class Device(
    @PrimaryKey(autoGenerate = true) val deviceId: Long, val networkId: Long,
    val ip: Inet4Address,
    val hwAddress: MacAddress?
) {
    suspend fun isPortOpen(port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                Log.d("asd", "trying socket: $ip : $port")
                socket = Socket(ip, port)
                return@withContext true
            } catch (ex: ConnectException) {
                Log.d("asd", "Got connection error: $ex")
                return@withContext false
            } finally {
                socket?.close()
            }
        }
    }

    suspend fun scanPorts() = withContext(Dispatchers.Main) {
        Port.commonPorts.map {
            async {
                it to this@Device.isPortOpen(it)
            }
        }
    }
}

@DatabaseView("SELECT Device.deviceId, Device.networkId, Device.ip, Device.hwAddress, MacVendor.name as vendorName FROM Device LEFT JOIN MacVendor ON MacVendor.mac = substr(Device.hwAddress, 0, 9)")
data class DeviceWithName(
    val deviceId: Long, val networkId: Long, val ip: Inet4Address, val hwAddress: MacAddress?
    , val vendorName: String?
)

@Entity
data class Network(
    @PrimaryKey(autoGenerate = true) val networkId: Long, val baseIp: Inet4Address,
    val mask: Short, val scanId: Long
) {
    companion object {
        fun from(ip: Inet4Address, mask: Short, scanId: Long): Network {
            Log.d("asd", ip.maskWith(mask).toString())
            return Network(0, ip.maskWith(mask), mask, scanId)
        }
    }

    fun enumerateAddresses(): Sequence<Inet4Address> {
        val maxOfMask = 2.shl(32 - mask.toInt() - 1)
        Log.d("asd", "${maxOfMask.toString()} mask: $mask")
        return generateSequence(0) {
            val next = it + 1
            if (next < maxOfMask) next else null
        }
            .map { baseIp.hashCode() + it }
            .map { inet4AddressFromInt("", it) }
    }

}

enum class Protocol {
    TCP, UDP
}

@Entity
data class Port(
    @PrimaryKey(autoGenerate = true) val portId: Long, val port: Int,
    val protocol: Protocol,
    val deviceId: Long
) {
    companion object {
        val commonPorts = listOf(80, 21, 22, 8080, 443)
    }
}

@Entity(primaryKeys = ["name", "mac"])
data class MacVendor(val name: String, val mac: String)