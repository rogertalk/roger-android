package com.rogertalk.roger.utils.extensions

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText

/**
 * Execute a given function when user presses Enter key on this control
 */
fun EditText.onEnterKey(func: () -> Any) {
    this.setOnKeyListener { view, keyCode, keyEvent ->
        if ((keyEvent.action == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER)) {
            func()
            return@setOnKeyListener true
        }
        false
    }
}

/**
 * Account for statusbar padding for this view
 */
fun View.statusBarPadding(activity: Activity) {
    this.setPadding(0, activity.getStatusBarHeight(), 0, 0)
}

/**
 * Account for statusbar padding for this view
 */
fun View.removeStatusBarPadding() {
    this.setPadding(0, 0, 0, 0)
}