package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.event.broadcasts.audio.AudioCommandEvent
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackStopReason.TAP_NOTIFICATION
import com.rogertalk.roger.manager.EventTrackingManager.RecordingReason
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.models.data.AudioCommand
import com.rogertalk.roger.models.data.NotificationType.BUZZ
import com.rogertalk.roger.models.data.NotificationType.NEW_CONTENT
import com.rogertalk.roger.repo.NotificationsRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logMethodCall
import com.rogertalk.roger.utils.log.logWarn


class NotificationsReceiver : BroadcastReceiver() {

    companion object {
        val NOTIFICATIONS_DISMISSED_UNPLAYED_ACTION = "com.rogertalk.roger.receiver.DISMISS_UNPLAYED_NOTIFICATIONS"
        val NOTIFICATIONS_DISMISSED_BUZZ_ACTION = "com.rogertalk.roger.receiver.DISMISS_BUZZ_NOTIFICATIONS"
        val NOTIFICATIONS_STOP_PLAYING_ACTION = "com.rogertalk.roger.receiver.STOP_PLAYING_NOTIFICATIONS"
        val NOTIFICATIONS_STOP_RECORDING_ACTION = "com.rogertalk.roger.receiver.STOP_RECORDING_NOTIFICATIONS"
        val NOTIFICATIONS_DISMISSED_VIEWING_ACTION = "com.rogertalk.roger.receiver.DISMISSED_VIEWING_ACTION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        logMethodCall()
        if (intent == null) {
            logWarn { "Intent was null" }
            return
        }

        when (intent.action) {
            NOTIFICATIONS_DISMISSED_UNPLAYED_ACTION -> {
                // Clear notifications list
                NotificationsRepo.removeAll(NEW_CONTENT)
            }

            NOTIFICATIONS_DISMISSED_BUZZ_ACTION -> {
                // Clear notifications list
                NotificationsRepo.removeAll(BUZZ)
            }

            NOTIFICATIONS_STOP_PLAYING_ACTION -> {
                // Clear notifications list
                NotificationsHandler.clearNotifications()

                // Actually stop playing
                postEvent(AudioCommandEvent(AudioCommand.STOP_PLAYING, playbackStopReason = TAP_NOTIFICATION))
            }

            NOTIFICATIONS_STOP_RECORDING_ACTION -> {
                // Clear notifications list
                NotificationsHandler.clearNotifications()

                // Actually stop playing
                postEvent(AudioCommandEvent(AudioCommand.STOP_RECORDING, recordingStopReason = RecordingReason.TAP_NOTIFICATION))
            }

            NOTIFICATIONS_DISMISSED_VIEWING_ACTION -> {
                logDebug { "Dismiss action for viewing notification" }
                // Clear viewing notifications
                GlobalManager.clearAttachmentsViewing()
            }
        }
    }

}