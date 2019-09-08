package com.rogertalk.roger.android.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.support.v4.app.NotificationCompat
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.R
import com.rogertalk.roger.android.receivers.NotificationsReceiver
import com.rogertalk.roger.android.services.NotificationsService
import com.rogertalk.roger.android.tasks.NotificationTasks
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.manager.PendingNotificationManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType
import com.rogertalk.roger.models.data.NotificationType.BUZZ
import com.rogertalk.roger.models.data.NotificationType.NEW_CONTENT
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.NotificationsRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.screens.AttachmentsActivity
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logMethodCall
import com.rogertalk.roger.utils.log.logWarn
import org.jetbrains.anko.notificationManager
import java.util.*


object NotificationsHandler {


    private val UNPLAYED_NOTIFICATION_ID = 4000
    private val PLAYING_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 1
    private val RECORDING_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 2
    private val REMINDER_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 3
    private val BUZZ_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 4
    private val TOP_TALKER_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 5
    private val UNKNOWN_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 6
    private val ATTACHMENT_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 7
    private val ATTACHMENT_SEEN_NOTIFICATION_ID = UNPLAYED_NOTIFICATION_ID + 8

    //
    // PUBLIC METHODS
    //

    fun addToNotifications(notificationData: NotificationData, context: Context, type: NotificationType) {
        NotificationsRepo.add(notificationData, type)

        if (type == NEW_CONTENT && NotificationsRepo.count(type) > 1) {
            updateUnplayedNotification(null)
            return
        }

        // load avatar must be executed in a background thread. We load it first, and then
        // the NotificationsServices will take care of calling this class again with the
        // appropriate info
        if (type == NEW_CONTENT) {
            NotificationsService.loadAvatarForUnplayed(context)
        } else {
            NotificationsService.loadAvatarForBuzz(context)
        }
    }

    fun clearNotifications() {
        // Cleared cached info about notifications
        NotificationsRepo.removeAll(NEW_CONTENT)
        NotificationsRepo.removeAll(BUZZ)
        GlobalManager.clearAttachmentsViewing()

        // Top-Talkers notification is not cleared on purpose
        appController().notificationManager.cancel(UNPLAYED_NOTIFICATION_ID)
        appController().notificationManager.cancel(PLAYING_NOTIFICATION_ID)
        appController().notificationManager.cancel(RECORDING_NOTIFICATION_ID)
        appController().notificationManager.cancel(REMINDER_NOTIFICATION_ID)
        appController().notificationManager.cancel(BUZZ_NOTIFICATION_ID)
        appController().notificationManager.cancel(UNKNOWN_NOTIFICATION_ID)
        appController().notificationManager.cancel(ATTACHMENT_NOTIFICATION_ID)
        appController().notificationManager.cancel(ATTACHMENT_SEEN_NOTIFICATION_ID)
    }

    fun clearReminderExpiringNotification(streamId: Long) {
        appController().notificationManager.cancel(streamId.toInt())
    }

    fun clearPlayingNotification() {
        appController().notificationManager.cancel(PLAYING_NOTIFICATION_ID)
    }

    fun clearPendingNotifications() {
        appController().notificationManager.cancel(REMINDER_NOTIFICATION_ID)
        appController().notificationManager.cancel(ATTACHMENT_SEEN_NOTIFICATION_ID)
    }


