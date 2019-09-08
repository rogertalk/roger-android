package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.rogertalk.kotlinjubatus.runIfNewerThan
import com.rogertalk.roger.event.broadcasts.streams.RefreshStreamsEvent
import com.rogertalk.roger.event.success.SecretCodeEvent
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logMethodCall

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val VALID_SENDER_ADDRESSES = arrayOf("Roger", "+14427776437",
                "+12156667044", "+16468876437", "31061", "98975")
    }


    //
    // OVERRIDE METHODS
    //

    override fun onReceive(context: Context?, intent: Intent?) {
        logMethodCall()
        if (context == null || intent == null) {
            return
        }

        runIfNewerThan(19) {
            // Check if this the correct intent action
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                // nothing
            } else {
                handleSMSContent(intent)
            }
        }
    }

    //
    // PUBLIC METHODS
    //

    fun handleSMSContent(intent: Intent) {
        val bundle = intent.extras
        val messages = bundle.get("pdus") as Array<*>
        for (message in messages) {
            val smsMessage = SmsMessage.createFromPdu(message as ByteArray)

            val senderAddress = smsMessage.originatingAddress

            // for now only accept messages from Roger dedicated phone number
            if ( VALID_SENDER_ADDRESSES.any { it == senderAddress }) {
                handleMessage(smsMessage.messageBody)
            }
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun handleMessage(messageBody: String) {
        // check if message start is a secret code
        val match = "^\\d{3}\\b".toRegex().find(messageBody)
        if (match == null) {
            logInfo { "SMS does not contain a secret code" }

            // probably this sms regards a message that was sent. fire broadcast to update Streams UI
            postEvent(RefreshStreamsEvent())
        } else {
            // broadcast event so app can auto-fill field
            postEvent(SecretCodeEvent(match.value))
        }
    }
}
