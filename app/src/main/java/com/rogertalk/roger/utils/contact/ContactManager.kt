package com.rogertalk.roger.utils.contact

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts.*
import android.support.v4.content.ContextCompat
import com.rogertalk.roger.ExtraContacts
import com.rogertalk.roger.models.data.ContactLabel
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.utils.extensions.appHelper
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.phone.PhoneUtils
import java.util.*

class ContactManager {

    fun queryContacts(ctx: Context): LinkedHashMap<Long, DeviceContactInfo> {
        // Don't proceed if we don't have READ_CONTACTS permission
        if (ContextCompat.checkSelfPermission(ctx, READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            logInfo { "There's no contact permission" }
            return LinkedHashMap(0)
        }

        // initialize contacts with existing in-memory device contacts
        val contacts = LinkedHashMap<Long, DeviceContactInfo>(appHelper().deviceContacts)

        val previousContactCount = contacts.size

        // Add extra contacts (way to inject fake contacts during staging builds)
        contacts.putAll(ExtraContacts.getExtraContacts())

        // This are the fields we want to be returned
        val projection = arrayOf(_ID, DISPLAY_NAME, PHOTO_URI)

        val sortBy = "$DISPLAY_NAME ASC"

        val cursor = ctx.contentResolver.query(CONTENT_URI, projection, null, null, sortBy)

        // handle device query results
        try {
            val contactIdIndex = cursor!!.getColumnIndex(_ID)
            val nameIndex = cursor.getColumnIndex(DISPLAY_NAME)
            val photoThumbIndex = cursor.getColumnIndex(PHOTO_URI)

            var id: Long?
            var name: String?
            var photoUriStr: String?

            loop@ while (cursor.moveToNext()) {
                id = cursor.getLong(contactIdIndex)

                if (contacts.containsKey(id)) {
                    continue@loop
                }

                name = cursor.getString(nameIndex)

                if (name != null) {
                    photoUriStr = cursor.getString(photoThumbIndex)
                    contacts.put(id, DeviceContactInfo(id, name, photoUriStr))
                }
            }
        } catch (e: Exception) {
            logError(e) { "Could not get contacts with numbers" }
        } finally {
            cursor?.close()
        }

        // Check if there is new data
        if (previousContactCount == contacts.size) {
            logInfo { "There have been no changes to device contacts, keeping current data" }
            return contacts

        }

        // Fill phone numbers and e-mails
        matchContactNumbers(ctx, contacts)

        // Remove contacts that have neither
        val finalContacts = LinkedHashMap<Long, DeviceContactInfo>(contacts.size)
        finalContacts.putAll(contacts.filter { it.value.aliases.size > 0 })

        // Cache and persist device contacts
        ContactMapRepo.setDeviceContactsList(finalContacts)

        return finalContacts
    }

    private fun matchContactNumbers(ctx: Context, contactsMap: LinkedHashMap<Long, DeviceContactInfo>) {
        val numberProjection = arrayOf(CommonDataKinds.Phone.NUMBER,
                CommonDataKinds.Phone.TYPE,
                CommonDataKinds.Phone.CONTACT_ID)

        val phoneCursor = ctx.contentResolver.query(
                CommonDataKinds.Phone.CONTENT_URI,
                numberProjection,
                null,
                null,
                null)

        try {
            if (phoneCursor.count == null || phoneCursor.count < 1) {
                return
            }
        } catch(e: Exception) {
            logError (e) { "For some weird reason contact query is not working" }
            return
        }

        if (phoneCursor.moveToFirst()) {
            val numberIndex = phoneCursor.getColumnIndex(CommonDataKinds.Phone.NUMBER)
            val numberTypeIndex = phoneCursor.getColumnIndex(CommonDataKinds.Phone.TYPE)
            val contactIdIndex = phoneCursor.getColumnIndex(CommonDataKinds.Phone.CONTACT_ID)

            var contactId: Long
            var phoneNumber: String?
            var phoneType: Int

            outer@while (!phoneCursor.isAfterLast) {
                contactId = phoneCursor.getLong(contactIdIndex)

                // make sure we match against an existing contact on the map
                if (contactsMap[contactId] != null) {
                    phoneNumber = phoneCursor.getString(numberIndex)
                    // Some contacts report null, skip those
                    if (phoneNumber == null) {
                        continue@outer
                    }

                    phoneType = phoneCursor.getInt(numberTypeIndex)

                    phoneNumber = PhoneUtils.getProperPhoneNumber(phoneNumber)

                    if (phoneNumber.isNotEmpty()) {
                        // Contact exists on the list, add phone number
                        val newContact = contactsMap[contactId]
                        newContact?.aliases?.add(ContactLabel(phoneNumber, ContactLabelMatch.getPhoneNumberLabel(phoneType)))
                        if (newContact != null) {
                            contactsMap.put(contactId, newContact)
                        }
                    }
                }

                phoneCursor.moveToNext()
            }
        }

        phoneCursor.close()
    }

}
