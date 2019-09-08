package com.rogertalk.roger.repo

import android.preference.PreferenceManager
import com.rogertalk.roger.utils.extensions.appController

/**
 * This preferences are NOT stored encrypted on the device. Use PrefRepo instead for user
 * sensible data.
 */
object ClearTextPrefRepo {

    private val DISMISSED_TALK_HEAD = "dismissedTalkHeads"

    private val STEREO_RECORDING = "record_stereo"
    private val RECORDING_SAMPLING_RATE = "recording_sampling_rate"
    private val DISABLE_NS = "force_disable_noise_suppression"

    private val FLOATING_LAST_X_POSITION = "floatingLastX"
    private val FLOATING_LAST_Y_POSITION = "floatingLastY"

    //
    // PUBLIC METHODS
    //

    var dismissedTalkHeads: Boolean
        get() = getPref(DISMISSED_TALK_HEAD, true)
        set(value) {
            putPref(DISMISSED_TALK_HEAD, value)
        }

    var stereoRecording: Boolean
        get() = getPref(STEREO_RECORDING, true)
        set(value) {
            putPref(STEREO_RECORDING, value)
        }

    var disableNoiseSuppression: Boolean
        get() = getPref(DISABLE_NS, true)
        set(value) {
            putPref(DISABLE_NS, value)
        }

    var floatingLastX: Int
        get() = getPref(FLOATING_LAST_X_POSITION, 0)
        set(value) {
            putPref(FLOATING_LAST_X_POSITION, value)
        }

    var floatingLastY: Int
        get() = getPref(FLOATING_LAST_Y_POSITION, 120)
        set(value) {
            putPref(FLOATING_LAST_Y_POSITION, value)
        }

    //
    // PRIVATE METHODS
    //

    private fun putPref(key: String, value: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appController())
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun putPref(key: String, value: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appController())
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    private fun putPref(key: String, value: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appController())
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    private fun putPref(key: String, value: Long) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appController())
        val editor = prefs.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    private fun getPref(key: String): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(appController())
        return preferences.getString(key, null)
    }

    private fun getPref(key: String, default: Boolean): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(appController())
        return preferences.getBoolean(key, default)
    }

    private fun getPref(key: String, default: Int): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(appController())
        return preferences.getInt(key, default)
    }

    private fun getPref(key: String, default: Long): Long {
        val preferences = PreferenceManager.getDefaultSharedPreferences(appController())
        return preferences.getLong(key, default)
    }
}