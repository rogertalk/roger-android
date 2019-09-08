package com.rogertalk.roger.utils.anim

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import com.rogertalk.roger.manager.PerformanceManager
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Shaker(val angleVariance: Double = 0.5,
             val distance: Double,
             val totalDuration: Long = 600,
             val initialDelay: Long = 750,
             val period: Long = 2600,
             val shakes: Int = 7,
             val view: View) {

    private var timer: Timer? = null

    fun start() {
        if (PerformanceManager.minimal) {
            // Don't run if in minimal performance mode
            return
        }

        if (timer != null) {
            return
        }
        timer = fixedRateTimer(initialDelay = initialDelay, period = period) {
            runAnimation()
        }
    }

    fun stop() {
        view.clearAnimation()
        timer?.let {
            it.cancel()
            timer = null
        }
    }

    private fun runAnimation() {
        val angle = angleVariance * (Math.random() * 2 - 1)
        val deltaX = (Math.cos(angle) * distance).toFloat()
        val deltaY = (Math.sin(angle) * distance).toFloat()
        val animation = TranslateAnimation(-deltaX, deltaX, -deltaY, deltaY)
        with(animation) {
            duration = totalDuration / (shakes * 3)
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = shakes * 2 - 1
            repeatMode = Animation.REVERSE
        }
        view.post {
            view.clearAnimation()
            view.startAnimation(animation)
        }
    }
}