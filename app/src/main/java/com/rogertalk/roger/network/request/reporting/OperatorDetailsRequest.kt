package com.rogertalk.roger.network.request.reporting

import com.rogertalk.roger.network.request.BaseRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.phone.PhoneUtils


class OperatorDetailsRequest() : BaseRequest() {

    override fun enqueueRequest() {
        val ctx = appController()
        val operatorName = PhoneUtils.getPhoneOperator(ctx) ?: return
        val mcc = PhoneUtils.getPhoneMCC(ctx) ?: return
        val mnc = PhoneUtils.getPhoneMNC(ctx) ?: return

        val callback = getCallback(Any::class.java)
        getRogerAPI().reportOperator(operatorName = operatorName,
                mcc = mcc,
                mnc = mnc).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        super.handleSuccess(t)
        // Don't report details anymore
        PrefRepo.reportedOperatorDetails = true
    }
}