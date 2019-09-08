package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ImmediateConvoFailEvent
import com.rogertalk.roger.event.success.ImmediateConvoSuccessEvent
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody
import java.util.*

class ImmediateConversationRequest(val participantUniqueId: Long?,
                                   val nameOnDevice: String,
                                   val deviceContactList: List<DeviceContactInfo>?) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        val participantsList = ArrayList<String>(1)
        participantsList.add(participantUniqueId.toString())

        getRogerAPI().createStream(participantsList, true, true).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        // Inject name in stream if none from server
        if (stream.title.isNullOrEmpty()) {
            stream.customTitle = nameOnDevice
        }

        // Update last interaction
        stream.lastInteraction = Date().time

        // Update cache immediately
        StreamCacheRepo.updateStreamInStreams(stream)

        // Notify app
        postEvent(ImmediateConvoSuccessEvent(stream))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        postEvent(ImmediateConvoFailEvent())
    }
}
