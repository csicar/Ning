package de.csicar.ning

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.csicar.ning.scanner.MacAddress
import de.csicar.ning.util.inet4AddressFromInt
import de.csicar.ning.util.maskWith
import java.net.Inet4Address

@Entity
data class Scan(@PrimaryKey(autoGenerate = true) val scanId: Long, val startedAt: Long)

@Entity
data class Device(
    @PrimaryKey(autoGenerate = true) val deviceId: Long,
    val networkId: Long,
    val ip: Inet4Address,
    val deviceName: String?,
    val hwAddress: MacAddress?,
    val isScanningDevice: Boolean = false
)

enum class DeviceType {
    PC,
    VM,
    PHONE,
    SPEAKER,
    SOC,
    ROUTER,
    NETWORK_DEVICE,
    GAME_CONSOLE,
    CAST,
    UNKNOWN
}

@DatabaseView("SELECT Device.deviceId, Device.networkId, Device.ip, Device.hwAddress, Device.deviceName, MacVendor.name as vendorName, Device.isScanningDevice FROM Device LEFT JOIN MacVendor ON MacVendor.mac = substr(Device.hwAddress, 0, 9)")
data class DeviceWithName(
    val deviceId: Long,
    val networkId: Long,
    val ip: Inet4Address,
    val hwAddress: MacAddress?,
    val deviceName: String?,
    val vendorName: String?,
    val isScanningDevice: Boolean
) {
    @Ignore
    val asDevice = Device(deviceId, networkId, ip, deviceName, hwAddress, isScanningDevice)

    private val deviceType
        get() = when {
            isScanningDevice -> DeviceType.PHONE

            /**
             * Device type based on MAC address
             */
            hwAddress?.address?.startsWith("52:54:00") == true -> DeviceType.VM // Unofficial mac address range used by KVM

            /**
             * Device type based on vendor name
             */
            vendorName == null -> DeviceType.UNKNOWN
            // Phone
            vendorName.contains("LG Electronics (Mobile Communications)", ignoreCase = true) -> DeviceType.PHONE
            vendorName.contains("HUAWEI", ignoreCase = true) -> DeviceType.PHONE
            vendorName.contains("Xiaomi", ignoreCase = true) -> DeviceType.PHONE
            vendorName.contains("Fairphone", ignoreCase = true) -> DeviceType.PHONE
            // Router
            vendorName.contains("Compal", ignoreCase = true) -> DeviceType.ROUTER
            vendorName.contains("Ubiquiti", ignoreCase = true) -> DeviceType.ROUTER
            vendorName.contains("AVM", ignoreCase = true) -> DeviceType.ROUTER
            vendorName.contains("TP-LINK", ignoreCase = true) -> DeviceType.ROUTER
            // Speaker
            vendorName.contains("Sonos", ignoreCase = true) -> DeviceType.SPEAKER
            // SoC
            vendorName.contains("Espressif", ignoreCase = true) -> DeviceType.SOC
            vendorName.contains("Raspberry", ignoreCase = true) -> DeviceType.SOC
            // Network device
            vendorName.contains("ADMTEK", ignoreCase = true) -> DeviceType.NETWORK_DEVICE
            // Video game
            vendorName.contains("Nintendo", ignoreCase = true) -> DeviceType.GAME_CONSOLE
            // Cast
            vendorName.contains("AzureWave", ignoreCase = true) -> DeviceType.CAST
            // VM
            vendorName.contains("VMware", ignoreCase = true) -> DeviceType.VM

            else -> DeviceType.UNKNOWN
        }

    val icon
        get() = when (deviceType) {
            DeviceType.PHONE -> R.drawable.ic_baseline_phone_android_48
            DeviceType.ROUTER -> R.drawable.ic_baseline_router_48
            DeviceType.SPEAKER -> R.drawable.ic_baseline_speaker_48
            DeviceType.SOC -> R.drawable.ic_memory_white_48dp
            DeviceType.NETWORK_DEVICE -> R.drawable.ic_baseline_settings_ethernet_48
            DeviceType.GAME_CONSOLE -> R.drawable.ic_baseline_videogame_asset_48
            DeviceType.CAST -> R.drawable.ic_baseline_cast_48
            DeviceType.PC -> R.drawable.ic_laptop_white_48dp
            DeviceType.VM -> R.drawable.ic_baseline_layers_24
            DeviceType.UNKNOWN -> R.drawable.ic_baseline_devices_other_24
        }
}

@Entity
data class Network(
    @PrimaryKey(autoGenerate = true) val networkId: Long,
    val baseIp: Inet4Address,
    val mask: Short,
    val scanId: Long,
    val interfaceName: String,
    val bssid: MacAddress?,
    val ssid: String?
) {
    companion object {
        fun from(
            ip: Inet4Address,
            mask: Short,
            scanId: Long,
            interfaceName: String,
            bssid: MacAddress?,
            ssid: String?
        ): Network {
            return Network(0, ip.maskWith(mask), mask, scanId, interfaceName, bssid, ssid)
        }
    }

    fun enumerateAddresses(): Sequence<Inet4Address> {
        return generateSequence(0) {
            val next = it + 1
            if (next < networkSize) next else null
        }
            .map { baseIp.hashCode() + it }
            .map { inet4AddressFromInt("", it) }
    }

    fun containsAddress(host: Inet4Address): Boolean {
        return this.baseIp.maskWith(mask) == host.maskWith(mask)
    }

    val networkSize get() = 2.shl(32 - mask.toInt() - 1)

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
    val description get() = PortDescription.commonPorts.find { it.port == port }
}

@Entity(primaryKeys = ["name", "mac"])
data class MacVendor(val name: String, val mac: String)

@Entity
data class PortDescription(
    @PrimaryKey
    val portId: Long,
    val port: Int,
    val protocol: Protocol,
    val serviceName: String,
    val serviceDescription: String
) {
    companion object {
        val commonPorts = listOf(
            PortDescription(0, 21, Protocol.TCP, "FTP", "File Transfer Protocol"),
            PortDescription(0, 22, Protocol.TCP, "SFTP/SSH", "Secure FTP or Secure Shell"),
            PortDescription(0, 80, Protocol.TCP, "HTTP", "Hypertext Transport Protocol"),
            PortDescription(0, 53, Protocol.UDP, "DNS", "DNS Server"),
            PortDescription(0, 443, Protocol.TCP, "HTTPS", "Secure HTTP"),
            PortDescription(0, 548, Protocol.TCP, "AFP", "AFP over TCP"),
            PortDescription(0, 631, Protocol.TCP, "IPP", "Internet Printing Protocol"),
            PortDescription(0, 989, Protocol.TCP, "FTPS", "FTP over TLS"),
            PortDescription(0, 1883, Protocol.TCP, "MQTT", "Message Queuing Telemetry Transport"),
            PortDescription(0, 5000, Protocol.TCP, "UPNP", "Universal Plug and Play"),
            PortDescription(0, 8000, Protocol.TCP, "HTTP Alt", "HTTP common alternative"),
            PortDescription(0, 8080, Protocol.TCP, "HTTP-Proxy", "HTTP Proxy"),
            PortDescription(0, 62078, Protocol.TCP, "iPhone-Sync", "lockdown iOS Service")

            )
    }
}