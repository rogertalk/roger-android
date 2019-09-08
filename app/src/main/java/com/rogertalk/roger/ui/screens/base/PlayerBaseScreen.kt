package com.rogertalk.roger.ui.screens.base

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.IBinder
import android.view.MotionEvent
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import com.rogertalk.kotlinjubatus.*
import com.rogertalk.roger.R
import com.rogertalk.roger.android.services.AudioService
import com.rogertalk.roger.event.broadcasts.audio.AudioServiceStateEvent
import com.rogertalk.roger.event.broadcasts.audio.CounterChangedEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackSource.TAP_AVATAR
import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.AudioState
import com.rogertalk.roger.models.data.AudioState.BUFFERING
import com.rogertalk.roger.models.data.AudioState.PLAYING
import com.rogertalk.roger.models.data.AudioStreamType
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.utils.anim.Shaker
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logWarn
import kotlinx.android.synthetic.main.talk_avatar_generic.*
import kotlinx.android.synthetic.main.talk_screen.*
import kotlinx.android.synthetic.main.talk_screen_audio_controls.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.toast
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Base player screen controls
 */
open class PlayerBaseScreen(override val logOutIfUnauthorized: Boolean) :
        EventAppCompatActivity(logOutIfUnauthorized),
        SpringListener {

    // Audio Service
    private var boundToAudioService = false
    protected var audioService: AudioService? = null

    // State animations for playback
    private var animateAudioViewsFlag = false
    private val unplayedShaker: Shaker by lazy(NONE) { Shaker(distance = 8.0, view = unplayedContainer) }

    private val badgeBlinkAnimation: ValueAnimator by lazy(NONE) { unplayedTimeLabel.createBlinkAnimation() }

    // Spring System
    private val avatarSpring = SpringSystem.create().createSpring()

    //
    // OVERRIDE METHODS
    //

    override fun onResume() {
        super.onResume()

        // Mark app as visible
        AppVisibilityRepo.chatIsForeground = true

        // Update Playback Controls visibility
        updatePlaybackControlsUI()

        // Update Playback speed display
        renderPlaybackSpeed()

        // Reset UI state of listening and recording
        if (PlaybackStateManager.notPlayingNorBuffering) {
            playbackVisualizer.beGone()
        }

        if (audioService != null) {
            // Start proximity for raise-to-talk.
            audioService?.startAutoPlayMonitoring()
        } else {
            logWarn { "AudioService is null" }
        }
    }

    override fun onPause() {
        super.onPause()

        // Mark app as non-visible
        AppVisibilityRepo.chatIsForeground = false

        // Prevent proximity events while app is in background.
        audioService?.stopAutoPlayMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Un-Bind from Service
        if (boundToAudioService) {
            unbindService(audioConnection)
        }

        // Mark app as non-visible (in case of a crash might be called)
        AppVisibilityRepo.chatIsForeground = false
    }

    override fun onStop() {
        super.onStop()

        // Stop shaker if available.
        unplayedShaker.stop()

        // Stop badge blink animation if running
        badgeBlinkAnimation.cancel()
    }

    //
    // Spring Overrides
    //

    override fun onSpringUpdate(spring: Spring) {
        var springValue = spring.currentValue.toFloat()
        val avatarScale = 1f - (springValue * 0.15f)
        avatarContainer.scaleX = avatarScale
        avatarContainer.scaleY = avatarScale

        if (springValue < 0f) {
            springValue = 0f
        }
        val avatarShadowScale = 1f - (springValue * 0.2f)
        avatarContainerShadow.scaleX = avatarShadowScale
        avatarContainerShadow.scaleY = avatarShadowScale
    }

    override fun onSpringAtRest(spring: Spring) {
    }

    override fun onSpringActivate(spring: Spring) {
    }

    override fun onSpringEndStateChange(spring: Spring) {
    }

    //
    // PROTECTED METHODS
    //

    open fun updatePlaybackControlsUI() {
        if (PlaybackStateManager.bufferingOrPlaying) {
            playbackControlsRibbon.makeVisible()
            listeningDramaticBackground.makeVisible()
            recordingButton.beGone()
            playbackControlsRibbon.fadeIn(newDuration = SHORT_ANIM_DURATION)
        } else {
            if (!conversationsList.isVisible()) {
                recordingButton.makeVisible()
            }
            playbackControlsRibbon.fadeOut(SHORT_ANIM_DURATION) { playbackControlsRibbon.beGone() }
            listeningDramaticBackground.beGone()
        }
    }

    /**
     * Remember to call this method right after defining the layout for the screen.
     * Usually during onCreate()
     */
    protected fun postLayoutSetup() {
        bindToAudioService()
        setupUI()
    }

    protected fun handleClicks() {
        // Playback audio controls click listening
        rewindContainer.setOnClickListener {
            audioService?.rewindPlayback()
            EventTrackingManager.pressedRewind()
        }

        skipContainer.setOnClickListener {
            audioService?.skip()
            EventTrackingManager.pressedForward()
        }

        AndroidVersion.fromApi(LOLLIPOP, true) {
            playbackSpeedControl.setOnClickListener { playbackSpeedPressed() }
        }

        avatarContainer.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    avatarSpring.endValue = 1.0
                }
                MotionEvent.ACTION_UP -> {
                    avatarSpring.endValue = 0.0
                    EventTrackingManager.playbackStop(EventTrackingManager.PlaybackStopReason.TAP_AVATAR_INSIDE)
                    if (PlaybackStateManager.bufferingOrPlaying) {
                        stopPlaybackAction()
                    } else {
                        playSelectedStream(TAP_AVATAR)
                    }
                }
            }
            true
        }
    }

    protected fun stopPlaybackAction() {
        audioService?.stopPlayback()
    }

    protected fun renderPlaybackCounter(seconds: Long) {
        if (seconds > 0) {
            unplayedContainer.makeOpaque(cancelAnimations = true)
            unplayedTimeLabel.makeOpaque(cancelAnimations = true)
            unplayedTimeLabel.text = seconds.toString()
        } else {
            unplayedContainer.makeTransparent()
        }

        updateAvatarContainerContentDescription()
    }

    /**
     * Content description varies on Avatar Container to give better action info for TalkBack users.
     */
    open protected fun updateAvatarContainerContentDescription() {
        if (PlaybackStateManager.bufferingOrPlaying) {
            avatarContainer.contentDescription = getString(R.string.ac_tap_to_stop)
            return
        }
    }

    protected fun handleAudioViewAnimationState(newState: AudioState) {
        if (newState == PLAYING || newState == AudioState.RECORDING) {
            animateAudioViewsFlag = true
        } else {
            animateAudioViewsFlag = false
        }
    }

    protected fun updateUnplayedUI(unplayed: Boolean) {
        // Decide whether the badge it's shaking.
        if (PlaybackStateManager.bufferingOrPlaying) {
            unplayedShaker.stop()
        } else {
            if (unplayed) {
                unplayedShaker.start()
            } else {
                unplayedShaker.stop()
            }
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun renderPlaybackSpeed() {
        val speedText = "${PlaybackStateManager.playbackSpeed}x"
        playbackSpeedControl.contentDescription = R.string.ac_playback_speed.stringResource(speedText)
        speedLabel.text = speedText
    }

    private fun playbackSpeedPressed() {
        // Only change if actually playing
        if (PlaybackStateManager.playing) {
            EventTrackingManager.toggledSpeed()

            PlaybackStateManager.toggleSpeed()
            renderPlaybackSpeed()

            audioService?.updateSpeedRuntime()
        } else {
            toast(R.string.audio_playback_speed_cant_change)
        }
    }

    private fun updateBadgeAnimation(state: AudioState) {
        // Blink the timer when buffering.
        if (state == BUFFERING) {
            if (unplayedContainer.alpha == 0f) {
                unplayedContainer.makeOpaque()
            }
            badgeBlinkAnimation.start()
        } else {
            badgeBlinkAnimation.cancel()
        }
    }

    private fun setupUI() {
        // Configure spring and related event listeners
        avatarSpring.springConfig = SpringConfig.fromBouncinessAndSpeed(21.0, 60.0)
        avatarSpring.addListener(this)

        // Visualizer starts hidden
        playbackVisualizer.beGone()

        handleClicks()

        unplayedContainer.makeTransparent()

        // Decide Playback Speed control visibility
        AndroidVersion.toApi(Build.VERSION_CODES.LOLLIPOP, false) {
            playbackSpeedControl.beGone()
            speedLabelBottom.beGone()
        }
    }

    // Audio Service connection

    private fun bindToAudioService() {
        if (audioService != null) {
            return
        }
        // Service should be started before bind.
        startService(AudioService.startEmpty(this))
        boundToAudioService = bindService(Intent(this, AudioService::class.java), audioConnection, Context.BIND_AUTO_CREATE)

        // Register this app to control audio for this particular audio stream.
        volumeControlStream = AudioStreamType.LOUDSPEAKER.intValue
    }

    private val audioConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            logDebug { "AudioService was connected successfully" }
            val binder = service as AudioService.AudioBinder
            audioService = binder.service

            // We need to start this on re-bind as well as onResume, because the user can exit the app with Back key
            // or simply by navigating away from it with Home, change app, etc.
            // Start proximity for raise-to-talk.
            binder.service.startAutoPlayMonitoring()

            // Tell UI to refresh content if playing or recording
            afterAudioServiceBind()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            logError { "Lost connection to AudioService" }
            audioService = null
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onAudioServiceState(event: AudioServiceStateEvent) {
        logDebug { "AudioEvent. new: ${event.newState}, old: ${event.oldState}" }
        updateBadgeAnimation(event.newState)

        // Toggle frame-by-frame updates for playing/recording.
        handleAudioViewAnimationState(event.newState)

        // Display playback visualizer if available
        if (event.newState == PLAYING) {
            playbackVisualizer.makeVisible()
        }
        handleAudioStateChange(event.oldState, event.newState)
    }

    @Subscribe(threadMode = MAIN)
    fun onCounterChanged(event: CounterChangedEvent) {
        renderPlaybackCounter(PlaybackCounterManager.remainingSeconds)
    }

    //
    // Methods that the child classes should override
    //

    open protected fun playSelectedStream(playbackSource: EventTrackingManager.PlaybackSource) {
        //  OVERRIDE IN CHILD CLASS
    }

    open protected fun scaleRecordingVisualizer(scaleLevel: Float) {
        //  OVERRIDE IN CHILD CLASS
    }

    /**
     * Override to handle class-specific logic regarding audio state changes
     */
    open protected fun handleAudioStateChange(oldState: AudioState, newState: AudioState) {
        //  OVERRIDE IN CHILD CLASS
    }

    /**
     * This method gets called after AudioService has successfully bounded
     */
    open protected fun afterAudioServiceBind() {
        //  OVERRIDE IN CHILD CLASS
    }
}
