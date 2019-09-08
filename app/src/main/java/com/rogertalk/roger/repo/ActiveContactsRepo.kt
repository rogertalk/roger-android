package com.rogertalk.roger.repo

import com.rogertalk.roger.event.broadcasts.ActiveContactAvailableEvent
import com.rogertalk.roger.event.broadcasts.ActiveContactUpdatedEvent
import com.rogertalk.roger.models.json.ActiveContact
import com.rogertalk.roger.utils.extensions.postEvent
import java.util.*

object ActiveContactsRepo {

    // Map phone number to ActiveContact instance (which contains active state and accountID)
    var activeContacts: HashMap<String, ActiveContact>

    // Maps a Roger account ID with a individual ID from device contacts
    val rogerToDeviceContactMap: HashMap<Long, Long>

    init {
        activeContacts = HashMap<String, ActiveContact>()
        rogerToDeviceContactMap = HashMap<Long, Long>()

        // Recover previous values from persisted storage
        if (PrefRepo.hasActiveContactsMap()) {
            activeContacts.putAll(PrefRepo.activeContactsMap)
        }

        if (PrefRepo.hasContactsMap()) {
            rogerToDeviceContactMap.putAll(PrefRepo.rogerToDeviceContactMap)
        }
    }

    /**
     * This method also persists the matches
     */
    fun matchActiveContactsWithContacts(contacts: HashMap<String, ActiveContact>) {
        // Remove self from actives list
        val userAccountId = UserAccountRepo.id()

        // Start with previous active contacts map
        val finalContacts = PrefRepo.activeContactsMap

        // Add new contacts, except self account
        if (userAccountId != null) {
            finalContacts.putAll(contacts.filter { it.value.accountId != userAccountId })
        } else {
            finalContacts.putAll(contacts)
        }

        // Save to persisted storage
        PrefRepo.activeContactsMap = finalContacts

        // Persist to memory
        activeContacts = finalContacts

        // Update mapping of device contacts
        ContactMapRepo.updateDeviceContactsWithActive()

        // Signal app to react to this change if necessary
        postEvent(ActiveContactUpdatedEvent())
        postEvent(ActiveContactAvailableEvent())
    }
}