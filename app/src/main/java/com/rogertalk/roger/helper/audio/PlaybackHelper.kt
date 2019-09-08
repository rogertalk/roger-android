package com.rogertalk.roger.helper.audio

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer.ExoPlayer
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.services.AudioService
import com.rogertalk.roger.audio.RogerPlayer
import com.rogertalk.roger.event.broadcasts.audio.ChunksSwappedEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager.currentChunk
import com.rogertalk.roger.manager.audio.PlaybackStateManager.currentStream
import com.rogertalk.roger.manager.audio.PlaybackStateManager.notPlayingNorBuffering
import com.rogertalk.roger.manager.audio.PlaybackStateManager.usingAlternateOutput
import com.rogertalk.roger.manager.audio.PlaybackStateManager.usingLoudspeaker
import com.rogertalk.roger.models.data.AudioMode
import com.rogertalk.roger.models.data.AudioState
import com.rogertalk.roger.models.data.AudioState.*
import com.rogertalk.roger.models.data.SoundojiType
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.StreamStatusRequest
import com.rogertalk.roger.network.request.StreamUpdatePlayedUntilRequest
import com.rogertalk.roger.realm.CachedAudioRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.afterChunk
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runIfConnected
import com.rogertalk.roger.utils.log.*
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class PlaybackHelper(val audioService: AudioService) {

    companion object {
        // Milliseconds to rollback when pausing/playing
        private val AUDIO_PAUSE_REWIND = 1800
        private val REWIND_SECONDS_DEFAULT = 5
    }

    val AUDIO_MODE: AudioMode
        get() {
            return if (PlaybackStateManager.usingLoudspeaker || PlaybackStateManager.usingAlternateOutput) {
                AudioMode.NORMAL
            } else {
                AudioMode.IN_COMMUNICATION
            }
        }

    private var audioManager: AudioManager? = null

    // ExoPlayer specific
    private val mainHandler = Handler()

    // We can use single audio URL instead of a stream and chuck for playback
    private var currentAudioURL: String? = null
    private var audioURLDuration = 0

    // Chunk order
    private var previousChunk: Chunk? = null
    private var previousPlaybackTime = 0L

    // Beeps and other sounds
    private val bleepsHelper: BleepsHelper by lazy(LazyThreadSafetyMode.NONE) { BleepsHelper(audioManager) }
    private var bufferTimer: Timer? = null

    // MediaSession
    private val mediaSession: MediaSessionHelper by lazy { MediaSessionHelper(audioService) }
    private var waitingForFocus = true

    // Playlist
    private val playlist = LinkedList<RogerPlayer>()
    private var playerIDCounter = 0L

    // Data for analytics
    var usingCachedAudio = false

    init {
        audioManager = audioService.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Setup bleeps
        bleepsHelper.loadSounds(audioService)
    }

    //
    // Playlist related code
    //

    val currentPlayer: RogerPlayer?
        get() {
            return playlist.firstOrNull()
        }

    val playerPositionMS: Long
        get() {
            return currentPlayer?.currentPosition ?: 0L
        }

    val currentPlaybackState: Int
        get() {
            val curPlayer = currentPlayer
            return if (curPlayer != null) {
                curPlayer.lastPlayerState
            } else {
                ExoPlayer.STATE_IDLE
            }
        }

    fun stopAllPlayback() {
        logVerbose{"Stopping all playback"}
        // Stop and recycle all players
        for (player in playlist) {
            player.halt()
        }
        playlist.clear()
    }

    fun stopCurrentPlayer() {
        currentPlayer?.halt()
        destroyCurrentPlayer()
    }

    fun destroyCurrentPlayer() {
        logMethodCall()
        if (playlist.isNotEmpty()) {
            playlist.removeAt(0)
        }
    }

    fun stopAndDestroyPlayer(playerID: Long){
        val player = playlist.firstOrNull { it.playerId == playerID } ?: return
        player.halt()
        playlist.remove(player)
    }

    /**
     * Validate if the provided player id belong to the current player
     */
    private fun isCurrentPlayer(playerID: Long): Boolean {
        val curPlayer = currentPlayer ?: return false
        return curPlayer.playerId == playerID
    }

    // RogerPlayer Callbacks

    fun errorOnPlayer(playerID: Long) {
        logError { "Error on player: $playerID" }
        if (!isCurrentPlayer(playerID)) {
            stopAndDestroyPlayer(playerID)
            return
        }

        logError { "Player with error is the current one, stopping all playback first" }
        // Get info relevant for handling
        val playerChunkId = currentPlayer?.chunkId

        if (playerChunkId == null){
            logError { "Chunk ID was null!" }
        }
        val chunk = currentStream?.chunks?.firstOrNull { it.id == playerChunkId }

        // Invalidate the whole current playlist
        stopAllPlayback()
        stopPlaybackPressed()

        if (chunk == null){
            logError { "Could not find chunk for this player" }
            return
        }

        // Was audio cached offline?
        if (isAudioOffline(chunk)){
            logVerbose { "Audio was cached, will try using the online version" }
            invalidateAudioFile(chunk)
            currentChunk = null
            previousChunk = null
            nextChunk()
            return
        }

        logVerbose { "Skipping this chunk for good" }
        nextChunk()
    }

    fun playerFinishedPlayback(playerID: Long) {
        if (!isCurrentPlayer(playerID)) {
            return
        }
        finishedChunkPlayback()
    }

    fun playerNewState(playerID: Long, newState: Int) {
        if (!isCurrentPlayer(playerID)) {
            return
        }

        logVerbose { "New state for current player[$playerID]: $newState" }

        when (newState) {
            ExoPlayer.STATE_READY -> {
                if (audioService.hasAudioFocus()) {
                    waitingForFocus = false
                    readyToPlay()
                } else {
                    waitingForFocus = true
                }
            }

            ExoPlayer.STATE_IDLE -> {
                audioService.state = AudioState.IDLE
            }
        }
    }

    //
    // OVERRIDE METHODS
    //


    //
    // PUBLIC METHODS
    //

    fun skipChunk(currentChunk: Chunk) {
        // To skip we simply jump to the end of the current chunk, therefore reusing the existing infrastructure
        currentPlayer?.seekTo(currentChunk.duration * 1000L)
    }

    fun startPlaybackPressed() {
        initPlaybackSetup()

        if (PlaybackStateManager.pausedStream == currentStream) {
            val chuckToPlayFrom = PlaybackStateManager.pausedChunk
            if (chuckToPlayFrom != null) {
                EventTrackingManager.resumePlayback()
                logDebug { "Will resume playback" }
                playFromChunk(chuckToPlayFrom, true)
                return
            }
        }

        // We're already playing this stream. Don't rebuild the player, but ensure it's playing.
        if (currentPlaybackState == ExoPlayer.STATE_READY) {
            logWarn { "Already playing!?" }
            return
        }

        // Reset previous chunk data (could be in pause state from another stream)
        currentChunk = null

        nextChunk()
    }

    fun playRecordingStartSound() {
        bleepsHelper.playRecordingSound()
    }

    fun stopLoadingSound() {
        bleepsHelper.stopLoadingSound()
    }

    fun playSoundoji(type: SoundojiType) {
        when (type) {
            SoundojiType.LAUGHING -> bleepsHelper.playSoundojiLaughing()
            SoundojiType.AWKWARD_CRICKET -> bleepsHelper.playSoundojiAwkwardCricket()
            SoundojiType.RIMSHOT -> bleepsHelper.playSoundojiRimShot()
        }
    }

    /**
     * Should call this once we have audio focus, and player is prepared
     */
    fun readyToPlay() {
        logMethodCall()

        val curPlayer = currentPlayer

        if (curPlayer == null) {
            logWarn { "Current Player was null" }
            return
        }

        if (arrayOf(ExoPlayer.STATE_ENDED, ExoPlayer.STATE_IDLE).contains(currentPlaybackState)) {
            logVerbose { "Player did not go trough preparation yet" }
            return
        }

        // Do we have Audio Focus
        if (waitingForFocus) {
            logVerbose { "Was waiting for focus, ignoring" }
            return
        }

        if (curPlayer.currentlyPlaying) {
            logVerbose { "Already playing" }
            return
        }

        // WAKELOCKS
        // Acquire WiFi lock if audio is not cached
        currentChunk?.let {
            if (!isAudioOffline(it)) {
                audioService.wakelockHelper.wifiLockAcquire()
            }
        }

        // Screen turn OFF on proximity but not if wearing headphones
        if (!PlaybackStateManager.usingAlternateOutput) {
            audioService.wakelockHelper.screenOffAcquire()
        }

        // Don't go to sleep
        audioService.wakelockHelper.audioIOLockAcquire()

        // Set playback speed if different than regular speed
        curPlayer.adjustPlaybackSpeed()

        // Set Media Session
        currentStream?.let {
            mediaSession.playing(it)
        }

        // Track playback
        audioService.registerStartPlaybackEvent()

        // Are we actually resuming this audio though?
        if (previousChunk == PlaybackStateManager.currentChunk) {
            logVerbose { "Resuming playback instead of playing from the start" }
            // Rewind the audio a little so the user perceives the addContactActivity of where it paused
            val newTime = previousPlaybackTime - AUDIO_PAUSE_REWIND
            if (newTime > 0) {
                curPlayer.seekTo(newTime)
            }

            // Clear temporary vars
            PlaybackStateManager.pausedStream = null
            PlaybackStateManager.pausedChunk = null
            previousChunk = null
        }

        // Report listening status to server
        currentStream?.let {
            audioService.reportStatusListening(it.id, PlaybackCounterManager.remainingSeconds * 1000)
        }

        // Play ASAP
        curPlayer.currentlyPlaying = true
        curPlayer.playWhenReady()
    }

    fun rewindPressed() {
        val stream = currentStream ?: return
        val chunk = PlaybackStateManager.currentChunk ?: return

        rewindDefault(stream, chunk)
    }

    fun skipPressed() {
        logMethodCall()
        val chunk = PlaybackStateManager.currentChunk ?: return

        // Advance chunk
        skipChunk(chunk)
    }

    fun stopPlaybackPressed() {
        logMethodCall()

        mediaSession.stopped()

        bleepsHelper.stopLoadingSound()

        // Next time wait for audio focus again
        waitingForFocus = true
        currentPlayer?.setNotCurrentlyPlaying()

        // Report IDLE status to backend
        val streamId = currentStream?.id ?: NO_ID
        if (streamId != NO_ID) {
            runIfConnected(audioService) {
                StreamStatusRequest(streamId, StreamStatus.IDLE).enqueueRequest()
            }
        }

        // Release screen proximity off lock.
        audioService.wakelockHelper.screenOffRelease()

        // Release audio IO lock
        audioService.wakelockHelper.audioIOLockRelease()

        // Release wifi lock.
        audioService.wakelockHelper.wifiLockRelease()

        // If not buffering nor playing there's nothing else to do.
        if (PlaybackStateManager.notPlayingNorBuffering) {
            logVerbose { "Not playing, cease" }
            return
        }

        // Stop timer now
        bufferTimer?.cancel()
        bufferTimer = null

        // Reset loudspeaker setting so the system doesn't use it as default.
        resetAudioSource()

        // Clear any pending playing notifications
        NotificationsHandler.clearPlayingNotification()

        val curPlayer = currentPlayer

        // PAUSE instead? Only if not forced reset
        if (curPlayer != null && currentPlaybackState == ExoPlayer.STATE_READY) {
            logDebug { "Playing. Gonna pause to resume later" }
            PlaybackStateManager.pausedStream = currentStream
            PlaybackStateManager.pausedChunk = currentChunk
            PlaybackStateManager.pauseRemainingMillis = PlaybackCounterManager.remainingSeconds * 1000
            previousChunk = PlaybackStateManager.currentChunk
            previousPlaybackTime = curPlayer.currentPosition

            // Stop players
            stopAllPlayback()

            val remainingDuration = PlaybackStateManager.pauseRemainingMillis / 1000
            val secondsIn = previousPlaybackTime / 1000L
            logDebug { "SecondsIn $secondsIn , Remaining: $remainingDuration" }
            EventTrackingManager.pausedPlayback(secondsIn, remainingDuration)

            audioService.state = AudioState.IDLE
            audioService.giveUpAudioFocus()
            return
        }

        // Play stop sound if playback is not loudspeaker
        if (!PlaybackStateManager.usingLoudspeaker || audioService.usingScreenReader) {
            bleepsHelper.playDoneSound()
        }

        // Stop players
        stopAllPlayback()

        PlaybackStateManager.pausedStream = null
        PlaybackStateManager.pausedChunk = null
        PlaybackStateManager.currentStream = null
        PlaybackStateManager.currentChunk = null
        currentAudioURL = null
        audioURLDuration = 0

        // Update Audio Service state
        audioService.state = AudioState.IDLE
        audioService.giveUpAudioFocus()
    }

    fun changeSpeedAtRuntimePressed() {
        if (notPlayingNorBuffering) {
            return
        }
        currentPlayer?.adjustPlaybackSpeed()
    }

    fun onDestroy() {
        stopAllPlayback()
        bleepsHelper.releaseManager()

        // Release MediaSession handling
        mediaSession.destroy()
    }

    //
    // PRIVATE METHODS
    //

    private fun buildBufferTimer() {
        if (bufferTimer != null) {
            logInfo { "There was already a bufferTimer" }
            return
        }
        if (usingAlternateOutput || !usingLoudspeaker) {
            bleepsHelper.playLoadingSound()
        }
        bufferTimer = Timer()
        bufferTimer?.scheduleAtFixedRate(100, 100) {
            synchronized(this) {
                val curPosition = currentPlayer?.currentPosition ?: 0L
                if (curPosition > 0) {
                    audioService.state = PLAYING
                    bufferTimer?.cancel()
                    bufferTimer = null
                }
            }
        }
    }

    private fun finishedChunkPlayback() {
        val streamId = currentStream?.id
        val chunkEnd = PlaybackStateManager.currentChunk?.end

        // Hide temporary stream upon completion
        StreamCacheRepo.properlyHideGhost()

        if (streamId != null && chunkEnd != null) {
            // Update listened until locally
            currentStream?.let {

                with(it) {
                    val ghostId = StreamCacheRepo.temporaryStream?.id ?: NO_ID
                    if (it.id != ghostId) {
                        // Create a copy with updated playedUntil field
                        val updatedStream = Stream(id, chunks, created,
                                customImageURL, customTitle, lastPlayedFrom,
                                othersListened, chunkEnd, totalDuration, lastInteraction,
                                visible, others, joined, service, inviteToken, attachments)

                        // Update local cache
                        logDebug { "Updating listed-to local cache" }
                        StreamCacheRepo.updateStreamInStreams(updatedStream)
                    }
                }
            }

            val lastChunkId = currentStream?.chunksFromOthers()?.last()?.id ?: NO_ID
            val currentChunkId = PlaybackStateManager.currentChunk?.id ?: NO_ID
            if (lastChunkId != NO_ID && (lastChunkId == currentChunkId)) {
                // Update listened until on the server if this is the last chunk of the bunch
                // that is also not from the current user
                runIfConnected(audioService) {
                    StreamUpdatePlayedUntilRequest(streamId, chunkEnd).enqueueRequest()
                }

                // Clear any reminder notification for this stream
                NotificationsHandler.clearReminderExpiringNotification(streamId)
            }
        }

        // Remove this player so we can move to the next one
        destroyCurrentPlayer()

        nextChunk()
    }

    /**
     * Rollbacks playback for the specified requested time.
     */
    private fun rewindDefault(currentStream: Stream, currentChunk: Chunk) {
        logMethodCall()
        val curPlayer = currentPlayer ?: return
        val resultingPosition = curPlayer.currentPosition - (1000 * REWIND_SECONDS_DEFAULT)

        if (resultingPosition >= 0L) {
            // Rollback playback on same chunk
            rollbackSameChunk(resultingPosition)
            return
        }

        // We need to rollback to a previous chunk, if possible
        val allChunks = currentStream.chunksFromOthers()

        val currentChunkPos = allChunks.indexOf(currentChunk)
        if (currentChunkPos < 1) {
            // Just rollback this chunk since there are no chunks before this one
            rollbackSameChunk(0L)
            return
        }

        // There's a chunk before this one take 5 seconds from it
        val previousChunk = allChunks[currentChunkPos - 1]

        // Stop playback of current chunk first
        stopCurrentPlayer()
        audioService.state = IDLE

        playFromChunk(previousChunk, false)
    }

    /**
     * Start playback from a specific chunk
     */
    private fun playFromChunk(chunk: Chunk, resuming: Boolean) {
        logMethodCall()
        // TODO support resuming playback of a paused stream

        val stream = currentStream ?: return

        // Select chunk to play from
        PlaybackStateManager.currentChunk = chunk
        if (PlaybackStateManager.notPlayingNorBuffering) {
            audioService.state = AudioState.BUFFERING
        }

        // Stop any previous loading sounds
        stopLoadingSound()

        // Play it
        stopAllPlayback()
        updatePlayers()
    }

    private fun rollbackSameChunk(resultingPosition: Long) {
        // Rollback playback on same chunk
        currentPlayer?.seekTo(resultingPosition)
    }

    /**
     * Selects the next chunk in the current stream and plays it. If there is no next chunk, stops
     * playback.
     */
    private fun nextChunk() {
        logMethodCall()

        val stream = currentStream ?: return
        val previousChunk = PlaybackStateManager.currentChunk

        // TODO : test if chunk belonged to this stream?

        if (previousChunk == null) {
            // Use first playable chunk then
            PlaybackStateManager.currentChunk = audioService.getChunksToPlay(stream).firstOrNull()
        } else {
            // Audio state is now buffering again
            if (PlaybackStateManager.notPlayingNorBuffering) {
                audioService.state = BUFFERING
            }

            // Move on to the next chunk
            val nextChunk = stream.chunksFromOthers().afterChunk(previousChunk).firstOrNull()
            PlaybackStateManager.currentChunk = nextChunk
        }

        // In case there are is no next chunk to play, stop playback altogether
        if (PlaybackStateManager.currentChunk == null) {
            logDebug { "No more chunks to play" }

            // Track this event
            EventTrackingManager.playbackStop(EventTrackingManager.PlaybackStopReason.FINISHED_SUCCESS)

            stopPlaybackPressed()
            return
        }

        // Now that the state is up-to-date, handle players
        updatePlayers()
    }

    private fun updatePlayers() {
        val currentChunk = PlaybackStateManager.currentChunk ?: return
        val stream = currentStream ?: return

        if (playlist.isEmpty()) {
            buildBufferTimer()
            preparePlayerWithChunk(currentChunk)
        } else {
            // Start next player if available and not yet player
            if (playlist.isNotEmpty()) {
                currentPlayer?.playWhenReady()
            }
        }

        // Is there a next chunk?
        val nextChunk = stream.chunksFromOthers().afterChunk(currentChunk).firstOrNull()

        if (nextChunk != null) {
            logVerbose { "There is a next chunk" }
            if (playlist.any { it.chunkId == nextChunk.id }) {
                logVerbose { "Next chunk was already part of this playlist" }
            } else {
                logVerbose { "This is really a new chunk, add it to the playlist" }
                preparePlayerWithChunk(nextChunk)
            }
        } else {
            logVerbose { "There is no next chunk" }
        }
    }

    private fun preparePlayerWithChunk(chunk: Chunk) {
        logMethodCall()

        if (!audioService.hasAudioFocus()) {
            audioService.audioFocusHelper?.requestAudioFocus()
            logVerbose { "Waiting for audio focus still" }
            return
        }

        // Signal UI that a new chunk will now play
        // TODO : this event doesn't belong here anymore
        postEvent(ChunksSwappedEvent())

        // Create a new Player
        val rogerPlayer = RogerPlayer(playerIDCounter, this, chunk.id)
        // Increment playerId Counter
        playerIDCounter++

        // Add to playlist
        playlist.add(rogerPlayer)

        // Tell player to prepare its audio
        rogerPlayer.prepareAudio(getAudioUri(chunk), audioService, mainHandler)
    }

    private fun initPlaybackSetup() {
        updateAudioMode()

        if (PlaybackStateManager.usingAlternateOutput || !PlaybackStateManager.usingLoudspeaker || audioService.usingScreenReader) {
            if (!PlaybackStateManager.playing) {
                bleepsHelper.playEarBleepSound()
            }
        }
    }

    private fun updateAudioMode() {
        logDebug { "Will use audio mode type: ${AUDIO_MODE.name}" }
        audioManager?.mode = AUDIO_MODE.intValue

        if (PlaybackStateManager.usingAlternateOutput) {
            logDebug { "Using alternate output" }
            if (PlaybackStateManager.usingBluetooth) {
                logDebug { "Bluetooth off call available: ${audioManager?.isBluetoothScoAvailableOffCall}" }
                logDebug { "Bluetooth A2DP on: ${audioManager?.isBluetoothA2dpOn}" }
                logDebug { "Bluetooth SCO on: ${audioManager?.isBluetoothScoOn}" }
            }
            // Don't proceed if headphones or bluetooth headset connected.
            return
        }

        // Ensure audio focus
        if (!audioService.hasAudioFocus() && !(audioService.audioFocusHelper?.requestAudioFocus() ?: false)) {
            logWarn { "No audio focus, can't change audio source" }
            // No audio focus, can't change audio source
            return
        }

        val useLoudspeaker = PlaybackStateManager.usingLoudspeaker
        logInfo { "Will use loudspeaker: $useLoudspeaker" }

        audioManager?.isSpeakerphoneOn = useLoudspeaker
    }

    private fun resetAudioSource() {
        logMethodCall()
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false
    }

    private fun getAudioUri(chunk: Chunk): Uri {
        val record = CachedAudioRepo.getRecord(chunk.id, chunk.streamId)
        if (record != null && !record.persistPath.isNullOrBlank()) {
            usingCachedAudio = true
            return Uri.parse(record.persistPath)
        } else {
            usingCachedAudio = false
            return Uri.parse(chunk.audioURL)
        }
    }

    private fun isAudioOffline(chunk: Chunk): Boolean {
        val record = CachedAudioRepo.getRecord(chunk.id, chunk.streamId)
        if (record != null && !record.persistPath.isNullOrBlank()) {
            return true
        } else {
            return false
        }
    }

    /**
     * Tries to invalidate the current cached audio file, if for some reason it became unplayable (or not found).
     * @return True if file reference was found and deleted, False otherwise
     */
    private fun invalidateAudioFile(chunk: Chunk): Boolean {
        // Was using cached audio, will invalidate the media file as it might have become corrupt somehow
        val streamId = currentStream?.id
        val audioURL = chunk.audioURL
        if (streamId != null) {
            logInfo { "Will invalidate chunk ${chunk.id} on stream $streamId" }
            CachedAudioRepo.deleteRecord(chunk.id, streamId, audioURL)
            return true
        }
        return false
    }


}