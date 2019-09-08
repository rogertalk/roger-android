package com.rogertalk.roger.network.request.notifications

import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.BaseRequest
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logDebug

/**
 * This is special type of request, that will execute the necessary logic to display a new user notification after executed
 */
class JoinedStreamNotificationRequest(val senderId: Long, val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().stream(streamId).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        val ownId = UserAccountRepo.id() ?: return
        if (ownId == senderId) {
            logDebug { "Sender is the user, nothing to show" }
            return
        }

        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)

            val sender = stream.othersOrEmpty.firstOrNull { it.id == senderId }

            if (sender != null && !AppVisibilityRepo.chatIsForeground) {
                NotificationsHandler.joinedConversationNotification(stream.title,
                        stream.imageURL,
                        sender.displayName)
            }
        }
    }
}