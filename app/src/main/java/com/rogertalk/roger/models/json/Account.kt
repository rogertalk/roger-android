package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.AccountStatus
import com.rogertalk.roger.models.data.AccountStatus.*
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.data.StreamStatus.IDLE
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logError
import java.io.Serializable
import java.util.*

class Account(
        val id: Long,
        @SerializedName("display_name") val customDisplayName: String?,
        @SerializedName("display_name_set") val displayNameSet: Boolean?,
        @SerializedName("image_url") val imageURL: String?,
        val location: String?,
        val timezone: String?,
        val username: String?,
        @SerializedName("share_location") val shareLocation: Boolean,
        val identifiers: List<String>?,
        val service: Boolean = false,
        val text: String?,
        val status: String,
        @SerializedName("owner_id") val ownerId: Long?) : Serializable {

    companion object {
        private val CONVERSATION_STATUS_EXPIRATION_MILLIS = 60000L

        fun temporaryStreamParticipant(username: String, identifiers: List<String>?): Account {
            return Account(id = NO_ID,
                    displayNameSet = true,
                    imageURL = null,
                    username = username,
                    shareLocation = false,
                    identifiers = identifiers,
                    customDisplayName = username,
                    location = null,
                    timezone = null,
                    text = null,
                    service = false,
                    status = AccountStatus.INVITED.text,
                    ownerId = null)
        }

        fun temporaryStreamBotParticipant(botName: String, imageURL: String?): Account {
            return Account(id = NO_ID,
                    displayNameSet = true,
                    imageURL = imageURL,
                    username = null,
                    shareLocation = false,
                    identifiers = null,
                    customDisplayName = botName,
                    location = null,
                    timezone = null,
                    text = null,
                    service = false,
                    status = AccountStatus.BOT.text,
                    ownerId = UserAccountRepo.id())
        }
    }

    // Actual internal value for conversation status
    var _conversationStatusInternal: StreamStatus? = null

    val active: Boolean
        get() {
            if (arrayOf(ACTIVE.text, BOT.text, EMPLOYEE.text).contains(status)) {
                return true
            }
            return false
        }


    val bot: Boolean
        get() {
            if (status == BOT.text) {
                return true
            }
            return false
        }

    val accountReachable: Boolean
        get() {
            if (active) {
                return true
            }

            // For fake contacts, we set a display name as a way to tell this device to render it locally
            if (displayNameSet != null && displayNameSet) {
                return true
            }

            // Check if we can match this account with device contacts
            if (ContactMapRepo.deviceIdForRogerId(id) != null) {
                return true
            }

            return false
        }

    val conversationStatus: StreamStatus
        get() {
            // If there's no timestamp, IDLE
            if (conversationStatusTimestamp == null) {
                return IDLE
            }
            val pastDate = conversationStatusTimestamp
            if (pastDate != null) {
                val currentDate = Date().time
                val durationToExpire = estimatedDuration ?: CONVERSATION_STATUS_EXPIRATION_MILLIS
                val expired = (currentDate - pastDate) > durationToExpire
                if (expired) {
                    // Reset estimated duration
                    estimatedDuration = CONVERSATION_STATUS_EXPIRATION_MILLIS

                    // Reset internal status
                    _conversationStatusInternal = IDLE
                }

                val status = _conversationStatusInternal
                if (status != null) {
                    return status
                }
            }
            return IDLE
        }

    var conversationStatusTimestamp: Long? = null
    var estimatedDuration: Long? = null

    // Computed values

    /**
     * @return True if the account is associated with a phone number or e-mail, False otherwise
     */
    val hasContactInfo: Boolean
        get() {
            if (identifiers == null) {
                return false
            }
            if (identifiers.any { it.contains("+") }) {
                return true
            }
            if (identifiers.any { it.contains("@") }) {
                return true
            }
            return false
        }

    val displayName: String
        get() = remoteDisplayName

    val remoteDisplayName: String
        get() {
            val unknownLabel = appController().getString(R.string.unknown_person)
            if (active) {
                return customDisplayName ?: username ?: ContactMapRepo.getContactDisplayName(id) ?: unknownLabel
            } else {
                return ContactMapRepo.getContactDisplayName(id) ?: customDisplayName ?: username ?: unknownLabel
            }
        }

    val shortName: String
        get() = displayName.split(" ").first().dropLastWhile { it == ',' }

    //
    // OVERRIDE METHODS
    //

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    //
    // PUBLIC METHODS
    //

    fun getUserTime(): Calendar {
        if (timezone == null) {
            val localTime = Calendar.getInstance()
            return localTime
        }

        val userTimeZone = TimeZone.getTimeZone(timezone)
        val userTime = Calendar.getInstance(userTimeZone)
        return userTime
    }


    /**
     * Processed value. Will try to use local avatar if a server one doesn't exist
     */
    val userAvatar: String?
        get() {
            if (imageURL != null) {
                return imageURL
            }
            val localAvatar = ContactMapRepo.getContactAvatarURI(id)
            if (localAvatar != null) {
                return localAvatar
            }
            return null
        }

    // The following values and methods only make sense for the user's own account

    val email: String?
        get() = identifiers?.find { it.contains("@") } ?: null

    val phoneNumber: String?
        get() = identifiers?.find { it.startsWith("+") } ?: null


    /**
     * Get contact handle, it could be a **username** or a **phone number**
     */
    val handle: String?
        get() {
            if (!username.isNullOrEmpty()) {
                return username
            }
            if (phoneNumber != null) {
                return phoneNumber
            }

            logError { "No handle for account!" }
            return ""
        }


}