package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ParticipantRemovedFailEvent
import com.rogertalk.roger.event.success.ParticipantRemovedSuccessEvent
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody


class StreamRemoveParticipantsRequest(val streamId: Long, val participants: List<String>) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().streamRemoveParticipants(streamId, participants).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        StreamCacheRepo.updateStreamInStreams(stream)
        postEvent(ParticipantRemovedSuccessEvent())
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(ParticipantRemovedFailEvent())
    }
}