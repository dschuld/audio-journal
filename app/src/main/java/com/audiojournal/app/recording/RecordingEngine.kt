package com.audiojournal.app.recording

import com.audiojournal.app.storage.RecordingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Monotonic time source, injectable for tests.
 * Production uses [android.os.SystemClock.elapsedRealtime].
 */
fun interface TimeSource {
    fun elapsedRealtimeMillis(): Long
}

/**
 * The recording state machine: idle -> recording <-> paused -> idle.
 *
 * Pure Kotlin (no Android framework types) so it is fully unit testable.
 * Invalid transitions (e.g. pause while idle) are ignored rather than throwing,
 * because UI events and service intents can race.
 *
 * A single instance is shared between the UI (which observes [state]) and the
 * foreground [RecordingService] (which drives the transitions).
 */
class RecordingEngine(
    private val recorderFactory: () -> AudioRecorder,
    private val store: RecordingStore,
    private val timeSource: TimeSource,
) {

    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private var recorder: AudioRecorder? = null
    private var recordedBeforePauseMillis = 0L
    private var recordingSinceMillis: Long? = null

    val isActive: Boolean
        get() = _state.value.phase != RecorderPhase.IDLE

    /** Wall time recorded so far, excluding paused stretches. */
    fun elapsedMillis(): Long {
        val running = recordingSinceMillis?.let { timeSource.elapsedRealtimeMillis() - it } ?: 0L
        return recordedBeforePauseMillis + running
    }

    @Synchronized
    fun start(): Boolean {
        if (_state.value.phase != RecorderPhase.IDLE) return false
        val output = store.newRecordingFile()
        return try {
            val newRecorder = recorderFactory()
            newRecorder.start(output)
            recorder = newRecorder
            recordedBeforePauseMillis = 0L
            recordingSinceMillis = timeSource.elapsedRealtimeMillis()
            _state.update {
                RecorderState(
                    phase = RecorderPhase.RECORDING,
                    activeFile = output,
                    lastSaved = it.lastSaved,
                )
            }
            true
        } catch (e: Exception) {
            output.delete()
            recorder = null
            _state.update {
                it.copy(
                    phase = RecorderPhase.IDLE,
                    activeFile = null,
                    errorMessage = "Could not start recording: ${e.message}",
                )
            }
            false
        }
    }

    @Synchronized
    fun pause() {
        if (_state.value.phase != RecorderPhase.RECORDING) return
        recorder?.pause()
        recordedBeforePauseMillis = elapsedMillis()
        recordingSinceMillis = null
        _state.update { it.copy(phase = RecorderPhase.PAUSED) }
    }

    @Synchronized
    fun resume() {
        if (_state.value.phase != RecorderPhase.PAUSED) return
        recorder?.resume()
        recordingSinceMillis = timeSource.elapsedRealtimeMillis()
        _state.update { it.copy(phase = RecorderPhase.RECORDING) }
    }

    /**
     * Stops and finalizes the current recording.
     * Returns the saved recording, or null if there was nothing to stop or
     * finalizing failed (in which case the broken file is deleted).
     */
    @Synchronized
    fun stop(): SavedRecording? {
        val current = _state.value
        if (current.phase == RecorderPhase.IDLE) return null
        val duration = elapsedMillis()
        val file = current.activeFile
        val activeRecorder = recorder
        recorder = null
        recordedBeforePauseMillis = 0L
        recordingSinceMillis = null
        return try {
            activeRecorder?.stop()
            val saved = file?.let { SavedRecording(it, duration) }
            _state.update {
                RecorderState(phase = RecorderPhase.IDLE, lastSaved = saved ?: it.lastSaved)
            }
            saved
        } catch (e: Exception) {
            file?.delete()
            _state.update {
                RecorderState(
                    phase = RecorderPhase.IDLE,
                    lastSaved = it.lastSaved,
                    errorMessage = "Recording could not be saved: ${e.message}",
                )
            }
            null
        }
    }

    /**
     * Stops the current recording and deletes its file without saving.
     * No-op when idle. Finalize failures are ignored because the file is
     * being thrown away anyway.
     */
    @Synchronized
    fun discard() {
        val current = _state.value
        if (current.phase == RecorderPhase.IDLE) return
        val activeRecorder = recorder
        recorder = null
        recordedBeforePauseMillis = 0L
        recordingSinceMillis = null
        try {
            activeRecorder?.stop()
        } catch (_: Exception) {
        }
        current.activeFile?.delete()
        _state.update { RecorderState(phase = RecorderPhase.IDLE, lastSaved = it.lastSaved) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
