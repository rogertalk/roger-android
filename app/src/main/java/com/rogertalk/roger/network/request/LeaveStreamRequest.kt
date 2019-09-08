package com.rogertalk.roger.network.request

import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.log.logInfo
import okhttp3.ResponseBody

class LeaveStreamRequest(val streamId: Long) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Any::class.java)
        getRogerAPI().leaveGroup(streamId).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        logInfo { "Successfully left streamId: $streamId" }
        StreamCacheRepo.removeStream(streamId)
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)

        // Failed, so request Streams again to refresh content
        StreamsRequest().enqueueRequest()
    }
}