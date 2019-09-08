package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.StreamAddParticipantsFailEvent
import com.rogertalk.roger.event.success.StreamAddParticipantsSuccessEvent
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import okhttp3.ResponseBody


class StreamAddParticipantsRequest(val streamId: Long, val participants: List<String>,
                                   val deviceContactsToMatch: List<DeviceContactInfo>?) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().streamAddParticipants(streamId, participants).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        // Try to match device contacts if provided
        if (deviceContactsToMatch != null) {
            ActiveContactsRequest(deviceContactsToMatch).enqueueRequest()
        }

        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
        }
        postEvent(StreamAddParticipantsSuccessEvent(stream))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(StreamAddParticipantsFailEvent())
    }
}