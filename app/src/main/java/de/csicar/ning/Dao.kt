package de.csicar.ning

import androidx.lifecycle.LiveData
import androidx.room.*
import de.csicar.ning.scanner.MacAddress
import java.net.Inet4Address

@Dao
interface NetworkDao {
    @Query("Select * FROM network WHERE scanId = :scanId")
    fun getAll(scanId: Long): LiveData<List<Network>>


    @Query("Select * FROM network WHERE scanId = :scanId")
    fun getAllNow(scanId: Long): List<Network>

    @Insert
    fun insert(network: Network): Long

    @Insert
    fun insertAll(vararg networks: Network)

    @Query("SELECT * FROM network WHERE networkId = :networkId")
    fun getByIdNow(networkId: Long): Network


    @Query("SELECT * FROM network WHERE networkId = :networkId")
    fun getById(networkId: Long): LiveData<Network>

}

@Dao
interface DeviceDao {
    @Query("Select * FROM DeviceWithName WHERE networkId = :networkId ORDER BY ip ASC")
    fun getAll(networkId: Long): LiveData<List<DeviceWithName>>


    @Query("SELECT * FROM Device WHERE networkId = :networkId")
    fun getAllNow(networkId: Long): List<Device>

    @Insert
    fun insert(device: Device): Long

    @Transaction
    fun upsertName(networkId: Long, ip: Inet4Address, name: String) {
        val existingDevice = getByAddressInNetwork(ip, networkId)
        if(existingDevice == null) {
            insert(Device(0, networkId, ip, name, null))
        } else {
            updateServiceName(existingDevice.deviceId, name)
        }
    }

    @Transaction
    fun upsertHwAddress(scanId: Long, ip: Inet4Address, hwAddress: MacAddress) {
        val existingDevice = getByAddress(ip, scanId)
        if(existingDevice == null) {
            //TODO("insert new device. need to find out IP-Range of network")
        } else {
            updateHwAddress(existingDevice.deviceId, hwAddress)
        }
    }

    @Update
    fun update(device: Device)

    @Query("SELECT * FROM DeviceWithName WHERE deviceId = :id")
    fun getById(id: Long): LiveData<DeviceWithName>

    @Query("SELECT * FROM DEVICE WHERE deviceId = :id")
    fun getByIdNow(id: Long): Device

    @Query("SELECT * FROM Device WHERE ip = :ip AND networkId = :networkId")
    fun getByAddressInNetwork(ip: Inet4Address, networkId: Long) : Device?

    @Query("SELECT * FROM Device WHERE ip = :ip AND networkId IN (SELECT networkId FROM Network WHERE scanId = :scanId)")
    fun getByAddress(ip: Inet4Address, scanId: Long): Device?

    @Query("UPDATE Device SET hwAddress = :hwAddress WHERE deviceId = :deviceId")
    fun updateHwAddress(deviceId: Long, hwAddress: MacAddress)

    @Query("UPDATE Device SET deviceName = :deviceName WHERE deviceId = :deviceId")
    fun updateServiceName(deviceId: Long, deviceName: String?)

    @Transaction
    fun insertIfNew(networkId: Long, ip: Inet4Address): Long {
        val existingAddress = getByAddressInNetwork(ip, networkId)
            ?: return insert(Device(0, networkId, ip, null, null))
        return existingAddress.deviceId
    }
}

@Dao
interface PortDao {
    @Insert
    fun insert(port: Port): Long

    @Transaction
    suspend fun upsert(port: Port) : Long{
        val portFromDB = getPortFromNumber(port.deviceId, port.port) ?: return insert(port)

        update(Port(portFromDB.portId, port.port, port.protocol, port.deviceId))
        return portFromDB.portId
    }

    @Update
    fun update(port: Port)

    @Query("SELECT * FROM Port WHERE deviceId = :deviceId AND port = :port")
    fun getPortFromNumber(deviceId: Long, port: Int): Port?


    @Query("SELECT * FROM Port WHERE deviceId = :deviceId")
    fun getAllForDevice(deviceId: Long): LiveData<List<Port>>
}

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(scan: Scan): Long

    @Query("Select * FROM SCAN")
    fun getAll(): LiveData<List<Scan>>

    @Query("Select * FROM SCAN")
    fun getAllNow(): List<Scan>

    @Query("SELECT * FROM SCAN WHERE scanId = :scanId")
    fun getById(scanId: Long) : LiveData<Scan?>

}

@Dao
interface MacVendorsDao {
    @Query("SELECT * FROM macvendor WHERE mac = :mac")
    fun getFromMac(mac: MacAddress): MacVendor
}
