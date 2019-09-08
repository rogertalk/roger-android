package com.rogertalk.roger.models.data

import android.media.AudioManager

enum class AudioMode(val intValue: Int) {
    NORMAL(AudioManager.MODE_NORMAL),
    IN_COMMUNICATION(AudioManager.MODE_IN_COMMUNICATION)
}