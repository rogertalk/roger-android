package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class ActiveContact(val active: Boolean,
                    @SerializedName("id") val accountId: Long) : Serializable