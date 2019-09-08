package com.rogertalk.roger.repo

import com.rogertalk.roger.utils.extensions.appHelper

object TaskStateRepo {
    fun get(key: Int): Boolean {
        return appHelper().taskStates[key] ?: false
    }

    fun set(key: Int, state: Boolean) {
        appHelper().taskStates[key] = state
    }
}
