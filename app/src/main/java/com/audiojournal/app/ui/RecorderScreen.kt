package com.audiojournal.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiojournal.app.R
import com.audiojournal.app.UploadBackend
import com.audiojournal.app.recording.RecorderPhase
import com.audiojournal.app.upload.UploadFolder
import com.audiojournal.app.upload.UploadStage
import com.audiojournal.app.upload.UploadStatus
import kotlinx.coroutines.launch

@Composable
fun RecorderScreen(viewModel: RecorderViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsedMillis by viewModel.elapsedMillis.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val driveConnected by viewModel.driveConnected.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val driveConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        viewModel.refreshDriveConnection()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startRecording()
        }
    }

    fun startWithPermissionCheck() {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.startRecording()
        } else {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf(Manifest.permission.RECORD_AUDIO)
            }
            permissionLauncher.launch(permissions)
        }
    }

    state.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = when (state.phase) {
                    RecorderPhase.IDLE -> stringResource(R.string.status_idle)
                    RecorderPhase.RECORDING -> stringResource(R.string.status_recording)
                    RecorderPhase.PAUSED -> stringResource(R.string.status_paused)
                },
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = formatElapsed(if (state.phase == RecorderPhase.IDLE) 0L else elapsedMillis),
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(Modifier.height(48.dp))

            when (state.phase) {
                RecorderPhase.IDLE -> RecordButton(onClick = ::startWithPermissionCheck)

                RecorderPhase.RECORDING, RecorderPhase.PAUSED -> {
                    var showDiscardDialog by remember { mutableStateOf(false) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PauseResumeButton(
                            isPaused = state.phase == RecorderPhase.PAUSED,
                            onPause = viewModel::pauseRecording,
                            onResume = viewModel::resumeRecording,
                        )
                        StopButton(onClick = viewModel::stopRecording)
                        DiscardButton(onClick = { showDiscardDialog = true })
                    }
                    if (showDiscardDialog) {
                        AlertDialog(
                            onDismissRequest = { showDiscardDialog = false },
                            title = { Text(stringResource(R.string.discard_dialog_title)) },
                            text = { Text(stringResource(R.string.discard_dialog_message)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDiscardDialog = false
                                        viewModel.discardRecording()
                                    },
                                    modifier = Modifier.testTag("discard_confirm_button"),
                                ) {
                                    Text(stringResource(R.string.discard))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDiscardDialog = false }) {
                                    Text(stringResource(R.string.keep_recording))
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (viewModel.folders.size > 1 && selectedFolder != null) {
                FolderSelector(
                    folders = viewModel.folders,
                    selected = selectedFolder!!,
                    onSelect = viewModel::selectFolder,
                )
                Spacer(Modifier.height(16.dp))
            }

            if (viewModel.uploadBackend == UploadBackend.DRIVE && driveConnected == false) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val consent = viewModel.driveConsentIntent()
                            if (consent != null) {
                                driveConsentLauncher.launch(
                                    IntentSenderRequest.Builder(consent.intentSender).build(),
                                )
                            } else {
                                viewModel.refreshDriveConnection()
                            }
                        }
                    },
                    modifier = Modifier.testTag("drive_connect_button"),
                ) {
                    Text(stringResource(R.string.drive_connect))
                }
                Spacer(Modifier.height(16.dp))
            }

            state.lastSaved?.let { saved ->
                if (state.phase == RecorderPhase.IDLE) {
                    Text(
                        text = stringResource(
                            R.string.last_saved,
                            saved.file.name,
                            formatElapsed(saved.durationMillis),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    UploadStatusRow(
                        status = uploadStatus,
                        backendLabel = viewModel.backendLabel,
                        folderLabel = selectedFolder?.label
                            ?: stringResource(R.string.drive_root),
                        driveNotConnected = viewModel.uploadBackend == UploadBackend.DRIVE &&
                            driveConnected == false,
                        isCloudConfigured = viewModel.isCloudConfigured,
                    )
                }
            }
        }
    }
}

/**
 * Upload status of the last saved recording. Missing credentials and missing
 * Drive consent are known up front; everything else comes from [status], which
 * tracks the background upload job live.
 */
@Composable
private fun UploadStatusRow(
    status: UploadStatus?,
    backendLabel: String,
    folderLabel: String,
    driveNotConnected: Boolean,
    isCloudConfigured: Boolean,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val current = status ?: UploadStatus(UploadStage.QUEUED)

    // The job's state wins over the "not configured" hints once it has run, so
    // a finished upload is never described as merely queued.
    val stage = when {
        current.stage != UploadStage.QUEUED -> current.stage
        driveNotConnected || !isCloudConfigured -> UploadStage.KEPT_LOCAL
        else -> UploadStage.QUEUED
    }

    val reason = current.errorMessage
    val (icon, text, color) = when (stage) {
        UploadStage.QUEUED -> Triple(
            Icons.Filled.CloudQueue,
            stringResource(R.string.upload_queued_to, backendLabel, folderLabel),
            muted,
        )

        UploadStage.UPLOADING -> Triple(
            Icons.Filled.CloudUpload,
            stringResource(R.string.upload_uploading, backendLabel, folderLabel),
            muted,
        )

        UploadStage.RETRYING -> Triple(
            Icons.Filled.Sync,
            if (reason != null) {
                stringResource(
                    R.string.upload_retrying_reason,
                    current.attempt,
                    current.maxAttempts,
                    reason,
                )
            } else {
                stringResource(R.string.upload_retrying, current.attempt, current.maxAttempts)
            },
            muted,
        )

        UploadStage.UPLOADED -> Triple(
            Icons.Filled.CloudDone,
            stringResource(R.string.upload_finished, backendLabel, folderLabel),
            MaterialTheme.colorScheme.primary,
        )

        UploadStage.KEPT_LOCAL -> Triple(
            Icons.Filled.CloudOff,
            if (driveNotConnected) {
                stringResource(R.string.upload_drive_not_connected)
            } else {
                stringResource(R.string.upload_not_configured)
            },
            muted,
        )

        UploadStage.FAILED -> Triple(
            Icons.Filled.ErrorOutline,
            if (reason != null) {
                stringResource(R.string.upload_failed_reason, current.attempt, reason)
            } else {
                stringResource(R.string.upload_failed, current.attempt)
            },
            MaterialTheme.colorScheme.error,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (stage == UploadStage.UPLOADING) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = color,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.testTag("upload_status"),
        )
    }
}

@Composable
private fun FolderSelector(
    folders: List<UploadFolder>,
    selected: UploadFolder,
    onSelect: (UploadFolder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("folder_selector"),
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(selected.label)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.folder_label),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.label) },
                    leadingIcon = if (folder == selected) {
                        { Icon(Icons.Filled.CloudDone, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onSelect(folder)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(160.dp)
            .testTag("record_button"),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.record),
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun PauseResumeButton(isPaused: Boolean, onPause: () -> Unit, onResume: () -> Unit) {
    FilledIconButton(
        onClick = if (isPaused) onResume else onPause,
        modifier = Modifier
            .size(88.dp)
            .testTag("pause_resume_button"),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = stringResource(if (isPaused) R.string.resume else R.string.pause),
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun DiscardButton(onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(88.dp)
            .testTag("discard_button"),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.discard),
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(120.dp)
                .testTag("stop_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = stringResource(R.string.stop),
                modifier = Modifier.size(56.dp),
            )
        }
    }
}
