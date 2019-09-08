package com.rogertalk.roger.android.services

import android.app.Service
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.tasks.LoadAvatarTask
import com.rogertalk.roger.event.broadcasts.NotificationAvatarLoadedEvent
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType.*
import com.rogertalk.roger.models.data.PendingNotificationData
import com.rogertalk.roger.repo.NotificationsRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.log.logMethodCall
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class NotificationsService : EventService() {

    companion object {
        private val UNPLAYED_NOTIFICATION_UPDATE = "u_notification_update"
        private val PLAYING_NOTIFICATION_UPDATE = "p_notification_update"
        private val REMINDER_NOTIFICATION_UPDATE = "r_notification_update"
        private val BUZZ_NOTIFICATION_UPDATE = "b_notification_update"
        private val TOP_TALKER_NOTIFICATION_UPDATE = "tt_notification_update"
        private val TOP_TALKER_RANK_POSITION = "rank_position"

        private val EXTRA_NOTIFICATION_DATA = "notification_data"
        private val EXTRA_PENDING_NOTIFICATION_DATA = "pending_notification_data"

        fun loadAvatarForShareTopTalker(context: Context, rankPosition: Int) {
            logMethodCall()
            val startIntent = Intent(context, NotificationsService::class.java)
            startIntent.putExtra(TOP_TALKER_NOTIFICATION_UPDATE, true)
            startIntent.putExtra(TOP_TALKER_RANK_POSITION, rankPosition)
            context.startService(startIntent)
        }

        fun loadAvatarForBuzz(context: Context) {
            val startIntent = Intent(context, NotificationsService::class.java)
            startIntent.putExtra(BUZZ_NOTIFICATION_UPDATE, true)
            context.startService(startIntent)
        }

        fun loadAvatarForUnplayed(context: Context) {
            val startIntent = Intent(context, NotificationsService::class.java)
            startIntent.putExtra(UNPLAYED_NOTIFICATION_UPDATE, true)
            context.startService(startIntent)
        }

        fun loadAvatarForPlaying(context: Context, notificationData: NotificationData): Intent {
            val startIntent = Intent(context, NotificationsService::class.java)
            startIntent.putExtra(PLAYING_NOTIFICATION_UPDATE, true)
            startIntent.putExtra(EXTRA_NOTIFICATION_DATA, notificationData)
            return startIntent
        }

        fun loadAvatarForReminder(context: Context, pendingNotificationData: PendingNotificationData): Intent {
            val startIntent = Intent(context, NotificationsService::class.java)
            startIntent.putExtra(REMINDER_NOTIFICATION_UPDATE, true)
            startIntent.putExtra(EXTRA_PENDING_NOTIFICATION_DATA, pendingNotificationData)
            return startIntent
        }
    }

    private var pendingNotifications = 0
    private var userRankPosition = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logMethodCall()
        if (intent != null) {
            handleIntent(intent)
            return Service.START_STICKY
        }
        if (pendingNotifications == 0) {
            stopSelf()
        }
        return Service.START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(UNPLAYED_NOTIFICATION_UPDATE)) {
            // execute background task to get avatar
            pendingNotifications++
            val notification = NotificationsRepo.first(NEW_CONTENT) ?: return
            LoadAvatarTask(NEW_CONTENT, notification).execute()
        }

        if (intent.hasExtra(BUZZ_NOTIFICATION_UPDATE)) {
            // execute background task to get avatar
            pendingNotifications++
            val notification = NotificationsRepo.first(BUZZ) ?: return
            LoadAvatarTask(BUZZ, notification).execute()
        }

        if (intent.hasExtra(PLAYING_NOTIFICATION_UPDATE)) {
            pendingNotifications++
            val notificationData = intent.extras.getSerializable(EXTRA_NOTIFICATION_DATA) as NotificationData
            LoadAvatarTask(PLAYING, notificationData).execute()
        }

        if (intent.hasExtra(REMINDER_NOTIFICATION_UPDATE)) {
            pendingNotifications++
            val pendingNotificationData = intent.extras.getSerializable(EXTRA_PENDING_NOTIFICATION_DATA) as PendingNotificationData
            val notificationData = NotificationData(NO_ID, pendingNotificationData.username, "", pendingNotificationData.senderImageURL, pendingNotificationData.streamId, pendingNotificationData.username)
            LoadAvatarTask(REMINDER, notificationData).execute()
        }

        if (intent.hasExtra(TOP_TALKER_NOTIFICATION_UPDATE)) {
            // TOP Talker uses the user's own account pic and data
            val userAccount = UserAccountRepo.current() ?: return

            pendingNotifications++
            userRankPosition = intent.getIntExtra(TOP_TALKER_RANK_POSITION, -1)

            val notificationData = NotificationData(NO_ID, userAccount.displayName, "", userAccount.imageURL, NO_ID, userAccount.username ?: userAccount.displayName)
            LoadAvatarTask(TOP_TALKER, notificationData).execute()
        }
    }

    /**
     * Callback for when we get the avatar
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationAvatarLoaded(event: NotificationAvatarLoadedEvent) {
        logEvent(event)

        pendingNotifications--

        when (event.notificationType) {
            NEW_CONTENT -> NotificationsHandler.updateUnplayedNotification(event.avatarBitmap)
            PLAYING -> NotificationsHandler.updatePlayingNotification(event.avatarBitmap, this)
            REMINDER -> NotificationsHandler.updateMissedNotificationReminder(event.avatarBitmap)
            BUZZ -> NotificationsHandler.updateBuzzNotifications(event.avatarBitmap)
            TOP_TALKER -> NotificationsHandler.displayTopTalkerNotification(userRankPosition, event.avatarBitmap)
        }

        // This service is no longer needed when all pending notification have been received
        if (pendingNotifications <= 0) {
            stopSelf()
        }
    }
}