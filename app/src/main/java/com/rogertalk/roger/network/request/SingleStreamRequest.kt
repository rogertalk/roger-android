package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread

class SingleStreamRequest(val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().stream(streamId).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val newOrUpdatedStream = t as? Stream ?: return
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(newOrUpdatedStream)
        }
    }
}