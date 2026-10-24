package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumePolicyTest {
    private val duration = 100_000

    @Test fun middleOfVideoResumes() {
        assertTrue(ResumePolicy.isResumable(50_000, duration))
        assertEquals(50_000, ResumePolicy.startPosition(50_000, duration))
    }

    @Test fun firstFiveSecondsStartOver() {
        assertFalse(ResumePolicy.isResumable(5_000, duration))
        assertEquals(0, ResumePolicy.startPosition(3_000, duration))
    }

    @Test fun lastFiveSecondsCountAsFinished() {
        assertFalse(ResumePolicy.isResumable(95_000, duration))
        assertTrue(ResumePolicy.isFinished(95_000, duration))
        assertFalse(ResumePolicy.isFinished(94_999, duration))
    }

    @Test fun unknownDurationNeverResumes() {
        assertFalse(ResumePolicy.isResumable(50_000, 0))
        assertEquals(0, ResumePolicy.startPosition(50_000, -1))
    }
}
