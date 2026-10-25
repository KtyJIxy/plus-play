package com.plusplay.app

/** Decides the screen orientation for a video from the saved choices. */
object OrientationPolicy {
    const val FOLLOW = 0 // leave it to the phone
    const val LANDSCAPE = 1
    const val PORTRAIT = 2
    const val MATCH = 3 // app-wide default only: landscape for wide videos, portrait for tall ones

    /** [perFile] is what the user last chose for this video (0 if nothing), [appDefault] the setting. */
    fun choose(perFile: Int, appDefault: Int, videoWidth: Int, videoHeight: Int): Int {
        if (perFile == LANDSCAPE || perFile == PORTRAIT) return perFile
        return when (appDefault) {
            LANDSCAPE -> LANDSCAPE
            PORTRAIT -> PORTRAIT
            MATCH -> when {
                videoWidth <= 0 || videoHeight <= 0 -> FOLLOW // shape not known yet
                videoHeight > videoWidth -> PORTRAIT
                else -> LANDSCAPE
            }
            else -> FOLLOW
        }
    }
}
