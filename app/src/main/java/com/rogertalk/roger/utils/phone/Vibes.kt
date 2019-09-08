package com.rogertalk.roger.utils.phone

import com.rogertalk.roger.utils.extensions.appController
import org.jetbrains.anko.vibrator
import kotlin.LazyThreadSafetyMode.NONE

/**
 * This is class is called Vibes because "vibrator" would just sound bad
 */
object Vibes {
    private val SHORT_VIBRATION_DURATION = 100L
    private val MEDIUM_VIBRATION_DURATION = 200L

    val hasVibes: Boolean by lazy(NONE) { appController().vibrator.hasVibrator() }

    fun shortVibration() {
        vibrate(SHORT_VIBRATION_DURATION)
    }

    fun mediumVibration() {
        vibrate(MEDIUM_VIBRATION_DURATION)
    }

    //
    // PRIVATE METHODS
    //

    private fun vibrate(durationMillis: Long) {
        if (hasVibes) {
            appController().vibrator.vibrate(durationMillis)
        }
    }
}