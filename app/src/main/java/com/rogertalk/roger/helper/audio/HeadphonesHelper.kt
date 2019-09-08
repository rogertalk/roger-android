package com.rogertalk.roger.helper.audio

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.android.receivers.HeadphonesStateReceiver

/**
 *
 */
class HeadphonesHelper(val context: Context) {

    val headphonesStateReceiver = HeadphonesStateReceiver()

    fun registerReceiver() {
        val action: String
        if (AndroidVersion.fromApiVal(Build.VERSION_CODES.LOLLIPOP, true)) {
            action = AudioManager.ACTION_HEADSET_PLUG
        } else {
            action = Intent.ACTION_HEADSET_PLUG
        }

        context.registerReceiver(headphonesStateReceiver, IntentFilter(action))
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(headphonesStateReceiver)
    }
}
