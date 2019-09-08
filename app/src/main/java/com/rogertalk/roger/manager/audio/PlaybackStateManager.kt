package com.rogertalk.roger.manager.audio

import android.os.Build
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.event.broadcasts.audio.AudioRouteChangedEvent
import com.rogertalk.roger.models.data.AudioState
import com.rogertalk.roger.models.data.AudioStreamType
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import org.jetbrains.anko.audioManager
import kotlin.properties.Delegates

object PlaybackStateManager {

    // Talk Head specific
    var previousAudioState = AudioState.RECORDING

    // Playback speed control
    val playbackSpeeds = arrayOf(1f, 1.5f, 2f)
    private var playbackSpeedIndex = 0

    var pausedStream: Stream? = null
    var pausedChunk: Chunk? = null
    var pauseRemainingMillis = 0L

    var currentChunk: Chunk? = null

    var currentStream: Stream? by Delegates.observable(pausedStream) {
        prop, old, new ->

        val oldId = old?.id ?: NO_ID
        val newId = new?.id ?: NO_ID
        if (newId != oldId) {
            // Invalidate current chunk
            currentChunk = null
        }

        // Pre-calc audio duration when selected stream changes
        PlaybackCounterManager.showInitialRemainingTime()
    }

    var state = AudioState.IDLE

    val idle: Boolean
        get() = state == AudioState.IDLE

    val playing: Boolean
        get() = state == AudioState.PLAYING

    val bufferingOrPlaying: Boolean
        get() = state == AudioState.BUFFERING || state == AudioState.PLAYING

    val notPlayingNorBuffering: Boolean
        get() = !bufferingOrPlaying

    val recording: Boolean
        get() = state == AudioState.RECORDING

    /**
     * @return During playback, this will return the participant currently speaking.
     * Null if none or not found.
     */
    val participantCurrentChunk: Account?
        get() {
            val currentChunkVal = currentChunk
            val currentStreamVal = currentStream
            if (currentStreamVal != null && currentChunkVal != null && bufferingOrPlaying) {
                val participantTalking = currentStreamVal.othersOrEmpty.firstOrNull { it.id == currentChunkVal.senderId }

                return participantTalking
            }
            return null
        }

    /**
     * Will be true if there is any kind of Audio I/O going on (playback, recording, buffering)
     */
    val doingAudioIO: Boolean
        get() = bufferingOrPlaying || recording


    /**
     * True if either headphones or Bluetooth headset is connected.
     */
    val usingAlternateOutput: Boolean
        get() {
            val manager = appController().audioManager
            return usingBluetooth || manager.isWiredHeadsetOn
        }

    val usingBluetooth: Boolean
        get() {
            val manager = appController().audioManager
            if (AndroidVersion.fromApiVal(Build.VERSION_CODES.JELLY_BEAN_MR2, true)) {
                return manager.isBluetoothA2dpOn || manager.isBluetoothScoOn
            }
            return false
        }

    // Playback modifiers
    var playbackSpeed = 1f

    var usingLoudspeaker: Boolean by Delegates.observable(false) {
        prop, old, new ->
        val newRoute = if (new == true) {
            AudioStreamType.LOUDSPEAKER
        } else {
            AudioStreamType.EARPIECE
        }
        postEvent(AudioRouteChangedEvent(newRoute))
    }

    //
    // PUBLIC METHODS
    //

    /**
     * @return New speed
     */
    fun toggleSpeed(): Float {
        playbackSpeedIndex++
        if (playbackSpeedIndex > (playbackSpeeds.size - 1)) {
            playbackSpeedIndex = 0
        }

        playbackSpeed = playbackSpeeds[playbackSpeedIndex]

        return playbackSpeed
    }
}
