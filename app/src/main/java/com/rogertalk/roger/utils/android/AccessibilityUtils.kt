package com.rogertalk.roger.utils.android

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import com.rogertalk.roger.utils.extensions.appController
import org.jetbrains.anko.accessibilityManager
import kotlin.LazyThreadSafetyMode.NONE

object AccessibilityUtils {

    val cachedScreenReaderActive: Int by lazy(NONE) { intValue() }

    fun isScreenReaderActive(context: Context): Boolean {
        try {
            val am = context.accessibilityManager

            // For proper Accessibility check, we check if accessibility itself is on, and if
            // its service will speak and denote audible (not spoken) feedback.
            if (am.isEnabled) {
                if (am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()) {
                    if (am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_AUDIBLE).isNotEmpty()) {
                        return true
                    }
                }
            }

            return false
        } catch(e: Exception) {
            return false
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun intValue(): Int {
        if (isScreenReaderActive(appController())) {
            return 1
        } else {
            return 0
        }
    }


}
