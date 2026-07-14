package com.audiojournal.app.upload.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveJsonTest {

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
    fun `created file id is extracted`() {
        assertEquals("xyz", DriveJson.parseFileId("""{"id":"xyz"}"""))
        assertNull(DriveJson.parseFileId("""{"kind":"drive#file"}"""))
    }
}
