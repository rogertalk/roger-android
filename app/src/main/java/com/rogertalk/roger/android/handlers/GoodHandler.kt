package com.rogertalk.roger.android.handlers

import android.os.Handler


abstract class GoodHandler() : Handler() {

    abstract fun removeMessages()
}