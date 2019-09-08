package com.rogertalk.roger.models.data

import java.io.Serializable
import java.util.*

class DeviceContactInfo(
        val internalId: Long,
        val displayName: String,
        val photoURI: String?,
        val aliases: HashSet<ContactLabel>,
        var activeOnRoger: Boolean = false,
        var isSearchContact: Boolean = false,
        val customDescriptionMessage: String? = null) : Serializable {

    /**
     * Constructor that does not specify phone numbers or emails
     */
    constructor(internalId: Long, displayName: String, photoURI: String?) : this(internalId, displayName, photoURI,
            HashSet(0)) {
    }

    /**
     * Get a copy of this contact with just 1 phone number contact
     */
    private fun copyWithAlias(number: ContactLabel): DeviceContactInfo {
        val tmpNumbers = HashSet<ContactLabel>(1)
        tmpNumbers.add(number)
        return DeviceContactInfo(internalId, displayName, photoURI, tmpNumbers, activeOnRoger)
    }

    /**
     * Explode this contact and return all the exploded variants
     */
    fun explode(): ArrayList<DeviceContactInfo> {
        if (activeOnRoger || isSearchContact) {
            // This contact is active on Roger, so don't explode it
            val singleList = ArrayList<DeviceContactInfo>(1)
            singleList.add(this)
            return singleList
        }

        val explodedContacts = ArrayList<DeviceContactInfo>(aliases.size)
        for (alias in aliases) {
            explodedContacts.add(copyWithAlias(alias))
        }
        return explodedContacts
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as DeviceContactInfo

        if (internalId != other.internalId) return false

        return true
    }

    override fun hashCode(): Int {
        return internalId.hashCode()
    }

}