package com.rogertalk.roger.utils.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat


fun hasPermission(context: Context, permissionName: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED
}