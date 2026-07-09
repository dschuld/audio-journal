package com.audiojournal.app.upload

import java.io.File

sealed interface UploadResult {
    /** The file was uploaded to cloud storage. */
    data object Success : UploadResult

    /** No cloud storage credentials are configured; the file stays local only. */
    data object NotConfigured : UploadResult

    /** The upload failed (e.g. no network, credentials rejected) and may be retried. */
    data class Error(val cause: Throwable) : UploadResult
}

/**
 * Destination-agnostic upload abstraction. The app ships with an S3
 * implementation ([S3CloudUploader]); other backends (Google Drive, ...) can
 * be added by implementing this interface and swapping it in AppContainer.
 */
interface CloudUploader {
    val isConfigured: Boolean

    suspend fun upload(file: File): UploadResult
}
