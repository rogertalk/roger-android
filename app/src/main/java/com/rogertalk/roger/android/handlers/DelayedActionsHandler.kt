package com.rogertalk.roger.android.handlers

import android.os.Message
import java.lang.ref.WeakReference

class DelayedActionsHandler(listener: DelayedActionListener, private val frequencyMillis: Long = 500, var actionCode: Int) : GoodHandler() {

    companion object {
        private val RUN_ACTION = 1
    }

    private val listenerRef: WeakReference<DelayedActionListener>

    init {
        listenerRef = WeakReference<DelayedActionListener>(listener)
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            RUN_ACTION -> if (listenerRef.get() != null) {
                listenerRef.get().delayedAction(actionCode)
                reRun()
            }

        }
    }

    override fun removeMessages() {
        removeMessages(RUN_ACTION)
    }

    fun startNow() {
        // Remove any previous pending messages first.
        removeMessages(RUN_ACTION)
        sendEmptyMessageDelayed(RUN_ACTION, frequencyMillis)
    }

    private fun reRun() {
        sendEmptyMessageDelayed(RUN_ACTION, frequencyMillis)
    }
}
