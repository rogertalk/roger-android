package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName

/**
 * Other user's simplified profile details with chunk and stream info. (Publicly accessible)
 */
class ProfileWithChunk(val profile: Profile,
                       @SerializedName("receiver_identifier") val receiverIdentifier: String?,
                       val stream_id: Long,
                       val chunk: Chunk?)