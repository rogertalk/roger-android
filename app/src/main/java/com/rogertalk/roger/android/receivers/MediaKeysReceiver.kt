package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.rogertalk.roger.event.broadcasts.media.MediaKeyPlayStopEvent
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError

/**
 * This broadcast receiver captures MediaKey presses
 */
class MediaKeysReceiver : BroadcastReceiver() {

    companion object {

        /**
         * @return True if handled media key, False otherwise
         */
        fun handleMediaKey(keyCode: Int, action: Int): Boolean {
            // ignore everything but key up
            if (action != KeyEvent.ACTION_UP) {
                return false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    pressedPlayStopToggle()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    pressedPlayStopToggle()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    pressedPlayStopToggle()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    pressedPlayStopToggle()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_RECORD -> {
                    pressedPlayStopToggle()
                    return true
                }
            }

            // MediaKeys didn't handle the request
            return false
        }


        fun pressedPlayStopToggle() {
            postEvent(MediaKeyPlayStopEvent())
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            logError { "Intent is null" }
            return
        }

        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        logDebug { "User pressed media key: ${event.keyCode} - ${event.action}" }

        handleMediaKey(event.keyCode, event.action)
    }

}