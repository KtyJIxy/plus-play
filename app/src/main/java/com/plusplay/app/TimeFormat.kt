package com.plusplay.app

/** Formats playback times the way most players do: 5:07 style under an hour, 1:02:03 from an hour. */
object TimeFormat {
    fun format(milliseconds: Long): String {
        val totalSeconds = (if (milliseconds < 0) 0 else milliseconds) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun format(milliseconds: Int): String = format(milliseconds.toLong())
}
