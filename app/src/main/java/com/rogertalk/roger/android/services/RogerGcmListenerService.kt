package com.rogertalk.roger.android.services

import android.os.Bundle
import com.google.android.gms.gcm.GcmListenerService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import com.rogertalk.roger.android.notification.NotificationMuteManager
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.tasks.NotificationTasks
import com.rogertalk.roger.event.broadcasts.audio.ConversationStatusChangeEvent
import com.rogertalk.roger.event.broadcasts.streams.RefreshStreamsEvent
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.manager.PushNotificationManager
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType.BUZZ
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.StreamsRequest
import com.rogertalk.roger.network.request.notifications.JoinedStreamNotificationRequest
import com.rogertalk.roger.network.request.notifications.ProfileForNewUserNotificationRequest
import com.rogertalk.roger.network.request.notifications.StreamForNewChunkNotificationRequest
import com.rogertalk.roger.repo.*
import com.rogertalk.roger.utils.constant.API_VERSION
import com.rogertalk.roger.utils.constant.AttachmentConstants
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.*
import java.util.*

class RogerGcmListenerService : GcmListenerService() {

    companion object {
        private val TYPE_NEW_CHUNK = "stream-chunk"
        private val TYPE_UPDATE_STREAM = "stream-change"
        private val TYPE_BUZZ = "stream-buzz"
        private val TYPE_TOP_TALK = "top-talker"
        private val TYPE_STATUS = "stream-status"
        private val TYPE_NEW_STREAM = "stream-new"
        private val TYPE_STREAM_HIDDEN = "stream-hidden"
        private val TYPE_STREAM_LEAVE = "stream-leave"
        private val TYPE_STREAM_JOIN = "stream-join"
        private val TYPE_STREAM_ATTACHMENT = "stream-attachment"
        private val TYPE_STREAM_PARTICIPANT_CHANGE = "stream-participant-change"

        // The following events contain stream
        val DATA_EVENTS = arrayListOf("stream-buzz",
                "stream-change",
                "stream-image",
                "stream-listen",
                "stream-new",
                "stream-shareable",
                "stream-title")
    }

    /**
     * Called when message is received.

     * @param from SenderID of the sender.
     * *
     * @param data Data bundle containing message data as key/value pairs.
     * *             For Set of keys use data.keySet().
     */
    override fun onMessageReceived(from: String?, data: Bundle?) {
        if (data == null) {
            logWarn { "Data is null!" }
            return
        }

        // Ensure that the notification data version is correct.
        val version = data.getString("api_version")
        if (version != API_VERSION.toString()) {
            logWarn { "Discarding notification (expected API version $API_VERSION, got $version)" }
            return
        }

        if (!UserAccountRepo.accountNameSet()) {
            logWarn { "Name not set yet. Discard this notification." }
            return
        }

        // Ensure that user is logged in.
        if (!SessionRepo.loggedIn()) {
            return
        }

        logDebug { "Notification data: $data" }
        val notificationType = data.getString("type") ?: ""
        when (notificationType) {
            TYPE_NEW_CHUNK -> newChunk(data)
            TYPE_UPDATE_STREAM -> streamChanged(data)
            TYPE_BUZZ -> buzz(data)
            TYPE_TOP_TALK -> topTalker(data)
            TYPE_STATUS -> status(data)
            TYPE_NEW_STREAM -> newUserFromBatch(data)
            TYPE_STREAM_HIDDEN -> streamHidden(data)
            TYPE_STREAM_LEAVE -> leftStream(data)
            TYPE_STREAM_JOIN -> joinStream(data)
            TYPE_STREAM_ATTACHMENT -> streamAttachment(data)
            TYPE_STREAM_PARTICIPANT_CHANGE -> streamParticipantChange(data)
            else -> {
                logInfo { "Unknown notification type: $notificationType" }
                handleUnknownNotificationType(data, notificationType)
            }
        }
    }

    private fun streamParticipantChange(bundle: Bundle) {
        logInfo { "Stream participant changed" }
        val streamId = streamIdFromData(bundle) ?: return

        postEvent(RefreshStreamsEvent())
    }

    private fun streamAttachment(bundle: Bundle) {
        logInfo { "Got an attachment for stream" }
        val streamId = streamIdFromData(bundle) ?: return
        val senderId = senderIdFromData(bundle) ?: return
        val ownId = UserAccountRepo.id() ?: return

        // Get new attachment data
        val message = bundle.getString("attachment")
        val attachments = HashMap<String, LinkedTreeMap<String, String>>()
        val childAttachments = LinkedTreeMap<String, String>()
        try {
            val childAttachment = Gson().fromJson(message, childAttachments.javaClass)
            attachments.put(AttachmentConstants.ATTACHMENT_KEY, childAttachment)

            if (attachments.isNotEmpty()) {

                if (ownId == senderId) {
                    logInfo { "Current user send this, don't show it" }

                    // But still refresh UI. Update cache
                    StreamCacheRepo.updateAttachmentForStream(streamId, attachments)

                    return
                }

                // Display notification
                runOnUiThread {
                    // Mark attachment as not seen
                    GlobalManager.removeFromSeenAttachments(streamId)

                    // Update cache
                    StreamCacheRepo.updateAttachmentForStream(streamId, attachments)

                    // TODO : should we group attachment notification with other notifications?
                    // Display notification
                    NotificationTasks.attachmentNotificationTask(senderId, streamId)
                }
            }
        } catch(e: Exception) {
            logError(e) { "Could not handle attachment from notification" }
            return
        }
    }

