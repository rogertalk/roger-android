package com.rogertalk.roger.utils.log

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.android.AppController

/**
 * Logging extensions.
 * On __Production builds__ will only log Error and Warnings.
 *
 * Extra info is provided as a method, this way for complex operations (costlier in terms of performance)
 * the result is not calculated on Production builds.
 */

fun logSimpleText(content: () -> String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    Log.d("Roger", content())
}

fun logDebug(content: () -> String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val logElem = LogElem(Throwable().stackTrace)
    Log.d(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")
}

fun logInfo(content: () -> String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val logElem = LogElem(Throwable().stackTrace)
    Log.i(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")
}

fun logVerbose(content: () -> String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val logElem = LogElem(Throwable().stackTrace)
    Log.v(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")
}


fun logWarn(content: () -> String) {
    val logElem = LogElem(Throwable().stackTrace)
    Log.w(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")

    // Log To Crashlytics on Production builds
    if (AppController.instance.canUseFabric()) {
        Crashlytics.log("Warn - ${content()}")
    }
}

fun logCrashlytics(content: () -> String) {
    // Log To Crashlytics on Production builds
    if (AppController.instance.canUseFabric()) {
        Crashlytics.log(content())
    } else {
        logSimpleText { content() }
    }
}

fun logError(content: () -> String) {
    val logElem = LogElem(Throwable().stackTrace)
    Log.e(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")

    // Log To Crashlytics on Production builds
    if (AppController.instance.canUseFabric()) {
        Crashlytics.log(content())
    }
}

fun logError(e: Throwable, logToCrashlytics: Boolean = false, content: () -> String) {
    val logElem = LogElem(e.stackTrace)
    Log.e(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${content()}")

    // Log To Crashlytics on Production builds
    if (logToCrashlytics && AppController.instance.canUseFabric()) {
        Crashlytics.log(content())
        Crashlytics.logException(e)
    } else {
        // On debug builds print stack trace to logcat
        e.printStackTrace()
    }
}

fun logError(e: Throwable) {
    val logElem = LogElem(e.stackTrace)
    Log.e(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) - ${e.message ?: ""}")

    // Log To Crashlytics on Production builds
    if (AppController.instance.canUseFabric()) {
        Crashlytics.logException(e)
    } else {
        // On debug builds print stack trace to logcat
        e.printStackTrace()
    }
}

fun logMethodCall() {
    if (!BuildConfig.DEBUG) {
        return
    }
    val logElem = LogElem(Throwable().stackTrace)
    Log.v(logElem.className, "${logElem.methodName}(${logElem.lineNumber}) Called!")
}

fun logEvent(event: Any) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val eventName = event.javaClass.simpleName
    val logElem = LogElem(Throwable().stackTrace)
    Log.v(logElem.className, "$eventName(${logElem.lineNumber}) fired by ${logElem.getEventCallerTrace()}()")
}