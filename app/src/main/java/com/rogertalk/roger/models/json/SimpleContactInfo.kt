package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName

class SimpleContactInfo(val id: Long,
                        @SerializedName("image_url") val imageURL: String?,
                        @SerializedName("display_name") val displayName: String?,
                        val username: String?)
