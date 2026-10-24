package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun underAnHourShowsMinutesAndSeconds() {
        assertEquals("00:00", TimeFormat.format(0))
        assertEquals("00:59", TimeFormat.format(59_999))
        assertEquals("59:59", TimeFormat.format(3_599_000))
    }

    @Test fun anHourOrMoreShowsHours() {
        assertEquals("1:00:00", TimeFormat.format(3_600_000))
        assertEquals("1:02:03", TimeFormat.format(3_723_000))
        assertEquals("12:34:56", TimeFormat.format(45_296_000))
    }

    @Test fun negativeIsZero() {
        assertEquals("00:00", TimeFormat.format(-5000))
    }
}
