package de.csicar.ning

import android.app.Application
import androidx.room.*
import de.csicar.ning.scanner.MacAddress
import de.csicar.ning.util.inet4AddressFromInt
import java.net.Inet4Address
import java.util.*

@Database(
    entities = [Network::class, Device::class, Port::class, MacVendor::class, Scan::class],
    views = [DeviceWithName::class],
    version = 22,
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao

    abstract fun deviceDao(): DeviceDao

    abstract fun portDao(): PortDao

    abstract fun scanDao(): ScanDao

    companion object {
        fun createInstance(application: Application): AppDatabase {
            return Room
                .databaseBuilder(
                    application.applicationContext,
                    AppDatabase::class.java, "ning-db"
                )
                .createFromAsset("mac_devices.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

class Converter {
    @TypeConverter
    fun toInet4Address(value: Int?): Inet4Address? =
        if (value != null) {
            inet4AddressFromInt(
                "",
                value,
            )
        } else {
            null
        }

    @TypeConverter
    fun fromInet4Address(value: Inet4Address?): Int? = value?.hashCode()

    @TypeConverter
    fun toProtocol(value: String?): Protocol? {
        if (value == null) return null
        return Protocol.valueOf(value)
    }

    @TypeConverter
    fun fromProtocol(value: Protocol?): String? {
        if (value == null) return null
        return value.name
    }

    @TypeConverter
    fun toMacAddress(value: String?): MacAddress? = if (value != null) MacAddress(value.uppercase(Locale.getDefault())) else null

    @TypeConverter
    fun fromMacAddress(value: MacAddress?): String? = value?.address?.uppercase(Locale.getDefault())

    @TypeConverter fun fromScanId(id: ScanId) = id.value

    @TypeConverter fun toScanId(value: Long) = ScanId(value)

    @TypeConverter fun fromNetworkId(id: NetworkId) = id.value

    @TypeConverter fun toNetworkId(value: Long) = NetworkId(value)

    @TypeConverter fun fromDeviceId(id: DeviceId) = id.value

    @TypeConverter fun toDeviceId(value: Long) = DeviceId(value)

    @TypeConverter fun fromPortId(id: PortId) = id.value

    @TypeConverter fun toPortId(value: Long) = PortId(value)
}
