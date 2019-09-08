package com.rogertalk.roger.utils.extensions

import com.rogertalk.roger.android.AppController
import com.rogertalk.roger.android.AppControllerHelper

/*
 * This class contains App-Wise extensions. And easy way to get the Application class and dependant classes and managers.
 */

fun appController(): AppController {
    return AppController.instance
}

/**
 * Post a runnable to run on the main application handler
 */
fun runOnUiThread(func: () -> Unit) {
    val runnable = java.lang.Runnable(func)
    AppController.instance.applicationHandler.post(runnable)
}

fun runOnUiDelayed(delayMillis: Long, func: () -> Unit) {
    val runnable = java.lang.Runnable(func)
    AppController.instance.applicationHandler.postDelayed(runnable, delayMillis)
}

fun appHelper(): AppControllerHelper {
    return AppController.instance.helper
}