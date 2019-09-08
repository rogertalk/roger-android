package com.rogertalk.roger.ui.screens.base

import android.graphics.Color
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.view.Gravity
import android.widget.TextView
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.R
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.getStatusBarHeight
import com.rogertalk.roger.utils.log.logError


open class EventSnackActivity : EventActivity() {

    private var snackbar: Snackbar? = null

    private fun displaySnackbar(stringRef: Int) {
        snackbar = Snackbar.make(findViewById(R.id.rel_parent), stringRef, Snackbar.LENGTH_INDEFINITE)

        val view = snackbar?.view
        AndroidVersion.fromApi(LOLLIPOP, true) {
            view?.elevation = 0f
        }
        val params = view?.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = getStatusBarHeight()
        view?.layoutParams = params

        AndroidVersion.toApi(LOLLIPOP, false) {
            snackbar?.let {
                try {
                    it.setActionTextColor(R.color.opaque_white.colorResource(this))
                    val tv = it.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
                    tv.setTextColor(Color.WHITE)
                } catch (e: Exception) {
                    logError(e) { "Could not change snackbar color" }
                }
            }
        }

        snackbar?.show()
    }

    private fun hideSnackbar() {
        snackbar?.dismiss()
    }

    override fun onResume() {
        super.onResume()

        // change connectivity display based on current state
        evaluateConnectivityState()
    }

    protected fun evaluateConnectivityState() {
        if (ConnectivityHelper.isConnected(this)) {
            hideSnackbar()
        } else {
            displaySnackbar(R.string.offline_message)
        }
    }
}