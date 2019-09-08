package com.rogertalk.roger.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.extensions.colorResource

class PlayerVisualizer(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()
    private var arcRect = RectF(0f, 0f, 0f, 0f)

    var circlePercentage = 60f
        set(value) {
            field = value

            // Redraw
            invalidate()
        }

    init {
        paint.isAntiAlias = true
        paint.color = R.color.white_60.colorResource(context)
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        // Transparent background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Draw the arc according to the percentage we have
        val startAngle = (circlePercentage * (-180f) + 9000f) / 100f
        val fillSweepAngle = 360f * (circlePercentage / 100f)

        arcRect = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
        canvas.drawArc(arcRect, startAngle,
                fillSweepAngle, true, paint)
    }

}
