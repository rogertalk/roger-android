package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ChallengeSecretFailEvent
import com.rogertalk.roger.event.success.ChallengeSecretCodeSuccessEvent
import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class ChallengeSecretRequest(val identifier: String, val secret: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Session::class.java)
        getRogerAPI().challengeRespond(identifier, secret).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val session = t as? Session
        if (session == null) {
            postEvent(ChallengeSecretFailEvent(-1))
            return
        }

        postEvent(ChallengeSecretCodeSuccessEvent(session))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(ChallengeSecretFailEvent(responseCode))
    }
}