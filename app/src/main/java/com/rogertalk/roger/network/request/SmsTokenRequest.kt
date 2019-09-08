package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.success.SmsTokenAnswerSuccessEvent
import com.rogertalk.roger.models.json.SmsTokenAnswer
import com.rogertalk.roger.utils.extensions.postEvent

class SmsTokenRequest(val code: String) : BaseRequest() {

    override fun <T : Any> handleSuccess(t: T) {
        val smsTokenAnswer = t as? SmsTokenAnswer ?: return
        postEvent(SmsTokenAnswerSuccessEvent(smsTokenAnswer))
    }

    override fun enqueueRequest() {
        val callback = getCallback(SmsTokenAnswer::class.java)
        getRogerAPI().smsToken(code).enqueue(callback)
    }
}