package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.android.services.UploadRetryManager
import com.rogertalk.roger.event.broadcasts.ConnectivityChangedEvent
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.utils.extensions.postEvent


class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            // the info that is contained on the intent respect to a single network interface,
            // so instead of trying to figure out if there is internet, we simple re-issue internet
            // verification to the app
            postEvent(ConnectivityChangedEvent())

            // re-upload content if network is up again
            if (context != null && ConnectivityHelper.isConnected(context)) {
                UploadRetryManager.run()
            }
        }
    }

}