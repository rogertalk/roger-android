package com.rogertalk.roger.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rogertalk.roger.manager.PendingNotificationManager
import com.rogertalk.roger.manager.PushNotificationManager
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.utils.log.logMethodCall

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        logMethodCall()
        if (context == null || intent == null) {
            return
        }

        if (SessionRepo.loggedIn()) {
            PushNotificationManager.initPushNotifications()
        }

        // Schedule verification of expiring chunks
        PendingNotificationManager.registerUnheardAudioCheckRepeat()
    }

}