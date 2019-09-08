package com.rogertalk.roger.android.services.talkhead

import android.view.ViewTreeObserver

class FloatingUIGlobalLayoutListeners {

    class TalkHeadLayoutListener(val floatingRogerService: FloatingRogerService) : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            // Redraw CTA so position gets corrected when on the right side of the screen
            floatingRogerService.resetCTA()

            floatingRogerService.refreshHeadProperties()
        }
    }
}
