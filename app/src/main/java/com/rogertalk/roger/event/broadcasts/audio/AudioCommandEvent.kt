package com.rogertalk.roger.event.broadcasts.audio

import com.rogertalk.roger.manager.EventTrackingManager.PlaybackStopReason
import com.rogertalk.roger.manager.EventTrackingManager.RecordingReason
import com.rogertalk.roger.models.data.AudioCommand

class AudioCommandEvent(val audioCommand: AudioCommand,
                        val playbackStopReason: PlaybackStopReason = PlaybackStopReason.NOT_SPECIFIED,
                        val recordingStopReason: RecordingReason = RecordingReason.NOT_SPECIFIED)