package com.audiojournal.app.upload

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderConfigTest {

    @Test
    fun `blank config falls back to the default folder`() {
        assertEquals(listOf("AudioJournal"), FolderConfig.parse(""))
        assertEquals(listOf("AudioJournal"), FolderConfig.parse("  , ,"))
    }

    @Test
    fun `single folder is parsed`() {
        assertEquals(listOf("Journal"), FolderConfig.parse("Journal"))
    }

    @Test
    fun `multiple folders are trimmed and kept in order`() {
        assertEquals(
            listOf("Journal", "Ideas", "Meeting Notes"),
            FolderConfig.parse("Journal, Ideas , Meeting Notes"),
        )
    }

    @Test
    fun `empty entries are dropped`() {
        assertEquals(listOf("A", "B"), FolderConfig.parse("A,,B,"))
    }
}
