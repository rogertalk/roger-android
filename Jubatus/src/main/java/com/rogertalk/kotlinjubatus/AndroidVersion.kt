package com.rogertalk.kotlinjubatus

import android.os.Build

object AndroidVersion {

    private val version: Int
        get() = Build.VERSION.SDK_INT

    fun toApi(toVersion: Int, inclusive: Boolean = false, action: () -> Unit) {
        if (version < toVersion || (inclusive && version == toVersion)) action()
    }

    fun toApiVal(toVersion: Int, inclusive: Boolean = false): Boolean {
        return (version < toVersion || (inclusive && version == toVersion))
    }

    fun fromApi(fromVersion: Int, inclusive: Boolean = true, action: () -> Unit) {
        if (version > fromVersion || (inclusive && version == fromVersion)) action()
    }

    fun fromApiVal(fromVersion: Int, inclusive: Boolean = true): Boolean {
        return (version > fromVersion || (inclusive && version == fromVersion))
    }

}