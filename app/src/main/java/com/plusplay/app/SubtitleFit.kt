package com.plusplay.app

/** Picks the biggest text size that still fits, so short phrases are large and long ones shrink. */
object SubtitleFit {
    /**
     * Largest size in [min, max] for which [fits] is true (assumes that if a size fits, every
     * smaller one fits too). If even [min] does not fit, [min] is returned.
     */
    fun largestFitting(min: Int, max: Int, fits: (Int) -> Boolean): Int {
        if (max <= min) return min
        if (fits(max)) return max
        var low = min // assumed to fit (or the best we can do)
        var high = max // known not to fit
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (fits(mid)) low = mid else high = mid
        }
        return low
    }

    /** The largest size for the current window: the setting scaled by window width, never above it. */
    fun scaledMax(settingDp: Int, windowWidth: Int, displayWidth: Int, minDp: Int): Int {
        val scale = if (windowWidth > 0 && displayWidth > 0) minOf(1f, windowWidth.toFloat() / displayWidth) else 1f
        return maxOf(minDp, Math.round(settingDp * scale))
    }
}
