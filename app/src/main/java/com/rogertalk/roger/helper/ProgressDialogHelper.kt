package com.rogertalk.roger.helper

import android.app.ProgressDialog
import android.content.Context
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.extensions.safeShow
import com.rogertalk.roger.utils.extensions.stringResource

/**
 * Use this class add Progress dialog handling to a screen.
 */
class ProgressDialogHelper(val context: Context) {

    private var pd: ProgressDialog? = null

    fun dismiss() {
        pd?.dismiss()
    }

    fun showWaiting() {
        showProgressDialog(R.string.wait_generic.stringResource())
    }

    fun showProgressDialog(title: String) {
        // Dismiss previous one if any
        pd?.dismiss()

        pd = ProgressDialog(context)
        pd?.setTitle(title)
        pd?.setCancelable(false)
        pd?.setCanceledOnTouchOutside(false)
        pd?.isIndeterminate = true
        pd?.safeShow()
    }


}
