package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.StreamShareableFailEvent
import com.rogertalk.roger.event.success.StreamShareableSuccessEvent
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import okhttp3.ResponseBody

class MakeStreamShareableRequest(val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().makeStreamShareable(streamId.toString()).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
            postEvent(StreamShareableSuccessEvent(streamId))
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        postEvent(StreamShareableFailEvent(streamId))
    }
}