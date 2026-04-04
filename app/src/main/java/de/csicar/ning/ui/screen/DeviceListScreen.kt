package de.csicar.ning.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import de.csicar.ning.DeviceWithName
import de.csicar.ning.R
import de.csicar.ning.ScanRepository
import de.csicar.ning.ScanViewModel
import de.csicar.ning.util.AppPreferences

@Composable
fun DeviceListScreen(
    viewModel: ScanViewModel,
    interfaceName: String,
    onDeviceClick: (DeviceWithName) -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var permissionsGranted by remember { mutableStateOf(false) }
    val isRefreshing = scanProgress is ScanRepository.ScanProgress.ScanRunning

    // Request location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Trigger initial scan
    LaunchedEffect(interfaceName) {
        val network = viewModel.startScan(interfaceName)
        if (network == null) {
            Toast.makeText(context, context.getString(R.string.error_network_not_found), Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress bar
        when (val progress = scanProgress) {
            is ScanRepository.ScanProgress.ScanRunning -> {
                LinearProgressIndicator(
                    progress = { progress.progress.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {}
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    viewModel.startScan(interfaceName)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (devices.isEmpty() && scanProgress is ScanRepository.ScanProgress.ScanNotStarted) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_downward_white_64dp),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.swipe_down_to_scan_the_network),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices, key = { it.deviceId.value }) { device ->
                        DeviceItem(
                            device = device,
                            onClick = { onDeviceClick(device) },
                            onLongClick = { copyToClipboard(context, device.ip.hostAddress ?: "") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceItem(
    device: DeviceWithName,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val hideMac = remember { AppPreferences(context).hideMacDetails }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(device.deviceType.icon),
                contentDescription = stringResource(R.string.device_icon),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = device.ip.hostAddress ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    device.hwAddress?.getAddress(hideMac)?.let { mac ->
                        Text(
                            text = mac,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    Text(
                        text = if (device.isScanningDevice) {
                            stringResource(R.string.this_device)
                        } else {
                            device.deviceName ?: ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    device.vendorName?.let { vendor ->
                        Text(
                            text = vendor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(text, text))
    Toast.makeText(
        context,
        context.getString(R.string.snackbar_copy_message, text),
        Toast.LENGTH_SHORT
    ).show()
}
