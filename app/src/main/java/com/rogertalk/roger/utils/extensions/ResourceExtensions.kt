package com.rogertalk.roger.utils.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat

fun Int.colorResource(context: Context = appController()): Int {
    return ContextCompat.getColor(context, this)
}

fun Int.drawableResource(context: Context = appController()): Drawable {
    return ContextCompat.getDrawable(context, this)
}

fun Int.stringResource(vararg formatArgs: Any, context: Context = appController()): String {
    return context.resources.getString(this, *formatArgs)
}

fun Int.pluralResource(quantity: Int, vararg formatArgs: Any, context: Context = appController()): String {
    return context.resources.getQuantityString(this, quantity, *formatArgs)
}

fun Int.dimensionResource(context: Context = appController()): Float {
    return context.resources.getDimension(this)
}

fun Int.dimensionResourceInPixels(context: Context = appController()): Int {
    return context.resources.getDimensionPixelSize(this)
}