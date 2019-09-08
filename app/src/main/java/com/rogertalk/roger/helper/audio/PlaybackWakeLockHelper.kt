package com.rogertalk.roger.helper.audio

import android.net.wifi.WifiManager
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.PowerManager
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.utils.log.logError
import kotlin.LazyThreadSafetyMode.NONE

class PlaybackWakeLockHelper(val powerManager: PowerManager, val wifiManager: WifiManager) {

    companion object {
        private val WIFI_LOCK_TAG = "rogerWifiLock"
    }

    private val wifiLock: WifiManager.WifiLock by lazy(NONE) { initWifiLock() }
    private var field = 0x00000020
    private val screenOffWakeLock: PowerManager.WakeLock by lazy(NONE) { initScreenWakeLock() }
    private val audioIOWakeLock: PowerManager.WakeLock by lazy(NONE) { initAudioIOWakeLock() }


    //
    // PUBLIC METHODS
    //

    fun screenOffAcquire() {
        if (!screenOffWakeLock.isHeld) {
            if (AppVisibilityRepo.chatIsForeground) {
                screenOffWakeLock.acquire()
            }
        }
    }

    fun wifiLockAcquire() {
        if (!(wifiLock.isHeld)) {
            wifiLock.acquire()
        }
    }

    fun wifiLockRelease() {
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }

    fun audioIOLockRelease() {
        if (audioIOWakeLock.isHeld) {
            audioIOWakeLock.release()
        }
    }

    fun audioIOLockAcquire() {
        if (!audioIOWakeLock.isHeld) {
            audioIOWakeLock.acquire()
        }
    }

    fun screenOffRelease() {
        if (screenOffWakeLock.isHeld) {
            screenOffWakeLock.release()
        }
    }

    //
    // PRIVATE METHODS
    //

    /**
     * This screen lock turns off the screen when the user's face is near so there's no accidental
     * interaction with the app or the system.
     */
    private fun initScreenWakeLock(): PowerManager.WakeLock {
        if (AndroidVersion.fromApiVal(LOLLIPOP, true)) {
            // This call is only available from API 21 onwards
            return powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, this.javaClass.simpleName)
        }

        try {
            // Yeah, this is hidden field.
            field = powerManager.javaClass.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (e: Throwable) {
            logError(e) { "Could not get field for screen off wakelock" }
        }
        return powerManager.newWakeLock(field, this.javaClass.simpleName)
    }

    private fun initAudioIOWakeLock(): PowerManager.WakeLock {
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.javaClass.simpleName)
    }

    private fun initWifiLock(): WifiManager.WifiLock {
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        return wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG)
    }
}