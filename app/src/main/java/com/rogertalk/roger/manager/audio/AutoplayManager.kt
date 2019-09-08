package com.rogertalk.roger.manager.audio

import com.rogertalk.roger.android.services.talkhead.FloatingRogerService
import com.rogertalk.roger.event.broadcasts.audio.SwitchAndPlayStreamEvent
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.android.ScreenUtils
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logMethodCall
import org.jetbrains.anko.activityManager

object AutoplayManager {

    fun autoplayStream(stream: Stream) {
        logMethodCall()

        // If there's audio I/O in progress, don't try to autoplay
        if (PlaybackStateManager.doingAudioIO) {
            return
        }

        if (!ScreenUtils.screenOn()) {
            // Screen is OFF, don't autoplay
            return
        }

        if (appController().backgroundManager.isInBackground) {
            // Only auto-play in background if TalkHeads are visible
            if (!talkHeadsRunning()) {
                logDebug { "TalkHeads are not visible, not LivePlaying" }
                return
            }
        }

        // If chat is visible, have TalkActivity change to it and do the autoplay
        postEvent(SwitchAndPlayStreamEvent(stream))
    }

    private fun talkHeadsRunning(): Boolean {
        val manager = appController().activityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingRogerService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}