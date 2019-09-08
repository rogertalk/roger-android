package com.rogertalk.roger.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.*
import android.support.v4.content.ContextCompat
import com.rogertalk.roger.utils.contact.device.DeviceUserProfile
import com.rogertalk.roger.utils.extensions.appHelper

object DeviceProfileRepo {


    private val MIMETYPE = "mimetype"
    private val IS_PRIMARY = "is_primary"
    private val PHOTO_URI = "photo_uri"

    /**
     * Get a cached version of User Profile. (Will be lazily cached).
     * User Profile is the one that can be accessed from contacts (AKA, current device user, typically the owner)
     */
    fun getCachedProfile(): DeviceUserProfile {
        return appHelper().deviceUserProfile
    }

    /**
     * Get user profile (the one you can see at the top of settings when one pulls down the status bar)
     */
    fun getUserProfileToCache(context: Context): DeviceUserProfile {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            return DeviceUserProfile()
        }

        val content = context.contentResolver
        val cursor = content.query(
                // Retrieves data rows for the device user's 'profile' contact
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                ProfileQuery.PROJECTION,

                // Selects only email addresses or names
                "$MIMETYPE=? OR $MIMETYPE=? OR $MIMETYPE=? OR $MIMETYPE=?",
                arrayOf(Email.CONTENT_ITEM_TYPE, StructuredName.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Photo.CONTENT_ITEM_TYPE),

                // Show primary rows first. Note that there won't be a primary email address if the
                // user hasn't specified one.
                IS_PRIMARY + " DESC")

        val user_profile = DeviceUserProfile()
        var mime_type: String
        while ((cursor != null && cursor.moveToNext())) {
            mime_type = cursor.getString(ProfileQuery.MIME_TYPE)
            when (mime_type) {
                Email.CONTENT_ITEM_TYPE ->
                    user_profile.addPossibleEmail(cursor.getString(ProfileQuery.EMAIL),
                            cursor.getInt(ProfileQuery.IS_PRIMARY_EMAIL) > 0)

                StructuredName.CONTENT_ITEM_TYPE -> {
                    val givenName = cursor.getString(ProfileQuery.GIVEN_NAME) ?: ""
                    val familyName = cursor.getString(ProfileQuery.FAMILY_NAME) ?: ""
                    user_profile.addPossibleName("$givenName $familyName")
                }

                Phone.CONTENT_ITEM_TYPE ->
                    user_profile.addPossiblePhoneNumber(cursor.getString(ProfileQuery.PHONE_NUMBER),
                            cursor.getInt(ProfileQuery.IS_PRIMARY_PHONE_NUMBER) > 0)

                Photo.CONTENT_ITEM_TYPE -> {
                    val photoURL = cursor.getString(ProfileQuery.PHOTO)
                    if (photoURL != null) {
                        user_profile.addPossiblePhoto(Uri.parse(photoURL))
                    }
                }
            }
        }

        cursor?.close()

        return user_profile
    }

    /**
     * Contacts user profile query interface.
     */
    private interface ProfileQuery {
        companion object {
            /**
             * The set of columns to extract from the profile query results
             */
            val PROJECTION = arrayOf(Email.ADDRESS, IS_PRIMARY,
                    StructuredName.FAMILY_NAME,
                    StructuredName.GIVEN_NAME,
                    Phone.NUMBER,
                    IS_PRIMARY, PHOTO_URI, MIMETYPE)

            /**
             * Column index for the email address in the profile query results
             */
            val EMAIL = 0
            /**
             * Column index for the primary email address indicator in the profile query results
             */
            val IS_PRIMARY_EMAIL = 1
            /**
             * Column index for the family name in the profile query results
             */
            val FAMILY_NAME = 2
            /**
             * Column index for the given name in the profile query results
             */
            val GIVEN_NAME = 3
            /**
             * Column index for the phone number in the profile query results
             */
            val PHONE_NUMBER = 4
            /**
             * Column index for the primary phone number in the profile query results
             */
            val IS_PRIMARY_PHONE_NUMBER = 5
            /**
             * Column index for the photo in the profile query results
             */
            val PHOTO = 6
            /**
             * Column index for the MIME type in the profile query results
             */
            val MIME_TYPE = 7
        }
    }

}