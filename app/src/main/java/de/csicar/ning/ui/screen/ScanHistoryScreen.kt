package de.csicar.ning.ui.screen

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.csicar.ning.Network
import de.csicar.ning.R
import de.csicar.ning.Scan
import de.csicar.ning.ScanViewModel

@Composable
fun ScanHistoryScreen(
    viewModel: ScanViewModel,
    onScanClick: (Scan) -> Unit
) {
    val scans by viewModel.getAllScans().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        if (scans.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_network_check_black_48dp),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No scan history",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(scans.sortedByDescending { it.startedAt }, key = { it.scanId }) { scan ->
                    ScanHistoryItemWithNetwork(
                        scan = scan,
                        viewModel = viewModel,
                        onClick = { onScanClick(scan) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryItemWithNetwork(
    scan: Scan,
    viewModel: ScanViewModel,
    onClick: () -> Unit
) {
    val networks by viewModel.getNetworksForScan(scan.scanId).collectAsState(initial = emptyList())
    val firstNetwork = networks.firstOrNull()

    ScanHistoryItem(
        scan = scan,
        network = firstNetwork,
        onClick = onClick
    )
}

@Composable
private fun ScanHistoryItem(
    scan: Scan,
    network: Network?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_network_check_black_48dp),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Show interface and SSID if available
                if (network != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = network.interfaceName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        network.ssid?.let { ssid ->
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ssid,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Scan #${scan.scanId}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(scan.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
