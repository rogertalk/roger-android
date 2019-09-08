package com.rogertalk.roger.repo

import com.rogertalk.roger.manager.RealTimeManager
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.utils.log.logWarn
import java.util.*

/**
 * Repository that acts upon the conversation status memory cache
 */
object ConversationStatusCacheRepo {

    fun updateStatusForAccount(streamId: Long, accountId: Long, status: StreamStatus,
                               estimatedDuration: Long) {
        val streamToUpdate = StreamCacheRepo.getCached().filter { it.id == streamId }.firstOrNull()
        if (streamToUpdate == null) {
            logWarn { "did NOT find stream for updating RT status" }
            return
        }

        // Send information to RealTime manager
        RealTimeManager.handleNewStreamState(streamId, status)

        // Found stream to update
        for (participant in streamToUpdate.othersOrEmpty) {
            if (participant.id == accountId) {
                participant._conversationStatusInternal = status
                participant.conversationStatusTimestamp = Date().time
                participant.estimatedDuration = estimatedDuration
            }
        }
    }


}
