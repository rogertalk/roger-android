package com.rogertalk.roger.network.request.notifications

import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.models.json.Profile
import com.rogertalk.roger.network.request.BaseRequest
import com.rogertalk.roger.repo.AppVisibilityRepo

/**
 * This is special type of request, that will execute the necessary logic to display a new user notification after executed
 */
class ProfileForNewUserNotificationRequest(val accountHandle: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Profile::class.java)
        getRogerAPI().userProfile(accountHandle).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val profile = t as? Profile ?: return

        if (!AppVisibilityRepo.chatIsForeground) {
            NotificationsHandler.displayNewUserAddedNotification(profile.displayName, profile.id, profile.imageURL)
        }
    }
}