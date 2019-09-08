package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReferrerInfo(@SerializedName("public_profile") val publicProfile: String?,
                        @SerializedName("chunk_token") val chunkToken: String?,
                        @SerializedName("default_identifier") val defaultIdentifier: String?,
                        @SerializedName("invite_token") val inviteToken: String?) : Serializable
