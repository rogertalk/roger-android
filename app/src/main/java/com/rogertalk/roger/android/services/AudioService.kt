package com.rogertalk.roger.android.services

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.IBinder
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.audio.RecordAudioTask
import com.rogertalk.roger.audio.RecordingEventListener
import com.rogertalk.roger.audio.RecordingEventListener.AudioRecordEvent.*
import com.rogertalk.roger.event.broadcasts.AudioTokenEvent
import com.rogertalk.roger.event.broadcasts.RecordingFinishedBroadcastEvent
import com.rogertalk.roger.event.broadcasts.audio.*
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.helper.audio.*
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.PendingNotificationManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager.currentChunk
import com.rogertalk.roger.manager.audio.PlaybackStateManager.currentStream
import com.rogertalk.roger.manager.audio.PlaybackStateManager.usingAlternateOutput
import com.rogertalk.roger.manager.audio.PlaybackStateManager.usingBluetooth
import com.rogertalk.roger.manager.audio.PlaybackStateManager.usingLoudspeaker
import com.rogertalk.roger.models.data.AudioCommand
import com.rogertalk.roger.models.data.AudioState.*
import com.rogertalk.roger.models.data.AudioStreamType
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.data.VisualizerType
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.SendAudioChunkRequest
import com.rogertalk.roger.network.request.StreamStatusRequest
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.cta.sentToast
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.constant.RuntimeConstants
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runIfConnected
import com.rogertalk.roger.utils.log.*
import com.rogertalk.roger.utils.phone.Vibes
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.powerManager
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class AudioService :
        AudioFocusListener,
        EventService(),
        RecordingEventListener {

    // TODO: Make this a foreground service (add notification to playback).
    // TODO: Handle user disconnecting headphones, and pausing playback (NOISY_INTENT).

    companion object {

        /** The type of the audio streams played by the service. */
        val AUDIO_STREAM_TYPE: AudioStreamType
            get() {
                return if (usingLoudspeaker || usingAlternateOutput) {
                    AudioStreamType.LOUDSPEAKER
                } else {
                    AudioStreamType.EARPIECE
                }
            }

        fun startEmpty(context: Context): Intent {
            return Intent(context, AudioService::class.java)
        }
    }

    class AudioBinder(val service: AudioService) : Binder()

    private val playbackHelper: PlaybackHelper by lazy { PlaybackHelper(this) }

    // Audio focus
    var audioFocusHelper: AudioFocusHelper? = null

    // Wake Locks
    val wakelockHelper: PlaybackWakeLockHelper by lazy(NONE) { initWakeLockManager() }
    private val headphonesClient: HeadphonesHelper by lazy(NONE) { HeadphonesHelper(this) }

    // Auto-Play detection
    private val autoplayHelper: TriggerPlayHelper by lazy(NONE) { TriggerPlayHelper(this) }

    private var audioManager: AudioManager? = null
    private val binder = AudioBinder(this)

    // Current recordings tasks in progress (filepath, task)
    private val recordingTaskStack = LinkedList<RecordAudioTask>()

    private var globalStartTimestamp: Long? = null

    val usingScreenReader: Boolean
        get() = AccessibilityUtils.isScreenReaderActive(this)

    /**
     * Local state of the audio service.
     */
    var state = IDLE
        set(value) {
            logDebug { "AudioService state change: $field -> $value" }
            if (value == PLAYING) {
                // Cancel any pending notification for missed messages
                PendingNotificationManager.unregisterSpokenNotification()
            }

            val oldValue = field
            if (oldValue == value) return

            if (oldValue == BUFFERING || value == PLAYING) {
                // Stop loading sound
                logDebug { "will stop loading sound" }
                playbackHelper.stopLoadingSound()
            }

            field = value

            // Update Global setting
            PlaybackStateManager.state = value

            // Broadcast state change
            postEvent(AudioServiceStateEvent(oldValue, value))
        }


    //
    // OVERRIDE METHODS
    //

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        startAutoPlayMonitoring()

        // Listen for when headphones are plugged in or out
        headphonesClient.registerReceiver()

        // Initialize audio focus manager.
        audioFocusHelper = AudioFocusHelper(audioManager, this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        playbackHelper.onDestroy()

        stopAutoPlayMonitoring()

        // Stop any pending recording tasks
        recordingTaskStack.pollFirst()?.stop()

        // Release WiFi lock
        wakelockHelper.wifiLockRelease()

        // Release refusal for deep-sleep lock while recording
        wakelockHelper.audioIOLockRelease()

        // Don't listen for headphone state changes anymore.
        headphonesClient.unregisterReceiver()

        // Stop connection with bluetooth headset
        if (usingBluetooth) {
            AndroidVersion.fromApi(JELLY_BEAN_MR2, inclusive = true) {
                audioManager?.stopBluetoothSco()
            }
        }
    }

    fun updateSpeedRuntime() {
        playbackHelper.changeSpeedAtRuntimePressed()
    }

    /**
     * Called during recording encoding
     */
    override fun recordingEventCallback(recordEvent: RecordingEventListener.AudioRecordEvent) {
        val isError = when (recordEvent) {
            ERROR_AUDIO_ENCODE -> true
            ERROR_CLOSE_FILE -> true
            ERROR_CREATE_FILE -> true
            ERROR_REC_START -> true
            ERROR_GET_MIN_BUFFER_SIZE -> true
            ERROR_AUDIO_RECORD -> true
            ERROR_RECORD_STEREO -> true
            else -> false
        }

        if (recordEvent == ERROR_RECORD_STEREO) {
            postEvent(AudioRecordAdjustmentEvent())
        }

        if (isError) {
            logError { "There was an error during recording: " + recordEvent.name }

            // Stop any pending recording tasks
            recordingTaskStack.pollFirst()?.stop()

            // Stop any playback tasks as well
            stopPlayback()
        } else {
            logDebug { "RecordEvent: ${recordEvent.name}" }
        }
    }

    //
    // PUBLIC METHODS
    //

    fun switchToEarpiece() {
        if (!PlaybackStateManager.usingLoudspeaker) {
            // Already using earpiece
            return
        }
        // Only switch if buffering or playing
        if (PlaybackStateManager.bufferingOrPlaying) {
            // Only switch if not currently using alternate output
            if (!PlaybackStateManager.usingAlternateOutput) {
                logDebug { "Will switch to earpiece" }
                stopPlayback()

                // Switch to earpiece next
                PlaybackStateManager.usingLoudspeaker = false

                // Restart playback
                playStream()
            }
        }
    }

    fun stopAutoPlayMonitoring() {
        autoplayHelper.stopDetection()

        // Release screen proximity off lock.
        wakelockHelper.screenOffRelease()
    }

    fun startAutoPlayMonitoring() {
        if (PlaybackStateManager.usingAlternateOutput) {
            return
        }

        autoplayHelper.startDetection()
    }

    fun getChunksToPlay(stream: Stream): List<Chunk> {
        if (stream.unplayed) {
            return stream.unplayedChunks()
        }

        // Use last chunk instead
        return listOf(stream.chunksFromOthers().last())
    }

    fun rewindPlayback() {
        playbackHelper.rewindPressed()
    }

    fun skip() {
        playbackHelper.skipPressed()
    }

    /**
     * Will play current stream, if available
     */
    fun playCurrentStream() {
        if (!PlaybackStateManager.bufferingOrPlaying) {
            playStream()
        }
    }

    /**
     * Asks the audio service to play the provided stream
     */
    fun playStream() {
        logMethodCall()
        // Inform app of control route
        postEvent(AudioRouteChangedEvent(AUDIO_STREAM_TYPE))

        val stream = currentStream
        if (stream == null) {
            logWarn { "Stream is null" }
            return
        }

        // Map chunks to stream object
        stream.mapChunksToStream()

        // Reset previous chunk data (could be in pause state)
        currentChunk = null

        // Request AudioFocus
        if (!hasAudioFocus()) {
            audioFocusHelper?.requestAudioFocus()
        }

        playbackHelper.startPlaybackPressed()
    }

    /**
     * Begin recording to a stream. It's an error to record while playing.
     */
    fun startRecording(stream: Stream, createChunkToken: Boolean) {
        // Report status
        runIfConnected(this) {
            StreamStatusRequest(stream.id, StreamStatus.TALKING).enqueueRequest()
        }

        if (state != IDLE) {
            logWarn { "Started a recording before another had finished" }
        }

        if (!hasAudioFocus()) {
            audioFocusHelper?.requestAudioFocus()
        }

        // Play recording bleep.
        playbackHelper.playRecordingStartSound()

        // Create new task for recording.
        val recordingTask = RecordAudioTask(stream, createChunkToken)
        recordingTask.registerCallbackListener(this)
        recordingTask.start()

        // add it to stack of recording
        addTaskToStack(recordingTask)

        globalStartTimestamp = Date().time
        state = RECORDING

        wakelockHelper.audioIOLockAcquire()

        // Turn screen off upon proximity for recording
        if (!PlaybackStateManager.usingAlternateOutput) {
            wakelockHelper.screenOffAcquire()
        }
    }


    /**
     * Asks the audio service to stop playing. If it's not playing, nothing will happen.
     */
    fun stopPlayback() {
        logMethodCall()
        playbackHelper.stopPlaybackPressed()
    }

    /**
     * Asks the audio service to stop recording. If it's not recording, nothing will happen.
     *
     * @return A recording object that contains information about what was recorded.
     */
    fun stopRecording() {
        wakelockHelper.screenOffRelease()

        // Stop holding the wake lock that prevents device from sleep
        wakelockHelper.audioIOLockRelease()

        state = IDLE

        val task = recordingTaskStack.pollFirst()
        if (task == null) {
            logWarn { "Recording task was null" }
            return
        }

        // Report stream status
        runIfConnected(this) {
            StreamStatusRequest(task.stream.id, StreamStatus.IDLE).enqueueRequest()
        }

        // Issue task stop.
        task.stop()

        // Give up audio focus
        audioFocusHelper?.abandonAudioFocus()
    }

    //
    // PRIVATE METHODS
    //

    // Audio Focus implementation

    override fun gainedAudioFocus() {
        logMethodCall()

        // If recording don't go further
        if (state == RECORDING){
            return
        }
        playbackHelper.readyToPlay()
    }

    override fun lostAudioFocus() {
        logInfo { "Lost audio focus" }

        if (!PlaybackStateManager.recording && PlaybackStateManager.playing) {
            // If we lost focus stop playback
            EventTrackingManager.playbackStop(EventTrackingManager.PlaybackStopReason.FINISHED_AUDIO_LOSS)
            stopPlayback()
        }
    }

    fun giveUpAudioFocus() {
        if (hasAudioFocus()) {
            audioFocusHelper?.abandonAudioFocus()
        }
    }

    fun reportStatusListening(streamId: Long, estimatedDuration: Long) {
        StreamStatusRequest(streamId, StreamStatus.LISTENING, estimatedDuration).enqueueRequest()
    }

    fun registerStartPlaybackEvent() {
        val duration = (currentChunk?.duration?.toDouble() ?: 0.0) / 1000f
        val unplayed = currentStream?.unplayed ?: false
        val usingHeadphones = audioManager?.isSpeakerphoneOn ?: false

        EventTrackingManager.playbackStart(duration, unplayed,
                playbackHelper.usingCachedAudio, usingHeadphones, usingBluetooth)
    }

    private fun initWakeLockManager(): PlaybackWakeLockHelper {
        return PlaybackWakeLockHelper(powerManager, getSystemService(Context.WIFI_SERVICE) as WifiManager)
    }

    fun hasAudioFocus(): Boolean {
        return (audioFocusHelper?.hasFocus() ?: true)
    }


    private fun addTaskToStack(recordingTask: RecordAudioTask) {
        // TODO : Terminate existing threads - fix recording in background bug

        // safety check - stack should be empty
        if (recordingTaskStack.isNotEmpty()) {
            val taskToRemove = recordingTaskStack.pop()
            taskToRemove.stop()
            logWarn { "Recording stack was not empty when adding a new element!" }
        }

        recordingTaskStack.push(recordingTask)
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onVisualizerData(event: AudioAmplitudeEvent) {
        if (event.visualizerType == VisualizerType.PLAYBACK) {
            PlaybackCounterManager.updateRemainingTime(playbackHelper.playerPositionMS)
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onHeadphoneStateChange(event: HeadphoneStateEvent) {
        if (event.pluggedIn) {
            stopAutoPlayMonitoring()
        } else {
            startAutoPlayMonitoring()
        }
    }

    /**
     * Recording finished and was successfully encoded to storage.
     */
    @Subscribe(threadMode = MAIN)
    fun onRecordingFinished(event: RecordingFinishedBroadcastEvent) {
        // TODO : Should we only mark the Audio recording as Idle now?

        Vibes.shortVibration()

        // Visually inform user that content has been sent
        sentToast()

        // Get current state, then reset variables.
        this.globalStartTimestamp = null

        if (!event.file.exists()) {
            // We failed to get a recorded audio file.
            logError { "Recording failed (there was no file)." }
            return
        }
        val duration = event.duration.toInt()

        // Discard recordings that are too short.
        if (duration < RuntimeConstants.MINIMUM_RECORD_PERIOD_MILLIS) {
            logWarn { "Discarding recording because it was too short." }
            event.file.delete()
            return
        }

        val stream = StreamCacheRepo.getStream(event.streamId)
        if (stream != null) {
            if (stream.participants.any { it.id == RogerConstants.DIRECT_SHARE_ACCOUNT_ID }) {
                logDebug { "Talking to DirectShare" }
                postEvent(SavedAudioFileEvent(event.file.name))
                return
            }
        }

        // Send audio, and optionally also propagate the generated token to the rest of the app
        if (event.createChunkToken) {
            with(event) {
                // Sound clip with token, in a 1-1 conversation
                val token = SendAudioChunkRequest(streamId, file, duration,
                        persist = persistAudio).enqueueRequestForToken()
                // Broadcast event immediately
                postEvent(AudioTokenEvent(event.streamId, token))
            }
        } else {
            with(event) {
                SendAudioChunkRequest(streamId, file, duration,
                        persist = persistAudio).enqueueRequest()
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onStreamsChanged(event: StreamsChangedEvent) {
        logEvent(event)

        // Take this opportunity to update the currently playing stream
        if (PlaybackStateManager.bufferingOrPlaying) {
            val curStreamId = currentStream?.id ?: return
            val newlyUpdatedStream = StreamCacheRepo.getStream(curStreamId)
            if (newlyUpdatedStream != null) {
                currentStream = newlyUpdatedStream
                logVerbose { "UPDATED CURRENT PLAYING STREAM!" }
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioCommand(event: AudioCommandEvent) {
        val command = event.audioCommand
        logDebug { "AudioCommand: ${command.name}" }

        when (command) {

            AudioCommand.PLAY -> {
                usingLoudspeaker = true
                playStream()
            }
            AudioCommand.STOP_PLAYING -> {
                EventTrackingManager.playbackStop(event.playbackStopReason)
                stopPlayback()
            }
            AudioCommand.RECORD -> {
                val stream = StreamManager.selectedStream ?: return
                startRecording(stream, false)
            }
            AudioCommand.STOP_RECORDING -> {
                EventTrackingManager.recordingCompleted(event.recordingStopReason)
                stopRecording()
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onSoundojiPlayCommand(event: SoundojiPlayEvent) {
        logEvent(event)
        playbackHelper.playSoundoji(event.type)

        // Track this event
        EventTrackingManager.usedSoundoji(event.type)
    }
}
