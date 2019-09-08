package com.rogertalk.roger.utils.extensions

import android.app.ProgressDialog

/**
 * Safely show a progress dialog
 */
fun ProgressDialog.safeShow() {
    try {
        show()
    } catch (ignored: Exception) {
    }
}