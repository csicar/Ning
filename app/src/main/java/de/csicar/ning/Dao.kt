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

    @Insert
    fun insert(network: Network): Long

    @Insert
    fun insertAll(vararg networks: Network)

}

@Dao
interface DeviceDao {
    @Query("Select * FROM DeviceWithName")
    fun getAll(): LiveData<List<DeviceWithName>>


    @Query("SELECT * FROM Device")
    fun getAllNow(): List<Device>

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

    @Query("SELECT * FROM SCAN WHERE scanId = :scanId")
    fun getById(scanId: Long) : LiveData<Scan?>

}

@Dao
interface MacVendorsDao {
    @Query("SELECT * FROM macvendor WHERE mac = :mac")
    fun getFromMac(mac: MacAddress): MacVendor

}