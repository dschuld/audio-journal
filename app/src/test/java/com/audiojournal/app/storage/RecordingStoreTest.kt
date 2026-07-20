package com.audiojournal.app.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecordingStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // 2026-07-09 14:30:00 UTC
    private val fixedMillis = 1_783_002_600_000L

    @Test
    fun `file name is timestamped m4a`() {
        val store = RecordingStore(tempFolder.root) { fixedMillis }
        val file = store.newRecordingFile()
        assertTrue(
            "unexpected name: ${file.name}",
            file.name.matches(Regex("recording_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.m4a")),
        )
        assertEquals(tempFolder.root, file.parentFile)
    }

    @Test
    fun `creates the recordings directory if missing`() {
        val nested = File(tempFolder.root, "does/not/exist")
        val store = RecordingStore(nested) { fixedMillis }
        store.newRecordingFile()
        assertTrue(nested.isDirectory)
    }

    @Test
    fun `same-second recordings get distinct names`() {
        val store = RecordingStore(tempFolder.root) { fixedMillis }
        val first = store.newRecordingFile()
        first.createNewFile()
        val second = store.newRecordingFile()
        second.createNewFile()
        val third = store.newRecordingFile()
        assertNotEquals(first.name, second.name)
        assertNotEquals(second.name, third.name)
        assertTrue(second.name.endsWith("_2.m4a"))
        assertTrue(third.name.endsWith("_3.m4a"))
    }
}
