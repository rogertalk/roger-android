package com.rogertalk.roger.android.notification

import android.app.Notification
import android.app.PendingIntent
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.support.v4.app.NotificationCompat
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.R
import com.rogertalk.roger.android.services.talkhead.FloatingRogerService
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.extensions.colorResource

object FloatingNotificationManager {

    val NOTIFICATION_ID = 824579286

    fun buildNotification(floatingService: FloatingRogerService, useSound: Boolean): Notification {
        val intent = TalkActivityUtils.getStartTalkScreen(floatingService)
        val requestID = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(floatingService, requestID,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationTitle = floatingService.getString(R.string.th_notification_title)
        val notificationDescription = floatingService.getString(R.string.th_notification_description)

        val notificationBuilder = NotificationCompat.Builder(floatingService)
                .setContentTitle(notificationTitle)
                .setContentText(notificationDescription)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setContentIntent(pendingIntent)

        if (useSound) {
            notificationBuilder.setSound(NotificationsHandler.defaultAppNotificationSound(floatingService))
        }

        // Set lollipop+ features
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE)
            notificationBuilder.setPriority(Notification.PRIORITY_MIN)
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        // Set image avatar
        notificationBuilder.setSmallIcon(R.drawable.roger_notification_small)
        notificationBuilder.setColor(R.color.roger_red.colorResource())

        return notificationBuilder.build()
    }
}