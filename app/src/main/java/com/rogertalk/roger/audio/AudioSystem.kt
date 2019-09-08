package com.rogertalk.roger.audio

import android.os.Build
import com.rogertalk.kotlinjubatus.utils.DeviceUtils

/**
 * Various properties of the Audio System for this device
 */
object AudioSystem {

    /**
     * @return True if device belongs to the ones we now are guaranteed to fail to record
     * properly in stereo.
     */
    val recordInStereo: Boolean
        get() {
            val osVersion = Build.VERSION.SDK_INT

            // Ignore all devices older than API 20
            if (osVersion < Build.VERSION_CODES.LOLLIPOP) {
                return false
            }
            return true
        }


    /**
     * This property indicates if we should disable audio effects on recording explicitly.
     */
    val disableRecordingEffects: Boolean
        get() {
            val deviceName = DeviceUtils.deviceName
            if (deviceName == "OUKITEL K6000 Pro") {
                return true
            }
            return false
        }

}
