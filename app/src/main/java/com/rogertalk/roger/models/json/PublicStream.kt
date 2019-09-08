package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName

/**
 * Publicly accessible stream
 */
class PublicStream(
        @SerializedName("image_url") val imageURL: String?,
        @SerializedName("invite_token") val inviteToken: String,
        val participants: List<Account>?,
        val title: String,
        val description: String? = null,
        val goIntoGroupCreation: Boolean = false,
        val localDrawable: Int? = null)