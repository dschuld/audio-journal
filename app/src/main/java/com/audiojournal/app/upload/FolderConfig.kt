package com.audiojournal.app.upload

/**
 * Parses the comma-separated `drive.folders` build property into a clean
 * folder list. The first entry is the default destination. Falls back to a
 * single default folder when the property is blank.
 */
object FolderConfig {

    const val DEFAULT_FOLDER = "AudioJournal"

    fun parse(raw: String, fallback: String = DEFAULT_FOLDER): List<String> =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(fallback) }
}
