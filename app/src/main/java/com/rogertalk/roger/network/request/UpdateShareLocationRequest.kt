package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.ShareLocationFailEvent
import com.rogertalk.roger.event.success.ShareLocationSuccessEvent
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class UpdateShareLocationRequest(val share: Boolean) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Account::class.java)
        getRogerAPI().changeShareLocation(share).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val account = t as? Account ?: return
        postEvent(ShareLocationSuccessEvent(account))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(ShareLocationFailEvent())
    }
}