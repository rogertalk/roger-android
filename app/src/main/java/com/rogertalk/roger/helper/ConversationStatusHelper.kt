package com.rogertalk.roger.helper

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.widget.ImageView
import com.rogertalk.kotlinjubatus.AnimProperties
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Stream

class ConversationStatusHelper(val circleView: ImageView) {

    companion object {
        private val BIGGER_RADIUS = 1.8f
        private val SMALLER_RADIUS = 1f
        private val ANIMATION_DURATION = 1500L
    }

    private val circleGrowAnimator: AnimatorSet by lazy(LazyThreadSafetyMode.NONE) { createGrowAnimation(circleView) }
    private val circleShrinkAnimator: AnimatorSet by lazy(LazyThreadSafetyMode.NONE) { createShrinkAnimation(circleView) }

    //
    // PUBLIC METHODS
    //

    fun updateAnimations(currentStream: Stream) {
        when (currentStream.statusForStream) {
            StreamStatus.LISTENING -> displayListeningAnimation()
            StreamStatus.TALKING -> displayTalkingToYouAnimation()
            StreamStatus.IDLE -> stopAnimations()
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun stopAnimations() {
        circleGrowAnimator.cancel()
        circleShrinkAnimator.cancel()
        circleView.beGone()
    }

    private fun displayTalkingToYouAnimation() {
        if (!circleGrowAnimator.isRunning) {
            if (circleShrinkAnimator.isRunning) {
                circleShrinkAnimator.cancel()
            }

            circleView.makeVisible()
            circleGrowAnimator.start()
        }
    }

    private fun displayListeningAnimation() {
        if (!circleShrinkAnimator.isRunning) {
            if (circleGrowAnimator.isRunning) {
                circleGrowAnimator.cancel()
            }

            circleView.makeVisible()
            circleShrinkAnimator.start()
        }
    }

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

        val alphaAnimation = ObjectAnimator.ofFloat(view, AnimProperties.ALPHA, 1f, 0.1f)
        alphaAnimation.duration = ANIMATION_DURATION
        alphaAnimation.repeatCount = ValueAnimator.INFINITE
        alphaAnimation.repeatMode = ValueAnimator.RESTART

        animatorSet.playTogether(animationX, animationY, alphaAnimation)

        return animatorSet
    }

    private fun createShrinkAnimation(view: View): AnimatorSet {
        val animatorSet = AnimatorSet()

        val animationX = ObjectAnimator.ofFloat(view, AnimProperties.SCALE_X, BIGGER_RADIUS, SMALLER_RADIUS)
        animationX.duration = ANIMATION_DURATION
        animationX.repeatCount = ValueAnimator.INFINITE
        animationX.repeatMode = ValueAnimator.RESTART

        val animationY = ObjectAnimator.ofFloat(view, AnimProperties.SCALE_Y, BIGGER_RADIUS, SMALLER_RADIUS)
        animationY.duration = ANIMATION_DURATION
        animationY.repeatCount = ValueAnimator.INFINITE
        animationY.repeatMode = ValueAnimator.RESTART

        val alphaAnimation = ObjectAnimator.ofFloat(view, AnimProperties.ALPHA, 0.1f, 1f)
        alphaAnimation.duration = ANIMATION_DURATION
        alphaAnimation.repeatCount = ValueAnimator.INFINITE
        alphaAnimation.repeatMode = ValueAnimator.RESTART

        animatorSet.playTogether(animationX, animationY, alphaAnimation)

        return animatorSet
    }

}
