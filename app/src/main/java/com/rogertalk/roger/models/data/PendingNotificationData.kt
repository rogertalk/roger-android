package com.rogertalk.roger.models.data

import java.io.Serializable

class PendingNotificationData(val streamId: Long,
                              val username: String,
                              val senderImageURL: String?,
                              val show: Boolean = true) : Serializable