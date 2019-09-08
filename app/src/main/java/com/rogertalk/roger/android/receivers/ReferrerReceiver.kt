package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.rogertalk.roger.event.broadcasts.ReferrerInfoUpdatedEvent
import com.rogertalk.roger.models.json.ReferrerInfo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logMethodCall

class ReferrerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        logMethodCall()
        if (intent != null) {
            val referrer = intent.getStringExtra("referrer") ?: return
            val decodedReferrer: String
            try {
                val decoded = Base64.decode(referrer, Base64.DEFAULT)
                decodedReferrer = String(decoded)
            } catch(e: Exception) {
                logError(e) { "Failed to decode referrer" }
                return
            }

            val gson = GsonBuilder().create()
            val referrerInfo: ReferrerInfo
            try {
                referrerInfo = gson.fromJson(decodedReferrer, ReferrerInfo::class.java)
            } catch (e: JsonSyntaxException) {
                logError (e)
                return
            }

            // Persist it
            PrefRepo.referrerInfo = referrerInfo

            // Broadcast so the app can update itself
            postEvent(ReferrerInfoUpdatedEvent(referrerInfo))
        }
    }

}