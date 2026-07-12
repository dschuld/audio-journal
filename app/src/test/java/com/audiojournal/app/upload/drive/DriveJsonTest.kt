package com.audiojournal.app.upload.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveJsonTest {

    @Test
    fun `folder query matches folders by name`() {
        assertEquals(
            "name = 'Journal' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            DriveJson.folderQuery("Journal"),
        )
    }

    @Test
    fun `query values escape quotes and backslashes`() {
        assertEquals("It\\'s mine", DriveJson.escapeQueryValue("It's mine"))
        assertEquals("a\\\\b", DriveJson.escapeQueryValue("a\\b"))
    }

    @Test
    fun `file metadata includes name mime type and parent`() {
        assertEquals(
            """{"name":"rec.m4a","mimeType":"audio/mp4","parents":["folder123"]}""",
            DriveJson.fileMetadataJson("rec.m4a", "audio/mp4", "folder123"),
        )
    }

    @Test
    fun `file metadata omits parents when no folder is given`() {
        assertEquals(
            """{"name":"rec.m4a","mimeType":"audio/mp4"}""",
            DriveJson.fileMetadataJson("rec.m4a", "audio/mp4", null),
        )
    }

    @Test
    fun `file names with quotes survive JSON encoding`() {
        assertEquals(
            """{"name":"my \"note\".m4a","mimeType":"audio/mp4"}""",
            DriveJson.fileMetadataJson("my \"note\".m4a", "audio/mp4", null),
        )
    }

    @Test
    fun `folder metadata uses the Drive folder mime type`() {
        assertEquals(
            """{"name":"Ideas","mimeType":"application/vnd.google-apps.folder"}""",
            DriveJson.folderMetadataJson("Ideas"),
        )
    }

    @Test
    fun `first file id is extracted from a list response`() {
        val body = """{"files":[{"id":"abc","name":"Journal"},{"id":"def","name":"Journal"}]}"""
        assertEquals("abc", DriveJson.parseFirstFileId(body))
    }

    @Test
    fun `empty list response yields null`() {
        assertNull(DriveJson.parseFirstFileId("""{"files":[]}"""))
        assertNull(DriveJson.parseFirstFileId("""{}"""))
    }

    @Test
    fun `created file id is extracted`() {
        assertEquals("xyz", DriveJson.parseFileId("""{"id":"xyz"}"""))
        assertNull(DriveJson.parseFileId("""{"kind":"drive#file"}"""))
    }
}
