package com.rogertalk.roger.models.data

import android.media.AudioManager

enum class AudioStreamType(val intValue: Int) {
    EARPIECE(AudioManager.STREAM_VOICE_CALL),
    LOUDSPEAKER(AudioManager.STREAM_MUSIC)
}