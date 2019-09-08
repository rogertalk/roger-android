package com.rogertalk.roger.manager

import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Stream

object RealTimeManager {

    private var recordingStream: Stream? = null

    var isListening = false

    fun handleNewStreamState(streamId: Long, status: StreamStatus) {
        val recordingStreamId = recordingStream?.id ?: return
        if (recordingStreamId == streamId) {
            if (status == StreamStatus.LISTENING) {
                isListening = true
            }
        }
    }

    fun resetState(newStream: Stream) {
        recordingStream = newStream
        isListening = false
    }
}