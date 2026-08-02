package com.audiojournal.app.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadStatusTest {

    private fun status(
        workState: UploadWorkState,
        runAttemptCount: Int = 0,
        keptLocal: Boolean = false,
        errorMessage: String? = null,
    ) = uploadStatusOf(
        workState = workState,
        runAttemptCount = runAttemptCount,
        maxAttempts = MAX_ATTEMPTS,
        keptLocal = keptLocal,
        errorMessage = errorMessage,
    )

    @Test
    fun `a job that has not run yet is queued`() {
        assertEquals(
            UploadStatus(UploadStage.QUEUED, attempt = 1, maxAttempts = MAX_ATTEMPTS),
            status(UploadWorkState.PENDING),
        )
    }

    @Test
    fun `a running job reports the attempt it is on`() {
        assertEquals(
            UploadStatus(UploadStage.UPLOADING, attempt = 3, maxAttempts = MAX_ATTEMPTS),
            status(UploadWorkState.RUNNING, runAttemptCount = 2),
        )
    }

    @Test
    fun `pending again after a failed attempt means retrying, with the reason`() {
        assertEquals(
            UploadStatus(
                stage = UploadStage.RETRYING,
                attempt = 2,
                maxAttempts = MAX_ATTEMPTS,
                errorMessage = "network down",
            ),
            status(
                UploadWorkState.PENDING,
                runAttemptCount = 1,
                errorMessage = "network down",
            ),
        )
    }

    @Test
    fun `a retry without a known reason still reports the attempt`() {
        assertEquals(
            UploadStatus(UploadStage.RETRYING, attempt = 4, maxAttempts = MAX_ATTEMPTS),
            status(UploadWorkState.PENDING, runAttemptCount = 3),
        )
    }

    @Test
    fun `success is an upload unless the file was only kept locally`() {
        assertEquals(
            UploadStage.UPLOADED,
            status(UploadWorkState.SUCCEEDED)?.stage,
        )
        assertEquals(
            UploadStage.KEPT_LOCAL,
            status(UploadWorkState.SUCCEEDED, keptLocal = true)?.stage,
        )
    }

    @Test
    fun `failure reports every attempt made and the reason`() {
        assertEquals(
            UploadStatus(
                stage = UploadStage.FAILED,
                attempt = MAX_ATTEMPTS,
                maxAttempts = MAX_ATTEMPTS,
                errorMessage = "403 Forbidden",
            ),
            status(
                UploadWorkState.FAILED,
                runAttemptCount = MAX_ATTEMPTS - 1,
                errorMessage = "403 Forbidden",
            ),
        )
    }

    @Test
    fun `cancelled work has nothing to show`() {
        assertNull(status(UploadWorkState.CANCELLED))
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
