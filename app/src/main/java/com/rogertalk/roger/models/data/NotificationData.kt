package com.rogertalk.roger.models.data

import java.io.Serializable

class NotificationData(
        val senderId: Long,
        val title: String,
        val text: String,
        val senderImageURL: String?,
        val streamId: Long,
        val username: String) : Serializable