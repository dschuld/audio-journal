package com.audiojournal.app.upload.drive

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class DriveApiException(val statusCode: Int, message: String) : IOException(message)

/**
 * Minimal Drive v3 REST client using HttpURLConnection — just the single
 * call this app needs (multipart file upload into a folder id). Must be
 * called from a background dispatcher.
 */
class DriveApi {

    /** Uploads [file] via multipart upload, returns the new file's id. */
    fun uploadFile(
        accessToken: String,
        file: File,
        mimeType: String,
        folderId: String?,
    ): String {
        val boundary = "audio-journal-${UUID.randomUUID()}"
        val metadata = DriveJson.fileMetadataJson(file.name, mimeType, folderId)
        val url = "$API_BASE/upload/drive/v3/files?uploadType=multipart&fields=id&supportsAllDrives=true"
        val response = request("POST", url, accessToken) { conn ->
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true
            conn.outputStream.buffered().use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(metadata.toByteArray(Charsets.UTF_8))
                out.write("\r\n--$boundary\r\n".toByteArray())
                out.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                file.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
        }
        return DriveJson.parseFileId(response)
            ?: throw IOException("Drive upload response had no id")
    }

    private inline fun request(
        method: String,
        url: String,
        accessToken: String,
        configure: (HttpURLConnection) -> Unit,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            configure(connection)
            val status = connection.responseCode
            if (status !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw DriveApiException(status, "Drive API $method $url failed with $status: ${error.take(500)}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val API_BASE = "https://www.googleapis.com"
        const val TIMEOUT_MILLIS = 60_000
    }
}
