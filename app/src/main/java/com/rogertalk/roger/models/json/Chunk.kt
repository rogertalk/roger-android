package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.utils.constant.NO_ID
import java.io.Serializable

class Chunk(val id: Long,
            val end: Long,
            val start: Long,
            @SerializedName("sender_id") val senderId: Long,
            @SerializedName("audio_url") val audioURL: String,
            val duration: Int) : Serializable {

    // computer values

    var streamId = NO_ID

    /**
     * Computed value that indicates if this chunks belongs to current user
     */
    val byCurrentUser: Boolean
        get() {
            return senderId == SessionRepo.sessionId()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Chunk

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}