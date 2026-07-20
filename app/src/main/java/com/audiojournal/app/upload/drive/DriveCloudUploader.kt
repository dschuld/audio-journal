package com.audiojournal.app.upload.drive

import com.audiojournal.app.upload.CloudUploader
import com.audiojournal.app.upload.UploadFolder
import com.audiojournal.app.upload.UploadResult
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uploads recordings into the user's Google Drive, directly into the folder
 * id from the configuration (no name lookup — folder names are ambiguous in
 * Drive). Without a configured folder, files land in the My Drive root.
 */
class DriveCloudUploader(
    private val auth: DriveAuthManager,
    private val api: DriveApi = DriveApi(),
) : CloudUploader {

    // Drive needs no build-time secrets; the user connects their account at runtime.
    override val isConfigured: Boolean = true

    override val backendLabel: String = "Google Drive"

    override suspend fun upload(file: File, folder: UploadFolder?): UploadResult {
        return try {
            val token = auth.getAccessToken()
                ?: return UploadResult.Error(IllegalStateException("Google Drive is not connected"))
            withContext(Dispatchers.IO) {
                api.uploadFile(token, file, "audio/mp4", folder?.folderId)
            }
            UploadResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UploadResult.Error(e)
        }
    }
}
