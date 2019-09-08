package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread

class StreamVisibleRequest(val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().showStream(streamId.toString()).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
        }
    }
}