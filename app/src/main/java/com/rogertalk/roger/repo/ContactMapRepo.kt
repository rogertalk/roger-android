package com.rogertalk.roger.repo

import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.utils.extensions.appHelper
import com.rogertalk.roger.utils.log.logError
import java.util.*


object ContactMapRepo {

    fun isContactActive(accountId: Long): Boolean {
        val deviceId = deviceIdForRogerId(accountId) ?: return false
        return getDeviceContactMap()[deviceId]?.activeOnRoger ?: false
    }

    /**
     * Maps account ids to contact list objects.
     */
    fun getDeviceContactMap(): HashMap<Long, DeviceContactInfo> {
        return appHelper().deviceContacts
    }

    fun isDeviceContactMapCached(): Boolean {
        return appHelper().deviceContacts.isNotEmpty()
    }

    fun deviceIdForRogerId(rogerId: Long): Long? {
        return ActiveContactsRepo.rogerToDeviceContactMap[rogerId]
    }

    fun getRogerIdForDeviceId(deviceId: Long): Long? {
        val rogerToDeviceMap = ActiveContactsRepo.rogerToDeviceContactMap
        for ((rogerID, deviceID) in rogerToDeviceMap) {
            if (deviceID == deviceId) {
                return rogerID
            }
        }
        return null
    }

    /**
     * Update device contact map with active on Roger.
     */
    fun updateDeviceContactsWithActive() {
        val resultingDeviceContacts = getDeviceContactMap()
        val activeContacts = ActiveContactsRepo.activeContacts
        // TODO: This is a high complexity search which should be avoidable with a set of hash maps.
        for ((phoneNumber, activeContact) in activeContacts) {
            for ((contactID, deviceContact) in resultingDeviceContacts) {
                // Try to match device contact and active contact
                if (deviceContact.aliases.any { it.value == phoneNumber }) {
                    // Update In-Memory map between the Device contacts and Active Contacts
                    try {
                        updateRogerToDeviceContactMap(activeContact.accountId,
                                deviceContact.internalId)
                    } catch (e: ClassCastException) {
                        logError(e) { "ActiveContact class exception, resetting active contacts" }
                        PrefRepo.clearActiveContactData()
                        return
                    }

                    // Only update active state if this active contact was actually active on Roger
                    if (activeContact.active) {
                        deviceContact.activeOnRoger = true
                    }
                }
            }
        }
    }

    /**
     * Returns a given device contact if found, null otherwise
     */
    fun getDeviceContactFromAccountId(accountId: Long): DeviceContactInfo? {
        val deviceId = deviceIdForRogerId(accountId)
        return getDeviceContactMap()[deviceId]
    }

    /**
     * Return the display name for a given account id, empty if not found!
     */
    fun getContactDisplayName(accountId: Long): String? {
        val deviceId = deviceIdForRogerId(accountId) ?: return null
        return getDeviceContactMap()[deviceId]?.displayName ?: null
    }

    fun getContactAvatarURI(accountId: Long): String? {
        val deviceId = deviceIdForRogerId(accountId) ?: return null

        return getDeviceContactMap()[deviceId]?.photoURI ?: null
    }

    fun updateRogerToDeviceContactMap(accountId: Long, deviceId: Long) {
        ActiveContactsRepo.rogerToDeviceContactMap.put(accountId, deviceId)
        persistRogerToDeviceContactMap()
    }

    private fun persistRogerToDeviceContactMap() {
        PrefRepo.rogerToDeviceContactMap = ActiveContactsRepo.rogerToDeviceContactMap
    }

    fun setDeviceContactsList(deviceContactsMap: Map<Long, DeviceContactInfo>) {
        if (getDeviceContactMap().isEmpty()) {
            getDeviceContactMap().putAll(deviceContactsMap)
        } else {
            appHelper().deviceContacts = LinkedHashMap<Long, DeviceContactInfo>(deviceContactsMap)
        }

        // Update mapping between device contacts and active contacts
        ContactMapRepo.updateDeviceContactsWithActive()
    }
}