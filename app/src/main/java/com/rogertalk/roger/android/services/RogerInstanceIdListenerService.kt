package com.rogertalk.roger.android.services


import android.content.Intent
import com.google.android.gms.iid.InstanceIDListenerService
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.runOnUiThread

class RogerInstanceIdListenerService : InstanceIDListenerService() {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    override fun onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        runOnUiThread {
            val intent = Intent(appController(), RegisterDeviceIntentService::class.java)
            startService(intent)
        }
    }
}
