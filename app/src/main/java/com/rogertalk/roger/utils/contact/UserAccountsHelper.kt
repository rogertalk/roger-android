package com.rogertalk.roger.utils.contact


import android.content.Context
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.repo.DeviceProfileRepo
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logVerbose
import com.rogertalk.roger.utils.phone.PhoneUtils

class UserAccountsHelper {

    companion object {

        /**
         * Get own display name
         */
        fun getDisplayName(context: Context, deviceContacts: List<DeviceContactInfo>): String {
            val fromContactsList = getDisplayNameFromContacts(context, deviceContacts)
            if (fromContactsList.isNotBlank()) {
                logDebug { "Found from contacts!" }
                return fromContactsList
            }

            val possibleNames = DeviceProfileRepo.getCachedProfile().possibleNames()
            for (name in possibleNames) {
                if (name.isNotBlank()) {
                    logVerbose { "Found by user profile!" }
                    return name
                }
            }

            // nothing found!
            return ""
        }

        /**
         * Try to get own username from the already existing contacts
         */
        fun getDisplayNameFromContacts(context: Context, deviceContacts: List<DeviceContactInfo>): String {
            val ownNumber = PhoneUtils.getOwnPhoneNumber(context)
            for (contact in deviceContacts) {
                for (number in contact.aliases) {

                    if (number.value == ownNumber) {
                        return contact.displayName
                    }
                }
            }
            return ""
        }


    }
}
