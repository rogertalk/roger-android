package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logWarn
import okhttp3.ResponseBody

class RefreshTokenRequest(val refreshToken: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Session::class.java)
        val call = getRogerAPI().refreshToken(refreshToken)
        call.enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val session = t as? Session ?: return
        runOnUiThread {
            SessionRepo.session = session
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        logWarn { "Failed to refresh session. Response code: $responseCode" }
    }
}