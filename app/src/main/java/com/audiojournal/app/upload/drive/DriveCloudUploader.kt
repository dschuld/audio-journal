package com.audiojournal.app.upload.drive

import com.audiojournal.app.upload.CloudUploader
import com.audiojournal.app.upload.UploadResult
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uploads recordings into the user's Google Drive. The destination folder is
 * found by name (or created in the Drive root if it does not exist yet), so
 * a folder your other tools already use is picked up as-is.
 */
class DriveCloudUploader(
    private val auth: DriveAuthManager,
    private val api: DriveApi = DriveApi(),
) : CloudUploader {

    // Drive needs no build-time secrets; the user connects their account at runtime.
    override val isConfigured: Boolean = true

    override val backendLabel: String = "Google Drive"

    override suspend fun upload(file: File, folderName: String?): UploadResult {
        return try {
            val token = auth.getAccessToken()
                ?: return UploadResult.Error(IllegalStateException("Google Drive is not connected"))
            withContext(Dispatchers.IO) {
                val folder = folderName?.takeIf { it.isNotBlank() }
                val folderId = folder?.let {
                    api.findFolder(token, it) ?: api.createFolder(token, it)
                }
                api.uploadFile(token, file, "audio/mp4", folderId)
            }
            UploadResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UploadResult.Error(e)
        }
    }
}
