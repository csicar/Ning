package de.csicar.ning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScanService : Service() {

    companion object {
        const val EXTRA_INTERFACE_NAME = "interface_name"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "scan_channel"

        val scanProgress = MutableStateFlow<ScanRepository.ScanProgress>(ScanRepository.ScanProgress.ScanNotStarted)
        val currentNetworkId = MutableStateFlow<NetworkId?>(null)
        val currentScanId = MutableStateFlow<ScanId?>(null)

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scanJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val interfaceName = intent?.getStringExtra(EXTRA_INTERFACE_NAME)
        if (interfaceName == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.scan_notification_title))
            .setContentText(getString(R.string.scan_notification_progress, 0))
            .setSmallIcon(R.drawable.ic_baseline_router_48)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        _isRunning.value = true

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                runScan(interfaceName)
            } finally {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun runScan(interfaceName: String) {
        val db = AppDatabase.createInstance(application)
        val repository = ScanRepository(
            db.networkDao(),
            db.scanDao(),
            db.deviceDao(),
            db.portDao(),
            application
        )

        scanProgress.value = ScanRepository.ScanProgress.ScanNotStarted

        val progressCollector = serviceScope.launch {
            scanProgress.collect { progress ->
                updateNotification(progress)
            }
        }

        try {
            val network = repository.startScan(interfaceName, scanProgress, currentNetworkId)
            if (network != null) {
                currentScanId.value = network.scanId
            }
        } finally {
            progressCollector.cancel()
        }
    }

    private fun updateNotification(progress: ScanRepository.ScanProgress) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        when (progress) {
            is ScanRepository.ScanProgress.ScanRunning -> {
                val percent = (progress.progress * 100).toInt().coerceIn(0, 100)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.scan_notification_title))
                    .setContentText(getString(R.string.scan_notification_progress, percent))
                    .setSmallIcon(R.drawable.ic_baseline_router_48)
                    .setOngoing(true)
                    .setProgress(100, percent, false)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            is ScanRepository.ScanProgress.ScanFinished -> {
                notificationManager.cancel(NOTIFICATION_ID)
            }
            is ScanRepository.ScanProgress.ScanNotStarted -> {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.scan_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scanJob?.cancel()
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }
}
