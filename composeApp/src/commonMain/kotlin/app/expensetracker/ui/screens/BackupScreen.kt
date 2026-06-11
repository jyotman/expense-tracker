@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.util.DateFormat
import app.expensetracker.viewmodel.BackupViewModel
import kotlin.time.Instant

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm: BackupViewModel = viewModel { BackupViewModel() }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Automatic backup card — shown first so users know their data is already safe
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            "Automatically backed up",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        "Your expenses are automatically backed up to ${state.autoBackupProviderName}. " +
                            "A fresh install or new device restores everything — no action needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (state.canOpenAutoBackupSettings) {
                        TextButton(
                            onClick = vm::openAutoBackupSettings,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "View ${state.autoBackupProviderName} backup settings",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Manual export / import — framed as an opt-in extra, not a requirement
            Text(
                "Manual export & import",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Export a copy of your data for safekeeping or to move it to another device. " +
                    "Pick any destination in the system picker — Files, Drive, Dropbox, wherever.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.supported) {
                Text(
                    "Manual backup isn't available on this platform yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (state.lastBackupMillis > 0) {
                    Text(
                        "Last export: ${DateFormat.full(Instant.fromEpochMilliseconds(state.lastBackupMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = vm::exportNow,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export backup")
                }
                OutlinedButton(
                    onClick = vm::importNow,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import / restore")
                }

                if (state.busy) CircularProgressIndicator()
                state.message?.let {
                    Text(
                        it,
                        color = if (state.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
