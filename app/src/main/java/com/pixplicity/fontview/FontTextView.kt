package com.pixplicity.fontview

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView

import com.pixplicity.fontview.utils.FontUtil

/**
 * Extension of [TextView] to cope with custom typefaces. Specify the desired font using the
 * `font=&quot;myfont.ttf&quot;` attribute, or specify it directly using [.setCustomTypeface].
 *
 *
 * Typeface management is regulated through [FontUtil].
 *

 * @author Pixplicity
 */
class FontTextView : TextView {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setCustomTypeface(attrs, android.R.attr.textViewStyle)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setCustomTypeface(attrs, defStyle)
    }

    fun setCustomTypeface(font: String) {
        val tf = FontUtil.getTypeface(context, font)
        setCustomTypeface(tf)
    }

    private fun setCustomTypeface(attrs: AttributeSet, defStyle: Int) {
        val tf = FontUtil.getTypeface(context, attrs, defStyle)
        setCustomTypeface(tf)
    }

    private fun setCustomTypeface(tf: Typeface?) {
        paintFlags = paintFlags or Paint.SUBPIXEL_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        typeface = tf
    }

}
