package com.rogertalk.kotlinjubatus

import android.os.Build

fun runIfNewerThan(apiVersionNumber: Int, func: () -> Any) {
    if (Build.VERSION.SDK_INT > apiVersionNumber) {
        func()
    }
}
