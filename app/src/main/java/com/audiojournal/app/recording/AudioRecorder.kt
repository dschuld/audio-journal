package com.audiojournal.app.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Thin abstraction over the platform recorder so the recording state machine
 * ([RecordingEngine]) can be unit tested with a fake implementation.
 *
 * Implementations are single-use: one instance records one file.
 */
interface AudioRecorder {
    /** Starts recording into [output]. Throws if the recorder cannot start. */
    fun start(output: File)

    fun pause()

    fun resume()

    /** Stops recording and finalizes the output file. Throws if nothing valid was recorded. */
    fun stop()
}

/**
 * Production implementation backed by [MediaRecorder], producing an
 * AAC-encoded .m4a file.
 */
class MediaRecorderAudioRecorder(private val context: Context) : AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null

    override fun start(output: File) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // Voice-oriented settings: a spoken-word journal does not need music
        // fidelity, and these keep a one-hour entry around 14 MB instead of 58 MB.
        recorder.setAudioChannels(1)
        recorder.setAudioSamplingRate(22_050)
        recorder.setAudioEncodingBitRate(32_000)
        recorder.setOutputFile(output.absolutePath)
        try {
            recorder.prepare()
            recorder.start()
        } catch (e: Exception) {
            recorder.release()
            throw e
        }
        mediaRecorder = recorder
    }

    override fun pause() {
        mediaRecorder?.pause()
    }

    override fun resume() {
        mediaRecorder?.resume()
    }

    override fun stop() {
        val recorder = mediaRecorder ?: return
        mediaRecorder = null
        try {
            recorder.stop()
        } finally {
            recorder.release()
        }
    }
}
