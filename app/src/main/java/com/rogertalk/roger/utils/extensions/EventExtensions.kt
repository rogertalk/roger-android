package com.rogertalk.roger.utils.extensions

import org.greenrobot.eventbus.EventBus


fun postEvent(obj: Any) {
    EventBus.getDefault().post(obj)
}