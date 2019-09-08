package com.rogertalk.roger.ui.view

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout


class RelativeLayoutWithInverseBehavior : CoordinatorLayout.Behavior<RelativeLayout> {


    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: RelativeLayout?, dependency: View?): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: RelativeLayout?, dependency: View?): Boolean {
        val height = dependency?.height ?: 0
        val translationY = dependency?.translationY ?: 0f
        val newTranslation = Math.min(0f, translationY - height)
        child?.translationY = -1 * newTranslation
        return true
    }
}