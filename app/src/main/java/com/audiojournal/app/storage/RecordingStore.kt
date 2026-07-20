package com.audiojournal.app.storage

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Owns the directory recordings are written to and generates timestamped,
 * collision-free file names like `recording_2026-07-09_14-30-00.m4a`.
 */
class RecordingStore(
    private val recordingsDir: File,
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
) {

    fun newRecordingFile(): File {
        recordingsDir.mkdirs()
        val timestamp = FILE_NAME_FORMAT.format(Date(wallClockMillis()))
        var candidate = File(recordingsDir, "recording_$timestamp$EXTENSION")
        var suffix = 2
        while (candidate.exists()) {
            candidate = File(recordingsDir, "recording_${timestamp}_$suffix$EXTENSION")
            suffix++
        }
        return candidate
    }

    companion object {
        const val EXTENSION = ".m4a"

        private val FILE_NAME_FORMAT: SimpleDateFormat
            get() = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }
}
