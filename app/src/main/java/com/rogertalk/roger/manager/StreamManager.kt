package com.rogertalk.roger.manager

import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.log.logWarn
import kotlin.properties.Delegates

/**
 * Manages the state of the current stream.
 */
object StreamManager {

    /** The id of the currently selected stream. */
    var selectedStreamId  by Delegates.observable(NO_ID) {
        prop, old, new ->

        // Update stream
        selectedStream = StreamCacheRepo.getStream(new)
        if (selectedStream == null) {
            logWarn { "Stream for provided ID not found!" }
        }
    }

    private val emptyStream: Stream? = null

    /**
     * The currently selected stream's data object.
     */
    var selectedStream: Stream? by Delegates.observable(emptyStream) {
        prop, old, new ->

        // Update playback stream as well if no audio playback
        if (new != null && !PlaybackStateManager.doingAudioIO) {
            PlaybackStateManager.currentStream = StreamCacheRepo.getStream(new.id)
        }
    }

    fun removeStream(streamId: Long) {
        if (streamId == selectedStreamId) {
            selectedStreamId = NO_ID
            selectedStream = null
        }
    }
}