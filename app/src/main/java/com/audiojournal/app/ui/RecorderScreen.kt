package com.audiojournal.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.audiojournal.app.recording.RecorderPhase

@Composable
fun RecorderScreen(viewModel: RecorderViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsedMillis by viewModel.elapsedMillis.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (viewModel.isCloudConfigured) {
                            stringResource(R.string.upload_queued)
                        } else {
                            stringResource(R.string.upload_not_configured)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
