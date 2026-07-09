package com.audiojournal.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiojournal.app.AudioJournalApp
import com.audiojournal.app.recording.RecorderPhase
import com.audiojournal.app.recording.RecorderState
import com.audiojournal.app.recording.RecordingService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioJournalApp).container
    private val engine = container.recordingEngine

    val state: StateFlow<RecorderState> = engine.state

    val isCloudConfigured: Boolean = container.cloudUploader.isConfigured

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

    fun stopRecording() = sendAction(RecordingService.ACTION_STOP)

    fun clearError() = engine.clearError()

    private fun sendAction(action: String) {
        RecordingService.sendAction(getApplication(), action)
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 100L
    }
}
