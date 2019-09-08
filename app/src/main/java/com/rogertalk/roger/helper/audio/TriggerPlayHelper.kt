package com.rogertalk.roger.helper.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_UI
import com.rogertalk.roger.event.broadcasts.audio.ProximityEvent
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.*
import kotlin.LazyThreadSafetyMode.NONE

/**
 * This manager controls trigger based auto-play behavior (proximity and accelerometer/gravity sensors come into play).
 */
class TriggerPlayHelper(val context: Context) : SensorEventListener {

    companion object {
        private val ACCEL_X_ANGLE_TRIGGER = 1f
        private val ACCEL_Z_ANGLE_TRIGGER = 4.5f

        // Number of times gyroscope can fail before falling back to accelerometer
        private val GYROSCOPE_FAILS_BEFORE_FALLBACK = 2
    }

    private val FAR_VALUE: Float by lazy { proximitySensor?.maximumRange ?: 1f }

    // Flags
    private var isActive = false
    private var isAccelerometerFallbackActive = false

    // System Sensors and managers
    private val sensorManager: SensorManager by lazy(NONE) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val proximitySensor: Sensor? by lazy(NONE) { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    private val gravitySensor: Sensor? by lazy(NONE) { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) }
    private val accelerometerSensor: Sensor? by lazy(NONE) { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    private var lastXvalue = 0.0f
    private var lastYvalue = 0.0f

    // We only use Z value for accelerometer
    private var lastZvalue = 0.0f

    private var gyroscopeFailCount = 0
    private var lastProximityValue = FAR_VALUE
    private var lastFiredValue = false

    //
    // OVERRIDE METHODS
    //

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignored
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            lastProximityValue = event.values[0]

            autoPlayValidation()
            return
        }

        if (event.sensor.type == Sensor.TYPE_GRAVITY || event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            lastXvalue = event.values[0]
            lastYvalue = event.values[1]
            lastZvalue = event.values[2]
            autoPlayValidation()
        }
    }

    //
    // PUBLIC METHODS
    //

    fun stopDetection() {
        // Unregister proximity and accelerometer sensors.
        sensorManager.unregisterListener(this, proximitySensor)
        sensorManager.unregisterListener(this, gravitySensor)
        sensorManager.unregisterListener(this, accelerometerSensor)

        isActive = false
    }

    fun startDetection() {
        logMethodCall()
        if ( proximitySensor == null) {
            logWarn { "No proximity sensor found" }
            PrefRepo.raiseToHearPossible = false
            return
        }

        if (gravitySensor == null) {
            logWarn { "No gravity sensor found" }
            if (accelerometerSensor == null) {
                logWarn { "No accelerometer sensor found as well" }
                PrefRepo.raiseToHearPossible = false
                return
            }
        }

        // Flag raise to hear as possible at hardware level
        PrefRepo.raiseToHearPossible = true

        // Only activate if app is foreground
        if (!AppVisibilityRepo.chatIsForeground) {
            return
        }

        if (isActive) {
            logDebug { "Sensors already active" }
            return
        }

        isActive = true

        logInfo { "Initializing proximity and 'angle' sensors" }

        // Register listeners
        val proximityEnabled = sensorManager.registerListener(this, proximitySensor, SENSOR_DELAY_UI)
        var gravityEnabled: Boolean

        try {
            gravityEnabled = sensorManager.registerListener(this, gravitySensor, SENSOR_DELAY_UI)
        } catch(e: NullPointerException) {
            logError { "NPE when trying to register listener for Gravity sensor" }
            gravityEnabled = false
        }

        if (!proximityEnabled) {
            logWarn { "Proximity sensor not enabled!" }
            isActive = false
        }
        if (!gravityEnabled) {
            logWarn { "Tried to enable Gravity sensor but failed" }
            fallbackToAccelerometer()
            isActive = false
        } else {
            isAccelerometerFallbackActive = false
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun fallbackToAccelerometer() {
        val accelerometerEnable = sensorManager.registerListener(this, accelerometerSensor, SENSOR_DELAY_UI)
        if (accelerometerEnable) {
            logInfo { "Falling back to accelerometer sensor" }

            isActive = true

            isAccelerometerFallbackActive = true

            // We don't need gravity sensor anymore
            sensorManager.unregisterListener(this, gravitySensor)

            // Reset gyroscope failure count
            gyroscopeFailCount = 0
        } else {
            logInfo { "Accelerometer not available as well :/" }
        }

    }

    private fun autoPlayValidation() {
        val newValue = isAgainstEar() && angleWithinRange()
        if (newValue != lastFiredValue) {
            postEvent(ProximityEvent(newValue))
            lastFiredValue = newValue
        }
    }

    private fun isAgainstEar(): Boolean {
        return lastProximityValue != FAR_VALUE
    }

    /**
     * Check if the device's angle is within activation range.
     * Underlying implementation can vary.
     */
    private fun angleWithinRange(): Boolean {
        // Fallback to accelerometer if gravity is being silent about giving us values.. damn gravity
        if (!isAccelerometerFallbackActive) {
            if (lastXvalue == 0f && lastYvalue == 0f) {
                gyroscopeFailCount++
                if (gyroscopeFailCount == GYROSCOPE_FAILS_BEFORE_FALLBACK) {
                    fallbackToAccelerometer()
                }
            }
        }

        // Choose the proper validation check depending on the sensor choice
        if (isAccelerometerFallbackActive) {
            return angleWithinRangeAccelerometer()
        } else {
            return angleWithinRangeGravity()
        }
    }

    private fun angleWithinRangeGravity(): Boolean {
        return (Math.abs(lastYvalue) + Math.abs(lastXvalue)) > 7f
    }

    private fun angleWithinRangeAccelerometer(): Boolean {
        // X must be bigger than 1 or lower than -1
        // Y not tested for now..
        // Z must be between 5 and -5


        if (lastXvalue > ACCEL_X_ANGLE_TRIGGER || lastXvalue < (-ACCEL_X_ANGLE_TRIGGER)) {
            return lastZvalue < ACCEL_Z_ANGLE_TRIGGER && lastZvalue > (-ACCEL_Z_ANGLE_TRIGGER)
        }
        return false
    }
}