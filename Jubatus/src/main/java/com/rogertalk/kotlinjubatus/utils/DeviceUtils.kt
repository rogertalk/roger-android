package com.rogertalk.kotlinjubatus.utils

import android.os.Build


object DeviceUtils {

    //
    // PUBLIC METHODS
    //

    val deviceName: String by lazy() { internalDeviceName() }

    fun deviceManufacturer(): String {
        val manufacturer = Build.MANUFACTURER
        return capitalize(manufacturer)
    }

    //
    // PRIVATE METHODS
    //

    private fun capitalize(s: String?): String {
        val sub = s ?: return ""
        val first = sub[0]
        if (Character.isUpperCase(first)) {
            return sub
        } else {
            return Character.toUpperCase(first) + sub.substring(1)
        }
    }

    private fun internalDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer)) {
            return capitalize(model)
        } else {
            return "${capitalize(manufacturer)} $model"
        }
    }
}