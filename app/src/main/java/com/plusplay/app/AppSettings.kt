package com.plusplay.app

/** App-wide settings: keys, defaults and limits, shared by the settings screen and the player. */
object AppSettings {
    const val PREFS_NAME = "VideoPlayerPrefs"

    const val KEY_AUTO_RESUME = "auto_resume"
    const val KEY_TAP_SHRINK = "pip_tap_shrink"
    const val KEY_PIP_SIZE = "pip_size"
    const val KEY_ASPECT_MODE = "aspect_mode"

    class IntSetting(val key: String, val default: Int, val min: Int, val max: Int, val step: Int) {
        fun clamp(value: Int): Int = value.coerceIn(min, max)
        fun next(value: Int, direction: Int): Int = clamp(clamp(value) + direction * step)
    }

    // Largest subtitle size in dp (also adjustable while playing)
    val SUB_SIZE = IntSetting("sub_size_dp", 16, 8, 72, 1)
    // Most lines a subtitle may use before its text is made smaller
    val LINES_FULL = IntSetting("sub_lines_full", 2, 1, 4, 1)
    val LINES_PIP = IntSetting("sub_lines_pip", 2, 1, 4, 1)
    // Height of the subtitle area as a percentage of the window
    val BOX_FULL = IntSetting("sub_box_full_pct", 25, 10, 50, 5)
    val BOX_PIP = IntSetting("sub_box_pip_pct", 33, 10, 60, 5)
}
