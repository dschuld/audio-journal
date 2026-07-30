package com.audiojournal.app.upload

/** Where a saved recording is on its way to cloud storage. */
enum class UploadStage {
    /** Enqueued, waiting for connectivity or for a worker slot. */
    QUEUED,
    UPLOADING,
    /** An attempt failed; another one is scheduled (exponential backoff). */
    RETRYING,
    UPLOADED,
    /** No cloud backend configured, so the file stays on the device. */
    KEPT_LOCAL,
    /** Every attempt failed; the file is still on the device. */
    FAILED,
}

/** What the main screen shows about the upload of the last saved recording. */
data class UploadStatus(
    val stage: UploadStage,
    /** 1-based attempt: the one running, the one scheduled next, or the last one tried. */
    val attempt: Int = 1,
    val maxAttempts: Int = 1,
    /** Failure detail from the last attempt, when known. */
    val errorMessage: String? = null,
)

/**
 * WorkManager-free mirror of `WorkInfo.State` so [uploadStatusOf] stays a pure,
 * unit-testable function. `BLOCKED` folds into [PENDING] — from the UI's point
 * of view both mean "not started yet".
 */
enum class UploadWorkState { PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED }

/**
 * Maps one upload job's WorkManager state onto the status shown in the UI.
 *
 * [runAttemptCount] is WorkManager's count of attempts already made, so the
 * attempt that is running (or scheduled next) is `runAttemptCount + 1`.
 * Returns null when there is nothing worth showing.
 */
fun uploadStatusOf(
    workState: UploadWorkState,
    runAttemptCount: Int,
    maxAttempts: Int,
    keptLocal: Boolean = false,
    errorMessage: String? = null,
): UploadStatus? {
    val attempt = runAttemptCount + 1
    return when (workState) {
        UploadWorkState.PENDING ->
            if (runAttemptCount > 0) {
                UploadStatus(UploadStage.RETRYING, attempt, maxAttempts, errorMessage)
            } else {
                UploadStatus(UploadStage.QUEUED, attempt, maxAttempts)
            }

        UploadWorkState.RUNNING -> UploadStatus(UploadStage.UPLOADING, attempt, maxAttempts)

        UploadWorkState.SUCCEEDED -> UploadStatus(
            stage = if (keptLocal) UploadStage.KEPT_LOCAL else UploadStage.UPLOADED,
            attempt = attempt,
            maxAttempts = maxAttempts,
        )

        // runAttemptCount is not incremented for the attempt that gave up, so
        // `attempt` is the number of attempts actually made.
        UploadWorkState.FAILED ->
            UploadStatus(UploadStage.FAILED, attempt, maxAttempts, errorMessage)

        UploadWorkState.CANCELLED -> null
    }
}
