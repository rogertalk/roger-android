package com.rogertalk.roger.network.request


import com.rogertalk.roger.models.json.DeviceRegister
import com.rogertalk.roger.utils.android.DeviceUUID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logDebug

class DeviceTokenRequest(val deviceToken: String) : BaseRequest() {

    override fun enqueueRequest() {
        if (appController().lastGCMToken == deviceToken) {
            // Don't send GCM token to server, since it matches the previous one
            return
        }

        val callback = getCallback(DeviceRegister::class.java)
        val deviceId = DeviceUUID.getDeviceUUID()
        getRogerAPI().device(deviceToken, deviceId, "gcm").enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        // Save as last GCM Token uploaded with success to our server
        appController().lastGCMToken = deviceToken

        val deviceRegister = t as? DeviceRegister ?: return
        logDebug { "Was successful: " + deviceRegister.success }
    }
}