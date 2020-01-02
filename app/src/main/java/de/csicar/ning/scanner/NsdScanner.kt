package de.csicar.ning.scanner

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import de.csicar.ning.Device
import de.csicar.ning.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class NsdScanner(application: Application, private val onUpdate : (ScanResult) -> Unit) {
    val nsdManager =
        application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceTypes = listOf(
        "_workstation._tcp",
        "_companion-link._tcp",
        "_ssh._tcp",
        "_adisk._tcp",
        "_afpovertcp._tcp",
        "_device-info._tcp",
        "_googlecast._tcp",
        "_printer._tcp"
    )

    suspend fun scan() = withContext(Dispatchers.IO) {
        serviceTypes.map { serviceType ->
            async {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, NsdListener())
            }
        }
    }

    inner class NsdResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) return
            Log.e("asd", "failed $serviceInfo $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            if (serviceInfo == null) return
            val host = serviceInfo.host
            if (host !is Inet4Address) return
            onUpdate(ScanResult(host, serviceInfo.serviceName))

        }
    }

    inner class NsdListener : NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            nsdManager.resolveService(serviceInfo, NsdResolveListener())
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.d("asd", "discovery stop failed $serviceType $errorCode")
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.d("asd", "discovery start failed $serviceType $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
        }

        override fun onDiscoveryStopped(serviceType: String?) {
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        }

    }

    data class ScanResult(val ipAddress: Inet4Address, val name: String)
}