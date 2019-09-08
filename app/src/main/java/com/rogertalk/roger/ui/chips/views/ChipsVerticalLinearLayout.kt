package com.rogertalk.roger.ui.chips.views

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.rogertalk.roger.ui.chips.ChipsView
import java.util.*

class ChipsVerticalLinearLayout(context: Context) : LinearLayout(context) {

    private val lineLayouts = ArrayList<LinearLayout>()

    private val densityValue: Float

    init {

        densityValue = resources.displayMetrics.density

        init()
    }

    private fun init() {
        orientation = LinearLayout.VERTICAL
    }

    fun onChipsChanged(chips: List<ChipsView.Chip>): TextLineParams? {
        clearChipsViews()

        val width = width
        if (width == 0) {
            return null
        }
        var widthSum = 0
        var rowCounter = 0

        var ll = createHorizontalView()

        for (chip in chips) {
            val view = chip.view
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

            // if width exceed current width. create a new LinearLayout
            if (widthSum + view.measuredWidth > width) {
                rowCounter++
                widthSum = 0
                ll = createHorizontalView()
            }

            widthSum += view.measuredWidth
            ll.addView(view)
        }

        // check if there is enough space left
        if (width - widthSum < width * 0.1f) {
            widthSum = 0
            rowCounter++
        }
        if (width == 0) {
            rowCounter = 0
        }
        return TextLineParams(rowCounter, widthSum)
    }

    private fun createHorizontalView(): LinearLayout {
        val ll = LinearLayout(context)
        ll.setPadding(0, (ChipsView.CHIP_BOTTOM_PADDING * densityValue).toInt(), 0, 0)
        ll.orientation = LinearLayout.HORIZONTAL
        addView(ll)
        lineLayouts.add(ll)
        return ll
    }

    private fun clearChipsViews() {
        for (linearLayout in lineLayouts) {
            linearLayout.removeAllViews()
        }
        lineLayouts.clear()
        removeAllViews()
    }

    fun linesCount(): Int {
        return lineLayouts.size
    }

    class TextLineParams(var row: Int, var lineMargin: Int)
}
