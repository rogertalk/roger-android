package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.log.logDebug

/**
 * This is a request to join a group prior to WS API 18
 */
class JoinGroupRequestV1(val token: String, val inviterName: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().joinOpenGroup("$inviterName/$token").enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        logDebug { "joined group via open invitation :)" }
        StreamCacheRepo.updateStreamInStreams(stream)
    }
}