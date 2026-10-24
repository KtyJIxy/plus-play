package com.plusplay.app

/** Decides which saved positions are worth resuming from. */
object ResumePolicy {
    // Positions closer than this to the start or the end are not worth resuming
    const val MIN_RESUME_POSITION = 5000
    const val END_MARGIN_MS = 5000

    /** True if a saved position is far enough from both ends to resume from. */
    fun isResumable(position: Int, duration: Int): Boolean =
        duration > 0 && position > MIN_RESUME_POSITION && position < duration - END_MARGIN_MS

    /** Where playback should start: the saved position if resumable, otherwise 0. */
    fun startPosition(saved: Int, duration: Int): Int = if (isResumable(saved, duration)) saved else 0

    /** True if the video is close enough to its end to count as finished. */
    fun isFinished(position: Int, duration: Int): Boolean = duration > 0 && position >= duration - END_MARGIN_MS
}
