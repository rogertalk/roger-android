package com.rogertalk.roger.utils.extensions

import android.app.Activity
import android.content.Context
import android.os.Build.VERSION_CODES.KITKAT
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.R


fun Activity.getStatusBarHeight(): Int {
    // Before kitkat there was no translucent statusbar, so no need to shift content
    if (AndroidVersion.toApiVal(KITKAT)) {
        return 0
    }
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}

fun Context.isTablet(): Boolean {
    return resources.getBoolean(R.bool.md_is_tablet)
}