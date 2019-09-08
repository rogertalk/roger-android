package com.rogertalk.roger.utils.android

import android.app.Activity
import org.jetbrains.anko.inputMethodManager


class KeyboardUtils() {

    companion object {

        fun hideKeyboard(activity: Activity) {
            val view = activity.currentFocus
            view?.let {
                activity.inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }

        }

    }
}