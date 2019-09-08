package com.rogertalk.kotlinjubatus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

fun Context.openWebpageIntent(webpageURL: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(webpageURL)
    this.startActivity(intent)
}

/**
 * If True, there are possible consumers for this intent, so it is safe to call
 */
fun Intent.isSafe(context: Context): Boolean {
    val packageManager = context.packageManager
    val activities = packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
    return activities.isNotEmpty()
}

/**
 * Similar to isSafe(), but for Intent Choosers.
 * If True, there are possible consumers for this intent, so it is safe to call
 */
fun Intent.isSafeChooser(context: Context): Boolean {
    return (this.resolveActivity(context.packageManager) != null)
}