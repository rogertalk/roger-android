package com.rogertalk.roger.repo

import android.content.Context
import com.rogertalk.kotlinjubatus.utils.DeviceUtils
import com.rogertalk.roger.R
import com.rogertalk.roger.android.AppController
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.models.data.NotificationType
import com.rogertalk.roger.models.data.NotificationType.NEW_CONTENT
import com.rogertalk.roger.utils.extensions.*
import me.leolin.shortcutbadger.ShortcutBadger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


object NotificationsRepo {

    private fun getListByType(type: NotificationType): CopyOnWriteArrayList<NotificationData> {
        if (type == NEW_CONTENT) {
            return appHelper().unPlayedNotificationList
        } else {
            return appHelper().buzzNotificationList
        }
    }

    fun add(notificationData: NotificationData, type: NotificationType) {
        getListByType(type).add(notificationData)

        if (type == NEW_CONTENT) {
            try {
                if (DeviceUtils.deviceName == "SAMSUNG SM-G935T") {
                    if (!AppController.instance.hasSamsungBadgePermission()) {
                        return
                    }
                }
                ShortcutBadger.applyCountOrThrow(appController(), count(type))
            } catch (e: Exception) {
            }
        }
    }

    fun first(type: NotificationType): NotificationData? {
        return getListByType(type).firstOrNull()
    }

    fun last(type: NotificationType): NotificationData? {
        return getListByType(type).lastOrNull()
    }

    fun getAll(type: NotificationType): List<NotificationData> {
        val reversedList = ArrayList(getListByType(type)).reversed()
        return reversedList
    }

    fun removeAll(type: NotificationType) {
        getListByType(type).clear()

        if (type == NEW_CONTENT) {
            // Clear badge
            try {
                if (DeviceUtils.deviceName == "SAMSUNG SM-G935T") {
                    if (!AppController.instance.hasSamsungBadgePermission()) {
                        return
                    }
                }
                ShortcutBadger.removeCountOrThrow(appController())
            } catch (e: Exception) {
            }
        }
    }

    fun buzzNotificationsTitle(context: Context, type: NotificationType): String {
        if (count(type) > 1) {
            return R.string.notification_people_title.stringResource(count(type).toString(), context = context)
        }
        return first(type)?.username ?: R.string.unknown_person.stringResource()
    }

    fun unplayedNotificationsTitle(): String {
        val lastNotification = last(NEW_CONTENT) ?: return ""
        val count = countDifferentParticipants(NEW_CONTENT)
        val who = lastNotification.username
        if (count == 1) {
            return who
        }
        val stream = StreamCacheRepo.getStream(lastNotification.streamId)
        stream?.let {
            if (it.isGroup) {
                val groupName = stream.title
                return R.string.notification_multiple_title.stringResource(who, groupName)
            }
        }
        val toYou = R.string.notification_multiple_title_you.stringResource()
        return R.string.notification_multiple_title.stringResource(who, toYou)
    }

    fun unplayedNotificationsDescription(): String {
        val lastNotification = last(NEW_CONTENT) ?: return ""
        val count = countDifferentParticipants(NEW_CONTENT)
        if (count > 1) {
            return R.plurals.notification_people_talked.pluralResource(count - 1, count - 1)
        }
        val stream = StreamCacheRepo.getStream(lastNotification.streamId)
        stream?.let {
            if (it.isGroup) {
                return R.string.notification_single_message_to_group.stringResource(it.title)
            }
        }
        return R.string.notification_single_message_text.stringResource()
    }

    /**
     * Number of notifications by different stream
     */
    fun count(type: NotificationType): Int {
        // count same person as the same
        return getListByType(type).groupBy { it.streamId }.size
    }

    /**
     * Number of notifications by different people
     */
    fun countDifferentParticipants(type: NotificationType): Int {
        // count same person as the same
        return getListByType(type).groupBy { it.senderId }.size
    }

    /**
     * The total number of notifications from all users
     */
    fun countRaw(type: NotificationType): Int {
        // count same person as the same
        return getListByType(type).size
    }
}