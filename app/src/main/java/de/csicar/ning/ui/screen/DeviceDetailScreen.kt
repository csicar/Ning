package de.csicar.ning.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.csicar.ning.DeviceId
import de.csicar.ning.DeviceWithName
import de.csicar.ning.Port
import de.csicar.ning.PortDescription
import de.csicar.ning.R
import de.csicar.ning.ScanViewModel
import de.csicar.ning.util.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceDetailScreen(
    viewModel: ScanViewModel,
    deviceId: DeviceId
) {
    val device by viewModel.getDevice(deviceId).collectAsState(initial = null)
    val ports by viewModel.getPortsForDevice(deviceId).collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Trigger port scan when device is loaded
    LaunchedEffect(device) {
        device?.let {
            viewModel.scanPorts(it.asDevice)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            device?.let { dev ->
                DeviceInfoCard(dev, context)
            }
        }

        item {
            Text(
                text = stringResource(R.string.title_open_ports),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(ports, key = { it.portId.value }) { port ->
            PortItem(
                port = port,
                viewModel = viewModel,
                context = context
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceInfoCard(device: DeviceWithName, context: Context) {
    val hideMac = remember { AppPreferences(context).hideMacDetails }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.title_card_device_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                label = stringResource(R.string.field_name_device_name),
                value = if (device.isScanningDevice) {
                    stringResource(R.string.this_device)
                } else {
                    device.deviceName ?: ""
                },
                context = context
            )
            InfoRow(
                label = stringResource(R.string.field_name_device_type),
                value = stringResource(device.deviceType.label),
                context = context
            )
            InfoRow(
                label = stringResource(R.string.field_name_device_ip),
                value = device.ip.hostAddress ?: "",
                context = context
            )
            InfoRow(
                label = stringResource(R.string.field_name_device_mac),
                value = device.hwAddress?.getAddress(hideMac) ?: "",
                context = context
            )
            InfoRow(
                label = stringResource(R.string.field_name_vendor_name),
                value = device.vendorName ?: "",
                context = context
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoRow(label: String, value: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { copyToClipboard(context, value) }
            )
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortItem(
    port: Port,
    viewModel: ScanViewModel,
    context: Context
) {
    val portDescription = PortDescription.commonPorts.find { it.port == port.port }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    scope.launch {
                        val ip = withContext(Dispatchers.IO) {
                            viewModel.deviceDao.getByIdNow(port.deviceId).ip
                        }
                        if (portDescription?.urlSchema != null) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("${portDescription.urlSchema}://${ip}:${port.port}")
                            context.startActivity(intent)
                        } else {
                            copyToClipboard(context, "${ip.hostAddress}:${port.port}")
                        }
                    }
                },
                onLongClick = {
                    scope.launch {
                        val ip = withContext(Dispatchers.IO) {
                            viewModel.deviceDao.getByIdNow(port.deviceId).ip
                        }
                        copyToClipboard(context, "${ip.hostAddress}:${port.port}")
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = port.port.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = portDescription?.serviceName ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = port.protocol.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
