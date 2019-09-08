package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ChallengeFailEvent
import com.rogertalk.roger.event.success.ChallengeSuccessEvent
import com.rogertalk.roger.models.json.Challenge
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class ChallengeRequest(val identifier: String, var call: Boolean = false) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Challenge::class.java)
        val call = getRogerAPI().challenge(identifier, call.toString())
        call.enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        postEvent(ChallengeSuccessEvent())
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(ChallengeFailEvent(responseCode))
    }
}