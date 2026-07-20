package com.audiojournal.app.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderConfigTest {

    @Test
    fun `blank config yields no folders`() {
        assertTrue(FolderConfig.parse("").isEmpty())
        assertTrue(FolderConfig.parse("  , ,").isEmpty())
    }

    @Test
    fun `bare folder id uses the id as label`() {
        assertEquals(
            listOf(UploadFolder("1AbCdEfGh", "1AbCdEfGh")),
            FolderConfig.parse("1AbCdEfGh"),
        )
    }

    @Test
    fun `label equals id entries are split and trimmed`() {
        assertEquals(
            listOf(
                UploadFolder("Journal", "1AbCdEfGh"),
                UploadFolder("Meeting Notes", "9XyZ_-123"),
            ),
            FolderConfig.parse("Journal=1AbCdEfGh, Meeting Notes = 9XyZ_-123"),
        )
    }

    @Test
    fun `entries with an empty id are dropped`() {
        assertEquals(
            listOf(UploadFolder("Journal", "1AbC")),
            FolderConfig.parse("Journal=1AbC,Broken=,"),
        )
    }

    @Test
    fun `order is preserved and first entry is the default`() {
        val folders = FolderConfig.parse("A=1,B=2,C=3")
        assertEquals(listOf("A", "B", "C"), folders.map { it.label })
        assertEquals("1", folders.first().folderId)
    }
}
