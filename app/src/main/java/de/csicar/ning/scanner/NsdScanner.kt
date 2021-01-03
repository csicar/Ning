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
    companion object {
        val TAG = NsdScanner::class.java.name
    }
    val nsdManager =
        application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    /** mDNS Service Types, that the application checks for.
     * Unfortunately, android does not offer an API for discovering all services
     * See: http://www.dns-sd.org/servicetypes.html
     */
    private val serviceTypes = setOf(
        "_workstation._tcp",
        "_companion-link._tcp",
        "_ssh._tcp",
        "_adisk._tcp",
        "_afpovertcp._tcp",
        "_device-info._tcp",
        "_googlecast._tcp",
        "_printer._tcp",
        "_ipp._tcp",
        "_http._tcp",
        "_smb._tcp",
        "_nfs._tcp",
        "_ftp._tcp",
        "_coap._udp"
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
            Log.e(TAG, "failed $serviceInfo $errorCode")
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
            Log.e(TAG, "discovery stop failed $serviceType $errorCode")
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "discovery start failed $serviceType $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        }

    }

    data class ScanResult(val ipAddress: Inet4Address, val name: String)
}