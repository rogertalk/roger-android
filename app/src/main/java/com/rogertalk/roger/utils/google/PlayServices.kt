package com.rogertalk.roger.utils.google

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.rogertalk.roger.utils.log.logError


class PlayServices() {

    companion object {

        fun checkPlayServices(ctx: Context): Boolean {
            try {
                val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx)
                if (resultCode != ConnectionResult.SUCCESS) {
                    if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                        logError { "No Google Play services on this device, or they are not up-to-date" }
                    }
                    return false
                }
                return true
            } catch (e: Exception) {
                logError(e) { "Could not check Google Play Service availability" }
            }
            return false
        }

        /**
         * Similar to @checkPlayServices, but only takes in account if PlayServices are installed,
         * not if they're updated. Useful for GCM registration.
         */
        fun playServicesInstalled(ctx: Context): Boolean {
            val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx)
            when (resultCode) {
                ConnectionResult.SUCCESS -> return true
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> return true
                else -> return false
            }
        }
    }

}