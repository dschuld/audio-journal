package com.audiojournal.app.upload

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.audiojournal.app.AudioJournalApp
import java.io.File

/**
 * Decides how a finished upload attempt maps onto a WorkManager result.
 * Extracted as a pure function so retry behavior is unit testable.
 */
enum class UploadDecision { SUCCESS, RETRY, GIVE_UP }

fun decideUploadOutcome(result: UploadResult, runAttemptCount: Int, maxAttempts: Int): UploadDecision =
    when (result) {
        is UploadResult.Success -> UploadDecision.SUCCESS
        // Nothing to do without credentials; the file is kept locally.
        is UploadResult.NotConfigured -> UploadDecision.SUCCESS
        is UploadResult.Error ->
            if (runAttemptCount + 1 < maxAttempts) UploadDecision.RETRY else UploadDecision.GIVE_UP
    }

/**
 * Background upload of one recording. WorkManager persists the request, waits
 * for network connectivity, and retries with exponential backoff, so a
 * recording finished offline is uploaded when the device is back online.
 */
class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val file = File(path)
        if (!file.exists()) {
            Log.w(TAG, "Recording $path no longer exists; skipping upload")
            return Result.success()
        }

        val uploader = (applicationContext as AudioJournalApp).container.cloudUploader
        val result = uploader.upload(file)
        return when (decideUploadOutcome(result, runAttemptCount, MAX_ATTEMPTS)) {
            UploadDecision.SUCCESS -> {
                if (result is UploadResult.Success) {
                    Log.i(TAG, "Uploaded ${file.name}")
                } else {
                    Log.i(TAG, "Cloud storage not configured; ${file.name} kept locally")
                }
                Result.success()
            }

            UploadDecision.RETRY -> {
                Log.w(TAG, "Upload of ${file.name} failed, will retry", (result as UploadResult.Error).cause)
                Result.retry()
            }

            UploadDecision.GIVE_UP -> {
                Log.e(TAG, "Upload of ${file.name} failed permanently", (result as UploadResult.Error).cause)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val MAX_ATTEMPTS = 8
        private const val TAG = "UploadWorker"
    }
}
