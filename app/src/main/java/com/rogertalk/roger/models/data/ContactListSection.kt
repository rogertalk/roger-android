package com.rogertalk.roger.models.data

import com.rogertalk.roger.ui.adapters.ContactPickerAdapter.ContactSectionType.*
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.isNumeric
import com.rogertalk.roger.utils.misc.NameUtils
import com.rogertalk.roger.utils.phone.PhoneUtils
import java.util.*

/**
 * This class represents the various components of a section in Contacts List.
 * That means it contains the section display, list elements to present, filtering,
 * and handles unique ids, etc.
 */
class ContactListSection(var contacts: List<DeviceContactInfo>,
                         val sectionName: String,
                         val sectionPosition: Int,
                         val searchSection: Boolean = false,
                         var displayPermissionElement: Boolean = false) {

    companion object {
        private val SECTION_MAX_SIZE = 100000L
    }

    var filteredContacts = ArrayList<DeviceContactInfo>()
    var filterIsActive = false

    // We increment this number every time a search is performed in a search section so that the
    // generated IDs are always different
    var searchContactCount = 0

    init {
        filteredContacts.addAll(contacts)
    }

    //
    // PUBLIC METHODS
    //

    fun updateContacts(newContacts: List<DeviceContactInfo>) {
        contacts = newContacts
        filteredContacts = ArrayList<DeviceContactInfo>()
        filteredContacts.addAll(contacts)
    }

    fun updateDisplayPermissionElement(newState: Boolean) {
        displayPermissionElement = newState
    }

    fun getSectionSize(): Int {
        if (!shouldRender()) {
            // This section won't be displayed at all
            return 0
        }

        // Add section header
        var extraToAdd = 1
        if (displayPermissionElement) {
            // Add permission element
            extraToAdd += 1
        }

        return filteredContacts.size + extraToAdd
    }

    fun filterContacts(comparableName: String) {
        if (comparableName.length > 1) {
            filterIsActive = true
        } else {
            filterIsActive = false
        }

        // Search query to short, display all contacts
        if (comparableName.length <= 1) {
            // Reset filtered contacts
            filteredContacts = ArrayList<DeviceContactInfo>(contacts)
            return
        }

        // Add element on search section
        if (searchSection) {
            searchContactCount++
            val searchAliases = HashSet<ContactLabel>(1)

            var actualSearchText = comparableName
            if (actualSearchText.isNumeric()) {
                val countryNumber = PhoneUtils.getCountryPrefix(appController())
                actualSearchText = "$countryNumber $actualSearchText"
            }
            if (actualSearchText.startsWith("+")) {
                searchAliases.add(ContactLabel(actualSearchText, "mobile"))
            } else {
                searchAliases.add(ContactLabel(actualSearchText, "username"))
            }
            val uniqueID = SECTION_MAX_SIZE * sectionPosition + searchContactCount
            val searchContact = DeviceContactInfo(uniqueID, actualSearchText, null, searchAliases, activeOnRoger = false, isSearchContact = true)
            filteredContacts = ArrayList<DeviceContactInfo>(1)
            filteredContacts.add(searchContact)
            return
        }

        // Update filtered contact list otherwise
        filteredContacts = ArrayList<DeviceContactInfo>(contacts.size)
        filteredContacts.addAll(contacts.filter { containsContact(it, comparableName) })
    }

    fun getContactForPosition(givenPosition: Int): DeviceContactInfo {
        // Subtract the section header position and permission element if available
        var finalPosition = givenPosition - 1
        if (displayPermissionElement) {
            finalPosition -= 1
        }

        return filteredContacts[finalPosition]
    }


    fun getUniqueElementId(realPosition: Int): Long {
        // Subtract the section header position
        var finalPosition = realPosition - 1

        // Section header
        if (realPosition == 0) {
            return SECTION_MAX_SIZE * sectionPosition
        }

        // Permission element
        if (displayPermissionElement) {
            if (realPosition == 1) {
                return SECTION_MAX_SIZE * sectionPosition - 2
            }

            // Permission request element is displaying, shift final position 1 more unit
            finalPosition -= 1
        }

        // Element from filtered contacts
        return filteredContacts[finalPosition].internalId + (SECTION_MAX_SIZE * sectionPosition)
    }

    fun getItemType(realPosition: Int): Int {
        // Section header
        if (realPosition == 0) {
            return SECTION.ordinal
        }

        // Permission element
        if (displayPermissionElement && realPosition == 1) {
            return REQUEST_PERMISSION.ordinal
        }

        // Element from filtered contacts
        return CONTENT.ordinal
    }


    fun shouldRender(): Boolean {
        if (filteredContacts.size > 0 || displayPermissionElement) {
            return true
        }
        return false
    }

    //
    // PRIVATE METHODS
    //

    private fun containsContact(deviceContactInfo: DeviceContactInfo, searchTerm: String): Boolean {
        // Compare with name
        val contactName = NameUtils.comparableName(deviceContactInfo.displayName)
        if (contactName.contains(searchTerm)) {
            return true
        }

        // Compare with numbers
        if (deviceContactInfo.aliases.any { it.value.contains(searchTerm) }) {
            return true
        }

        return false
    }

}