package com.audiojournal.app.upload

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Live upload status of a single recording, read back from the WorkManager job
 * that [UploadScheduler] enqueued for it. WorkManager keeps the job's state in
 * its own database, so this works across process death too.
 */
class UploadStatusRepository(private val workManager: WorkManager) {

    /** Emits whenever the upload of [fileName] changes state; null when unknown. */
    fun statusFor(fileName: String): Flow<UploadStatus?> =
        workManager
            .getWorkInfosForUniqueWorkFlow(UploadScheduler.uniqueWorkName(fileName))
            .map { infos -> infos.lastOrNull()?.toUploadStatus() }
}

private fun WorkInfo.toUploadStatus(): UploadStatus? = uploadStatusOf(
    workState = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> UploadWorkState.PENDING
        WorkInfo.State.RUNNING -> UploadWorkState.RUNNING
        WorkInfo.State.SUCCEEDED -> UploadWorkState.SUCCEEDED
        WorkInfo.State.FAILED -> UploadWorkState.FAILED
        WorkInfo.State.CANCELLED -> UploadWorkState.CANCELLED
    },
    runAttemptCount = runAttemptCount,
    maxAttempts = UploadWorker.MAX_ATTEMPTS,
    keptLocal = outputData.getBoolean(UploadWorker.KEY_KEPT_LOCAL, false),
    // Finished work carries the reason in its output data; between retries the
    // worker publishes it as progress, which WorkManager may drop — so the
    // retry message has to read fine without it.
    errorMessage = outputData.getString(UploadWorker.KEY_ERROR)
        ?: progress.getString(UploadWorker.KEY_ERROR),
)
