package com.rogertalk.roger.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.Button

class BlueButton : Button {

    private var fakeClickEnabled = true
    private var previousString = ""

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun callOnClick(): Boolean {
        if (fakeClickEnabled) {
            return super.callOnClick()
        }
        return true
    }

    override fun isClickable(): Boolean {
        if (!fakeClickEnabled) {
            return false
        }
        return super.isClickable()
    }

    override fun hasOnClickListeners(): Boolean {
        if (!fakeClickEnabled) {
            return false
        }
        return super.hasOnClickListeners()
    }

    override fun performClick(): Boolean {
        if (!fakeClickEnabled) {
            return false
        }
        return super.performClick()
    }

    fun setLoadingState(loading: Boolean) {
        if (loading) {
            if (previousString.isBlank()) {
                previousString = text.toString()
            }
            text = ""
            fakeClickEnabled = false
        } else {
            if (previousString.isNotBlank()) {
                text = previousString
            }
            fakeClickEnabled = true
        }

    }
}
