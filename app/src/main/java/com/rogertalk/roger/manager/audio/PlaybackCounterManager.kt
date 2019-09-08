package com.rogertalk.roger.manager.audio

import android.os.SystemClock
import com.rogertalk.roger.event.broadcasts.audio.CounterChangedEvent
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.duration
import com.rogertalk.roger.utils.extensions.fromChunk
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logWarn

/**
 * This class holds the playback time information application-wise.
 * It takes care of throttling the frequency this information is updated, and takes care of
 * notifying the UI about changes.
 */
object PlaybackCounterManager {

    // Cadence of new values acceptance
    private val UPDATE_THRESHOLD = 333L

    private var lastUpdateTimestamp = NO_TIME

    var playerCurrentPosition = 0L

    /**
     * Remaining playback seconds
     */
    val remainingSeconds: Long
        get() {
            return getRemainingTimeSeconds(playerCurrentPosition)
        }

    //
    // PUBLIC METHODS
    //

    fun updateRemainingTime(playerCurrentPosition: Long) {
        this.playerCurrentPosition = playerCurrentPosition
        val now = SystemClock.elapsedRealtime()
        val elapsedTime = now - lastUpdateTimestamp
        if (elapsedTime > UPDATE_THRESHOLD) {
            // Update timestamp
            lastUpdateTimestamp = now

            // Tell app to read the value again
            postEvent(CounterChangedEvent())
        }
    }

    /**
     * Call this when switching to a new stream
     */
    fun showInitialRemainingTime() {
        if (!PlaybackStateManager.doingAudioIO) {
            playerCurrentPosition = 0
        } else {
            logWarn { "Doing Audio I/O, not updating remaining time atm" }
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun getRemainingTimeSeconds(playerCurrentPosition: Long): Long {
        // When recording, time is always 0
        if (PlaybackStateManager.recording) {
            return 0L
        }

        val playbackStream = StreamCacheRepo.getStream(PlaybackStateManager.currentStream?.id)
        val selectedStream = StreamCacheRepo.getStream(StreamManager.selectedStream?.id)
        val pausedStream = PlaybackStateManager.pausedStream
        val stream = playbackStream ?: selectedStream ?: return 0L

        // If NOT currently playing and there this is a paused stream, just use that!
        if (!PlaybackStateManager.doingAudioIO && pausedStream != null && pausedStream.id == stream.id) {
            // Current stream is paused
            return Math.ceil(PlaybackStateManager.pauseRemainingMillis / 1000.0).toLong()
        }

        var chunkList = listOf<Chunk>()

        // Use selected stream since there is not playback one
        if (playbackStream == null) {
            // NOT playing and NOT paused
            if (!stream.unplayed) {
                // If nothing to play, then counter should read 0
                return 0L
            }

            // Use all the chunks since stream is not paused either
            chunkList = stream.unplayedChunks()
        } else {
            // There is a playback stream
            if (PlaybackStateManager.doingAudioIO) {
                val chunk = PlaybackStateManager.currentChunk ?: return (Math.ceil(getChunksToPlay(stream).duration.toLong() / 1000.0).toLong())
                chunkList = stream.chunksFromOthers().fromChunk(chunk)
            } else {
                // In NOT playing, NOT paused and fully played, then counter should read 0
                if (!stream.unplayed) {
                    return 0L
                }

                // Use all the chunks since stream is not paused either
                chunkList = stream.unplayedChunks()
            }
        }

        if (chunkList.isEmpty()) {
            // There were no chunks
            return 0L
        }

        val remainingMilliseconds = Math.max(chunkList.duration - playerCurrentPosition, 0)
        return Math.ceil(remainingMilliseconds / 1000.0).toLong()
    }

    private fun getChunksToPlay(stream: Stream): List<Chunk> {
        if (stream.unplayed) {
            return stream.unplayedChunks()
        }

        return stream.playableChunks()
    }
}
