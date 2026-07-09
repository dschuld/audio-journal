package com.audiojournal.app.recording

import java.io.File

enum class RecorderPhase { IDLE, RECORDING, PAUSED }

data class SavedRecording(
    val file: File,
    val durationMillis: Long,
)

data class RecorderState(
    val phase: RecorderPhase = RecorderPhase.IDLE,
    val activeFile: File? = null,
    val lastSaved: SavedRecording? = null,
    val errorMessage: String? = null,
)
