package com.rogertalk.roger.event.broadcasts.audio

import com.rogertalk.roger.models.data.VisualizerType


class AudioAmplitudeEvent(val amplitude: Double, val visualizerType: VisualizerType)