package com.rogertalk.roger.event.broadcasts

import android.graphics.Bitmap
import com.rogertalk.roger.models.data.NotificationType

data class NotificationAvatarLoadedEvent(val avatarBitmap: Bitmap?,
                                         val notificationType: NotificationType)
