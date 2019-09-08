package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Stream

class StreamStatusRequest(val streamId: Long, val status: StreamStatus, val estimatedDuration: Long? = null) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().updateStreamStatus(streamId.toString(),
                status.statusText, estimatedDuration).enqueue(callback)
    }
}