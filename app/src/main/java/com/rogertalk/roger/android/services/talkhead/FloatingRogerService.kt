package com.rogertalk.roger.android.services.talkhead

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.*
import com.bumptech.glide.Glide
import com.rogertalk.kotlinjubatus.*
import com.rogertalk.roger.R
import com.rogertalk.roger.android.notification.FloatingNotificationManager
import com.rogertalk.roger.android.services.EventService
import com.rogertalk.roger.event.broadcasts.AppVisibilityChangeEvent
import com.rogertalk.roger.event.broadcasts.GroupAvatarReadyEvent
import com.rogertalk.roger.event.broadcasts.audio.*
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackSource.AUTOPLAY_TALKHEAD
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackSource.TAP_TALKHEAD
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackStopReason.TAP_AVATAR_TALKHEAD
import com.rogertalk.roger.manager.EventTrackingManager.RecordingReason.TAP_TALK_HEAD
import com.rogertalk.roger.manager.GroupAvatarManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.AudioCommand
import com.rogertalk.roger.models.data.AudioState
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.VisualizerType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.ClearTextPrefRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.MaterialIcon.*
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiDelayed
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.*
import com.rogertalk.roger.utils.phone.Vibes
import kotlinx.android.synthetic.main.talk_head.view.*
import kotlinx.android.synthetic.main.talk_head_cta_left_side.view.*
import kotlinx.android.synthetic.main.talk_head_cta_right_side.view.*
import kotlinx.android.synthetic.main.talk_head_trash.view.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.windowManager

