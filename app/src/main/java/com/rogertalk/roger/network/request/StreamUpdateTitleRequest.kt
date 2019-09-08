package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo

class StreamUpdateTitleRequest(val streamId: Long, val newTitle: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().updateStreamTitle(streamId, newTitle).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        // Update the stream
        val stream = t as? Stream ?: return
        StreamCacheRepo.updateStreamInStreams(stream)
    }
}