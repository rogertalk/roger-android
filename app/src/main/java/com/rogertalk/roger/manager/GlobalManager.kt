package com.rogertalk.roger.manager

import com.rogertalk.roger.models.data.InviteContact
import com.rogertalk.roger.repo.PrefRepo
import java.util.*

/**
 * Class that holds global variables, usually of temporary nature
 */
object GlobalManager {

    var invitedContacts = ArrayList<InviteContact>()

    var seenAttachments = setOf<Long>()
        private set

    // Pair with stream id and sender id
    var attachmentsViewingSet = setOf<Pair<Long, Long>>()
        private set


    fun addToAttachmentsViewing(streamId: Long, senderId: Long) {
        val newSet = HashSet(attachmentsViewingSet)
        newSet.add(Pair(streamId, senderId))
        attachmentsViewingSet = newSet
    }

    fun clearAttachmentsViewing() {
        attachmentsViewingSet = setOf<Pair<Long, Long>>()
    }

    fun addToSeenAttachments(streamId: Long) {
        val previousSet = HashSet(seenAttachments)
        previousSet.add(streamId)
        seenAttachments = previousSet

        PrefRepo.seenAttachments = previousSet
    }

    fun removeFromSeenAttachments(streamId: Long) {
        val previousSet = HashSet(seenAttachments)
        previousSet.remove(streamId)
        seenAttachments = previousSet

        PrefRepo.seenAttachments = previousSet
    }

    fun initializeSeenAttachment() {
        val possibleAttachments = PrefRepo.seenAttachments
        if (possibleAttachments != null) {
            seenAttachments = possibleAttachments
        }
    }
}
