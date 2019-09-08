package com.rogertalk.roger.network.request.reporting

import android.os.Build
import com.rogertalk.kotlinjubatus.utils.DeviceUtils
import com.rogertalk.roger.network.request.BaseRequest


class StereoUnavailableRequest() : BaseRequest() {

    override fun enqueueRequest() {
        val model = DeviceUtils.deviceName
        val osVersion = Build.VERSION.SDK_INT

        val callback = getCallback(Any::class.java)
        getRogerAPI().reportStereoFailure(eventName = "stereo-unavailable",
                model = model,
                osVersion = osVersion).enqueue(callback)
    }
}