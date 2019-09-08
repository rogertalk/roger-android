package com.rogertalk.roger.models.data

import com.google.gson.annotations.SerializedName
import com.rogertalk.roger.models.json.Greeting

class ProfileFromURI(val id: Long,
                     @SerializedName("display_name") val customDisplayName: String?,
                     @SerializedName("image_url") val imageURL: String?,
                     val username: String?,
                     val greeting: Greeting?,
                     @SerializedName("audio_url") val audioURL: String?,
                     @SerializedName("chunk_token") val chunkToken: String?,
                     val duration: Int) {
}