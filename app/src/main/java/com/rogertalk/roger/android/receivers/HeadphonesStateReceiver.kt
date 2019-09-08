package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.event.broadcasts.audio.HeadphoneStateEvent
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logMethodCall

/**
 * This class is responsible for acting upon system broadcasts regarding headphone state changes
 */
class HeadphonesStateReceiver : BroadcastReceiver() {

    companion object {
        val PLUGGED_OFF = 0
        val PLUGGED_IN = 1

        val EXTRA_STATE = "state"
        val EXTRA_HAS_MICROPHONE = "microphone"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        logMethodCall()
        if (intent == null) {
            return
        }

        if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val pluggedIn = intent.getIntExtra(EXTRA_STATE, PLUGGED_OFF) == PLUGGED_IN
            val haveMicrophone = intent.getIntExtra(EXTRA_HAS_MICROPHONE, 0) == 1

            logInfo { "Headphones plugged in: $pluggedIn. Headphones have mic: $haveMicrophone" }

            // Fire app-wise event.
            postEvent(HeadphoneStateEvent(pluggedIn))
        }

    }
}