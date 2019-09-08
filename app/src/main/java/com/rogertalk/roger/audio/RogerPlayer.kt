package com.rogertalk.roger.audio

import android.media.MediaCodec
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import com.google.android.exoplayer.*
import com.google.android.exoplayer.audio.AudioCapabilities
import com.google.android.exoplayer.audio.AudioTrack
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer.upstream.DefaultUriDataSource
import com.google.android.exoplayer.util.PlayerControl
import com.google.android.exoplayer.util.Util
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.android.services.AudioService
import com.rogertalk.roger.helper.audio.PlaybackHelper
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.utils.log.*
import java.io.IOException

/**
 * This is the class that is actually responsible for the playback of a given chunk.
 * It serves as an abstraction on top an instance of ExoPlayer, and it is kept simple and
 * inexpensive so there could be several instances of this class to form a Playlist.
 */
class RogerPlayer(val playerId: Long, val playbackHelper: PlaybackHelper, val chunkId: Long) :
        ExoPlayer.Listener,
        ExtractorSampleSource.EventListener,
        MediaCodecAudioTrackRenderer.EventListener {

    companion object {
        // ExoPlayer
        private val BUFFER_SEGMENT_SIZE = 64 * 1024
        private val BUFFER_SEGMENT_COUNT = 256
    }

    private var exoPlayer: ExoPlayer
    private var playerControl: PlayerControl
    private var audioRenderer: RogerAudioRenderer? = null

    // State variables
    var lastPlayerState = ExoPlayer.STATE_ENDED
    var currentlyPlaying = false

    init {
        exoPlayer = ExoPlayer.Factory.newInstance(1, 1000, 5000)
        exoPlayer.addListener(this)
        playerControl = PlayerControl(exoPlayer)
    }

    val currentPosition: Long
        get() {
            return exoPlayer.currentPosition
        }

    //
    // OVERRIDE METHODS
    //

    // ExoPlayer Callbacks

    override fun onPlayerError(error: ExoPlaybackException?) {
        logError(error)
        playbackHelper.errorOnPlayer(playerId)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_PREPARING -> {
                currentlyPlaying = false
                logVerbose { "[$playerId] Preparing" }
            }

            ExoPlayer.STATE_ENDED -> {
                logVerbose { "[$playerId] Ended" }
                exoPlayer.release()
                playbackHelper.playerFinishedPlayback(playerId)
            }

            ExoPlayer.STATE_BUFFERING -> logVerbose { "[$playerId] Buffering" }

            ExoPlayer.STATE_READY -> {
                logVerbose { "[$playerId] Ready" }

            }

            ExoPlayer.STATE_IDLE -> {
                logVerbose { "[$playerId] Idle" }
            }
        }

        lastPlayerState = playbackState
        playbackHelper.playerNewState(playerId, playbackState)
    }

    override fun onPlayWhenReadyCommitted() {
        logMethodCall()
    }

    // ExoPlayer Audio Media Callbacks

    override fun onAudioTrackInitializationError(e: AudioTrack.InitializationException?) {
        logError(e)
        playbackHelper.errorOnPlayer(playerId)
    }

    override fun onAudioTrackWriteError(e: AudioTrack.WriteException?) {
        logError(e)
        playbackHelper.errorOnPlayer(playerId)
    }

    override fun onAudioTrackUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
        logMethodCall()
    }

    override fun onDecoderInitializationError(e: MediaCodecTrackRenderer.DecoderInitializationException?) {
        logError(e)
        playbackHelper.errorOnPlayer(playerId)
    }

    override fun onDecoderInitialized(decoderName: String?, elapsedRealtimeMs: Long, initializationDurationMs: Long) {
        logInfo { "[$playerId] DecoderInitialized: $decoderName, TimeTookMs: $initializationDurationMs" }
    }

    override fun onCryptoError(e: MediaCodec.CryptoException?) {
        logError(e)
    }

    // Extractor Callbacks

    override fun onLoadError(sourceId: Int, e: IOException?) {
        logError(e)
        playbackHelper.errorOnPlayer(playerId)
    }

    // Other override methods

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as RogerPlayer

        if (playerId != other.playerId) return false
        if (chunkId != other.chunkId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playerId.hashCode()
        result = 31 * result + chunkId.hashCode()
        return result
    }

    //
    // PUBLIC METHODS
    //

    fun prepareAudio(audioURI: Uri, audioService: AudioService, mainHandler: Handler) {
        logDebug { "Preparing URI: ${audioURI.path}" }

        // Start by removing previous exoPlayer listener
        exoPlayer.removeListener(this)
        exoPlayer = ExoPlayer.Factory.newInstance(2, 1000, 5000)
        exoPlayer.addListener(this)
        playerControl = PlayerControl(exoPlayer)

        val allocator = DefaultAllocator(BUFFER_SEGMENT_SIZE)
        val userAgent = Util.getUserAgent(audioService, "Roger")
        val bandwidthMeter = DefaultBandwidthMeter(mainHandler, null)
        val dataSource = DefaultUriDataSource(audioService, bandwidthMeter, userAgent)
        val sampleSource = ExtractorSampleSource(audioURI, dataSource, allocator,
                (BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE).toInt(),
                mainHandler, this, 0)

        audioRenderer = RogerAudioRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, mainHandler, this,
                AudioCapabilities.getCapabilities(audioService), AudioService.AUDIO_STREAM_TYPE.intValue)

        // Prepare playback
        exoPlayer.prepare(audioRenderer)
    }

    fun halt() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    fun setNotCurrentlyPlaying() {
        currentlyPlaying = false
    }

    fun playWhenReady() {
        logDebug { "[$playerId] Will play when ready" }
        exoPlayer.playWhenReady = true
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    /**
     * Will adjust playback speed regarding global settings
     */
    fun adjustPlaybackSpeed() {
        if (AndroidVersion.fromApiVal(Build.VERSION_CODES.M, inclusive = true)) {
            logDebug { "Playback speed set to: ${PlaybackStateManager.playbackSpeed}" }
            val playbackParams = PlaybackParams()
            playbackParams.speed = PlaybackStateManager.playbackSpeed
            exoPlayer.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_PLAYBACK_PARAMS, playbackParams)
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun logError(e: Exception?) {
        e?.let {
            logError(e) { "Error on player $playerId" }
        }
    }
}