package com.rogertalk.roger.android.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.greenrobot.eventbus.EventBus

open class EventService() : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}