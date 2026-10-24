package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleFitTest {
    @Test fun shortTextGetsTheMaximum() {
        assertEquals(40, SubtitleFit.largestFitting(3, 40) { true })
    }

    @Test fun longTextShrinksToTheLargestThatFits() {
        // Pretend anything up to 17 fits
        assertEquals(17, SubtitleFit.largestFitting(3, 40) { it <= 17 })
    }

    @Test fun nothingFitsReturnsTheMinimum() {
        assertEquals(3, SubtitleFit.largestFitting(3, 40) { false })
    }

    @Test fun maxBelowMinReturnsMin() {
        assertEquals(10, SubtitleFit.largestFitting(10, 5) { true })
    }

    @Test fun differentTextLengthsGiveDifferentSizes() {
        // Characters per line shrink as text grows: size * chars <= 600
        val short = SubtitleFit.largestFitting(3, 60) { it * 10 <= 600 }
        val long = SubtitleFit.largestFitting(3, 60) { it * 40 <= 600 }
        assertEquals(60, short)
        assertEquals(15, long)
    }

    @Test fun maxScalesWithWindowWidth() {
        assertEquals(24, SubtitleFit.scaledMax(24, 1080, 1080, 3))
        assertEquals(8, SubtitleFit.scaledMax(24, 360, 1080, 3))
        assertEquals(24, SubtitleFit.scaledMax(24, 2000, 1080, 3))
        assertEquals(3, SubtitleFit.scaledMax(8, 100, 1080, 3))
    }
}
