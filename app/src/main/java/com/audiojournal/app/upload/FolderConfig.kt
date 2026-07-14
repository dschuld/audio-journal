package com.audiojournal.app.upload

/**
 * A cloud upload destination. For Google Drive, [folderId] is the Drive
 * folder id (the last path segment of the folder's URL in Drive on the web);
 * [label] is what the in-app picker shows. For S3, [label] doubles as the
 * key prefix.
 */
data class UploadFolder(
    val label: String,
    val folderId: String,
)

/**
 * Parses the comma-separated `drive.folders` build property. Each entry is
 * either `Label=folderId` or a bare `folderId` (the id then doubles as the
 * label). The first entry is the default destination. An empty configuration
 * means no folder: uploads go to the Drive root.
 */
object FolderConfig {

    fun parse(raw: String): List<UploadFolder> =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { entry ->
                val separator = entry.indexOf('=')
                val (label, id) = if (separator > 0) {
                    entry.take(separator).trim() to entry.substring(separator + 1).trim()
                } else {
                    entry to entry
                }
                if (id.isEmpty()) null else UploadFolder(label, id)
            }
}
