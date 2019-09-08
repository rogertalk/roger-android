package com.pixplicity.fontview.utils

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import com.rogertalk.roger.R
import java.util.*

object FontUtil {

    private val TYPEFACES = Hashtable<String, Typeface>()

    private fun getFontFromAttributes(context: Context, attrs: AttributeSet, defStyle: Int): String {
        var fontName = ""
        // Look up any layout-defined attributes
        // First obtain the textStyle
        var a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textStyle))
        var fontStyle = 0
        for (i in 0..a.indexCount - 1) {
            val attr = a.getIndex(i)
            when (attr) {
                0 -> fontStyle = a.getInt(attr, 0)
            }
        }

        a.recycle()
        // Do the same for our custom attribute set
        a = context.obtainStyledAttributes(
                attrs, R.styleable.FontTextView, defStyle, 0)

        for (i in 0..a.indexCount - 1) {
            val attr = a.getIndex(i)
            if (attr == R.styleable.FontTextView_pix_font) {
                if (TextUtils.isEmpty(fontName)) {
                    fontName = a.getString(attr)
                }
            } else if (attr == R.styleable.FontTextView_pix_fontBold) {
                if (TextUtils.isEmpty(fontName) || fontStyle and Typeface.BOLD != 0 && fontStyle and Typeface.ITALIC == 0) {
                    fontName = a.getString(attr)
                }

            } else if (attr == R.styleable.FontTextView_pix_fontItalic) {
                if (TextUtils.isEmpty(fontName) || fontStyle and Typeface.BOLD == 0 && fontStyle and Typeface.ITALIC != 0) {
                    fontName = a.getString(attr)
                }

            } else if (attr == R.styleable.FontTextView_pix_fontBoldItalic) {
                if (TextUtils.isEmpty(fontName) || fontStyle and Typeface.BOLD != 0 && fontStyle and Typeface.ITALIC != 0) {
                    fontName = a.getString(attr)
                }
            }
        }
        a.recycle()
        return fontName
    }

    fun getTypeface(context: Context, attrs: AttributeSet, defStyle: Int): Typeface? {
        return getTypeface(context, getFontFromAttributes(context, attrs, defStyle))
    }

    fun getTypeface(context: Context, fontName: String): Typeface? {
        var tf: Typeface? = TYPEFACES[fontName]
        if (tf == null) {
            try {
                tf = Typeface.createFromAsset(context.assets, fontName)
            } catch (e: Exception) {
                return null
            }

            TYPEFACES.put(fontName, tf)
        }
        return tf

    }

}// Forbid class creation
