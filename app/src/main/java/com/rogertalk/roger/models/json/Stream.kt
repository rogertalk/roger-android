package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.data.StreamStatus.*
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.AttachmentConstants
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.CalendarWithMillis
import com.rogertalk.roger.utils.extensions.howLongAgoFormat
import com.rogertalk.roger.utils.extensions.shortFormat
import com.rogertalk.roger.utils.extensions.stringResource
import java.io.Serializable
import java.util.*

class Stream(
        val id: Long,
        // Chunks are listed from older to newer
        var chunks: List<Chunk>,
        val created: Long,
        @SerializedName("image_url") val customImageURL: String?,
        @SerializedName("title") var customTitle: String?,
        @SerializedName("last_played_from") val lastPlayedFrom: Long,
        @SerializedName("others_listened") val othersListened: Long?,
        @SerializedName("played_until") var playedUntil: Long,
        @SerializedName("total_duration") val totalDuration: Long,
        @SerializedName("last_interaction") var lastInteraction: Long,
        val visible: Boolean,
        var others: List<Account>? = null,
        val joined: Long,
        val service: String?,
        @SerializedName("invite_token") var inviteToken: String?,
        var attachments: HashMap<String, LinkedTreeMap<String, String>>) : Serializable {

    // The threshold at which chunks are considered expired.
    val MAX_CHUNK_AGE = 48 * 60 * 60 * 1000

    constructor() : this(
            NO_ID, emptyList(), NO_TIME, null, null, NO_TIME, null, NO_TIME, NO_TIME, NO_TIME,
            false, emptyList(), NO_TIME, null, null, HashMap<String, LinkedTreeMap<String, String>>())

    // Computed values

    val attachmentLink: String?
        get() {
            val attachment = attachments[AttachmentConstants.ATTACHMENT_KEY] as? Map<String, String> ?: return null
            return attachment[AttachmentConstants.LINK_FIELD]
        }

    val attachmentType: AttachmentType?
        get() {
            val attachment = attachments[AttachmentConstants.ATTACHMENT_KEY] as? Map<String, String> ?: return null
            val type = attachment[AttachmentConstants.ATTACHMENT_TYPE_FIELD] ?: return null
            return when (type) {
                AttachmentType.IMAGE.type -> AttachmentType.IMAGE
                AttachmentType.LINK.type -> AttachmentType.LINK
                else -> null
            }
        }

    val hasInvitedMembers: Boolean
        get() {
            return reachableParticipants.any { it.active == false }
        }

    val hasActiveMembers: Boolean
        get() {
            return othersOrEmpty.any { it.active == true }
        }

    /**
     * Participants that can be reached from this device
     */
    val reachableParticipants: List<Account>
        get() {
            return othersOrEmpty.filter { it.accountReachable == true }
        }

    /**
     * Get the participant for the last status.
     * Relevant for groups
     */
    val participantForLastStatus: Account?
        get() {
            val talking = othersOrEmpty.filter { it.conversationStatus == TALKING }
            if (talking.isNotEmpty()) {
                return talking.first()
            }
            val listening = othersOrEmpty.filter { it.conversationStatus == LISTENING }
            if (listening.isNotEmpty()) {
                return listening.first()
            }
            return null
        }

    val statusForStream: StreamStatus
        get() {
            if (isGroup) {
                // Get the most relevant status from the participants in this group.
                // Priority is as follows (most to least relevant): Talking, Listening, Idle
                var currentMaxStatus = IDLE
                val participants = others ?: emptyList()
                for (participant in participants) {
                    if (participant.conversationStatus == LISTENING && currentMaxStatus == IDLE) {
                        currentMaxStatus = LISTENING
                    } else if (participant.conversationStatus == TALKING && currentMaxStatus != TALKING) {
                        currentMaxStatus = TALKING
                    }
                }

                return currentMaxStatus
            } else {
                // Single participant. If no participant found, report IDLE
                val participant = others?.firstOrNull() ?: return IDLE
                return participant.conversationStatus
            }
        }

    /**
     * Use this instead of 'others' by default
     */
    val participants: List<Account>
        get() {
            val account = UserAccountRepo.current() ?: throw NullPointerException("Account is null")
            val otherParticipants = others
            if (otherParticipants == null || otherParticipants.isEmpty()) {
                // Conversation with self
                val singleList = ArrayList<Account>(1)
                singleList.add(account)
                return singleList
            } else {
                val multipleList = ArrayList<Account>(otherParticipants.size + 1)
                multipleList.addAll(otherParticipants)
                multipleList.add(account)
                return multipleList
            }
        }

    /**
     * Whether the current user has replied to this stream.
     */
    val currentUserHasReplied: Boolean
        get() {
            val lastChunk = chunks.lastOrNull() ?: return false
            return lastChunk.byCurrentUser
        }

    val imageURL: String?
        get() {
            customImageURL?.let { return it }
            if (isGroup) {
                // No avatar for groups for now.
                return null
            }
            val other = participants.firstOrNull() ?: return null
            // Prefer server photo
            if (other.imageURL != null) {
                return other.imageURL
            }

            // On last instance, use device's photo, if available
            return ContactMapRepo.getContactAvatarURI(other.id) ?: other.imageURL
        }

    val othersOrEmpty: List<Account>
        get() {
            val otherParticipants = others
            if (otherParticipants != null) {
                return otherParticipants
            }
            return emptyList()
        }

    val isGroup: Boolean
        get() {
            return othersOrEmpty.size > 1
        }

    val isOpenGroup: Boolean
        get() {
            val token = inviteToken
            return token != null && token.isNotEmpty()
        }

    val isEmptyConversation: Boolean
        get() {
            val activeOthers = othersOrEmpty.any { it.active == true }
            return !activeOthers
        }

    val isEmptyGroup: Boolean
        get() {
            if (!isOpenGroup) {
                // Older, non-open groups, can never display invite link. So don't consider them
                // as empty groups neither.
                return false
            }
            val activeOthers = othersOrEmpty.any { it.active == true }
            return !activeOthers
        }

    val otherListenedTime: Date?
        get() {
            if (othersListened == null) {
                return null
            }
            return Date(othersListened)
        }

    /**
     * Value that tell us if the other (sole) participant is already active on Roger
     */
    val otherIsActive: Boolean
        get() {
            val other: Account = othersOrEmpty.firstOrNull() ?: return false
            return other.active || ContactMapRepo.isContactActive(other.id)
        }

    /**
     * True if we should show greeter for this stream
     */
    val showGreet: Boolean
        get() {
            return playableChunks().isEmpty() && !currentUserHasReplied
        }

    val isService: Boolean
        get() = service != null && service.isNotEmpty()

    /**
     * Returns a list of chunks that are not from the current user
     */
    fun chunksFromOthers(): List<Chunk> {
        if (othersOrEmpty.isEmpty()) {
            return chunks
        }

        val accountId = SessionRepo.sessionId()
        val eligibleChunks = chunks.filter { it.senderId != accountId }
        return eligibleChunks
    }

    /**
     * Returns a list of chunks considered playable for the current user.
     */
    fun playableChunks(): List<Chunk> {
        val accountId = SessionRepo.sessionId()
        val threshold = Date().time - MAX_CHUNK_AGE
        var eligibleChunks = chunks.filter { it.end >= threshold }

        // Filter self out
        eligibleChunks = eligibleChunks.filter { it.senderId != accountId }

        return eligibleChunks.filter { it.end > lastPlayedFrom }
    }

    val shortTitle: String
        get() {
            val name = title.split(" ").first().dropLastWhile { it == ',' }
            val numOthers = othersOrEmpty.count()
            if (customTitle == null && numOthers > 1) {
                return "$name + ${numOthers - 1}"
            }
            return name
        }

    val title: String
        get() {
            if (isEmptyGroup) {
                return R.string.talk_screen_new_conversation.stringResource()
            }
            return when (othersOrEmpty.count()) {
                0 -> UserAccountRepo.current()?.displayName ?: R.string.you.stringResource()
                1 -> customTitle ?: othersOrEmpty.first().displayName
                else -> customTitle ?: othersOrEmpty.map { it.shortName }.joinToString(", ")
            }
        }

    /**
     * This will return true if there is **unplayed** content in this stream.
     */
    val unplayed: Boolean
        get() {
            if (isGroup && isEmptyGroup) {
                return false
            }
            val lastChunk = playableChunks().lastOrNull() ?: return false
            return lastChunk.end > playedUntil
        }

    /**
     * Get a list of **unplayed chunks**
     */
    fun unplayedChunks(): List<Chunk> {
        return playableChunks().takeLastWhile { it.end > playedUntil }
    }

    val lastInteractionLabel: String
        get() {
            // If there is no audio, show contact active status
            if (chunks.isEmpty()) {
                if (otherIsActive) {
                    return R.string.active_on_roger.stringResource()
                }

                return ""
            }

            if (currentUserHasReplied && othersListened == null) {
                return R.string.stream_interaction_sent_not_listened.stringResource()
            }

            // Get the last interaction time based on whether others listened or spoke.
            othersListened?.let {
                val othersListenedDate = CalendarWithMillis(it)
                if (currentUserHasReplied) {
                    return R.string.stream_interaction_sent_listened.stringResource(othersListenedDate.howLongAgoFormat())
                }
            }
            val lastChunk = playableChunks().lastOrNull() ?: return ""
            val lastInteractionDate = CalendarWithMillis(lastChunk.end)
            val talkedSuffix = R.string.stream_interaction_received.stringResource(lastInteractionDate.howLongAgoFormat())
            if (isGroup) {
                val lastContactToTalk = othersOrEmpty.find { it.id == lastChunk.senderId } ?: return ""
                return "${lastContactToTalk.displayName} $talkedSuffix"
            } else {
                return talkedSuffix
            }
        }

    val lastInteractionLabelShort: String
        get() = CalendarWithMillis(lastInteraction).shortFormat()

    // Methods

    /**
     * Insert stream id on each chunk for easy access.
     */
    fun mapChunksToStream() {
        // TODO: Don't mutate conditionally, instead calculate or mutate on access.
        chunks.forEach { it.streamId = id }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return id == (other as Stream).id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}