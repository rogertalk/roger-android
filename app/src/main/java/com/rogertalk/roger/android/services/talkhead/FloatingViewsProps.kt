package com.rogertalk.roger.android.services.talkhead

import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.rogertalk.roger.repo.ClearTextPrefRepo

/**
 * Pre-calculated properties to use on TalkHeads
 */
class FloatingViewsProps() {

    var talkHeadWidth = 0
    var talkheadHeight = 0

    var talkHeadX = ClearTextPrefRepo.floatingLastX.toFloat()
    var talkHeadY = ClearTextPrefRepo.floatingLastY.toFloat()

    var talkHeadCenterX = 0f
    var talkHeadCenterY = 0f

    var trashCenterX = 0f
    var trashCenterY = 0f

    var visualizerCenterX = 0f
    var visualizerCenterY = 0f
    var visualizerHeight = 0

    var visualizerActualX = 0f

    var screenWidth = 0
    var screenHeight = 0

    /**
     * Re-calculate HeadView dimensions and other properties for ease of access
     */
    fun refreshHeadData(talkHeadView: View, windowManager: WindowManager) {
        val measuredWidth = talkHeadView.measuredWidth
        val measuredHeight = talkHeadView.measuredHeight

        if (talkHeadWidth == 0 && measuredWidth > 0) {
            talkHeadWidth = measuredWidth
            talkheadHeight = measuredHeight
        }

        val headParams = talkHeadView.layoutParams as WindowManager.LayoutParams
        val properX = properX(talkHeadView.measuredWidth, headParams.x)
        talkHeadCenterX = (properX + (talkHeadWidth / 2)).toFloat()
        talkHeadCenterY = (headParams.y - (talkheadHeight / 2)).toFloat()

        // TODO : does this work with rotation?
        // Screen dimensions
        if (screenWidth == 0) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }

        // Update head view position variable
        if (screenWidth != 0) {
            talkHeadX = properX.toFloat()
            talkHeadY = headParams.y.toFloat()
        }
    }

    fun refreshTrashData(trashCanView: View) {
        val layoutParams = trashCanView.layoutParams as WindowManager.LayoutParams
        trashCenterX = (layoutParams.x + (trashCanView.measuredWidth / 2)).toFloat()
        trashCenterY = (layoutParams.y - (trashCanView.measuredHeight / 2)).toFloat()
    }

    private fun properX(width: Int, xPosition: Int): Int {
        if (xPosition < 0) {
            return 0
        }
        if (xPosition > screenWidth) {
            return screenWidth - width
        }
        return xPosition
    }
}