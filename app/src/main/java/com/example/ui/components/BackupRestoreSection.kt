package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings section component for local SQLite / Room database and audio files Backup and Restore
 * using Storage Access Foundation (SAF) launchers with ZIP archives.
 */
@Composable
fun BackupRestoreSection(
    modifier: Modifier = Modifier,
    dbName: String = BackupManager.DEFAULT_DB_NAME,
    onBackupSuccess: ((Uri) -> Unit)? = null,
    onRestoreSuccess: ((Uri) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var dbSizeBytes by remember { mutableLongStateOf(BackupManager.getDatabaseSizeBytes(context, dbName)) }

    // SAF CreateDocument launcher for Backup (Export as .zip)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Exporting backup archive..."
                isError = false
                val result = BackupManager.exportDatabase(context, uri, dbName)
                isProcessing = false
                result.onSuccess { res ->
                    val formattedSize = BackupManager.formatBytes(res.bytesCopied)
                    statusMessage = "Backup saved successfully ($formattedSize)"
                    isError = false
                    dbSizeBytes = BackupManager.getDatabaseSizeBytes(context, dbName)
                    onBackupSuccess?.invoke(uri)
                }.onFailure { err ->
                    statusMessage = "Backup failed: ${err.localizedMessage ?: "Unknown error"}"
                    isError = true
                    onError?.invoke(err)
                }
            }
        }
    }

    // SAF OpenDocument launcher for Restore (Import .zip)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Restoring backup archive..."
                isError = false
                val result = BackupManager.importDatabase(context, uri, dbName)
                isProcessing = false
                result.onSuccess { res ->
                    val formattedSize = BackupManager.formatBytes(res.bytesCopied)
                    statusMessage = "Backup restored successfully ($formattedSize)"
                    isError = false
                    dbSizeBytes = BackupManager.getDatabaseSizeBytes(context, dbName)
                    onRestoreSuccess?.invoke(uri)
                }.onFailure { err ->
                    statusMessage = "Restore failed: ${err.localizedMessage ?: "Unknown error"}"
                    isError = true
                    onError?.invoke(err)
                }
            }
        }
    }

    fun triggerBackup() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val defaultFileName = "${dbName}_backup_$timestamp.zip"
        exportLauncher.launch(defaultFileName)
    }

    fun triggerRestore() {
        val mimeTypes = arrayOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream",
            "*/*"
        )
        importLauncher.launch(mimeTypes)
    }

    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Database Backup & Restore",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (dbSizeBytes > 0) {
                            "Local database size: ${BackupManager.formatBytes(dbSizeBytes)}"
                        } else {
                            "Export or restore your full backup archive (.zip)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("backup_restore_summary"),
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { triggerBackup() },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("backup_export_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { showRestoreConfirmDialog = true },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("backup_restore_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Processing / Status indicator
            AnimatedVisibility(visible = isProcessing || statusMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = if (isError) Icons.Rounded.Info else Icons.Rounded.Restore,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before restoring database
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = {
                Text(
                    text = "Restore Backup?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "Restoring a backup archive will overwrite your current local database and offline tracks. Ensure you select a valid backup `.zip` file.\n\nAre you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        triggerRestore()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                    modifier = Modifier.testTag("confirm_restore_database_button"),
                ) {
                    Text("Proceed & Select File")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_restore_database_button"),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}