class FloatingRogerService : EventService(),
        ViewTreeObserver.OnGlobalLayoutListener {

    companion object {

        private val CLICK_TIME_THRESHOLD_MILLIS = 200
        private val DOUBLE_TAP_DELAY = 400L

        private val DEBUGGING_DOT = false

        // After this distance, the movement alone will invalidate a candidate 'tap'
        private val INVALIDATION_DISTANCE = 30

        private val EXTRA_NOTIFICATION_SOUND = "notificationSound"

        fun start(context: Context): Intent {
            val startIntent = Intent(context, FloatingRogerService::class.java)
            return startIntent
        }

        /**
         * Start Floating Service with a pre-selected streamID
         */
        fun startWithStream(context: Context, streamId: Long): Intent {
            val startIntent = Intent(context, FloatingRogerService::class.java)
            startIntent.putExtra(EXTRA_NOTIFICATION_SOUND, true)
            StreamManager.selectedStreamId = streamId
            return startIntent
        }

    }

    // State
    private var firstStart = true

    // UI Components
    private val floatingViewProps = FloatingViewsProps()
    private var floatingHeadView: View? = null
    private var trashView: View? = null
    private var ctaLeftSide: View? = null
    private var ctaRightSide: View? = null
    private var redDotView: View? = null

    private val LAYOUT_TYPE: Int
        get() {
            return if (Build.VERSION.SDK_INT < 19) {
                WindowManager.LayoutParams.TYPE_PHONE
            } else {
                WindowManager.LayoutParams.TYPE_TOAST
            }
        }

    // Visualizer
    private var audioLevel = 0.0
    private var audioLevelPrevious = 0.0

    // Touch-related
    private var dragStartedAt = NO_TIME
    private var preClickCount = 0
    private var movementDetected = false

    // We use this to ignore taps for a period of time
    private var coolDownTimestamp = 0L

    // Trash detection
    private val TRASH_PROXIMITY = 200
    private var collidedWithTrash = false

    //
    // OVERRIDE METHODS
    //

    override fun onCreate() {
        super.onCreate()
        logMethodCall()

        // Load the various UI elements
        loadLeftSideCTA()
        loadRightSideCTA()
        loadTrashCan()
        loadMainElem()
        if (DEBUGGING_DOT) {
            loadDebuggingRedDot()
        }

        // Hide Trashcan and CTAs upon start
        trashView?.beGone()
        ctaLeftSide?.beGone()
        ctaRightSide?.beGone()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logMethodCall()

        if (intent == null) {
            logWarn { "Intent was null" }
            return START_STICKY
        }

        val shouldUseSound = intent.getBooleanExtra(EXTRA_NOTIFICATION_SOUND, false)

        if (firstStart) {
            // TODO : Update display for number of unread stuff here?

            startForeground(FloatingNotificationManager.NOTIFICATION_ID,
                    FloatingNotificationManager.buildNotification(this,
                            shouldUseSound))

            firstStart = false
        } else {
            Vibes.shortVibration()
        }

        // Update UI
        renderMainUI()

        // Only draw CTA upon start if state differs from previous one
        if (PlaybackStateManager.state != PlaybackStateManager.previousAudioState) {
            renderCTA()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // Remove the various UI components
        try {
            if (floatingHeadView != null) {
                windowManager.removeView(floatingHeadView)
            }
            if (ctaLeftSide != null) {
                windowManager.removeView(ctaLeftSide)
            }
            if (ctaRightSide != null) {
                windowManager.removeView(ctaRightSide)
            }
            if (trashView != null) {
                windowManager.removeView(trashView)
            }
            if (redDotView != null) {
                windowManager.removeView(redDotView)
            }
        } catch (e: IllegalStateException) {
            logError(e)
        }

        super.onDestroy()
    }

    // TODO : Move this to TalkHeadGlobalLayoutListeners
    override fun onGlobalLayout() {
        trashView?.let {
            // Remove listener now
            if (Build.VERSION.SDK_INT < 16) {
                it.viewTreeObserver.removeGlobalOnLayoutListener(this)
            } else {
                it.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }

            // Finish trash can initialization and other parameters
            val params = it.layoutParams as WindowManager.LayoutParams

            params.gravity = Gravity.TOP or Gravity.LEFT

            it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val ownWidth = trashView?.measuredWidth ?: 0
            val ownHeight = trashView?.measuredHeight ?: 0
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            params.x = screenWidth / 2 - (ownWidth / 2)
            params.y = screenHeight - 340

            windowManager.updateViewLayout(it, params)
            floatingViewProps.refreshTrashData(it)
        }
    }

    //
    // PUBLIC METHODS
    //

    fun resetCTA() {
        // Render CTA again
        renderCTA()
    }

    fun refreshHeadProperties() {
        floatingHeadView?.let {
            floatingViewProps.refreshHeadData(it, windowManager)
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun resetAudioLevels() {
        audioLevel = 0.0
        audioLevelPrevious = 0.0
    }

    private fun loadMainElem() {
        // Load Main Floating Element
        floatingHeadView = layoutInflater.inflate(R.layout.talk_head, null, false)

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = ClearTextPrefRepo.floatingLastX
        params.y = ClearTextPrefRepo.floatingLastY

        // As soon as the main TalkHead finishes loading, redraw components that are dependent on it
        floatingHeadView?.let {
            it.viewTreeObserver?.
                    addOnGlobalLayoutListener(FloatingUIGlobalLayoutListeners.TalkHeadLayoutListener(this))
        }

        windowManager.addView(floatingHeadView, params)
        makeDraggable(params)

        // Draw contents of selected stream
        renderMainUI()
    }

    private fun loadDebuggingRedDot() {
        redDotView = layoutInflater.inflate(R.layout.red_dot, null, false)

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        // Position is the same as the main talk head element
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0
        params.y = 0

        windowManager.addView(redDotView, params)
    }

    private fun loadLeftSideCTA() {
        ctaLeftSide = layoutInflater.inflate(R.layout.talk_head_cta_left_side, null, false)

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        // Position is the same as the main talk head element
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = ClearTextPrefRepo.floatingLastX
        params.y = ClearTextPrefRepo.floatingLastY

        windowManager.addView(ctaLeftSide, params)
    }

    private fun loadRightSideCTA() {
        ctaRightSide = layoutInflater.inflate(R.layout.talk_head_cta_right_side, null, false)

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        // Position is the same as the main talk head element
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = ClearTextPrefRepo.floatingLastX
        params.y = ClearTextPrefRepo.floatingLastY

        windowManager.addView(ctaRightSide, params)
    }

    private fun loadTrashCan() {
        trashView = layoutInflater.inflate(R.layout.talk_head_trash, null, false)

        // Postpone adding trash since it needs to be measured first
        trashView?.let {
            val vto = it.viewTreeObserver
            vto.addOnGlobalLayoutListener(this)
        }

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0
        params.y = 0

        windowManager.addView(trashView, params)
    }

    private fun makeDraggable(params: WindowManager.LayoutParams) {
        try {
            floatingHeadView?.setOnTouchListener(object : View.OnTouchListener {
                private val paramsF = params
                private var initialX: Int = 0
                private var initialY: Int = 0
                private var initialTouchX: Float = 0.toFloat()
                private var initialTouchY: Float = 0.toFloat()

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            renderPressedState(true)
                            dragStartedAt = SystemClock.elapsedRealtime()
                            movementDetected = false
                            initialX = paramsF.x
                            initialY = paramsF.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                        }

                        MotionEvent.ACTION_UP -> {
                            renderPressedState(false)
                            // If released over trash, just remove
                            if (collidedWithTrash) {
                                // Mark talk heads as explicitly dismissed
                                ClearTextPrefRepo.dismissedTalkHeads = true

                                stopFloatingService()
                                return false
                            }

                            // Hide trashcan again
                            trashView?.beGone()

                            // Don't handle taps if we're in cool-down
                            val currentTime = SystemClock.elapsedRealtime()
                            if (currentTime < coolDownTimestamp) {
                                // YOH bro, cool-down time
                                return false
                            }

                            if (movementDetected) {
                                // Distance travelled
                                val deltaX = Math.abs(ClearTextPrefRepo.floatingLastX - paramsF.x)
                                val deltaY = Math.abs(ClearTextPrefRepo.floatingLastY - paramsF.y)

                                // Store this new position as the default one from now on
                                ClearTextPrefRepo.floatingLastX = paramsF.x
                                ClearTextPrefRepo.floatingLastY = paramsF.y

                                // Render CTA again
                                renderCTA(forceRedraw = true)

                                // TODO : otherwise go back to the same position the UI was before?
                                if (deltaX > INVALIDATION_DISTANCE || deltaY > INVALIDATION_DISTANCE) {
                                    return false
                                }
                            }

                            val endTime = SystemClock.elapsedRealtime()
                            val elapsedTime = endTime - dragStartedAt
                            if (elapsedTime < CLICK_TIME_THRESHOLD_MILLIS) {
                                // Reset drag start timestamp for double-tapping
                                dragStartedAt = SystemClock.elapsedRealtime()
                                handleAsTap()
                            } else {
                                if (!movementDetected) {
                                    // Otherwise, only consider click when there is no dragging
                                    handleAsTap()
                                }
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {
                            movementDetected = true

                            var endX = (initialX + (event.rawX - initialTouchX)).toInt()
                            val endY = (initialY + (event.rawY - initialTouchY)).toInt()

                            if ((endX + floatingViewProps.talkHeadWidth) > floatingViewProps.screenWidth) {
                                endX = floatingViewProps.screenWidth - floatingViewProps.talkHeadWidth
                            } else {
                                if (endX < 0) {
                                    endX = 0
                                }
                            }

                            paramsF.x = endX
                            paramsF.y = endY
                            windowManager.updateViewLayout(floatingHeadView, paramsF)

                            // Hide CTA
                            ctaRightSide?.beGone()
                            ctaLeftSide?.beGone()

                            // Check for collisions with trash can
                            trashCollisionDetection()
                        }
                    }
                    return false
                }
            })
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun handleAsTap() {
        // When recording, both tap and double tap end the recording
        if (PlaybackStateManager.recording) {
            chatPressed()
            coolDownTimestamp = SystemClock.elapsedRealtime() + DOUBLE_TAP_DELAY
            return
        }

        if (preClickCount == 0) {
            // This was the first tap
            runOnUiDelayed(DOUBLE_TAP_DELAY) {
                chatPressed()
            }
            val stream = StreamManager.selectedStream ?: return
            if (stream.playableChunks().isNotEmpty()) {
                logDebug { "No playable chunks on this stream" }
                renderPressedOnce()
            }
        }
        preClickCount++
        if (preClickCount > 1) {
            doubleTapped()
        }
    }

    private fun renderPressedState(isPressed: Boolean) {
        val parentView = floatingHeadView
        if (parentView == null) {
            logWarn { "ParentView is null" }
            return
        }

        if (isPressed) {
            parentView.headContainer.scaleX = 0.9f
            parentView.headContainer.scaleY = 0.9f
        } else {
            parentView.headContainer.scaleX = 1f
            parentView.headContainer.scaleY = 1f
        }
    }

    private fun renderPressedOnce() {
        val parentView = floatingHeadView
        if (parentView == null) {
            logWarn { "ParentView is null" }
            return
        }

        parentView.topOverlay.makeVisible()

        // Use counter as loading
        parentView.playbackCounter.makeVisible(true)
        parentView.playbackCounter.text = "..."
    }

    /**
     * Handle collision with trash
     */
    private fun trashCollisionDetection() {
        if (trashView?.isInvisibleOrGone() ?: false) {
            trashView?.makeVisible()
            return
        }

        val parentView = trashView ?: return
        if (floatingViewProps.trashCenterX == 0f) {
            return
        }

        // Test for proximity
        val deltaX = Math.abs(floatingViewProps.trashCenterX - floatingViewProps.talkHeadCenterX)
        if (deltaX < TRASH_PROXIMITY) {
            val deltaY = Math.abs(floatingViewProps.trashCenterY - floatingViewProps.talkHeadCenterY)
            if (deltaY < TRASH_PROXIMITY) {
                if (!collidedWithTrash) {
                    logDebug { "Collided with trash!" }
                    if (!collidedWithTrash) {
                        renderTrashCollision(true)
                    }
                    collidedWithTrash = true
                    Vibes.mediumVibration()
                }
            } else {
                if (collidedWithTrash) {
                    renderTrashCollision(false)
                }
                collidedWithTrash = false
            }
        } else {
            if (collidedWithTrash) {
                renderTrashCollision(false)
            }
            collidedWithTrash = false
        }
    }

    private fun renderTrashCollision(collided: Boolean) {
        val parentView = trashView ?: return
        if (collided) {
            parentView.innerCircle.setImageResource(R.drawable.circle_red_60)
        } else {
            parentView.innerCircle.setImageResource(R.drawable.circle_white_80)
        }
    }

    private fun chatPressed() {
        if (preClickCount > 1) {
            logDebug { "Pressed but not handled" }
            preClickCount = 0
            return
        }
        logDebug { "Pressed AND handled" }
        preClickCount = 0
        val stream = StreamManager.selectedStream ?: return

        // Not playing nor recording
        if (!PlaybackStateManager.doingAudioIO) {
            if (stream.playableChunks().isEmpty()) {
                logDebug { "No playable chunks on this stream" }
                renderMainUI()
                return
            }

            // Play or Replay content
            PlaybackStateManager.currentStream = stream
            Vibes.shortVibration()
            EventTrackingManager.playbackStartReason(TAP_TALKHEAD)
            postEvent(AudioCommandEvent(AudioCommand.PLAY))
            return
        }

        // Currently Playing
        if (PlaybackStateManager.bufferingOrPlaying) {
            // Stop playback
            Vibes.shortVibration()
            postEvent(AudioCommandEvent(AudioCommand.STOP_PLAYING, playbackStopReason = TAP_AVATAR_TALKHEAD))
            return
        }

        if (PlaybackStateManager.recording) {
            // Stop recording
            postEvent(AudioCommandEvent(AudioCommand.STOP_RECORDING, recordingStopReason = TAP_TALK_HEAD))
            return
        }

    }

    private fun doubleTapped() {
        logMethodCall()

        val stream = StreamManager.selectedStream ?: return

        // Not playing nor recording
        if (!PlaybackStateManager.doingAudioIO) {
            PlaybackStateManager.currentStream = stream
            Vibes.shortVibration()
            EventTrackingManager.recordingStart(TAP_TALK_HEAD)
            postEvent(AudioCommandEvent(AudioCommand.RECORD))
            renderMainUI()
        }
    }

    private fun redrawVisualizer(visualizerType: VisualizerType) {
        when (visualizerType) {
        // Playback visualizer
            VisualizerType.PLAYBACK -> {
                val playbackLevel = {
                    // Smooth out changes in the audio level
                    if (audioLevel < audioLevelPrevious) {
                        (audioLevelPrevious * 8 + audioLevel) / 9
                    } else {
                        audioLevel
                    }
                }()

                audioLevelPrevious = playbackLevel
                val visualizerScale = playbackLevel.toFloat() / 0.5f
                updateVisualizer(visualizerScale * 100f)
            }

        // Recording visualizer
            VisualizerType.RECORDING -> {
                val recordingLevel = {
                    // Smooth out changes in the audio level
                    if (audioLevel < audioLevelPrevious) {
                        (audioLevelPrevious * 5 + audioLevel) / 6
                    } else {
                        audioLevel
                    }
                }()

                audioLevelPrevious = recordingLevel
                val visualizerScale = (0.6 + recordingLevel / Math.sqrt(recordingLevel + 0.01f)).toFloat() - 0.5f
                updateVisualizer(visualizerScale * 100f)
            }
        }
    }

    /**
     * This red dot exists for debugging purposes
     */
    private fun renderRedDotAt(xPos: Int, yPos: Int) {
        if (!DEBUGGING_DOT) {
            return
        }
        val parentView = redDotView
        if (parentView == null) {
            logWarn { "RedDot view is null" }
            return
        }
        val layoutParams = parentView.layoutParams as WindowManager.LayoutParams
        layoutParams.x = xPos
        layoutParams.y = yPos
        windowManager.updateViewLayout(parentView, layoutParams)
    }

    /**
     * Main entry point that takes care of re-rendering the UI
     */
    private fun updateVisualizer(newValue: Float) {
        val parentView = floatingHeadView
        if (parentView == null) {
            logWarn { "ParentView is null" }
            return
        }
        parentView.playerVisualizer.circlePercentage = newValue
    }

    /**
     * Main entry point that takes care of re-rendering the UI
     */
    private fun renderMainUI() {
        val parentView = floatingHeadView
        if (parentView == null) {
            logWarn { "ParentView is null" }
            return
        }
        val stream = StreamManager.selectedStream
        if (stream == null) {
            logWarn { "Stream is null" }
            parentView.beGone()
            return
        }

        // Make sure parent UI is visible
        if (parentView.isInvisibleOrGone()) {
            parentView.makeVisible()
        }

        // Update Live status display
        if (PrefRepo.livePlayback) {
            parentView.avatarBorder.setImageResource(R.drawable.circumference_green_thin)
        } else {
            parentView.avatarBorder.setImageResource(R.drawable.circumference_white_thin)
        }

        // Recording UI
        if (PlaybackStateManager.recording) {
            renderRecordingUI(parentView)
            return
        }

        // Common to Idle and Playing
        parentView.recordingFace.beGone()

        // Playing UI
        if (PlaybackStateManager.playing) {
            renderPlayingUI(parentView)
            return
        }

        // Idle
        renderIdleUI(stream, parentView)
    }

    private fun renderCTA(forceRedraw: Boolean = false) {
        val useRightSideCTA = talkHeadIsRightSide()
        val parentView = if (useRightSideCTA) {
            ctaLeftSide?.beGone()
            ctaRightSide
        } else {
            ctaRightSide?.beGone()
            ctaLeftSide
        }

        if (parentView == null) {
            logWarn { "CTAView is null" }
            return
        }

        if (forceRedraw && parentView.isInvisibleOrGone()) {
            // CTA was NOT visible, skip redraw
            return
        }

        val stream = StreamManager.selectedStream
        if (stream == null) {
            logWarn { "Stream is null" }
            parentView.beGone()
            return
        }

        // CTA Label
        val ctaLabel = if (useRightSideCTA) {
            parentView.cta_label_right
        } else {
            parentView.cta_label_left
        }

        // Only re-display CTA if the audio state changed
        val currentAudioState = PlaybackStateManager.state
        if (forceRedraw || currentAudioState != PlaybackStateManager.previousAudioState) {
            parentView.makeOpaque()
            parentView.makeVisible()

            when (currentAudioState) {
                AudioState.IDLE -> {
                    if (stream.unplayed) {
                        ctaLabel.text = "${PLAY_CIRCLE_FILLED.text} " + R.string.th_tap_to_play.stringResource()
                    } else {
                        // Don't show recording message if just recorded one
                        if (PlaybackStateManager.previousAudioState != AudioState.RECORDING) {
                            ctaLabel.text = "${RECORD_VOICE.text} " + R.string.th_tap_to_start_recording.stringResource()
                        } else {
                            // Hide the CTA if just ended recording something, and we're IDLE without content to play
                            parentView.beGone()
                        }
                    }
                    parentView.makeVisible(true)
                }
                AudioState.BUFFERING -> {
                    // Buffering has no CTA text current
                    parentView.beGone()
                }
                AudioState.PLAYING -> {
                    ctaLabel.text = "${STOP.text} " + R.string.th_tap_to_stop.stringResource()
                    parentView.makeVisible(true)
                }
                AudioState.RECORDING -> {
                    ctaLabel.text = "${STOP.text} " + R.string.th_tap_to_stop_recording.stringResource()
                    parentView.makeVisible(true)
                }
            }

            // Reposition it
            runOnUiDelayed(1) {
                repositionCTA(parentView, useRightSideCTA)
            }

            // Hide after a while
            hideCTADelayed(parentView)

            // Save state
            PlaybackStateManager.previousAudioState = currentAudioState
        }
    }

    private fun repositionCTA(ctaView: View, isRightSide: Boolean) {
        val params = ctaView.layoutParams as WindowManager.LayoutParams
        if (isRightSide) {
            val ctaWidth = ctaView.width
            params.x = (floatingViewProps.talkHeadX.toInt()) - (ctaWidth + 10)
            params.y = floatingViewProps.talkHeadY.toInt()
        } else {
            // Left side CTA
            params.x = ClearTextPrefRepo.floatingLastX
            params.y = ClearTextPrefRepo.floatingLastY
        }
        windowManager.updateViewLayout(ctaView, params)
    }

    private fun talkHeadIsRightSide(): Boolean {
        if (floatingViewProps.talkHeadCenterX < (floatingViewProps.screenWidth / 2)) {
            // Talk head is on the left side
            return false
        }
        // Talk head is on the right side
        return true
    }

    private fun hideCTADelayed(ctaView: View) {
        // Hide after a while
        ctaView.let {
            it.animate()?.cancel()
            it.animate()
                    .setStartDelay(5000)
                    .setDuration(700)
                    .alpha(FULL_TRANSPARENCY)
                    .withEndAction {
                        it.beGone()
                        it.alpha = FULL_ALPHA
                    }
                    .start()
        }
    }

    private fun renderPlayingUI(parentView: View) {
        parentView.dotBorder.beGone()
        parentView.dot.beGone()
        parentView.dot.beGone()
        parentView.playbackCounter.makeVisible()
        parentView.topOverlay.makeVisible()
        parentView.playerVisualizer.makeVisible()

        // Update counter
        parentView.playbackCounter.text = PlaybackCounterManager.remainingSeconds.toString()
    }

    private fun renderRecordingUI(parentView: View) {
        parentView.recordingFace.makeVisible()
        parentView.dotBorder.beGone()
        parentView.dot.beGone()
        parentView.playbackCounter.beGone()
        parentView.topOverlay.beGone()
        parentView.playerVisualizer.makeVisible()
        Glide.with(this).load(R.color.transparent).dontAnimate().into(parentView.contactPhoto)
    }

    private fun renderIdleUI(stream: Stream, parentView: View) {
        // Hide Playing UI
        parentView.playbackCounter.beGone()
        parentView.topOverlay.beGone()
        parentView.playerVisualizer.beGone()

        // Unplayed state
        if (stream.unplayed) {
            parentView.dotBorder.makeVisible(true)
            parentView.dot.makeVisible(true)
        } else {
            parentView.dotBorder.beGone()
            parentView.dot.beGone()
        }

        // Avatar or Conversation title
        val avatarURI = stream.imageURL
        if (avatarURI != null) {
            RoundImageUtils.createRoundImage(this, parentView.contactPhoto,
                    avatarURI,
                    AvatarSize.CONTACT)
            parentView.conversationTitle.text = ""
        } else {
            if (stream.reachableParticipants.size > 1) {
                GroupAvatarManager.loadGroupAvatarInto(parentView.contactPhoto, stream, AvatarSize.CONTACT)
            } else {
                val title = stream.shortTitle
                if (title.length > 5) {
                    parentView.conversationTitle.text = "${title.take(4)}…"
                } else {
                    parentView.conversationTitle.text = title
                }
                Glide.with(this).load(R.color.transparent).dontAnimate().into(parentView.contactPhoto)
            }
        }
    }

    private fun stopFloatingService() {
        stopSelf()
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onAudioAmplitude(event: AudioAmplitudeEvent) {
        // Convert into our own leveling metric
        audioLevel = Math.pow(10.0, event.amplitude / 40)
        redrawVisualizer(event.visualizerType)
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioServiceState(event: AudioServiceStateEvent) {
        logDebug { "AudioEvent. new: ${event.newState}, old: ${event.oldState}" }
        if (event.newState == AudioState.IDLE) {
            resetAudioLevels()
        }
        renderMainUI()
        renderCTA()
    }


    @Subscribe(threadMode = MAIN)
    fun onAppVisibilityChange(event: AppVisibilityChangeEvent) {
        logEvent(event)
        if (event.visible) {
            stopFloatingService()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onStreamsChanged(event: StreamsChangedEvent) {
        logEvent(event)
        renderMainUI()
        renderCTA()
    }

    @Subscribe(threadMode = MAIN)
    fun onCounterChanged(event: CounterChangedEvent) {
        renderMainUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onLivePlaybackEvent(event: SwitchAndPlayStreamEvent) {
        logEvent(event)

        // Update stream
        StreamManager.selectedStreamId = event.stream.id
        PlaybackStateManager.currentStream = event.stream

        renderMainUI()

        EventTrackingManager.playbackStartReason(AUTOPLAY_TALKHEAD)
        postEvent(AudioCommandEvent(AudioCommand.PLAY))
    }

    @Subscribe(threadMode = MAIN)
    fun onGroupAvatarReady(event: GroupAvatarReadyEvent) {
        logEvent(event)
        val stream = StreamManager.selectedStream
        if (stream == null) {
            logWarn { "Stream is null" }
            return
        }
        if (event.streamId == stream.id) {
            logDebug { "Will update avatar display for this group stream" }
            renderMainUI()
        }
    }

}