package com.rogertalk.roger.helper.audio

import android.content.ComponentName
import android.os.Build
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.android.receivers.MediaKeysReceiver
import com.rogertalk.roger.ui.screens.TalkActivity
import org.jetbrains.anko.audioManager

/**
 * Class that contains the logic for MediaKey listeners *registering* and *un-registering*.
 * This class functions as a support for [TalkActivity]
 */
class MediaKeysHelper(val talkActivity: TalkActivity) {

    val component: ComponentName by lazy { ComponentName(talkActivity, MediaKeysReceiver::class.java) }

    fun registerMediaKeysReceiver() {
        AndroidVersion.toApi(Build.VERSION_CODES.LOLLIPOP, true) {
            talkActivity.audioManager.registerMediaButtonEventReceiver(component)
        }
    }

    fun unregisterMediaKeysReceiver() {
        AndroidVersion.toApi(Build.VERSION_CODES.LOLLIPOP, true) {
            talkActivity.audioManager.registerMediaButtonEventReceiver(component)
        }
    }
}