package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.LocationUpdateFailEvent
import com.rogertalk.roger.event.success.LocationUpdateSuccessEvent
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.ResponseBody

class UpdateLocationRequest(val latitude: String, val longitude: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Account::class.java)
        getRogerAPI().updateLocation(getFormattedLocation()).enqueue(callback)
    }

    private fun getFormattedLocation(): String {
        return "$latitude,$longitude"
    }

    override fun <T : Any> handleSuccess(t: T) {
        val account = t as? Account ?: return
        postEvent(LocationUpdateSuccessEvent(account))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(LocationUpdateFailEvent())
    }
}