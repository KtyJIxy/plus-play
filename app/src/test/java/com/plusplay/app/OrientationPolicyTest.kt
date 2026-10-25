package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationPolicyTest {
    @Test fun perFileChoiceWins() {
        assertEquals(OrientationPolicy.PORTRAIT, OrientationPolicy.choose(OrientationPolicy.PORTRAIT, OrientationPolicy.LANDSCAPE, 1920, 1080))
    }

    @Test fun fixedAppDefault() {
        assertEquals(OrientationPolicy.LANDSCAPE, OrientationPolicy.choose(0, OrientationPolicy.LANDSCAPE, 0, 0))
        assertEquals(OrientationPolicy.PORTRAIT, OrientationPolicy.choose(0, OrientationPolicy.PORTRAIT, 1920, 1080))
    }

    @Test fun matchFollowsVideoShape() {
        assertEquals(OrientationPolicy.LANDSCAPE, OrientationPolicy.choose(0, OrientationPolicy.MATCH, 1920, 1080))
        assertEquals(OrientationPolicy.PORTRAIT, OrientationPolicy.choose(0, OrientationPolicy.MATCH, 1080, 1920))
        assertEquals(OrientationPolicy.FOLLOW, OrientationPolicy.choose(0, OrientationPolicy.MATCH, 0, 0))
    }

    @Test fun followLeavesItToThePhone() {
        assertEquals(OrientationPolicy.FOLLOW, OrientationPolicy.choose(0, OrientationPolicy.FOLLOW, 1920, 1080))
    }
}
