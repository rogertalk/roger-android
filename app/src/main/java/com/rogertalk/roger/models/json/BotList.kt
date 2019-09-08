package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName

class BotList(@SerializedName("data") val bots: List<Bot>)