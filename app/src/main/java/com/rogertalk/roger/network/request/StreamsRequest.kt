package com.rogertalk.roger.network.request

import com.rogertalk.roger.android.services.AudioDownloadManager
import com.rogertalk.roger.event.success.StreamsSuccessEvent
import com.rogertalk.roger.models.json.StreamsResponse
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread

class StreamsRequest(val updateCacheImmediately: Boolean = false) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(StreamsResponse::class.java)
        getRogerAPI().streams().enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val response = t as? StreamsResponse ?: return

        // Handle cursor
        if (!StreamCacheRepo.gotCursorOnce) {
            // Update cursor the first time, while is wasn't set yet
            StreamCacheRepo.nextCursor = response.cursor
        }

        // Cache audio files
        runOnUiThread {
            AudioDownloadManager.cacheEntireStreamList(response.data)
        }

        if (updateCacheImmediately) {
            runOnUiThread {
                StreamCacheRepo.updateCache(response.data)
            }
        } else {
            // Propagate the response, it is the responsibility of the listener to update the cache
            postEvent(StreamsSuccessEvent(response.data))
        }


    }
}