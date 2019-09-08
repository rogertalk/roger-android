package com.rogertalk.roger.network

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build.VERSION_CODES.LOLLIPOP
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.utils.extensions.appController
import org.jetbrains.anko.connectivityManager

object ConnectivityHelper {

        /**
         * Returns connectivity state for the currently active network interface
         */
        fun isConnected(context: Context): Boolean {
            val activeNetwork = context.connectivityManager.activeNetworkInfo
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
            return isConnected
        }

        /**
         * @return True if connected to a WiFi network, False otherwise
         */
        @TargetApi(21)
        fun isConnectedToWiFi(context: Context): Boolean {
            val connectivityManager = context.connectivityManager

            if (AndroidVersion.fromApiVal(LOLLIPOP, true)) {
                val networks = connectivityManager.allNetworks
                if (networks == null) {
                    return false
                } else {
                    for (network in networks) {
                        val info = connectivityManager.getNetworkInfo(network)
                        if (info != null && info.type == ConnectivityManager.TYPE_WIFI) {
                            if (info.isAvailable && info.isConnected) {
                                return true
                            }
                        }
                    }
                }
                return false
            } else {
                // Test WiFi connectivity on Android versions previous to Lollipop
                val info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                return info != null && info.isAvailable && info.isConnected
            }

        }

    /**
     * If true, user is not in a metered network and we can preload certain content that despite not
     * essential will speed up the app execution and improve offline behavior.
     */
    fun canUseExtraData(): Boolean {
        val connectivityManager = appController().connectivityManager
        return !connectivityManager.isActiveNetworkMetered
    }
}
