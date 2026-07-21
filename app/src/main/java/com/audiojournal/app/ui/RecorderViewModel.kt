package com.audiojournal.app.ui

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiojournal.app.AudioJournalApp
import com.audiojournal.app.UploadBackend
import com.audiojournal.app.recording.RecorderPhase
import com.audiojournal.app.recording.RecorderState
import com.audiojournal.app.recording.RecordingService
import com.audiojournal.app.upload.UploadFolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioJournalApp).container
    private val engine = container.recordingEngine

    val state: StateFlow<RecorderState> = engine.state

    val uploadBackend: UploadBackend = container.uploadBackend
    val backendLabel: String = container.cloudUploader.backendLabel
    val isCloudConfigured: Boolean = container.cloudUploader.isConfigured

    /** Folder choices for recordings; the first entry is the default. */
    val folders: List<UploadFolder> = container.uploadFolders

    private val _selectedFolder = MutableStateFlow(folders.firstOrNull())
    val selectedFolder: StateFlow<UploadFolder?> = _selectedFolder.asStateFlow()

    /** null while the silent check is still running. */
    private val _driveConnected = MutableStateFlow<Boolean?>(null)
    val driveConnected: StateFlow<Boolean?> = _driveConnected.asStateFlow()

    init {
        if (uploadBackend == UploadBackend.DRIVE) refreshDriveConnection()
    }

    /** Ticks while recording so the timer in the UI stays current. */
    val elapsedMillis: StateFlow<Long> = engine.state
        .flatMapLatest { current ->
            when (current.phase) {
                RecorderPhase.RECORDING -> flow {
                    while (true) {
                        emit(engine.elapsedMillis())
                        delay(TIMER_TICK_MILLIS)
                    }
                }

                else -> flow { emit(engine.elapsedMillis()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun startRecording() = sendAction(RecordingService.ACTION_START)

    fun pauseRecording() = sendAction(RecordingService.ACTION_PAUSE)

    fun resumeRecording() = sendAction(RecordingService.ACTION_RESUME)

    fun stopRecording() = sendAction(RecordingService.ACTION_STOP, _selectedFolder.value)

    fun discardRecording() = sendAction(RecordingService.ACTION_DISCARD)

    fun selectFolder(folder: UploadFolder) {
        _selectedFolder.value = folder
    }

    /** Consent UI intent, or null when Drive access is already granted. */
    suspend fun driveConsentIntent(): PendingIntent? = container.driveAuthManager.consentIntent()

    fun refreshDriveConnection() {
        viewModelScope.launch {
            _driveConnected.value = container.driveAuthManager.getAccessToken() != null
        }
    }

    fun clearError() = engine.clearError()

    private fun sendAction(action: String, folder: UploadFolder? = null) {
        RecordingService.sendAction(getApplication(), action, folder)
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 100L
    }
}
