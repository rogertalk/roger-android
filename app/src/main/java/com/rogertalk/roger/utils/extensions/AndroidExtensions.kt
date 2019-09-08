package com.rogertalk.roger.utils.extensions

import android.content.Context
import com.rogertalk.roger.network.ConnectivityHelper

/**
 * Run the provided function if connected to a network
 */
inline fun runIfConnected(context: Context, func: () -> Any) {
    if (ConnectivityHelper.isConnected(context)) {
        func()
    }
}