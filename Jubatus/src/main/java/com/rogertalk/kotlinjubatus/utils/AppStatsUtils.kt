package com.rogertalk.kotlinjubatus.utils

import android.content.Context

class AppStatsUtils {

    companion object {

        fun getAppVersionCode(context: Context): String {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName ?: ""
        }

        fun getAppVersionNumber(context: Context): Int {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionCode
        }
    }
}
