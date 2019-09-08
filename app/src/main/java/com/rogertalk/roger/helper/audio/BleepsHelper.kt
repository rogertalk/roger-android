package com.rogertalk.roger.helper.audio

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build.VERSION_CODES.LOLLIPOP
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.phone.Vibes
import kotlin.LazyThreadSafetyMode.NONE

class BleepsHelper(val audioManager: AudioManager?) {

    companion object {
        private val SOUND_VOLUME = 0.8f
    }

    private val soundPool: SoundPool by lazy(NONE) { createSoundPool() }
    private var recordingBleepSound = 0
    private var earBleepSound = 0
    private var doneSound = 0
    private var loadingSound = 0
    private var loadingSoundPlaying = false
    private var loadingSoundID = 0

    private var soundojiLaughing = 0
    private var soundojiAwkwardCricket = 0
    private var soundojiRimShot = 0

    //
    // PUBLIC METHODS
    //

    fun loadSounds(context: Context){
        recordingBleepSound = soundPool.load(context, R.raw.bleep, 0)
        earBleepSound = soundPool.load(context, R.raw.earbeep, 0)
        loadingSound = soundPool.load(context, R.raw.loading, 0)
        doneSound = soundPool.load(context, R.raw.done, 0)
        soundojiLaughing = soundPool.load(context, R.raw.audience_laught, 0)
        soundojiAwkwardCricket = soundPool.load(context, R.raw.cricket, 0)
        soundojiRimShot = soundPool.load(context, R.raw.rimshot, 0)
    }

    fun releaseManager(){
        stopLoadingSound()
        soundPool.release()
    }

    fun playSoundojiLaughing() {
        soundPool.play(soundojiLaughing, 1f, 1f, 0, 0, 1f)
    }

    fun playSoundojiAwkwardCricket() {
        soundPool.play(soundojiAwkwardCricket, 1f, 1f, 0, 0, 1f)
    }

    fun playSoundojiRimShot() {
        soundPool.play(soundojiRimShot, 1f, 1f, 0, 0, 1f)
    }

    fun playRecordingSound() {
        Vibes.shortVibration()
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            soundPool.play(recordingBleepSound, 1f, 1f, 0, 0, 1f)
        }
    }

    fun playLoadingSound() {
        if (!loadingSoundPlaying) {
            loadingSoundPlaying = true
            // Stop previous sound before starting a new one
            soundPool.stop(loadingSoundID)
            loadingSoundID = soundPool.play(loadingSound, SOUND_VOLUME, SOUND_VOLUME, 0, -1, 1f)
        }
    }

    fun stopLoadingSound() {
        soundPool.stop(loadingSoundID)
        loadingSoundPlaying = false
    }

    fun playEarBleepSound() {
        soundPool.play(earBleepSound, SOUND_VOLUME, SOUND_VOLUME, 0, 0, 1f)
    }

    fun playDoneSound() {
        audioManager?.isSpeakerphoneOn = false
        soundPool.play(doneSound, 0.6f, 0.6f, 0, 0, 1f)
    }

    //
    // PRIVATE METHODS
    //

    @TargetApi(21)
    private fun createSoundPool(): SoundPool {
        if (AndroidVersion.fromApiVal(LOLLIPOP, true)) {
            val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            return SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(2).build()
        } else {
            return SoundPool(2, AudioManager.STREAM_MUSIC, 0)
        }
    }
}