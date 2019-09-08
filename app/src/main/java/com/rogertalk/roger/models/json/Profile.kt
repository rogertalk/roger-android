package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import com.rogertalk.roger.R
import com.rogertalk.roger.android.services.AudioDownloadManager
import com.rogertalk.roger.android.services.AudioDownloadManager.PendingAudioDownload
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.runOnUiThread

/**
 * Other user's simplified profile details. (Publicly accessible)
 */
class Profile(val id: Long,
              @SerializedName("display_name") val customDisplayName: String?,
              @SerializedName("image_url") val imageURL: String?,
              val username: String?,
              val greeting: Greeting?) {


    init {
        if (greeting != null) {
            // Cache audio immediately
            runOnUiThread {
                AudioDownloadManager.
                        cacheAudioHighPriority(PendingAudioDownload(greeting.audioURL, null, null))
            }
        }
    }

    // Computed values
    val displayName: String
        get() = ContactMapRepo.getContactDisplayName(id) ?: remoteDisplayName

    val remoteDisplayName: String
        get() = customDisplayName ?: username ?: appController().getString(R.string.unknown_person)

    val shortName: String
        get() = displayName.split(" ").first().dropLastWhile { it == ',' }

    /**
     * @return True if this profile belongs to the Roger account
     */
    val isRogerAccount: Boolean
        get() {
            return id == RogerConstants.ROGER_ACCOUNT_ID
        }
}