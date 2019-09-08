package com.rogertalk.roger.utils.misc

import com.rogertalk.roger.R
import com.rogertalk.roger.utils.extensions.appController

object TimeUtils {

    /**
     * Get a results like these:
     * "8 hours 10 minutes"
     * "12 minutes 5 seconds"
     * "30 seconds"
     */
    fun getDurationLong(milliseconds: Long): String {
        val hoursRaw = (milliseconds.toDouble() / 1000 / 60 / 60).toInt()
        val minutesRaw = (milliseconds.toDouble() / 1000 / 60).toInt()
        val minutes = minutesRaw - (hoursRaw * 60)
        val seconds = (milliseconds / 1000 - (minutesRaw * 60)).toInt()

        // Load labels
        val resources = appController().resources
        val minutesLabel = resources.getQuantityString(R.plurals.minutes, minutes)
        val secondsLabel = resources.getQuantityString(R.plurals.second, seconds)
        val hoursLabel = resources.getQuantityString(R.plurals.hours, hoursRaw)

        val stringBuffer = StringBuilder()
        if (hoursRaw > 0) {
            stringBuffer.append("$hoursRaw $hoursLabel ")
        }
        if (minutes > 0) {
            stringBuffer.append("$minutes $minutesLabel ")
        }
        if (hoursRaw == 0) {
            // Only display seconds if not displaying hours
            stringBuffer.append("$seconds $secondsLabel")
        }

        return stringBuffer.toString()
    }
}