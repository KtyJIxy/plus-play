package com.plusplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleChoiceTest {
    @Test fun exactNameWins() {
        assertEquals("Movie.srt", SubtitleChoice.pick("Movie.mkv", listOf("Movie.en.srt", "Movie.srt", "Other.srt")))
    }

    @Test fun nameWithLanguageSuffix() {
        assertEquals("Movie.en.srt", SubtitleChoice.pick("Movie.mkv", listOf("Other.srt", "Movie.en.srt")))
        assertEquals("Movie - English.srt", SubtitleChoice.pick("Movie.mp4", listOf("Movie - English.srt")))
    }

    @Test fun caseIsIgnored() {
        assertEquals("MOVIE.SRT", SubtitleChoice.pick("movie.mkv", listOf("MOVIE.SRT")))
    }

    @Test fun otherEpisodesDoNotMatch() {
        assertEquals(null, SubtitleChoice.pick("Show S01E1.mkv", listOf("Show S01E10.srt", "Show S01E2.srt")))
        assertEquals(null, SubtitleChoice.pick("Movie.mkv", emptyList()))
    }
}
