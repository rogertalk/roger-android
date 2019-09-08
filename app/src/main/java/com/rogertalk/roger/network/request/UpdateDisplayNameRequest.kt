package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.DisplayNameUpdateFailEvent
import com.rogertalk.roger.event.success.DisplayNameUpdateSuccessEvent
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class UpdateDisplayNameRequest(val displayName: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Account::class.java)
        getRogerAPI().changeDisplayName(displayName).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val account = t as? Account ?: return
        postEvent(DisplayNameUpdateSuccessEvent(account))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(DisplayNameUpdateFailEvent())
    }
}