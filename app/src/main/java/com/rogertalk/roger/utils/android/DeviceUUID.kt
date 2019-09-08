package com.rogertalk.roger.utils.android

import android.provider.Settings
import com.rogertalk.roger.utils.extensions.appController

class DeviceUUID {
    companion object {

        /**
         * Get device unique UUID
         */
        fun getDeviceUUID(): String? {
            try {
                return Settings.Secure.getString(appController().contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            } catch(e: Exception) {
                return null
            }
        }
    }
}