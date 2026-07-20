package com.audiojournal.app.upload

import org.junit.Assert.assertEquals
import org.junit.Test

class UploadDecisionTest {

    @Test
    fun `successful upload completes the work`() {
        assertEquals(
            UploadDecision.SUCCESS,
            decideUploadOutcome(UploadResult.Success, runAttemptCount = 0, maxAttempts = 8),
        )
    }

    @Test
    fun `missing configuration completes without retrying`() {
        assertEquals(
            UploadDecision.SUCCESS,
            decideUploadOutcome(UploadResult.NotConfigured, runAttemptCount = 0, maxAttempts = 8),
        )
    }

    @Test
    fun `errors are retried until the attempt limit`() {
        val error = UploadResult.Error(RuntimeException("network down"))
        assertEquals(UploadDecision.RETRY, decideUploadOutcome(error, runAttemptCount = 0, maxAttempts = 8))
        assertEquals(UploadDecision.RETRY, decideUploadOutcome(error, runAttemptCount = 6, maxAttempts = 8))
        assertEquals(UploadDecision.GIVE_UP, decideUploadOutcome(error, runAttemptCount = 7, maxAttempts = 8))
    }
}
