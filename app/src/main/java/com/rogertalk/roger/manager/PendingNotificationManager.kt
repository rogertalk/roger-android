package com.rogertalk.roger.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import com.rogertalk.roger.android.receivers.PendingNotificationReceiver
import com.rogertalk.roger.models.data.PendingNotificationData
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logMethodCall
import org.jetbrains.anko.alarmManager

/**
 * Control over pending notifications
 */
object PendingNotificationManager {

    var pendingNotification: PendingNotificationData? = null

    private val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L // 2 hours in millis
    private val ELAPSE_TIME = 10 * 60 * 1000L // number of milliseconds to wait until the next notification
    private val PENDING_NOTIFICATION_ID = 3886423
    private val AUDIO_CHECK_NOTIFICATION_ID = 400001

    // Intent Actions
    val ACTION_SHOW_PENDING_NOTIFICATION = "com.rogertalk.roger.receiver.SHOW_PENDING_NOTIFICATION"
    val ACTION_AUDIO_EXPIRATION_NOTIFICATION = "com.rogertalk.roger.receiver.AUDIO_EXPIRATION_NOTIFICATION"

    //
    // PUBLIC METHODS
    //

    /**
     * Register a new pending notification
     */
    fun registerSpokenNotification(streamId: Long, username: String, senderImageURL: String?) {
        logInfo { "Will REGISTER pending spoken notification" }
        val context = appController()
        val pendingIntent = spokenPendingIntent()

        // Set data for pending notification
        pendingNotification = PendingNotificationData(streamId, username, senderImageURL)

        // Schedule it
        context.alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + ELAPSE_TIME,
                pendingIntent)
    }

    fun unregisterSpokenNotification() {
        pendingNotification?.let {
            if (it.show) {
                logInfo { "Will UNregister pending notifications" }
                appController().alarmManager.cancel(spokenPendingIntent())

                // Clear data for pending notification
                pendingNotification = null
            }
        }
    }

    /**
     * Register a new pending unheard notification
     */
    fun registerUnheardAudioCheckRepeat() {
        logMethodCall()
        val context = appController()
        val pendingIntent = unheardAudioPendingIntent()

        // Cancel any previously existing repeating intents of this kind
        appController().alarmManager.cancel(pendingIntent)

        // Schedule a new repeating alarm
        context.alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), TWO_HOURS_MILLIS,
                pendingIntent)
    }

    //
    // PRIVATE METHODS
    //


    private fun spokenPendingIntent(): PendingIntent {
        val intent = Intent(appController(), PendingNotificationReceiver::class.java)
        intent.action = ACTION_SHOW_PENDING_NOTIFICATION
        return PendingIntent.getBroadcast(appController(), PENDING_NOTIFICATION_ID, intent, 0)
    }

    private fun unheardAudioPendingIntent(): PendingIntent {
        val intent = Intent(appController(), PendingNotificationReceiver::class.java)
        intent.action = ACTION_AUDIO_EXPIRATION_NOTIFICATION

        return PendingIntent.getBroadcast(appController(), AUDIO_CHECK_NOTIFICATION_ID, intent, 0)
    }
}