package com.rogertalk.roger.utils.extensions

import android.Manifest
import android.content.Context

fun Context.hasSamsungBadgePermission(): Boolean {
    return hasPermission(this, "com.sec.android.provider.badge.permission.WRITE")
}

fun Context.hasAudioRecordPermission(): Boolean {
    return hasPermission(this, Manifest.permission.RECORD_AUDIO)
}

fun Context.hasContactsPermission(): Boolean {
    return hasPermission(this, Manifest.permission.READ_CONTACTS)
}

fun Context.hasPhoneStatePermission(): Boolean {
    return hasPermission(this, Manifest.permission.READ_PHONE_STATE)
}

fun Context.hasSMSPermission(): Boolean {
    return hasPermission(this, Manifest.permission.READ_SMS)
}

fun Context.hasWriteToExternalStoragePermission(): Boolean {
    return hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

fun Context.hasCoarseLocationPermission(): Boolean {
    return hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
}

fun Context.hasDeviceAccountPermission(): Boolean {
    return hasPermission(this, Manifest.permission.GET_ACCOUNTS)
}