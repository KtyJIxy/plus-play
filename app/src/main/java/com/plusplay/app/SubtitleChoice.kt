package com.plusplay.app

/** Picks which .srt file in a video's folder belongs to the video. */
object SubtitleChoice {
    /**
     * An .srt named like the video wins ("Movie.srt"), then one that starts with the video's name
     * ("Movie.en.srt", "Movie - English.srt"). Returns null if none belongs to the video.
     */
    fun pick(videoFileName: String, srtNames: List<String>): String? {
        val base = videoFileName.substringBeforeLast('.', videoFileName)
        if (base.isEmpty()) return null
        srtNames.firstOrNull { it.equals("$base.srt", ignoreCase = true) }?.let { return it }
        return srtNames.sorted().firstOrNull {
            it.length > base.length + 4 &&
                it.startsWith(base, ignoreCase = true) &&
                !it[base.length].isLetterOrDigit()
        }
    }
}
