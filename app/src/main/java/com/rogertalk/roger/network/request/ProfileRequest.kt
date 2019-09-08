package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ProfileFailEvent
import com.rogertalk.roger.event.success.ProfileSuccessEvent
import com.rogertalk.roger.models.json.Profile
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class ProfileRequest(val accountHandle: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Profile::class.java)
        getRogerAPI().userProfile(accountHandle).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val response = t as? Profile ?: return
        postEvent(ProfileSuccessEvent(response))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        postEvent(ProfileFailEvent())
    }
}