package com.rogertalk.roger.ui.screens.base

import com.rogertalk.roger.event.broadcasts.ReAuthenticateEvent
import com.rogertalk.roger.ui.screens.AddContactInfoActivity
import com.rogertalk.roger.utils.log.logEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN


open class EventAppCompatActivity(open val logOutIfUnauthorized: Boolean) : BaseAppCompatActivity() {

    //
    // OVERRIDE METHODS
    //

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    //
    // PUBLIC METHODS
    //


    //
    // PRIVATE METHODS
    //


    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onReAuthenticateRequested(event: ReAuthenticateEvent) {
        if (logOutIfUnauthorized) {
            logEvent(event)
            startActivity(AddContactInfoActivity.startOnBoarding(this))
            finish()
        }
    }
}