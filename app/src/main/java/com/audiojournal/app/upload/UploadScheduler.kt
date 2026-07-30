package com.audiojournal.app.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import java.util.concurrent.TimeUnit

object UploadScheduler {

    /**
     * Name of the unique work that uploads [fileName]. Recording names are
     * timestamped, so one name maps to one upload job — which is what lets the
     * UI observe that job's status again later.
     */
    fun uniqueWorkName(fileName: String): String = "upload-$fileName"

    /** Queues [file] for upload into [folder] once the device is online. */
    fun enqueue(context: Context, file: File, folder: UploadFolder?) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                workDataOf(
                    UploadWorker.KEY_FILE_PATH to file.absolutePath,
                    UploadWorker.KEY_FOLDER_ID to folder?.folderId,
                    UploadWorker.KEY_FOLDER_LABEL to folder?.label,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(file.name),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
