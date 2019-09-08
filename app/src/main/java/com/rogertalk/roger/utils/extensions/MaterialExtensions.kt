package com.rogertalk.roger.utils.extensions

import android.graphics.Color
import android.os.Build
import android.view.View
import com.balysv.materialripple.MaterialRippleLayout
import com.rogertalk.kotlinjubatus.AndroidVersion

/**
 * @param rippleColor Color of the ripple
 * @param useOnNewerAPI Whether we should use material lib implementations on newer APIs that
 *                      natively support ripples. Useful for keeping same background
 */
fun View.materialize(rippleColor: Int = Color.WHITE, useOnNewerAPI: Boolean = false) {
    if (useOnNewerAPI || AndroidVersion.toApiVal(Build.VERSION_CODES.LOLLIPOP)) {
        MaterialRippleLayout.on(this)
                .rippleColor(rippleColor)
                .rippleHover(true)
                .rippleOverlay(true)
                .rippleDelayClick(true)
                .rippleAlpha(0.1f)
                .rippleDuration(250)
                .create()
    }
}


fun View.materializeRecyclerElement(): View {
    return MaterialRippleLayout.on(this)
            .rippleOverlay(true)
            .rippleAlpha(0.1f)
            .rippleDelayClick(false)
            .create()
}