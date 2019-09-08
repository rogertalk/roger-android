package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName

class Greeting(val duration: Int,
               @SerializedName("audio_url") val audioURL: String)