    private fun joinStream(bundle: Bundle) {
        logInfo { "Joined Stream notification" }
        val streamId = streamIdFromData(bundle) ?: return
        val senderId = senderIdFromData(bundle) ?: return

        JoinedStreamNotificationRequest(senderId, streamId).enqueueRequest()
    }

    private fun leftStream(bundle: Bundle) {
        logInfo { "Left Stream notification" }
        val streamId = streamIdFromData(bundle) ?: return

        // Remove stream from streams (if there)
        runOnUiThread {
            StreamCacheRepo.removeStream(streamId)
        }
    }

    private fun streamHidden(bundle: Bundle) {
        logInfo { "Stream Hidden notification" }
        val streamId = streamIdFromData(bundle) ?: return

        // Remove stream from streams (if there)
        runOnUiThread {
            StreamCacheRepo.removeStream(streamId)
        }
    }

    private fun newUserFromBatch(bundle: Bundle) {
        logInfo { "There's a new stream." }
        val senderId = senderIdFromData(bundle) ?: return
        val ownId = UserAccountRepo.id() ?: return
        if (ownId == senderId) {
            logInfo { "This event's sender is the same as the current user, so don't show it" }
            return
        }

        ProfileForNewUserNotificationRequest(senderId.toString()).enqueueRequest()

        // Refresh streams so we improve the chances of having the name of the sender cached
        StreamsRequest(updateCacheImmediately = true).enqueueRequest()
    }

    private fun status(bundle: Bundle) {
        logInfo { "Got status!" }
        val statusText = bundle.getString("status", "")
        val senderId = senderIdFromData(bundle) ?: return
        val streamId = streamIdFromData(bundle) ?: return
        val estimatedDuration = bundle.getString("estimated_duration", "60000").toLong()

        val conversationStatus = when (statusText) {
            StreamStatus.LISTENING.statusText -> StreamStatus.LISTENING
            StreamStatus.TALKING.statusText -> StreamStatus.TALKING
            StreamStatus.IDLE.statusText -> StreamStatus.IDLE
            StreamStatus.VIEWING_ATTACHMENT.statusText -> StreamStatus.VIEWING_ATTACHMENT
            else -> {
                logWarn { "Status not recognized: $statusText" }
                return
            }
        }

        // Show notification if someone if viewing content
        if (conversationStatus == StreamStatus.VIEWING_ATTACHMENT) {
            runOnUiThread {
                // Only show notification if user onboarded and if app is in background
                if (PrefRepo.completedOnboarding && AppVisibilityRepo.appIsBackground) {
                    logDebug { "Will update viewing attachments notification" }
                    GlobalManager.addToAttachmentsViewing(streamId, senderId)
                    NotificationsHandler.updateViewingAttachmentsNotification()
                }
            }
            return
        }

        // Update in-memory cache for conversation status
        runOnUiThread {
            ConversationStatusCacheRepo.updateStatusForAccount(streamId, senderId,
                conversationStatus, estimatedDuration)

            // Inform the UI to update itself
            ConversationStatusChangeEvent()
        }
    }

    private fun handleUnknownNotificationType(bundle: Bundle, notificationType: String) {
        if (DATA_EVENTS.contains(notificationType)) {
            logDebug { "This stream type might contain stream data. Use it to update cache" }
            handleStreamInNotification(bundle)
        }

        val alert = bundle.getString("alert", "false").toBoolean()
        if (!alert) {
            // Alerts contain the first unheard message, they were meant to improve iOS.
            // So we skip them.
            logInfo { "NOT an alert, skipping" }
            return
        }

        val bodyText = bundle.getString("gcm.notification.body", "")

        logDebug { "Got Text: $bodyText" }

        if (bodyText.isNotBlank()) {
            NotificationsHandler.genericNotification(bodyText, null)
        }
    }

    private fun topTalker(bundle: Bundle) {
        logInfo { "Got top talker ranking!" }
        val rankPosition = bundle.getString("rank", "-1").toInt()
        if (rankPosition == -1) {
            logWarn { "Could not obtain rank position" }
            return
        }

        // Find out if user has a photo
        val userPhoto = UserAccountRepo.current()?.imageURL ?: ""
        if (userPhoto.isEmpty()) {
            // Display notification immediately
            NotificationsHandler.displayTopTalkerNotification(rankPosition)
        } else {
            NotificationsService.loadAvatarForShareTopTalker(applicationContext, rankPosition)
        }
    }

