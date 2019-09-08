package com.rogertalk.roger.helper.audio

import android.media.AudioManager
import android.os.Build
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.android.services.AudioService
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logInfo

/**
 * Support class that manages audio focus conditions for AudioService.
 * 'Focus, Daniel-San. Concentrate!'
 */
class AudioFocusHelper(val am: AudioManager?, val listener: AudioFocusListener) :
        AudioManager.OnAudioFocusChangeListener {

    enum class FocusState {
        HAS_FOCUS, NO_FOCUS
    }

    var currentFocus = FocusState.NO_FOCUS

    //
    // OVERRIDE METHODS
    //

    override fun onAudioFocusChange(focusChange: Int) {
        logInfo { "FocusChange: $focusChange" }
        when (focusChange) {
        // focus loss
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> lostFocus()
            AudioManager.AUDIOFOCUS_LOSS -> lostFocus()

        // focus gain
            AudioManager.AUDIOFOCUS_GAIN -> listener.gainedAudioFocus()
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> listener.gainedAudioFocus()
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> listener.gainedAudioFocus()
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> listener.gainedAudioFocus()
        }
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Request audio focus to play
     */
    fun requestAudioFocus(): Boolean {
        // Request audio focus for playback
        val result = am?.requestAudioFocus(this,
                AudioService.AUDIO_STREAM_TYPE.intValue,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            logInfo { "Focus request granted" }
            gainedFocus()
            return true
        } else {
            lostFocus()
            logInfo { "Focus request denied" }
            return false
        }
    }

    /**
     * Give back the focus to the system
     */
    fun abandonAudioFocus() {
        if (am == null) {
            logError { "AudioManager is null!" }
        }
        if (currentFocus == FocusState.NO_FOCUS) {
            logInfo { "already had no focus, nothing to do" }
            // already had no focus, nothing to do
            return
        }

        val result = am?.abandonAudioFocus(this) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            logInfo { "Focus request granted" }
            lostFocus()
        } else {
            gainedFocus()
            logInfo { "Focus request denied" }
        }
    }

    fun hasFocus(): Boolean {
        if (AndroidVersion.toApiVal(Build.VERSION_CODES.JELLY_BEAN, true)) {
            // API 16 and below doesn't play well with Audio Focus, it will always get refused!
            // So, just assume it's there... (YOLO).
            return true
        }

        return currentFocus == FocusState.HAS_FOCUS
    }

    //
    // PRIVATE METHODS
    //

    private fun lostFocus() {
        currentFocus = FocusState.NO_FOCUS
        listener.lostAudioFocus()
    }

    private fun gainedFocus() {
        currentFocus = FocusState.HAS_FOCUS
        listener.gainedAudioFocus()
    }
}
