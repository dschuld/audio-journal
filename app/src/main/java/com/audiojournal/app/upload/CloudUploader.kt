package com.audiojournal.app.upload

import java.io.File

sealed interface UploadResult {
    /** The file was uploaded to cloud storage. */
    data object Success : UploadResult

    /** No cloud storage credentials are configured; the file stays local only. */
    data object NotConfigured : UploadResult

    /** The upload failed (e.g. no network, not signed in) and may be retried. */
    data class Error(val cause: Throwable) : UploadResult
}

/**
 * Destination-agnostic upload abstraction. The app ships with Google Drive
 * (default) and AWS S3 implementations, selected via the `upload.backend`
 * build property; other backends can be added by implementing this interface
 * and wiring it in AppContainer.
 */
interface CloudUploader {
    val isConfigured: Boolean

    /** Human-readable backend name for UI status messages. */
    val backendLabel: String

    /**
     * Uploads [file], placing it inside [folderName] when the backend supports
     * folders (a Drive folder, an S3 key prefix). Null means the backend root.
     */
    suspend fun upload(file: File, folderName: String?): UploadResult
}