    private fun buzz(bundle: Bundle) {
        logInfo { "Got a buzz" }
        if (AppVisibilityRepo.chatIsForeground) {
            // The app is open, do nothing
            return
        }

        val stream = handleStreamInNotification(bundle) ?: return
        val senderId = senderIdFromData(bundle) ?: return

        if (NotificationMuteManager.isStreamMuted(stream.id)) {
            logVerbose { "Muted notification for stream ${stream.id}" }
            return
        }

        // Get the actual stream and participant username
        val actualStream = StreamCacheRepo.getStream(stream.id)
        if (actualStream == null) {
            logWarn { "The stream for this buzz was NOT found" }
            return
        }

        // Search for this participant in all streams (it might be a new one, but we could have the same
        // participant in another stream)
        val participant = StreamCacheRepo.getAccountById(senderId)
        if (participant == null) {
            logWarn { "Could not find participant for this buzz" }
            return
        }

        val notificationData = NotificationData(participant.id, participant.displayName, "",
                participant.imageURL, stream.id, participant.displayName)

        NotificationsHandler.addToNotifications(notificationData, this, BUZZ)
    }

    /**
     * Got a new chunk for an existing stream
     */
    private fun newChunk(bundle: Bundle) {
        logInfo { "New chunk notification" }
        val alert = bundle.getString("alert", "false").toBoolean()
        if (alert) {
            // Alerts contain the first unheard message, they were meant to improve iOS.
            // So we skip them.
            logInfo { "This IS an alert, skipping" }
            return
        }

        val message = bundle.getString("chunk")
        val streamId = streamIdFromData(bundle) ?: return

        val chunk: Chunk
        try {
            chunk = Gson().fromJson(message, Chunk::class.java)
        } catch(e: Exception) {
            logError(e) { "Could not handle chunk from notification" }
            return
        }

        // GCM notifications sometimes arrive after the chunk in question has been listed to
        if (!isChunkUnheard(chunk, streamId)) {
            // Nothing else to do here
            logWarn { "Got a notification, but the chunk in question has been listened to before" }
            return
        }

        // Update cache with this new chunk.
        runOnUiThread {
            newChunkLogic(chunk, streamId)
        }
    }

    private fun newChunkLogic(chunk: Chunk, streamId: Long) {
        val possibleStream = StreamCacheRepo.getStream(streamId)

        if (possibleStream == null) {
            logDebug { "Stream for this new chunk not found, fetching it first" }
            StreamForNewChunkNotificationRequest(streamId, chunk).enqueueRequest()
            return
        }

        // Found stream, simply add the chink to it and move to possibly display it
        StreamCacheRepo.addChunkToStreams(chunk, streamId)
        PushNotificationManager.handleNewChunk(chunk, streamId)
    }

    private fun streamChanged(bundle: Bundle) {
        logInfo { "Stream changed notification" }
        val stream = handleStreamInNotification(bundle) ?: return
        val chunk = stream.chunks.lastOrNull() ?: return

        // Don't show if this stream is muted
        if (NotificationMuteManager.isStreamMuted(stream.id)) {
            logVerbose { "Muted notification for stream ${stream.id}" }
            return
        }

        PushNotificationManager.loadChunk(chunk, stream.id, null, false)
    }

    private fun streamIdFromData(bundle: Bundle): Long? {
        val streamId = bundle.getString("stream_id", "$NO_ID").toLong()
        if (streamId == NO_ID) {
            logWarn { "Could not obtain stream id" }
            return null
        }
        return streamId
    }

    private fun senderIdFromData(bundle: Bundle): Long? {
        val senderId = bundle.getString("sender_id", "$NO_ID").toLong()
        if (senderId == NO_ID) {
            logWarn { "Could not obtain sender id" }
            return null
        }
        return senderId
    }

    /**
     * If the notification contains a stream, retrieve it and use it to update cache.
     * @return The Stream contained in the notification if found, null otherwise
     */
    private fun handleStreamInNotification(bundle: Bundle): Stream? {
        val message = bundle.getString("stream") ?: return null
        val stream: Stream
        try {
            val hasOthers = if (message.contains("\"others\"")) {
                true
            } else {
                false
            }

            val gson = GsonBuilder().serializeNulls().create()
            stream = gson.fromJson(message, Stream::class.java)
            if (!hasOthers) {
                stream.others = null
            }
            runOnUiThread {
                StreamCacheRepo.updateStreamInStreams(stream)
            }
            return stream
        } catch(e: Exception) {
            logError(e) { "Could not handle stream from notification" }
            return null
        }
    }

    /**
     * GCM notifications sometimes arrive after the chunk in question has been listed to.
     * This function compares the chunk with local data storage in order to check that.
     */
    private fun isChunkUnheard(notificationChunk: Chunk, streamId: Long): Boolean {
        // Try to find the stream in question
        val stream = StreamCacheRepo.getStream(streamId) ?: return true
        val result = notificationChunk.end > stream.playedUntil

        return result
    }

}