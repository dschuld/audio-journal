package com.audiojournal.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import com.audiojournal.app.recording.MediaRecorderAudioRecorder
import com.audiojournal.app.recording.RecordingEngine
import com.audiojournal.app.storage.RecordingStore
import com.audiojournal.app.upload.CloudUploader
import com.audiojournal.app.upload.FolderConfig
import com.audiojournal.app.upload.UploadFolder
import com.audiojournal.app.upload.S3CloudUploader
import com.audiojournal.app.upload.S3Config
import com.audiojournal.app.upload.drive.DriveAuthManager
import com.audiojournal.app.upload.drive.DriveCloudUploader
import java.io.File

enum class UploadBackend { DRIVE, S3 }

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

    val uploadBackend: UploadBackend =
        if (BuildConfig.UPLOAD_BACKEND.equals("s3", ignoreCase = true)) UploadBackend.S3
        else UploadBackend.DRIVE

    /** Folder choices for recordings; the first entry is the default. */
    val uploadFolders: List<UploadFolder> = FolderConfig.parse(BuildConfig.DRIVE_FOLDERS)

    val driveAuthManager = DriveAuthManager(context)

    val cloudUploader: CloudUploader = when (uploadBackend) {
        UploadBackend.DRIVE -> DriveCloudUploader(driveAuthManager)
        UploadBackend.S3 -> S3CloudUploader(
            S3Config(
                bucket = BuildConfig.S3_BUCKET,
                region = BuildConfig.S3_REGION,
                accessKeyId = BuildConfig.S3_ACCESS_KEY_ID,
                secretAccessKey = BuildConfig.S3_SECRET_ACCESS_KEY,
            ),
        )
    }
}

class AudioJournalApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
