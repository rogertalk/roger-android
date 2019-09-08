package com.rogertalk.roger.event.broadcasts.audio

import com.rogertalk.roger.models.data.AudioState

data class AudioServiceStateEvent(val oldState: AudioState, val newState: AudioState)