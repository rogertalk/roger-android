package com.rogertalk.roger.manager

import android.os.Build.VERSION_CODES.LOLLIPOP
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.models.data.PerformanceMode
import com.rogertalk.roger.models.data.PerformanceMode.FULL
import com.rogertalk.roger.models.data.PerformanceMode.MINIMAL
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logWarn
import org.jetbrains.anko.powerManager

/**
 * There's certain situations the app might not behave with all the eye-candy and heavy-processing
 * enabled. This class controls the conditions for such modes.
 */
object PerformanceManager {

    var currentMode: PerformanceMode = FULL

    val minimal: Boolean
        get() = currentMode == MINIMAL

    val performant: Boolean
        get() = currentMode == FULL

    fun reassertPerformanceMode() {
        // Check if Power Save mode is ON
        val powerManager = appController().powerManager
        if (AndroidVersion.fromApiVal(LOLLIPOP, true) && powerManager.isPowerSaveMode) {
            logWarn { "Power Save is ON. Fallback to Minimal performance mode" }
            currentMode = MINIMAL
            return
        }

        // Check if a Screen Reader is enabled
        val screenReaderActive = AccessibilityUtils.isScreenReaderActive(appController())
        if (screenReaderActive) {
            logWarn { "Screen Reader is present. Fallback to Minimal performance mode" }
            currentMode = MINIMAL
            return
        }

        currentMode = FULL
    }
}