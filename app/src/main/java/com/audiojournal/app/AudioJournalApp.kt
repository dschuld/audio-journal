package com.audiojournal.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import com.audiojournal.app.recording.MediaRecorderAudioRecorder
import com.audiojournal.app.recording.RecordingEngine
import com.audiojournal.app.storage.RecordingStore
import com.audiojournal.app.upload.CloudUploader
import com.audiojournal.app.upload.S3CloudUploader
import com.audiojournal.app.upload.S3Config
import java.io.File

/**
 * Hand-rolled dependency container (no DI framework needed at this size).
 * Holds the single [RecordingEngine] shared by the UI and the foreground
 * service, and the configured [CloudUploader].
 */
class AppContainer(context: Context) {

    val recordingStore = RecordingStore(
        recordingsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "recordings"),
    )

    val recordingEngine = RecordingEngine(
        recorderFactory = { MediaRecorderAudioRecorder(context) },
        store = recordingStore,
        timeSource = { SystemClock.elapsedRealtime() },
    )

    val cloudUploader: CloudUploader = S3CloudUploader(
        S3Config(
            bucket = BuildConfig.S3_BUCKET,
            region = BuildConfig.S3_REGION,
            accessKeyId = BuildConfig.S3_ACCESS_KEY_ID,
            secretAccessKey = BuildConfig.S3_SECRET_ACCESS_KEY,
        ),
    )
}

class AudioJournalApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