    fun updateViewingAttachmentsNotification() {
        val attachmentViewingSet = GlobalManager.attachmentsViewingSet

        if (attachmentViewingSet.isEmpty()) {
            // No data to present, hide notification
            appController().notificationManager.cancel(ATTACHMENT_SEEN_NOTIFICATION_ID)
            return
        }

        val context = appController()

        // Dismiss intent should clear cache
        val dismissIntent = Intent(NotificationsReceiver.NOTIFICATIONS_DISMISSED_VIEWING_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        val dismissPendingIntent = PendingIntent.getBroadcast(context, requestID, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        // Single viewing notification
        if (attachmentViewingSet.size == 1) {
            val participantId = attachmentViewingSet.first().second
            val streamId = attachmentViewingSet.first().first

            val stream = StreamCacheRepo.getStream(streamId) ?: return
            val participant = stream.othersOrEmpty.firstOrNull { it.id == participantId } ?: return

            val notificationText = R.string.notification_viewing_attachment_single.stringResource(participant.shortName, stream.shortTitle)
            val notificationTitle = EmojiUtils.eyes


            genericNotification(notificationText, null, ATTACHMENT_SEEN_NOTIFICATION_ID, notificationTitle, dismissPendingIntent)
            return
        }

        // Multiple viewings notification
        val notificationTitle = R.string.notification_viewing_attachment_multiple_compact.stringResource(attachmentViewingSet.size)

        val notificationBuilder = NotificationCompat.Builder(context)
                .setNumber(attachmentViewingSet.size)
                .setAutoCancel(true)
                .setContentTitle(notificationTitle)
                .setDeleteIntent(dismissPendingIntent)

        // Add various messages
        val inboxStyle = NotificationCompat.InboxStyle()
        inboxStyle.setBigContentTitle(notificationTitle)
        inboxStyle.addLine("")
        for (view in attachmentViewingSet) {
            val participantId = view.second
            val streamId = view.first

            val stream = StreamCacheRepo.getStream(streamId)
            val participant = stream?.others?.firstOrNull { it.id == participantId }

            if (stream != null && participant != null) {
                inboxStyle.addLine(participant.displayName)
            }
        }

        notificationBuilder.setStyle(inboxStyle)

        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            notificationBuilder.setCategory(Notification.CATEGORY_SOCIAL)
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT)
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        setUserIcon(notificationBuilder, null)

        context.notificationManager.notify(ATTACHMENT_SEEN_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun newAttachmentNotification(streamId: Long, text: String, customBitmap: Bitmap?) {
        val notificationTitle = R.string.notification_new_attachment_title.stringResource() + " ${EmojiUtils.gift}"

        val intent = AttachmentsActivity.start(appController(), streamId, cameFromNotification = true)
        val requestID = System.currentTimeMillis().toInt()
        val contentIntent = PendingIntent.getActivity(appController(), requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        genericNotification(text, customBitmap, ATTACHMENT_NOTIFICATION_ID, notificationTitle, contentIntent = contentIntent)
    }

    /**
     * Display a simple text notification. When pressed goes to Talk Screen.
     */
    fun expiringAudioNotification(conversationName: String, streamId: Long) {
        // TODO : Use person's avatar on notification as well

        val usableContext = appController()
        val contentPI = getPendingIntentStartWithStream(usableContext, streamId)

        val notificationTitle = usableContext.getString(R.string.audio_expiration_notification_title)
        val contentText = R.string.audio_expiration_notification.stringResource(conversationName)

        val notificationBuilder = NotificationCompat.Builder(usableContext)
                .setContentTitle(notificationTitle)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setContentIntent(contentPI)

        // Set lollipop+ features
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            notificationBuilder.setCategory(Notification.CATEGORY_REMINDER)
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT)
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }
        // Get Avatar or App Launcher Icon
        setUserIcon(notificationBuilder, getAppIconAsBitmap())

        // We use streamId as the notification identifier, so if there is a new notification it is
        // going to replace an existing one instead of creating a duplicate.
        usableContext.notificationManager.notify(streamId.toInt(), notificationBuilder.build())
    }

    /**
     * Display a simple text notification. When pressed goes to Talk Screen.
     */
    fun genericNotification(text: String, customBitmap: Bitmap?, notificationId: Int = UNKNOWN_NOTIFICATION_ID,
                            title: String? = null, deleteIntent: PendingIntent? = null,
                            contentIntent: PendingIntent? = null) {
        val usableContext = appController()
        val contentPI = getActionPendingIntent(usableContext)

        val notificationTitle = title ?: usableContext.getString(R.string.app_name)

        val notificationBuilder = NotificationCompat.Builder(usableContext)
                .setContentTitle(notificationTitle)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setContentIntent(contentPI)

        // Add delete intent
        if (deleteIntent != null) {
            notificationBuilder.setDeleteIntent(deleteIntent)
        }

        // Add content intent
        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent)
        }

        // Set lollipop+ features
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            notificationBuilder.setCategory(Notification.CATEGORY_MESSAGE)
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT)
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }
        // Get Avatar or App Launcher Icon
        if (customBitmap != null) {
            setUserIcon(notificationBuilder, customBitmap)
        } else {
            setUserIcon(notificationBuilder, getAppIconAsBitmap())
        }

        usableContext.notificationManager.notify(notificationId, notificationBuilder.build())
    }


    fun displayNewUserAddedNotification(userName: String, senderId: Long, imageURL: String?) {
        val textToDisplay = "${EmojiUtils.partyPopper} ${R.string.user_just_added_you.stringResource(userName)}"
        NotificationTasks.genericNotificationTask(textToDisplay, senderId, userName, imageURL)
    }

    fun joinedConversationNotification(conversationName: String,
                                       conversationImageURL: String?, senderName: String) {
        val textToDisplay = "${EmojiUtils.partyPopper} " +
                R.string.notification_added_you_to_group.stringResource(senderName, conversationName)
        NotificationTasks.genericNotificationTaskRogerLogo(textToDisplay, conversationImageURL)
    }

    /**
     * Update recording notification display, this happens when the user navigates outside the app while
     * a recording is in progress.
     */
    fun displayTopTalkerNotification(rankPosition: Int, userAvatar: Bitmap? = null) {
        val usableContext = appController()
        val requestID = System.currentTimeMillis().toInt()
        val contentIntent = TalkActivityUtils.startTalkScreenForRankSharing(usableContext, rankPosition)
        val contentPI = PendingIntent.getActivity(usableContext, requestID - 1, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        val shareIntent = TalkActivityUtils.startTalkScreenForRankSharing(usableContext, rankPosition)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val sharePendingIntent = PendingIntent.getActivity(usableContext, requestID - 2, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        val shareAction = NotificationCompat.Action.Builder(R.drawable.ic_share_black_24dp,
                usableContext.getString(R.string.share_top_talker_cta),
                sharePendingIntent).build()

        val colorRed = R.color.red.colorResource(usableContext)

        val title = usableContext.getString(R.string.notification_top_talker_title)
        var description = R.string.notification_top_talker_description.stringResource(rankPosition.toString())
        description = "${EmojiUtils.trophy} $description ${EmojiUtils.partyPopper}"

        val notificationBuilder = NotificationCompat.Builder(usableContext)
                .setContentTitle(title)
                .setContentText(description)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(description))
                .setColor(colorRed)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setContentIntent(contentPI)
                .addAction(shareAction)


        // Set lollipop+ features
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            notificationBuilder.setCategory(Notification.CATEGORY_SOCIAL)
            notificationBuilder.setPriority(Notification.PRIORITY_LOW)
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        // Get Avatar or App Launcher Icon
        if (userAvatar != null) {
            setUserIcon(notificationBuilder, userAvatar)
        } else {
            setUserIcon(notificationBuilder, getAppIconAsBitmap())
        }

        usableContext.notificationManager.notify(TOP_TALKER_NOTIFICATION_ID, notificationBuilder.build())
    }


    /**
     * Update recording notification display, this happens when the user navigates outside the app while
     * a recording is in progress.
     */
    fun updateRecordingNotification(usableContext: Context) {
        if (!PlaybackStateManager.recording) {
            logWarn { "Not recording, don't present notification" }
            // Not recording, give up
            return
        }

        val contentIntent = getResumeTalkScreenIntent(usableContext)
        val deleteIntent = getDeletePendingIntent(usableContext)
        val stopIntent = getStopRecordingIntent(usableContext)

        val stopAction = NotificationCompat.Action.Builder(R.drawable.ic_call_end_black_24dp,
                usableContext.getString(R.string.notification_playing_stop_action),
                stopIntent).build()

        val colorRed = R.color.red.colorResource(usableContext)
        val contentText = usableContext.getString(R.string.notification_recording_description)

        val notificationBuilder = NotificationCompat.Builder(usableContext)
                .setContentTitle(usableContext.getString(R.string.notification_recording_title))
                .setContentText(contentText)
                .setUsesChronometer(true)
                .setColor(colorRed)
                .setOngoing(true)
                .addAction(stopAction)
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)

        setLollipopFeaturesPlaying(notificationBuilder)

        // Get App icon as bitmap
        setUserIcon(notificationBuilder, getAppIconAsBitmap())

        usableContext.notificationManager.notify(PLAYING_NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Update playing notification display, this is when there's a stream playing in the background
     */
    fun updatePlayingNotification(avatarBitmap: Bitmap?, context: Context?) {
        if (!PlaybackStateManager.bufferingOrPlaying) {
            // Not playing, give up
            return
        }

        val usableContext = context ?: appController()

        // TODO : use proper intents for this one
        val contentIntent = getResumeTalkScreenIntent(usableContext)
        val deleteIntent = getDeletePendingIntent(usableContext)
        val stopIntent = getStopPlayingIntent(usableContext)

        val stopAction = NotificationCompat.Action.Builder(R.drawable.ic_call_end_black_24dp,
                usableContext.getString(R.string.notification_playing_stop_action),
                stopIntent).build()

        val colorRed = R.color.red.colorResource(usableContext)
        val contentText = usableContext.getString(R.string.notification_playing_description)

        val notificationBuilder = NotificationCompat.Builder(usableContext)
                .setContentTitle(usableContext.getString(R.string.notification_playing_title))
                .setContentText(contentText)
                .setUsesChronometer(true)
                .setWhen(Date().time)
                .setColor(colorRed)
                .setOngoing(true)
                .addAction(stopAction)
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)

        setLollipopFeaturesPlaying(notificationBuilder)

        setUserIcon(notificationBuilder, avatarBitmap)

        val notificationManager =
                usableContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(PLAYING_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun updateBuzzNotifications(avatarBitmap: Bitmap?) {
        logMethodCall()

        if (AppVisibilityRepo.chatIsForeground) {
            // Don't display notification if app is visible
            return
        }

        val context = appController()

        val contentIntent = getActionPendingIntent(context)
        val deleteIntent = getDeleteBuzzPendingIntent(context)

        val buzzedYouText = context.getString(R.string.notification_buzz_description)
        val buzzTitle = NotificationsRepo.buzzNotificationsTitle(context, BUZZ)
        val buzzDescription = "${EmojiUtils.bee} $buzzedYouText"

        val buzzTalkBack = "$buzzTitle $buzzedYouText"

        // TODO : If talkback is enabled, don't use setSound, as it will interfere with the ticker text
        // TODO use addPeople and addPerson to be able to notify under Android special interruptions mode

        val notificationBuilder = NotificationCompat.Builder(context)
                .setContentTitle(buzzTitle)
                .setContentText(buzzDescription)
                .setAutoCancel(true)
                .setTicker(buzzTalkBack)
                .setShowWhen(true)
                .setSound(buzzNotificationSound(context))
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)

        setNotificationPulseLight(context, notificationBuilder)

        setLollipopFeaturesUnplayed(notificationBuilder)

        if (NotificationsRepo.count(BUZZ) > 1) {
            // Use app icon for avatar
            setUserIcon(notificationBuilder, getAppIconAsBitmap())
        } else {
            setUserIcon(notificationBuilder, avatarBitmap)
        }

        context.notificationManager.notify(BUZZ_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun updateMissedNotificationReminder(avatarBitmap: Bitmap?) {
        if (AppVisibilityRepo.chatIsForeground) {
            // Don't display notification if app is visible
            return
        }

        // Clear unplayed notification first
        appController().notificationManager.cancel(UNPLAYED_NOTIFICATION_ID)

        val context = appController()
        val pendingNotification = PendingNotificationManager.pendingNotification ?: return

        val contentIntent = getActionPendingIntent(context)
        val deleteIntent = getDeleteReminderPendingIntent(context)
        val emojiMissed = EmojiUtils.happyPersonRaisingHand
        val notificationText = "$emojiMissed ${context.getString(R.string.notification_single_reminder_message_text)}"

        // TODO use addPeople and addPerson to be able to notify under Android special interruptions mode

        val notificationBuilder = NotificationCompat.Builder(context)
                .setContentTitle(pendingNotification.username)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setSound(defaultAppNotificationSound(context))
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)

        setNotificationPulseLight(context, notificationBuilder)

        setLollipopFeaturesUnplayed(notificationBuilder)

        setUserIcon(notificationBuilder, avatarBitmap)

        context.notificationManager.notify(REMINDER_NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Update unplayed notification display.
     */
    fun updateUnplayedNotification(avatarBitmap: Bitmap?) {
        if (AppVisibilityRepo.chatIsForeground) {
            // Don't display notification if app is visible
            return
        }

        val rawNotificationCount = NotificationsRepo.countRaw(NEW_CONTENT)

        val context = appController()

        val contentIntent = getActionPendingIntent(context)
        val deleteIntent = getDeletePendingIntent(context)

        val title = NotificationsRepo.unplayedNotificationsTitle()
        val description = NotificationsRepo.unplayedNotificationsDescription()
        val talkbackText = "$title $description"

        // TODO use addPeople and addPerson to be able to notify under Android special interruptions mode

        val notificationsCount = NotificationsRepo.countDifferentParticipants(NEW_CONTENT)

        // TODO : If talkback is enabled, don't use setSound, as it will interfere with the ticker text
        val notificationBuilder = NotificationCompat.Builder(context)
                .setNumber(rawNotificationCount)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText("${EmojiUtils.smilingFace}️ $description")
                .setTicker(talkbackText)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)

        // Add various messages
        if (notificationsCount > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
            inboxStyle.setBigContentTitle(title)

            // Don't use last item and don't repeat usernames
            val allNotifications = NotificationsRepo.getAll(NEW_CONTENT).drop(1).distinctBy { it.username }
            inboxStyle.addLine(R.string.notification_multiple_description_element.stringResource())
            for (notification in allNotifications) {
                // TODO : (Possible improvement) Add group name as well.
                inboxStyle.addLine(notification.username)
            }

            notificationBuilder.setStyle(inboxStyle)
        }

        // Decide when to ring the Roger notification sound
        val allNotifications = NotificationsRepo.getAll(NEW_CONTENT)
        if (allNotifications.size < 2) {
            notificationBuilder.setSound(defaultAppNotificationSound(context))
        } else {
            val mostRecentStreamId = allNotifications.last().streamId
            val previousMostRecentStreamId = allNotifications.elementAt(allNotifications.size - 2).streamId
            if (mostRecentStreamId != previousMostRecentStreamId) {
                // Only notify again if the new stream is different from the previous
                notificationBuilder.setSound(defaultAppNotificationSound(context))
            }
        }

        setNotificationPulseLight(context, notificationBuilder)

        setLollipopFeaturesUnplayed(notificationBuilder)

        // Decide if we remind if someone spoke
        val firstNotification = NotificationsRepo.first(NEW_CONTENT)
        if (NotificationsRepo.count(NEW_CONTENT) == 1 && firstNotification != null) {
            val stream = StreamCacheRepo.getStream(firstNotification.streamId)
            if (stream != null) {
                // Don't register for groups
                if (!stream.isGroup) {
                    PendingNotificationManager.registerSpokenNotification(firstNotification.streamId, firstNotification.username, firstNotification.senderImageURL)
                }
            }
        } else {
            PendingNotificationManager.unregisterSpokenNotification()

            // Also clear any pending notifications that might be already showing
            clearPendingNotifications()
        }

        setUserIcon(notificationBuilder, avatarBitmap)

        context.notificationManager.notify(UNPLAYED_NOTIFICATION_ID, notificationBuilder.build())
    }


    fun defaultAppNotificationSound(context: Context): Uri {
        return Uri.parse("android.resource://"
                + context.packageName + "/" + R.raw.roger)
    }

    //
    // PRIVATE METHODS
    //

    private fun getAppIconAsBitmap(): Bitmap {
        return BitmapFactory.decodeResource(appController().resources, R.mipmap.ic_launcher)
    }

    private fun setNotificationPulseLight(context: Context, notificationBuilder: NotificationCompat.Builder) {
        val colorRed = R.color.red.colorResource(context)
        notificationBuilder.setLights(colorRed, 600, 2000)
    }

    private fun buzzNotificationSound(context: Context): Uri {
        return Uri.parse("android.resource://"
                + context.packageName + "/" + R.raw.buzz)
    }

    private fun setUserIcon(builder: NotificationCompat.Builder, avatarBitmap: Bitmap?) {
        // set the small icon
        builder.setSmallIcon(R.drawable.roger_notification_small)
        builder.setColor(R.color.roger_red.colorResource())

        // if avatar is null, that's probably because there's more than 1 person in conversion
        if (avatarBitmap == null) {
            return
        }

        builder.setLargeIcon(avatarBitmap)
    }

    private fun getActionPendingIntent(context: Context): PendingIntent {
        val intent = TalkActivityUtils.getStartTalkScreen(context)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getPendingIntentStartWithStream(context: Context, streamId: Long): PendingIntent {
        val intent = TalkActivityUtils.startWithStream(context, streamId, cameFromNotification = true)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getResumeTalkScreenIntent(context: Context): PendingIntent {
        val intent = TalkActivityUtils.getStartTalkScreen(context)
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getDeletePendingIntent(context: Context): PendingIntent {
        val intent = Intent(NotificationsReceiver.NOTIFICATIONS_DISMISSED_UNPLAYED_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getBroadcast(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getDeleteReminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(NotificationsReceiver.NOTIFICATIONS_DISMISSED_UNPLAYED_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getBroadcast(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getDeleteBuzzPendingIntent(context: Context): PendingIntent {
        val intent = Intent(NotificationsReceiver.NOTIFICATIONS_DISMISSED_BUZZ_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getBroadcast(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getStopPlayingIntent(context: Context): PendingIntent {
        val intent = Intent(NotificationsReceiver.NOTIFICATIONS_STOP_PLAYING_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getBroadcast(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getStopRecordingIntent(context: Context): PendingIntent {
        val intent = Intent(NotificationsReceiver.NOTIFICATIONS_STOP_RECORDING_ACTION)
        val requestID = System.currentTimeMillis().toInt()
        return PendingIntent.getBroadcast(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun setLollipopFeaturesUnplayed(builder: NotificationCompat.Builder) {
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            builder.setCategory(Notification.CATEGORY_MESSAGE)
            builder.setPriority(Notification.PRIORITY_MAX)

            // This hide info from the lock-screen, respecting user privacy settings
            builder.setVisibility(Notification.VISIBILITY_PRIVATE)
        }
    }

    private fun setLollipopFeaturesPlaying(builder: NotificationCompat.Builder) {
        AndroidVersion.fromApi(LOLLIPOP, inclusive = true) {
            builder.setCategory(Notification.CATEGORY_CALL)
            builder.setPriority(Notification.PRIORITY_MAX)
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }
    }

}