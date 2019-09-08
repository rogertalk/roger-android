package com.rogertalk.roger.android.handlers

interface DelayedActionListener {

    /**
     * @param actionCode Numeric value used to distinguish callback from different simultaneous delayed actions
     */
    fun delayedAction(actionCode: Int)
}