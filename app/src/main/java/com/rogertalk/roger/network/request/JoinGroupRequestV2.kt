package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.success.CreateStreamSuccessEvent
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug

/**
 * This is a request to join group starting on WS API 18
 */
class JoinGroupRequestV2(val inviteToken: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().joinOpenGroup(inviteToken).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        logDebug { "joined group via open invitation :)" }
        StreamCacheRepo.updateStreamInStreams(stream)
        postEvent(CreateStreamSuccessEvent(stream, null))
    }
}