package com.audiojournal.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `zero is 00-00`() {
        assertEquals("00:00", formatElapsed(0))
    }

    @Test
    fun `sub-second values round down`() {
        assertEquals("00:00", formatElapsed(999))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("01:01", formatElapsed(61_000))
        assertEquals("59:59", formatElapsed(3_599_000))
    }

    @Test
    fun `hours appear once reached`() {
        assertEquals("1:00:00", formatElapsed(3_600_000))
        assertEquals("1:01:01", formatElapsed(3_661_000))
        assertEquals("12:34:56", formatElapsed(((12 * 3600) + (34 * 60) + 56) * 1000L))
    }
}
