package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleParserTest {
    @Test fun parsesCuesAndMultilineText() {
        val srt = "1\n00:00:01,000 --> 00:00:02,500\nHello\nthere\n\n2\n01:00:00.250 --> 01:00:03,000\nBye\n"
        val cues = SubtitleParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals(SubtitleEntry(1000, 2500, "Hello\nthere"), cues[0])
        assertEquals(3_600_250L, cues[1].startTime)
    }

    @Test fun byteOrderMarkDoesNotDropFirstCue() {
        val cues = SubtitleParser.parse("﻿1\n00:00:01,000 --> 00:00:02,000\nFirst\n")
        assertEquals(1, cues.size)
        assertEquals("First", cues[0].text)
    }

    @Test fun windowsLineEndingsAndOddSpacing() {
        val cues = SubtitleParser.parse("1\r\n00:00:01,000-->00:00:02,000\r\nHi\r\n\r\n")
        assertEquals(1, cues.size)
        assertEquals(2000L, cues[0].endTime)
    }

    @Test fun badTimeIsZero() {
        assertEquals(0L, SubtitleParser.parseTime("nonsense"))
    }

    @Test fun emptyInputGivesNoCues() {
        assertEquals(0, SubtitleParser.parse("").size)
    }
}
