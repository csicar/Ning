package de.csicar.ning

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

}

@Dao
interface DeviceDao {
    @Query("Select * FROM DeviceWithName WHERE networkId = :networkId ORDER BY ip ASC")
    fun getAll(networkId: Long): LiveData<List<DeviceWithName>>


    @Query("SELECT * FROM Device WHERE networkId = :networkId")
    fun getAllNow(networkId: Long): List<Device>

    @Insert
    fun insertAll(vararg devices: Device)

    @Insert
    fun insert(device: Device): Long

    @Update
    fun update(device: Device)

    @Query("SELECT * FROM DEVICE WHERE deviceId = :id")
    fun getById(id: Long): LiveData<Device>

    @Query("SELECT * FROM DEVICE WHERE deviceId = :id")
    fun getByIdNow(id: Long): Device

    @Query("SELECT * FROM Device WHERE ip = :ip AND networkId IN (SELECT networkId FROM Network WHERE scanId = :scanId)")
    fun getByAddress(ip: Inet4Address, scanId: Long): Device?

    @Query("UPDATE Device SET hwAddress = :hwAddress WHERE deviceId = :deviceId")
    fun updateHwAddress(deviceId: Long, hwAddress: MacAddress)

    @Query("UPDATE Device SET deviceName = :deviceName WHERE deviceId = :deviceId")
    fun updateServiceName(deviceId: Long, deviceName: String?)
}

@Dao
interface PortDao {
    @Insert
    fun insert(port: Port): Long

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
