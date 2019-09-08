package com.rogertalk.roger.network.request.notifications

import com.rogertalk.roger.manager.PushNotificationManager
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.BaseRequest
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread

/**
 * This call will get a Stream for a new chunk whose Stream was not found on the local cache.
 */
class StreamForNewChunkNotificationRequest(val streamId: Long, val chunk: Chunk) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().stream(streamId).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
            PushNotificationManager.handleNewChunk(chunk, streamId)
        }

    }
}