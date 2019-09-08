package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.log.logInfo

class BuzzStreamRequest(val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().buzz(streamId).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        logInfo { "Buzzed sent successfully" }
    }
}