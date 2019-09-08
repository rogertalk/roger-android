package com.rogertalk.kotlinjubatus

import android.view.View

/**
 * View utilities
 */

/**
 * Change view visibility to GONE
 */
fun View.beGone(animated: Boolean = false) {
    if (animated) {
        if (visibility != View.GONE) {
            makeTransparent()
            fadeOut { beGone() }
        }
    } else {
        visibility = View.GONE
    }
}

/**
 * Change view visibility to VISIBLE
 */
fun View.makeVisible(animated: Boolean = false) {
    if (animated) {
        if (visibility != View.VISIBLE) {
            makeTransparent()
            visibility = View.VISIBLE
            fadeIn()
        }
    } else {
        visibility = View.VISIBLE
    }
}

/**
 * Change view visibility to INVISIBLE
 */
fun View.makeInvisible() {
    visibility = View.INVISIBLE
}

/**
 * Check if the view is visible
 */
fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

/**
 * Check if the view is either invisible or gone
 */
fun View.isInvisibleOrGone(): Boolean {
    return visibility != View.VISIBLE
}