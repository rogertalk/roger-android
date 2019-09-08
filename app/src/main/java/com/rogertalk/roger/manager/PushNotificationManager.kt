package com.rogertalk.roger.manager

import android.content.Intent
import com.rogertalk.roger.R
import com.rogertalk.roger.android.notification.NotificationMuteManager
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.services.AudioDownloadManager
import com.rogertalk.roger.android.services.RegisterDeviceIntentService
import com.rogertalk.roger.android.services.talkhead.FloatingRogerService
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.ClearTextPrefRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.google.PlayServices
import com.rogertalk.roger.utils.log.logVerbose
import com.rogertalk.roger.utils.log.logWarn
import com.rogertalk.roger.utils.phone.Vibes

object PushNotificationManager {

    //
    // PUBLIC METHODS
    //

    fun initPushNotifications() {
        // Give up if not logged in yet
        if (!PrefRepo.loggedIn) {
            return
        }
        runOnUiThread {
            val context = appController()
            // Register for GCM push notifications.
            if (PlayServices.playServicesInstalled(context)) {
                val registerDeviceService = Intent(context, RegisterDeviceIntentService::class.java)
                context.startService(registerDeviceService)
            } else {
                logWarn { "No Google Play services, can't enable push notifications" }
            }
        }
    }

    fun handleNewChunk(chunk: Chunk, streamId: Long) {
        // If we have floating UI, don't display this notification
        if (FloatingManager.notificationsPreferFloating()) {
            // Will start floating service if needed

            // Reset state for TalkHead dismissal
            ClearTextPrefRepo.dismissedTalkHeads = false

            with(appController()) {
                if (!PlaybackStateManager.doingAudioIO) {
                    startService(FloatingRogerService.startWithStream(this, streamId))
                }
            }
        }

        // Get existing stream from the provided stream id.
        val matchingStream = StreamCacheRepo.getStream(streamId)
        var imageURL = matchingStream?.imageURL
        val senderId = chunk.senderId
        val cachedSenderAccount = StreamCacheRepo.getAccountById(senderId)
        if (imageURL == null && matchingStream != null && matchingStream.isGroup) {
            // Use the image of the user who last spoke instead
            if (cachedSenderAccount != null) {
                imageURL = cachedSenderAccount.imageURL
            }
        }
        val displayName = cachedSenderAccount?.displayName ?: R.string.unknown_person.stringResource()

        // Don't show if this stream is muted
        if (NotificationMuteManager.isStreamMuted(streamId)) {
            logVerbose { "Muted notification for stream $streamId" }
            return
        }

        // Build notification data.
        val notificationData = NotificationData(senderId, "", "", imageURL, streamId, displayName)

        loadChunk(chunk, streamId, notificationData, true)

        // Don't display notifications if the user is in the app.
        if (AppVisibilityRepo.chatIsForeground && !PlaybackStateManager.doingAudioIO) {
            Vibes.mediumVibration()
            return
        }
    }

    fun loadChunk(chunk: Chunk, streamId: Long,
                  notificationData: NotificationData?, displayNotification: Boolean) {
        // Display the notification.
        // TODO: Use the sender in the chunk to determine name/image of sender (for groups).
        if (displayNotification && notificationData != null) {
            NotificationsHandler.addToNotifications(notificationData, appController(), NotificationType.NEW_CONTENT)
        }

        // Preload audio in the background.
        runOnUiThread {
            AudioDownloadManager.cacheAudioHighPriority(
                    AudioDownloadManager.PendingAudioDownload(chunk.audioURL, chunk.id, streamId))
        }
    }
}
