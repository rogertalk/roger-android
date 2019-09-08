package com.rogertalk.roger.helper

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.widget.ImageView
import com.rogertalk.kotlinjubatus.AnimProperties
import kotlin.LazyThreadSafetyMode.NONE

class LobbyAnimationsHelper(val circleView: ImageView) {
    companion object {
        private val BIGGER_RADIUS = 1.0f
        private val SMALLER_RADIUS = 0.2f
        private val ANIMATION_DURATION = 1500L
    }

    private val circleGrowAnimator: AnimatorSet by lazy(NONE) { createGrowAnimation(circleView) }

    //
    // PUBLIC METHODS
    //

    fun startAnimations() {
        if (!circleGrowAnimator.isRunning) {
            circleGrowAnimator.start()
        }
    }

    fun stopAnimations() {
        circleGrowAnimator.cancel()
    }

    //
    // PRIVATE METHODS
    //

    private fun createGrowAnimation(view: View): AnimatorSet {
        val animatorSet = AnimatorSet()

        val animationX = ObjectAnimator.ofFloat(view, AnimProperties.SCALE_X, SMALLER_RADIUS, BIGGER_RADIUS)
        animationX.duration = ANIMATION_DURATION
        animationX.repeatCount = ValueAnimator.INFINITE
        animationX.repeatMode = ValueAnimator.RESTART

        val animationY = ObjectAnimator.ofFloat(view, AnimProperties.SCALE_Y, SMALLER_RADIUS, BIGGER_RADIUS)
        animationY.duration = ANIMATION_DURATION
        animationY.repeatCount = ValueAnimator.INFINITE
        animationY.repeatMode = ValueAnimator.RESTART

        val alphaAnimation = ObjectAnimator.ofFloat(view, AnimProperties.ALPHA, 1f, 0.0f)
        alphaAnimation.duration = ANIMATION_DURATION
        alphaAnimation.repeatCount = ValueAnimator.INFINITE
        alphaAnimation.repeatMode = ValueAnimator.RESTART

        animatorSet.playTogether(animationX, animationY, alphaAnimation)

        return animatorSet
    }

}