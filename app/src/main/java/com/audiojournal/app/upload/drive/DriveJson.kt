package com.audiojournal.app.upload.drive

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Pure request/response helpers for the Drive v3 REST API, kept free of any
 * Android or HTTP types so they are unit testable.
 */
object DriveJson {

    /** Metadata JSON for the multipart file upload. */
    fun fileMetadataJson(fileName: String, mimeType: String, parentId: String?): String =
        buildJsonObject {
            put("name", fileName)
            put("mimeType", mimeType)
            if (parentId != null) {
                putJsonArray("parents") { add(parentId) }
            }
        }.toString()

    /** Extracts the id field of a files.create response. */
    fun parseFileId(responseBody: String): String? =
        Json.parseToJsonElement(responseBody).jsonObject["id"]?.jsonPrimitive?.content
}
