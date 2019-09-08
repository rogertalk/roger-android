package com.rogertalk.roger.models.migrations

import android.content.Context
import android.content.SharedPreferences
import com.orhanobut.hawk.Hawk
import com.rogertalk.roger.models.data.AccountStatus
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logWarn
import java.util.*

class MigrateSessionData {

    companion object {
        private val SESSION_TEMP = "sessionTmp"


        private fun sessionDataNeedsMigration(): Boolean {
            if (Hawk.contains(PrefRepo.SESSION_V1) && !Hawk.contains(PrefRepo.SESSION_V2)) {
                return true
            }
            return false
        }

        fun migrateSessionData() {
            if (!sessionDataNeedsMigration()) {
                return
            }
            logDebug { "Session data is going to be migrated" }

            val context = appController()
            val preferences: SharedPreferences = context.getSharedPreferences("HAWK", Context.MODE_PRIVATE)
            val originalSessionString = preferences.getString(PrefRepo.SESSION_V1, "")
            logDebug { "Original session: $originalSessionString" }

            val indexOfSeparator = originalSessionString.indexOf("@")
            val newSessionString = "java.lang.String#java.lang.Object#2V" + originalSessionString.substring(indexOfSeparator)

            logDebug { "New modified session : $newSessionString" }
            val editor = preferences.edit()
            editor.putString(SESSION_TEMP, newSessionString)
            editor.apply()

            var savedMap = HashMap<String, Object>()
            savedMap = Hawk.get(SESSION_TEMP)

            val accessToken = savedMap["access_token"].toString()
            val refreshToken = savedMap["refresh_token"].toString()

            if (accessToken.isNullOrBlank()) {
                logWarn { "We didn't get access token!" }
                return
            }

            val mockAccount = Account(NO_ID, "", true, "", null, null, null, false, null,
                    false, null, AccountStatus.ACTIVE.text, null)



            // Build new Session data
            val newSession = Session(refreshToken, accessToken, 3600, "active", mockAccount, LinkedList())

            // Save new session to variable
            Hawk.put(PrefRepo.SESSION_V2, newSession)

            // Erase Session V1 data and temporary data
            Hawk.remove(PrefRepo.SESSION_V1)
            Hawk.remove(SESSION_TEMP)
        }
    }
}