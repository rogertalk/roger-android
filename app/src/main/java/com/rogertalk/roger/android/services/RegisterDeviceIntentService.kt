package com.rogertalk.roger.android.services

import android.app.IntentService
import android.content.Intent
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import com.rogertalk.roger.R
import com.rogertalk.roger.network.request.DeviceTokenRequest
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logWarn

class RegisterDeviceIntentService : IntentService(RegisterDeviceIntentService.TAG) {

    companion object {
        private val TAG = "RegIntentService"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            logWarn { "Intent was null" }
        }

        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                val instanceID = InstanceID.getInstance(this)
                val token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)

                sendRegistrationToServer(token)
            }
        } catch (e: Exception) {
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            // TODO : try registering GCM again later
            logError(e) { "Failed to register to GCM" }
        }

    }

    /**
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String) {
        DeviceTokenRequest(token).enqueueRequest()
    }

}
