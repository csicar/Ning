package de.csicar.ning

import androidx.room.*
import de.csicar.ning.scanner.MacAddress
import kotlinx.coroutines.flow.Flow
import java.net.Inet4Address

@Dao
interface NetworkDao {
    @Query("Select * FROM network WHERE scanId = :scanId")
    fun getAll(scanId: ScanId): Flow<List<Network>>

    @Query("Select * FROM network WHERE scanId = :scanId")
    fun getAllNow(scanId: ScanId): List<Network>

    @Insert
    fun insertRaw(network: Network): Long

    @Transaction
    fun insert(network: Network): NetworkId = NetworkId(insertRaw(network))

    @Insert
    fun insertAll(vararg networks: Network)

    @Query("SELECT * FROM network WHERE networkId = :networkId")
    fun getByIdNow(networkId: NetworkId): Network

    @Query("SELECT * FROM network WHERE networkId = :networkId")
    fun getById(networkId: NetworkId): Flow<Network>
}

@Dao
interface DeviceDao {
    @Query("Select * FROM DeviceWithName WHERE networkId = :networkId ORDER BY ip ASC")
    fun getAll(networkId: NetworkId): Flow<List<DeviceWithName>>

    @Query("SELECT * FROM Device WHERE networkId = :networkId")
    fun getAllNow(networkId: NetworkId): List<Device>

    @Insert
    fun insertRaw(device: Device): Long

    @Transaction
    fun insert(device: Device): DeviceId = DeviceId(insertRaw(device))

    @Transaction
    fun upsert(device: Device): DeviceId {
        val existingDevice = getByAddressInNetwork(device.ip, device.networkId)
        return if (existingDevice == null) {
            insert(device)
        } else {
            update(device.copy(deviceId = existingDevice.deviceId))
            existingDevice.deviceId
        }
    }

    @Transaction
    fun upsertName(
        networkId: NetworkId,
        ip: Inet4Address,
        name: String,
        allowNew: Boolean = true,
    ): DeviceId? {
        val existingDevice = getByAddressInNetwork(ip, networkId)
        if (existingDevice != null) {
            updateServiceName(existingDevice.deviceId, name)
            return existingDevice.deviceId
        } else if (allowNew) {
            return insert(Device(DeviceId(0), networkId, ip, name, null))
        }
        return null
    }

    @Transaction
    fun upsertHwAddress(
        networkId: NetworkId,
        ip: Inet4Address,
        hwAddress: MacAddress,
        allowNew: Boolean,
    ) {
        val existingDevice = getByAddressInNetwork(ip, networkId)
        if (existingDevice != null) {
            updateHwAddress(existingDevice.deviceId, hwAddress)
        } else if (allowNew) {
            insert(Device(DeviceId(0), networkId, ip, null, hwAddress))
        }
    }

    @Update
    fun update(device: Device)

    @Query("SELECT * FROM DeviceWithName WHERE deviceId = :id")
    fun getById(id: DeviceId): Flow<DeviceWithName?>

    @Query("SELECT * FROM DEVICE WHERE deviceId = :id")
    fun getByIdNow(id: DeviceId): Device

    @Query("SELECT * FROM Device WHERE ip = :ip AND networkId = :networkId")
    fun getByAddressInNetwork(
        ip: Inet4Address,
        networkId: NetworkId,
    ): Device?

    @Query("SELECT * FROM Device WHERE ip = :ip AND networkId IN (SELECT networkId FROM Network WHERE scanId = :scanId)")
    fun getByAddress(
        ip: Inet4Address,
        scanId: ScanId,
    ): Device?

    @Query(
        "SELECT * FROM device WHERE networkId IN (SELECT networkId FROM Network WHERE ssid=:ssid and bssid= :bssid and baseIp = :baseIp)",
    )
    fun getDevicesInPreviousScans(
        ssid: String?,
        bssid: MacAddress?,
        baseIp: Inet4Address,
    ): List<Device>

    @Query("UPDATE Device SET hwAddress = :hwAddress WHERE deviceId = :deviceId")
    fun updateHwAddress(
        deviceId: DeviceId,
        hwAddress: MacAddress,
    )

    @Query("UPDATE Device SET deviceName = :deviceName WHERE deviceId = :deviceId")
    fun updateServiceName(
        deviceId: DeviceId,
        deviceName: String?,
    )

    @Transaction
    fun insertIfNew(
        networkId: NetworkId,
        ip: Inet4Address,
    ): DeviceId {
        val existingAddress =
            getByAddressInNetwork(ip, networkId)
                ?: return insert(Device(
                    deviceId = DeviceId(0),
                    networkId = networkId,
                    ip = ip,
                    deviceName = null,
                    hwAddress = null
                ))
        return existingAddress.deviceId
    }
}

@Dao
interface PortDao {
    @Insert
    fun insertRaw(port: Port): Long

    @Transaction
    fun insert(port: Port): PortId = PortId(insertRaw(port))

    @Transaction
    suspend fun upsert(port: Port): PortId {
        val portFromDB = getPortFromNumber(port.deviceId, port.port) ?: return insert(port)

        update(Port(portFromDB.portId, port.port, port.protocol, port.deviceId))
        return portFromDB.portId
    }

    @Update
    fun update(port: Port)

    @Query("SELECT * FROM Port WHERE deviceId = :deviceId AND port = :port")
    fun getPortFromNumber(
        deviceId: DeviceId,
        port: Int,
    ): Port?

    @Query("SELECT * FROM Port WHERE deviceId = :deviceId")
    fun getAllForDevice(deviceId: DeviceId): Flow<List<Port>>
}

@Dao
interface ScanDao {
    @Insert
    suspend fun insertRaw(scan: Scan): Long

    @Transaction
    suspend fun insert(scan: Scan): ScanId = ScanId(insertRaw(scan))

    @Query("Select * FROM SCAN")
    fun getAll(): Flow<List<Scan>>

    @Query("Select * FROM SCAN")
    fun getAllNow(): List<Scan>

    @Query("SELECT * FROM SCAN WHERE scanId = :scanId")
    fun getById(scanId: ScanId): Flow<Scan?>
}

@Dao
interface MacVendorsDao {
    @Query("SELECT * FROM macvendor WHERE mac = :mac")
    fun getFromMac(mac: MacAddress): MacVendor
}
