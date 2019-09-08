package com.rogertalk.roger.android.tasks

import com.rogertalk.roger.R
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.ContactsRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.phone.Vibes
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

/**
 * Background tasks that fetch network-related info to display on notifications
 */
object NotificationTasks {

    /**
     * This call will display a generic notification with the provided image,
     * or produce a image based on username in case the image is not found
     */
    fun genericNotificationTask(text: String, senderId: Long, username: String, imageURL: String?) {
        doAsync {
            val bitmap = ContactsRepo.getContactBitmapAvatar(senderId,
                    username,
                    imageURL)
            uiThread {
                NotificationsHandler.genericNotification(text, bitmap)
            }
        }
    }

    fun attachmentNotificationTask(senderId: Long, streamId: Long) {
        val stream = StreamCacheRepo.getStream(streamId) ?: return
        val destination = if (stream.isGroup) {
            // Group name
            stream.title
        } else {
            R.string.notification_multiple_title_you.stringResource()
        }

        val sender = stream.othersOrEmpty.firstOrNull { it.id == senderId } ?: return

        val imageToUse = if (stream.imageURL != null) {
            stream.imageURL
        } else {
            sender.imageURL
        }

        val notificationText = R.string.notification_sent_attachment_to
                .stringResource(sender.displayName, destination)

        doAsync {
            val bitmap = ContactsRepo.getContactBitmapAvatar(senderId,
                    sender.displayName,
                    imageToUse)
            uiThread {
                if (AppVisibilityRepo.chatIsForeground) {
                    // Toast doesn't interrupt Audio, so always display it if app showing
                    appController().toast(notificationText)

                    if (!PlaybackStateManager.doingAudioIO) {
                        // Vibrate slightly as well if no audio in progress
                        Vibes.shortVibration()
                    }
                } else {
                    NotificationsHandler.newAttachmentNotification(streamId, notificationText, bitmap)
                }
            }
        }
    }

    /**
     * This call will display a generic notification with the provided image,
     * or use Roger logo if there is no image.
     */
    fun genericNotificationTaskRogerLogo(text: String, imageURL: String?) {
        doAsync {
            val bitmap = ContactsRepo.possibleRemoteImage(imageURL)
            uiThread {
                NotificationsHandler.genericNotification(text, bitmap)
            }
        }
    }
}