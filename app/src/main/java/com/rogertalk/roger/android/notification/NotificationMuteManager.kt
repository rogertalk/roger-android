package com.rogertalk.roger.android.notification

import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.models.data.MuteDuration
import com.rogertalk.roger.models.data.MuteDuration.HOURS_8
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.PrefRepo
import java.util.*

/**
 * Control muted notifications
 */
object NotificationMuteManager {

    /**
     * Map stream id to timestamp of when the restriction ends
     */
    val mutedGroups: HashMap<Long, Long>

    init {
        if (PrefRepo.hasMutedStreams()) {
            mutedGroups = PrefRepo.mutedStreams
        } else {
            mutedGroups = HashMap<Long, Long>(1)
        }
    }


    fun muteStreamForDuration(stream: Stream, duration: MuteDuration) {
        val restrictionEnd = if (duration == HOURS_8) {
            EventTrackingManager.mutedFor8Hours(stream.isGroup)
            Date().time + 28800000
        } else {
            // Mute for a week's period
            EventTrackingManager.mutedFor1Week(stream.isGroup)
            Date().time + 604800000
        }

        mutedGroups.put(stream.id, restrictionEnd)

        // Persist this change
        persist()
    }

    fun unMuteStream(stream: Stream) {
        mutedGroups.remove(stream.id)

        // Persist this change
        persist()
    }

    fun isStreamMuted(streamId: Long): Boolean {
        val currentTime = Date().time
        val savedTime = mutedGroups[streamId] ?: return false

        if (savedTime < currentTime) {
            // This stream was muted, but it is not anymore.
            // Remove this entry.
            mutedGroups.remove(streamId)
            persist()
            return false
        }
        return true
    }


    private fun persist() {
        PrefRepo.mutedStreams = mutedGroups
    }
}
