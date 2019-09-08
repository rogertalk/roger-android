package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import com.rogertalk.roger.manager.BotCacheManager

class Bot(@SerializedName("account_id") val accountId: Long?,
          @SerializedName("id") val nameId: String,
          val title: String,
          val description: String,
          @SerializedName("image_url") val imageURL: String?,
          var connected: Boolean,
          @SerializedName("client_code") val clientCode: Boolean,
          @SerializedName("finish_pattern") val finishPattern: String?,
          @SerializedName("connect_url") val connectURL: String?) {


    val canDisplay: Boolean
        get() {
            if (!clientCode) {
                return true
            }
            return BotCacheManager.CLIENT_CODE_IDS.contains(nameId)
        }
}
