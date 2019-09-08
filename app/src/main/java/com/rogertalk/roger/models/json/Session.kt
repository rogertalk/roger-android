package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import java.util.*

data class Session(
        @SerializedName("refresh_token") val refreshToken: String?,
        @SerializedName("access_token") var accessToken: String,
        @SerializedName("expires_in") val expiresIn: Long,
        val status: String,
        val account: Account,
        val streams: LinkedList<Stream>) {

    /**
     * @return Account ID, or -1 if for some reason there is a problem
     */
    fun getId(): Long {
        return account.id
    }
}