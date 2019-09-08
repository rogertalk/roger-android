package com.rogertalk.roger.utils.contact.device

import android.net.Uri
import java.util.*

class DeviceUserProfile {

    /**
     * Adds an email address to the list of possible email addresses for the user. Retains information about whether this
     * email address is the primary email address of the user.

     * @param email      the possible email address
     * *
     * @param is_primary whether the email address is the primary email address
     */
    @JvmOverloads fun addPossibleEmail(email: String?, is_primary: Boolean = false) {
        if (email == null) return
        if (is_primary) {
            _primary_email = email
            _possible_emails.add(email)
        } else
            _possible_emails.add(email)
    }

    /**
     * Adds a name to the list of possible names for the user.

     * @param name the possible name
     */
    fun addPossibleName(name: String?) {
        if (name != null) _possible_names.add(name)
    }

    /**
     * Adds a phone number to the list of possible phone numbers for the user.

     * @param phone_number the possible phone number
     */
    fun addPossiblePhoneNumber(phone_number: String?) {
        if (phone_number != null) _possible_phone_numbers.add(phone_number)
    }

    /**
     * Adds a phone number to the list of possible phone numbers for the user.  Retains information about whether this
     * phone number is the primary phone number of the user.

     * @param phone_number the possible phone number
     * *
     * @param is_primary   whether the phone number is teh primary phone number
     */
    fun addPossiblePhoneNumber(phone_number: String?, is_primary: Boolean) {
        if (phone_number == null) return
        if (is_primary) {
            _primary_phone_number = phone_number
            _possible_phone_numbers.add(phone_number)
        } else
            _possible_phone_numbers.add(phone_number)
    }

    /**
     * Sets the possible photo for the user.

     * @param photo the possible photo
     */
    fun addPossiblePhoto(photo: Uri?) {
        if (photo != null) _possible_photo = photo
    }

    /**
     * Retrieves the list of possible email addresses.

     * @return the list of possible email addresses
     */
    fun possibleEmails(): List<String> {
        return _possible_emails
    }

    /**
     * Retrieves the list of possible names.

     * @return the list of possible names
     */
    fun possibleNames(): List<String> {
        return _possible_names
    }

    /**
     * Retrieves the list of possible phone numbers

     * @return the list of possible phone numbers
     */
    fun possiblePhoneNumbers(): List<String> {
        return _possible_phone_numbers
    }

    /**
     * Retrieves the possible photo.

     * @return the possible photo
     */
    fun possiblePhoto(): Uri? {
        return _possible_photo
    }

    /**
     * Retrieves the primary email address.

     * @return the primary email address
     */
    fun primaryEmail(): String {
        return _primary_email ?: ""
    }

    /**
     * Retrieves the primary phone number

     * @return the primary phone number
     */
    fun primaryPhoneNumber(): String {
        return _primary_phone_number ?: ""
    }

    /**
     * The primary email address
     */
    private var _primary_email: String? = null
    /**
     * The primary name
     */
    private val _primary_name: String? = null
    /**
     * The primary phone number
     */
    private var _primary_phone_number: String? = null
    /**
     * A list of possible email addresses for the user
     */
    private val _possible_emails = ArrayList<String>()
    /**
     * A list of possible names for the user
     */
    private val _possible_names = ArrayList<String>()
    /**
     * A list of possible phone numbers for the user
     */
    private val _possible_phone_numbers = ArrayList<String>()
    /**
     * A possible photo for the user
     */
    private var _possible_photo: Uri? = null
}
