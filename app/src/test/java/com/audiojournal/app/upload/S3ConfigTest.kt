package com.audiojournal.app.upload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class S3ConfigTest {

    @Test
    fun `fully populated config is configured`() {
        val config = S3Config("my-bucket", "eu-central-1", "AKIA123", "secret")
        assertTrue(config.isConfigured)
    }

    @Test
    fun `any blank field means not configured`() {
        assertFalse(S3Config("", "eu-central-1", "AKIA123", "secret").isConfigured)
        assertFalse(S3Config("my-bucket", "", "AKIA123", "secret").isConfigured)
        assertFalse(S3Config("my-bucket", "eu-central-1", "", "secret").isConfigured)
        assertFalse(S3Config("my-bucket", "eu-central-1", "AKIA123", " ").isConfigured)
    }
}
