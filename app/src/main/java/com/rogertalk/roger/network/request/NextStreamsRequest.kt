package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.broadcasts.streams.NextStreamsRequestFinishedEvent
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.models.json.StreamsResponse
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logDebug
import okhttp3.ResponseBody

class NextStreamsRequest(val cursor: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(StreamsResponse::class.java)
        getRogerAPI().nextStreams(cursor).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        postEvent(NextStreamsRequestFinishedEvent())
        val response = t as? StreamsResponse ?: return
        logDebug { "Got next streams!" }

        // Handle cursor
        StreamCacheRepo.nextCursor = response.cursor

        // Update stream cache
        runOnUiThread {
            StreamCacheRepo.updateCacheMemoryOnly(response.data)
        }

        // Fire event to update display of streams
        postEvent(StreamsChangedEvent())
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        // Event should also be fired in this case, so the app can update the loading state
        postEvent(NextStreamsRequestFinishedEvent())
    }
}
