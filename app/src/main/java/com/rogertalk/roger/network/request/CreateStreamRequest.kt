package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.CreateStreamFailEvent
import com.rogertalk.roger.event.success.CreateStreamSuccessEvent
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody
import java.util.*

class CreateStreamRequest(val participantHandles: ArrayList<String>?,
                          val participantUniqueId: Long?,
                          val nameOnDevice: String,
                          val deviceContactList: List<DeviceContactInfo>?,
                          val showInRecents: Boolean,
                          val updateImmediately: Boolean = false) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        val participantsList = ArrayList<String>()

        if (participantHandles != null) {
            participantsList.addAll(participantHandles)
        } else {
            participantsList.add(participantUniqueId.toString())
        }

        val isShareable: Boolean
        if (participantsList.size > 1) {
            isShareable = true
        } else {
            isShareable = false
        }


        getRogerAPI().createStream(participantsList, showInRecents, isShareable).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        // Inject name in stream if none from server
        if (stream.title.isNullOrEmpty()) {
            stream.customTitle = nameOnDevice
        }
        if (updateImmediately) {
            StreamCacheRepo.updateStreamInStreams(stream)
        } else {
            postEvent(CreateStreamSuccessEvent(stream, deviceContactList))
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        postEvent(CreateStreamFailEvent())
    }
}