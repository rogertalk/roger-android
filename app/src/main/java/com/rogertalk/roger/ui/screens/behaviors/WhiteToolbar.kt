package com.rogertalk.roger.ui.screens.behaviors

import android.animation.AnimatorSet
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.TextView
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.constant.MaterialIcon
import com.rogertalk.roger.utils.extensions.stringResource
import org.jetbrains.anko.textColor

interface WhiteToolbar {

    val _toolbar: Toolbar
    val _context: AppCompatActivity
    val _toolbarRightActionAnimation: AnimatorSet?

    fun setupToolbar(title: String, subtitle: String = "") {
        _context.setSupportActionBar(_toolbar)

        _context.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        _context.supportActionBar?.title = title
        _context.supportActionBar?.subtitle = subtitle
    }

    fun toolbarHasBackAction(pressAction: () -> Unit) {
        _context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        _toolbar.setNavigationOnClickListener({
            pressAction()
        })
    }

    fun setToolbarText(title: String) {
        _context.supportActionBar?.title = title
    }

    fun setRightActionIcon(materialIcon: MaterialIcon) {
        val topRightButton = _toolbar.findViewById(R.id.rightTopButton) as TextView
        topRightButton.text = materialIcon.text
    }

    fun setRightActionColor(color: Int) {
        val topRightButton = _toolbar.findViewById(R.id.rightTopButton) as TextView
        topRightButton.textColor = color
    }

    fun setRightActionContentDescription(stringResource: Int) {
        val topRightButton = _toolbar.findViewById(R.id.rightTopButton) as TextView
        topRightButton.contentDescription = stringResource.stringResource()
    }

    /**
     * Pulses the toolbar right action for a while to grab user's attention
     */
    fun startToolbarRightActionAnimation() {
        val animator = _toolbarRightActionAnimation ?: return
        if (!animator.isRunning) {
            animator.start()
        }
    }

    fun stopToolbarAnimations() {
        val animator = _toolbarRightActionAnimation ?: return
        animator.cancel()
    }
}
