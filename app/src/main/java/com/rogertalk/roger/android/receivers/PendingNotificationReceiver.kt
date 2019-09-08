package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.services.NotificationsService
import com.rogertalk.roger.manager.PendingNotificationManager
import com.rogertalk.roger.manager.PendingNotificationManager.ACTION_AUDIO_EXPIRATION_NOTIFICATION
import com.rogertalk.roger.manager.PendingNotificationManager.ACTION_SHOW_PENDING_NOTIFICATION
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logMethodCall
import java.util.*

/**
 * This class is responsible for consuming a pending notification event.
 * (used for 'talked minutes ago' notification)
 */
class PendingNotificationReceiver : BroadcastReceiver() {

    companion object {
        private val EXPIRATION_DURATION_MILLIS = 24 * 60 * 60 * 1000L // 24 hours in millis
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: "none"
        logDebug { "Pending notifications receiver called. Action: $action" }

        if (action == ACTION_SHOW_PENDING_NOTIFICATION) {
            handleSpokenNotification(context)
        } else if (action == ACTION_AUDIO_EXPIRATION_NOTIFICATION) {
            checkExpiringChunks(context)
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun handleSpokenNotification(context: Context?) {
        val pendingNotification = PendingNotificationManager.pendingNotification
        if (pendingNotification != null && !pendingNotification.show) {
            // There is no data for the pending notification anymore it seems
            return
        }

        // Issue a reminder notification to be presented
        pendingNotification?.let {
            context?.startService(NotificationsService.loadAvatarForReminder(context, it))
        }
    }

    private fun checkExpiringChunks(context: Context?) {
        logMethodCall()
        if (context == null) {
            return
        }

        val streams = StreamCacheRepo.getCachedCopy()
        val currentTime = Date().time
        for (stream in streams) {
            // Check if this stream has chunks about to expire
            val oldestUnPlayedChunk = stream.unplayedChunks().firstOrNull()
            if (oldestUnPlayedChunk != null) {
                val soonToExpireDate = oldestUnPlayedChunk.start + EXPIRATION_DURATION_MILLIS
                if (currentTime > soonToExpireDate) {
                    // Notify the user
                    NotificationsHandler.expiringAudioNotification(stream.title, stream.id)
                }
            }

        }
    }
}
