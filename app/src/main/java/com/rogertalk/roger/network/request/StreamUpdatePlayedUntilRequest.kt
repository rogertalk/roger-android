package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.success.SingleStreamSuccessEvent
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.extensions.postEvent

class StreamUpdatePlayedUntilRequest(val streamId: Long, val playedUntilTimestamp: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().updateStreamPlayedUntil(streamId.toString(), playedUntilTimestamp.toString()).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        // Update the stream
        val stream = t as? Stream ?: return
        postEvent(SingleStreamSuccessEvent(stream))
    }
}