package com.audiojournal.app.upload.drive

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Pure request/response helpers for the Drive v3 REST API, kept free of any
 * Android or HTTP types so they are unit testable.
 */
object DriveJson {

    const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"

    /** Escapes a value for use inside single quotes in a Drive search query. */
    fun escapeQueryValue(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")

    /** Search query matching non-trashed folders with the given name. */
    fun folderQuery(name: String): String =
        "name = '${escapeQueryValue(name)}' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"

    /** Metadata JSON for creating a folder. */
    fun folderMetadataJson(name: String): String = buildJsonObject {
        put("name", name)
        put("mimeType", FOLDER_MIME_TYPE)
    }.toString()

    /** Metadata JSON for the multipart file upload. */
    fun fileMetadataJson(fileName: String, mimeType: String, parentId: String?): String =
        buildJsonObject {
            put("name", fileName)
            put("mimeType", mimeType)
            if (parentId != null) {
                putJsonArray("parents") { add(parentId) }
            }
        }.toString()

    /** Extracts the id of the first file in a files.list response, if any. */
    fun parseFirstFileId(responseBody: String): String? =
        Json.parseToJsonElement(responseBody).jsonObject["files"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("id")
            ?.jsonPrimitive
            ?.content

    /** Extracts the id field of a files.create response. */
    fun parseFileId(responseBody: String): String? =
        Json.parseToJsonElement(responseBody).jsonObject["id"]?.jsonPrimitive?.content
}
