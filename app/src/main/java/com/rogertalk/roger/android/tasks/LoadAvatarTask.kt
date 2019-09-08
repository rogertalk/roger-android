package com.rogertalk.roger.android.tasks

import android.graphics.Bitmap
import android.os.AsyncTask
import com.rogertalk.roger.event.broadcasts.NotificationAvatarLoadedEvent
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType
import com.rogertalk.roger.repo.ContactsRepo
import com.rogertalk.roger.utils.extensions.postEvent

/**
 * Background task for loading an avatar for use in a notification.
 *
 * This is necessary since Android doesn't allow for the loading of images (either cached or from network)
 * on the main thread.
 */
class LoadAvatarTask(val notificationType: NotificationType,
                     val notificationData: NotificationData) : AsyncTask<Unit, Unit, Bitmap?>() {


    override fun doInBackground(vararg params: Unit?): Bitmap? {
        return ContactsRepo.getContactBitmapAvatar(notificationData.senderId,
                notificationData.username,
                notificationData.senderImageURL)
    }

    override fun onPostExecute(result: Bitmap?) {
        // Post event and @com.rogertalk.roger.android.services.NotificationsService will pick it up and display the notification
        postEvent(NotificationAvatarLoadedEvent(result, notificationType))
    }
}