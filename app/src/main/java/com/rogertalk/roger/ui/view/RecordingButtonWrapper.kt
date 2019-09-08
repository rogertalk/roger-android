package com.rogertalk.roger.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.RelativeLayout
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.constant.RuntimeConstants
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logWarn
import java.util.*

class RecordingButtonWrapper : RelativeLayout, SpringListener {

    private var isRecording = false
    private var displayingSent = false
    private var isFrozen = false
    private var toggleAction: (() -> Unit) = { }
    private var contentDescriptionMicrophone: String
    private var contentDescriptionStopRecording: String

    var shadow: ImageView? = null

    private var pressDownTimestamp = 0L

    // Spring System
    private val micButtonSpring: Spring by lazy { SpringSystem.create().createSpring() }

    init {
        contentDescriptionMicrophone = R.string.ac_microphone.stringResource(context = context)
        contentDescriptionStopRecording = R.string.ac_microphone_stop.stringResource(context = context)

        // Configure spring and related event listeners
        if (!isInEditMode) {
            micButtonSpring.springConfig = SpringConfig.fromBouncinessAndSpeed(17.0, 60.0)
            micButtonSpring.addListener(this)
        }
    }

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isFrozen) {
            return super.onTouchEvent(event)
        }
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressDownTimestamp = Date().time
                    buttonPressed()
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (isRecording) {
                        val currentTimestamp = Date().time
                        if ((currentTimestamp - pressDownTimestamp) > RuntimeConstants.LONG_PRESS_DURATION) {
                            // This is a long press, stop recording
                            buttonPressed()
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun buttonPressed() {
        isRecording = !isRecording
        micButtonSpring.endValue = if (isRecording) 1.0 else 0.0
        drawRecordingState()
        toggleAction()
        updateContentDescription()
    }

    private fun updateContentDescription() {
        if (isRecording) {
            contentDescription = contentDescriptionStopRecording
        } else {
            contentDescription = contentDescriptionMicrophone
        }
    }

    private fun drawRecordingState() {
        if (displayingSent) {
            return
        }
        val circle = findViewById(R.id.action_circle) as ImageView

        if (isRecording) {
            circle.setImageResource(R.drawable.smile_record)
        } else {
            circle.setImageResource(R.drawable.smile)
        }

        updateContentDescription()
    }

    private fun scaleButton(scale: Float) {
        scaleX = scale
        scaleY = scale

        // Update shadow scale as well
        shadow?.scaleX = scale
        shadow?.scaleY = scale
    }

    fun freeze() {
        if (isRecording) {
            logWarn { "Cannot freeze button while recording" }
            return
        }
        isFrozen = true
        drawRecordingState()
    }

    fun unFreeze() {
        isFrozen = false

        if (isRecording) {
            // Nothing else should be processed
            return
        }
        drawRecordingState()
    }

    fun setToggleAction(action: () -> Unit) {
        toggleAction = action
    }

    fun displayAsNotRecording() {
        isRecording = false
        scaleButton(1f)
        drawRecordingState()
    }

    fun displayAsRecording() {
        isRecording = true
        scaleButton(1f)
        drawRecordingState()
    }

    //
    // Spring Overrides
    //

    override fun onSpringUpdate(spring: Spring) {
        val scale = 1f - (spring.currentValue.toFloat() * 0.1f)
        scaleButton(scale)
    }

    override fun onSpringAtRest(spring: Spring) { }

    override fun onSpringActivate(spring: Spring) { }

    override fun onSpringEndStateChange(spring: Spring) { }

}
