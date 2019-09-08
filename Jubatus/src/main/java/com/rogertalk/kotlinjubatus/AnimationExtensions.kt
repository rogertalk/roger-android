package com.rogertalk.kotlinjubatus

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.BounceInterpolator

/**
 * This file contains extension methods and properties that allow safer ways
 * to create animations on Views.
 */

// Properties

val FULL_ALPHA: Float
    get() = 1f

val UN_PLAYED_ALPHA: Float
    get() = 0.6f

val FULL_TRANSPARENCY: Float
    get() = 0f

val SHORT_ANIM_DURATION: Long
    get() = 200L

val MEDIUM_ANIM_DURATION: Long
    get() = 400L

val LONG_ANIM_DURATION: Long
    get() = 500L


// Extension Functions

fun View.makeTransparent() {
    alpha = 0f
}

fun View.makeOpaque(cancelAnimations: Boolean = false) {
    if (cancelAnimations) {
        animate()?.cancel()
    }
    alpha = 1f
}

fun View.createBlinkAnimation(totalDuration: Long = LONG_ANIM_DURATION): ValueAnimator {
    val animation = ObjectAnimator.ofFloat(this, AnimProperties.ALPHA, FULL_ALPHA, UN_PLAYED_ALPHA)
    animation.duration = totalDuration
    animation.repeatCount = ValueAnimator.INFINITE
    animation.repeatMode = ValueAnimator.REVERSE
    return animation
}

fun View.createPulseAnimation(): AnimatorSet {
    val animatorSet = AnimatorSet()
    val BIGGER_RADIUS = 1.4f
    val SMALLER_RADIUS = 1.0f

    val animationX = ObjectAnimator.ofFloat(this, AnimProperties.SCALE_X, SMALLER_RADIUS, BIGGER_RADIUS)
    animationX.duration = LONG_ANIM_DURATION
    animationX.repeatCount = 4
    animationX.repeatMode = ValueAnimator.REVERSE
    animationX.interpolator = BounceInterpolator()

    val animationY = ObjectAnimator.ofFloat(this, AnimProperties.SCALE_Y, SMALLER_RADIUS, BIGGER_RADIUS)
    animationY.duration = LONG_ANIM_DURATION
    animationY.repeatCount = 4
    animationY.repeatMode = ValueAnimator.REVERSE
    animationY.interpolator = BounceInterpolator()

    animatorSet.playTogether(animationX, animationY)

    return animatorSet
}

/**
 * Fade in a view and execute a function when animation finishes
 */
fun View.fadeIn(duration: Long = SHORT_ANIM_DURATION, func: () -> Unit) {
    if (this.alpha == FULL_ALPHA) {
        func()
        return
    }
    // Cancel any existent previous animation first
    animate()?.cancel()
    animate()
            .setDuration(duration)
            .alpha(FULL_ALPHA)
            .withEndAction { func() }
            .start()
}

fun View.waitFor(duration: Long, func: () -> Unit) {
    animate().setStartDelay(duration)
            .withEndAction { func() }
            .start()
}

/**
 * Fade in a view
 */
fun View.fadeIn(newDuration: Long = SHORT_ANIM_DURATION) {
    fadeIn(duration = newDuration) { }
}

/**
 * Fade out a view and execute a function when animation finishes
 */
fun View.fadeOut(duration: Long = SHORT_ANIM_DURATION, func: () -> Unit) {
    if (this.alpha == FULL_TRANSPARENCY) {
        func()
        return
    }
    // Cancel any existent previous animation first
    animate()?.cancel()
    animate()
            .setDuration(duration)
            .alpha(FULL_TRANSPARENCY)
            .withEndAction { func() }
            .start()
}

/**
 * Fade out a view
 */
fun View.fadeOut(newDuration: Long = SHORT_ANIM_DURATION) {
    fadeOut(duration = newDuration) { }
}
