package com.rogertalk.roger.ui.chips.views

import android.content.Context
import android.support.v7.widget.AppCompatEditText
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class ChipsEditText(context: Context, private val mInputConnectionWrapperInterface: ChipsEditText.InputConnectionWrapperInterface?) : AppCompatEditText(context) {

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        if (mInputConnectionWrapperInterface != null) {
            return mInputConnectionWrapperInterface.getInputConnection(super.onCreateInputConnection(outAttrs))
        }

        return super.onCreateInputConnection(outAttrs)
    }

    interface InputConnectionWrapperInterface {
        fun getInputConnection(target: InputConnection): InputConnection
    }
}
