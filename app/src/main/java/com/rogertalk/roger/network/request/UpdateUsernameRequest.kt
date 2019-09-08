package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.UsernameUpdateFailEvent
import com.rogertalk.roger.event.success.UsernameUpdateSuccessEvent
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody


class UpdateUsernameRequest(val username: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Account::class.java)
        getRogerAPI().changeUsername(username).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val account = t as? Account ?: return
        postEvent(UsernameUpdateSuccessEvent(account))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(UsernameUpdateFailEvent(responseCode))
    }
}