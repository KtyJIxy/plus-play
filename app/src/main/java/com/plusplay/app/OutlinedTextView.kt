package com.plusplay.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView that draws its text with a black outline, for subtitles over video without a
 * background box. The outline width follows the text size, so it stays in proportion when the
 * size changes (for example when the text is auto-sized to fit).
 */
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var drawingOutline = false

    override fun onDraw(canvas: Canvas) {
        val fillColor = currentTextColor
        val textPaint = paint

        // Pass 1: the outline. The stroke is centred on the glyph edge, so half of it is covered
        // again by the fill in pass 2; the visible outline is half of this width.
        drawingOutline = true
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeJoin = Paint.Join.ROUND
        textPaint.strokeWidth = textSize * OUTLINE_FRACTION
        setTextColor(Color.BLACK)
        super.onDraw(canvas)

        // Pass 2: the text itself
        textPaint.style = Paint.Style.FILL
        setTextColor(fillColor)
        drawingOutline = false
        super.onDraw(canvas)
    }

    // setTextColor() invalidates the view; ignore that while drawing, or it would redraw forever
    override fun invalidate() {
        if (!drawingOutline) super.invalidate()
    }

    private companion object {
        const val OUTLINE_FRACTION = 0.16f
    }
}
