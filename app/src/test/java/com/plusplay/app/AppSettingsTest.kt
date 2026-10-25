package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test fun clampKeepsValuesInRange() {
        assertEquals(1, AppSettings.LINES_FULL.clamp(0))
        assertEquals(4, AppSettings.LINES_FULL.clamp(9))
        assertEquals(33, AppSettings.BOX_PIP.clamp(33))
    }

    @Test fun nextStepsAndStopsAtLimits() {
        assertEquals(30, AppSettings.BOX_FULL.next(25, 1))
        assertEquals(20, AppSettings.BOX_FULL.next(25, -1))
        assertEquals(50, AppSettings.BOX_FULL.next(50, 1))
        assertEquals(10, AppSettings.BOX_FULL.next(10, -1))
    }

    @Test fun defaultsAreInsideTheirLimits() {
        for (s in listOf(AppSettings.SUB_SIZE, AppSettings.LINES_FULL, AppSettings.LINES_PIP, AppSettings.BOX_FULL, AppSettings.BOX_PIP)) {
            assertEquals(s.default, s.clamp(s.default))
        }
    }
}
