package com.rogertalk.roger.android.tasks

import com.rogertalk.roger.event.broadcasts.ActiveContactAvailableEvent
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.network.request.ActiveContactsRequest
import com.rogertalk.roger.repo.ActiveContactsRepo
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logWarn
import org.jetbrains.anko.doAsync
import java.util.*

object ActiveContactsTask {

    private var lastScan = NO_TIME
    private var SCAN_PERIODICITY = 600000L

        fun updateActiveContacts(contactsList: MutableCollection<DeviceContactInfo>) {
            val timeNow = Date().time
            if ((timeNow - lastScan) > SCAN_PERIODICITY) {
                // Only scan again if there aren't any ActiveContacts mapped yet
                if (ActiveContactsRepo.activeContacts.isNotEmpty()) {
                    logDebug { "Active contacts are still fresh" }
                    postEvent(ActiveContactAvailableEvent())
                    return
                }
            }

            // Save timestamp
            lastScan = timeNow

            logInfo { "Will query active contacts" }
            doAsync {
                val activeContacts = ActiveContactsRequest(contactsList.toList()).executeRequest()

                if (activeContacts == null) {
                    logWarn { "Active contacts came back null" }
                } else {
                    logDebug { "Got active contacts answer. Count: ${activeContacts.body().activeContactsMap.size}" }

                    // Persist active contacts
                    val contactsMap = HashMap(activeContacts.body().activeContactsMap)
                    ActiveContactsRepo.matchActiveContactsWithContacts(contactsMap)
                }
            }
        }

}