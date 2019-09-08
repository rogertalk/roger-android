package com.rogertalk.roger.ui.screens

import android.content.Intent
import android.os.Bundle
import com.rogertalk.roger.event.failure.SmsTokenAnswerFailEvent
import com.rogertalk.roger.event.success.SmsTokenAnswerSuccessEvent
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.network.request.SmsTokenRequest
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.screens.base.EventActivity
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

class SMSHandleActivity : EventActivity() {

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSmsIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleSmsIntent(intent)
        }
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun handleSmsIntent(intent: Intent) {
        val path = intent.data.toString()

        // get the token from the link
        val token = getToken(path) ?: return

        logDebug { "Request Token is: " + token }

        // request info from server
        SmsTokenRequest(token).enqueueRequest()
    }

    private fun getToken(urlPath: String): String? {
        val paramIndex = urlPath.indexOf('~')

        // param not found
        if (paramIndex == -1) {
            return null
        }

        return urlPath.substring(paramIndex + 1)
    }


    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onSmsTokenFail(event: SmsTokenAnswerFailEvent) {
        logError { "Failed to obtain SMS Token answer. Probable cause: no internet connection" }

        // just proceed to TalkActivity as normal
        startActivity(TalkActivityUtils.getStartTalkScreen(this))
    }

    @Subscribe(threadMode = MAIN)
    fun onSmsTokenSuccess(event: SmsTokenAnswerSuccessEvent) {
        logEvent(event)
        // TODO : do something with sender object from event metadata

        SessionRepo.session = event.smsTokenAnswer.session

        // check if we have a stream element
        if (event.smsTokenAnswer.stream.id != NO_ID) {
            val stream = event.smsTokenAnswer.stream
            logDebug { "We got stream content. ID: " + stream.id }

            StreamCacheRepo.updateStreamInStreams(stream)

            // Pre-select this stream
            StreamManager.selectedStreamId = stream.id

            startActivity(TalkActivityUtils.getStartTalkScreen(this))
        } else {
            startActivity(TalkActivityUtils.getStartTalkScreen(this))
        }

    }
}