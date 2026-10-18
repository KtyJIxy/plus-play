package com.plusplay.app

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.VideoView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * VideoView with selectable scaling:
 *  - FIT: whole video visible, black bars if the shapes differ (the stock VideoView behaviour)
 *  - CROP: fill the screen keeping the aspect ratio, cutting off the overflow
 *  - STRETCH: fill the screen, distorting the picture if the shapes differ
 *
 * Only the size of the view changes; the surface is scaled by the system compositor,
 * so there is no per-frame work in the app.
 */
class ScalableVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    enum class Mode { FIT, CROP, STRETCH }

    var mode: Mode = Mode.FIT
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    private var contentWidth = 0
    private var contentHeight = 0

    /** Call once the video size is known (from OnPreparedListener). */
    fun setContentSize(width: Int, height: Int) {
        if (width != contentWidth || height != contentHeight) {
            contentWidth = width
            contentHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mode == Mode.FIT || contentWidth <= 0 || contentHeight <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val availableWidth = View.getDefaultSize(contentWidth, widthMeasureSpec)
        val availableHeight = View.getDefaultSize(contentHeight, heightMeasureSpec)

        if (mode == Mode.STRETCH) {
            setMeasuredDimension(availableWidth, availableHeight)
            return
        }

        // CROP: scale up until both dimensions cover the available area. The parent
        // centres the view and clips whatever sticks out.
        val scale = max(
            availableWidth.toFloat() / contentWidth,
            availableHeight.toFloat() / contentHeight
        )
        setMeasuredDimension(
            (contentWidth * scale).roundToInt(),
            (contentHeight * scale).roundToInt()
        )
    }
}
