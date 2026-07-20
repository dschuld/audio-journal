package com.audiojournal.app.recording

import com.audiojournal.app.storage.RecordingStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class FakeAudioRecorder : AudioRecorder {
    val calls = mutableListOf<String>()
    var failOnStart = false
    var failOnStop = false
    var startedFile: File? = null

    override fun start(output: File) {
        calls += "start"
        if (failOnStart) throw IllegalStateException("mic unavailable")
        output.writeBytes(byteArrayOf(1, 2, 3))
        startedFile = output
    }

    override fun pause() {
        calls += "pause"
    }

    override fun resume() {
        calls += "resume"
    }

    override fun stop() {
        calls += "stop"
        if (failOnStop) throw RuntimeException("nothing recorded")
    }
}

private class FakeTimeSource(var nowMillis: Long = 0L) : TimeSource {
    override fun elapsedRealtimeMillis(): Long = nowMillis

    fun advance(millis: Long) {
        nowMillis += millis
    }
}

class RecordingEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeRecorder: FakeAudioRecorder
    private lateinit var timeSource: FakeTimeSource
    private lateinit var engine: RecordingEngine

    @Before
    fun setUp() {
        fakeRecorder = FakeAudioRecorder()
        timeSource = FakeTimeSource()
        engine = RecordingEngine(
            recorderFactory = { fakeRecorder },
            store = RecordingStore(tempFolder.root),
            timeSource = timeSource,
        )
    }

    @Test
    fun `initial state is idle`() {
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertNull(state.activeFile)
        assertNull(state.lastSaved)
        assertFalse(engine.isActive)
    }

    @Test
    fun `start transitions to recording and starts the recorder`() {
        assertTrue(engine.start())
        val state = engine.state.value
        assertEquals(RecorderPhase.RECORDING, state.phase)
        assertNotNull(state.activeFile)
        assertEquals(listOf("start"), fakeRecorder.calls)
        assertTrue(engine.isActive)
    }

    @Test
    fun `start while already recording is rejected`() {
        assertTrue(engine.start())
        assertFalse(engine.start())
        assertEquals(listOf("start"), fakeRecorder.calls)
    }

    @Test
    fun `pause and resume drive the recorder and the phase`() {
        engine.start()
        engine.pause()
        assertEquals(RecorderPhase.PAUSED, engine.state.value.phase)
        engine.resume()
        assertEquals(RecorderPhase.RECORDING, engine.state.value.phase)
        assertEquals(listOf("start", "pause", "resume"), fakeRecorder.calls)
    }

    @Test
    fun `pause when idle is a no-op`() {
        engine.pause()
        assertEquals(RecorderPhase.IDLE, engine.state.value.phase)
        assertTrue(fakeRecorder.calls.isEmpty())
    }

    @Test
    fun `resume when recording is a no-op`() {
        engine.start()
        engine.resume()
        assertEquals(listOf("start"), fakeRecorder.calls)
    }

    @Test
    fun `elapsed time excludes paused stretches`() {
        engine.start()
        timeSource.advance(5_000)
        engine.pause()
        timeSource.advance(60_000) // paused for a minute, must not count
        assertEquals(5_000, engine.elapsedMillis())
        engine.resume()
        timeSource.advance(2_000)
        assertEquals(7_000, engine.elapsedMillis())
    }

    @Test
    fun `stop returns the saved recording with its duration`() {
        engine.start()
        timeSource.advance(3_000)
        val saved = engine.stop()
        assertNotNull(saved)
        assertEquals(3_000, saved!!.durationMillis)
        assertEquals(fakeRecorder.startedFile, saved.file)
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertEquals(saved, state.lastSaved)
        assertEquals(0, engine.elapsedMillis())
    }

    @Test
    fun `stop while paused works`() {
        engine.start()
        timeSource.advance(4_000)
        engine.pause()
        val saved = engine.stop()
        assertNotNull(saved)
        assertEquals(4_000, saved!!.durationMillis)
    }

    @Test
    fun `stop when idle returns null`() {
        assertNull(engine.stop())
        assertTrue(fakeRecorder.calls.isEmpty())
    }

    @Test
    fun `failure to start reports an error and stays idle`() {
        fakeRecorder.failOnStart = true
        assertFalse(engine.start())
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertNotNull(state.errorMessage)
        assertEquals(0, tempFolder.root.listFiles()!!.size)
    }

    @Test
    fun `failure to finalize deletes the broken file and reports an error`() {
        engine.start()
        fakeRecorder.failOnStop = true
        val saved = engine.stop()
        assertNull(saved)
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertNotNull(state.errorMessage)
        assertEquals(0, tempFolder.root.listFiles()!!.size)
    }

    @Test
    fun `discard while recording deletes the file and saves nothing`() {
        engine.start()
        timeSource.advance(3_000)
        engine.discard()
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertNull(state.lastSaved)
        assertNull(state.errorMessage)
        assertEquals(listOf("start", "stop"), fakeRecorder.calls)
        assertEquals(0, tempFolder.root.listFiles()!!.size)
        assertEquals(0, engine.elapsedMillis())
    }

    @Test
    fun `discard while paused deletes the file`() {
        engine.start()
        engine.pause()
        engine.discard()
        assertEquals(RecorderPhase.IDLE, engine.state.value.phase)
        assertEquals(0, tempFolder.root.listFiles()!!.size)
    }

    @Test
    fun `discard when idle is a no-op`() {
        engine.discard()
        assertEquals(RecorderPhase.IDLE, engine.state.value.phase)
        assertTrue(fakeRecorder.calls.isEmpty())
    }

    @Test
    fun `discard keeps the previously saved recording`() {
        engine.start()
        timeSource.advance(2_000)
        val saved = engine.stop()
        engine.start()
        engine.discard()
        assertEquals(saved, engine.state.value.lastSaved)
    }

    @Test
    fun `discard ignores finalize failures and still deletes the file`() {
        engine.start()
        fakeRecorder.failOnStop = true
        engine.discard()
        val state = engine.state.value
        assertEquals(RecorderPhase.IDLE, state.phase)
        assertNull(state.errorMessage)
        assertEquals(0, tempFolder.root.listFiles()!!.size)
    }

    @Test
    fun `can record again after discarding`() {
        engine.start()
        engine.discard()
        assertTrue(engine.start())
        assertEquals(RecorderPhase.RECORDING, engine.state.value.phase)
    }

    @Test
    fun `can record again after stopping`() {
        engine.start()
        engine.stop()
        assertTrue(engine.start())
        assertEquals(RecorderPhase.RECORDING, engine.state.value.phase)
    }

    @Test
    fun `clearError removes the error message`() {
        fakeRecorder.failOnStart = true
        engine.start()
        assertNotNull(engine.state.value.errorMessage)
        engine.clearError()
        assertNull(engine.state.value.errorMessage)
    }
}
