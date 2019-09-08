package com.rogertalk.roger.repo

import com.orhanobut.hawk.Hawk
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.MigrationManager
import com.rogertalk.roger.models.data.PersistedContacts
import com.rogertalk.roger.models.json.ActiveContact
import com.rogertalk.roger.models.json.ReferrerInfo
import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.models.json.WeatherCondition
import com.rogertalk.roger.models.migrations.MigrateSessionData
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.appController
import java.util.*

/**
 * This repository class manages all persisted key-value used within the app.
 * Current implementation uses Shared Preferences local to the app, encrypted by Hawk.
 */
object PrefRepo {

    private val LOGGED_IN = "loggedIn"
    val SESSION_V1 = "session"
    val SESSION_V2 = "sessionV2"

    private val GOD_MODE = "experimentalMode"

    private val DEVICE_CONTACTS_MAP = "deviceContactsMapV2"
    private val ROGER_TO_DEVICE_CONTACTS_MAP = "rogerToDeviceContactsMapV2"
    private val ACTIVE_CONTACTS_MAP = "activeContactsMapV3"
    private val CLEANUP_CACHE_LAST_EXECUTION = "cleanupCacheTimestamp"
    private val LOCATION_LAST_UPLOAD = "locationLastUpload"
    private val LAST_MIGRATION = "lastMigration"
    private val SEEN_ATTACHMENTS_LIST = "seenAttachmentsList"

    private val LAST_ACCESS_TOKEN_REFRESH = "lastAccessTokenRefresh"
    private val WEATHER_MAP = "weatherMap"
    private val ALEXA_CONNECTED = "alexaConnected"
    private val VOICEMAIL_CONFIGURED = "voicemailConfigured"
    private val RAISE_TO_HEAR_POSSIBLE = "raiseToHearPossible"
    private val LAST_INSTALLED_VERSION = "lastInstalledVersion"
    private val REFERRER_INFO = "referrerInfo"
    private val MUTED_STREAMS = "mutedConversations"
    private val LIVE_PLAYBACK = "autoplay"
    private val TALK_HEADS = "talkHeads"

    // Audio Recording
    private val SAMPLING_RATE_INDEX = "samplingRateIndex"

    // On-Boarding specific
    private val PENDING_CHOOSE_NAME_NEW = "pendingChooseNameNewUser"
    private val PENDING_PRIMER = "pendingPrimerV2"
    private val DID_TAP_MANAGE_CONVERSATION = "didTapManageConversation"

    // One-Time flags
    private val PERMISSION_TO_MATCH_CONTACTS = "permissionToMatchContacts"
    private val DID_TAP_TO_TALK = "didTapToTalk"
    private val DID_TAP_TO_LISTEN = "didTapToListen"
    private val DID_SEND_CHUNK = "didSendChunk"
    private val DID_SEE_ATTACHMENTS_SCREEN = "didSeeAttachmentsScreen"
    private val REPORTED_OPERATOR_DETAILS = "reportedOperator"
    private val DID_COMPLETE_ONBOARDING = "didCompletedOnboarding"
    private val SHOW_ADD_FAMILY_CONVERSATION = "addFamilyConversation"
    private val SHOW_ADD_FRIENDS_CONVERSATION = "addFriendsConversation"
    private val SHOW_ADD_TEAM_CONVERSATION = "addTeamConversation"

    var permissionToMatchContacts: Boolean
        get() {
            return Hawk.get(PERMISSION_TO_MATCH_CONTACTS, false)
        }
        set(value) {
            Hawk.put(PERMISSION_TO_MATCH_CONTACTS, value)
        }

    var talkHeads: Boolean
        get() {
            return Hawk.get(TALK_HEADS, true) || livePlayback
        }
        set(value) {
            Hawk.put(TALK_HEADS, value)
        }

    var livePlayback: Boolean
        get() = Hawk.get(LIVE_PLAYBACK, true)
        set(value) {
            // Track this change
            EventTrackingManager.liveModeChange(value)

            Hawk.put(LIVE_PLAYBACK, value)
        }

    var godMode: Boolean
        get() = Hawk.get(GOD_MODE, false)
        set(value) {
            Hawk.put(GOD_MODE, value)
        }

    var showAddFamily: Boolean
        get() = Hawk.get(SHOW_ADD_FAMILY_CONVERSATION, false)
        set(value) {
            Hawk.put(SHOW_ADD_FAMILY_CONVERSATION, value)
        }

    var showAddFriends: Boolean
        get() = Hawk.get(SHOW_ADD_FRIENDS_CONVERSATION, false)
        set(value) {
            Hawk.put(SHOW_ADD_FRIENDS_CONVERSATION, value)
        }

    var showAddTeam: Boolean
        get() = Hawk.get(SHOW_ADD_TEAM_CONVERSATION, false)
        set(value) {
            Hawk.put(SHOW_ADD_TEAM_CONVERSATION, value)
        }

    var reportedOperatorDetails: Boolean
        get() = Hawk.get(REPORTED_OPERATOR_DETAILS, false)
        set(value) {
            Hawk.put(REPORTED_OPERATOR_DETAILS, value)
        }

    var completedOnboarding: Boolean
        get() = Hawk.get(DID_COMPLETE_ONBOARDING, false)
        set(value) {
            Hawk.put(DID_COMPLETE_ONBOARDING, value)
        }

    var didSeeAttachmentsScreen: Boolean
        get() = Hawk.get(DID_SEE_ATTACHMENTS_SCREEN, false)
        set(value) {
            Hawk.put(DID_SEE_ATTACHMENTS_SCREEN, value)
        }

    var didTapManagerConversation: Boolean
        get() {
            if (AccessibilityUtils.isScreenReaderActive(appController())) {
                return true
            }
            return Hawk.get(DID_TAP_MANAGE_CONVERSATION, false)
        }
        set(value) {
            Hawk.put(DID_TAP_MANAGE_CONVERSATION, value)
        }

    var didTapToTalk: Boolean
        get() {
            if (AccessibilityUtils.isScreenReaderActive(appController())) {
                return true
            }
            return Hawk.get(DID_TAP_TO_TALK, false)
        }
        set(value) {
            Hawk.put(DID_TAP_TO_TALK, value)
        }

    var didTapToListen: Boolean
        get() {
            if (AccessibilityUtils.isScreenReaderActive(appController())) {
                return true
            }
            return Hawk.get(DID_TAP_TO_LISTEN, false)
        }
        set(value) {
            Hawk.put(DID_TAP_TO_LISTEN, value)
        }

    var didSendChunk: Boolean
        get() = Hawk.get(DID_SEND_CHUNK, false)
        set(value) {
            Hawk.put(DID_SEND_CHUNK, value)
        }

    var lastLocationUpload: Long
        get() = Hawk.get(LOCATION_LAST_UPLOAD, NO_TIME)
        set(value) {
            Hawk.put(LOCATION_LAST_UPLOAD, value)
        }

    var lastAccessTokenRefresh: Long
        get() = Hawk.get(LAST_ACCESS_TOKEN_REFRESH, NO_TIME)
        set(value) {
            Hawk.put(LAST_ACCESS_TOKEN_REFRESH, value)
        }

    var loggedIn: Boolean
        get() = Hawk.get(LOGGED_IN, false)
        set(value) {
            Hawk.put(LOGGED_IN, value)
        }

    var deviceContactsMap: PersistedContacts
        get() = Hawk.get(DEVICE_CONTACTS_MAP, PersistedContacts())
        set(value) {
            Hawk.put(DEVICE_CONTACTS_MAP, value)
        }

    var rogerToDeviceContactMap: HashMap<Long, Long>
        get() = Hawk.get(ROGER_TO_DEVICE_CONTACTS_MAP, HashMap<Long, Long>())
        set(value) {
            Hawk.put(ROGER_TO_DEVICE_CONTACTS_MAP, value)
        }

    var activeContactsMap: HashMap<String, ActiveContact>
        get() = Hawk.get(ACTIVE_CONTACTS_MAP) ?: HashMap(0)
        set(value) {
            Hawk.put(ACTIVE_CONTACTS_MAP, value)
        }

    var weatherMap: HashMap<Long, WeatherCondition>
        get() = Hawk.get(WEATHER_MAP) ?: HashMap(0)
        set(value) {
            Hawk.put(WEATHER_MAP, value)
        }

    /**
     * Safe to use sampling rate for this device
     */
    var samplingRateIndex: Int
        get() = Hawk.get(SAMPLING_RATE_INDEX, 0)
        set(value) {
            Hawk.put(SAMPLING_RATE_INDEX, value)
        }

    var pendingChooseNameNewUser: Boolean
        get() = Hawk.get(PENDING_CHOOSE_NAME_NEW, false)
        set(value) {
            Hawk.put(PENDING_CHOOSE_NAME_NEW, value)
        }

    var pendingPrimer: Boolean
        get() {
            if (AccessibilityUtils.isScreenReaderActive(appController())) {
                return false
            }
            return Hawk.get(PENDING_PRIMER, false)
        }
        set(value) {
            Hawk.put(PENDING_PRIMER, value)
        }

    var session: Session?
        get() {
            MigrateSessionData.migrateSessionData()

            return Hawk.get(SESSION_V2, null)
        }
        set(value) {
            Hawk.put(SESSION_V2, value)
        }

    var alexaConnected: Boolean
        get() = Hawk.get(ALEXA_CONNECTED, false)
        set(value) {
            Hawk.put(ALEXA_CONNECTED, value)
        }

    var raiseToHearPossible: Boolean
        get() = Hawk.get(RAISE_TO_HEAR_POSSIBLE, false)
        set(value) {
            Hawk.put(RAISE_TO_HEAR_POSSIBLE, value)
        }

    var lastCleanupTimestamp: Long
        get() = Hawk.get(CLEANUP_CACHE_LAST_EXECUTION, NO_TIME)
        set(value) {
            Hawk.put(CLEANUP_CACHE_LAST_EXECUTION, value)
        }

    var lastMigrationVersion: Int
        get() = Hawk.get(LAST_MIGRATION, MigrationManager.NO_VERSION)
        set(value) {
            Hawk.put(LAST_MIGRATION, value)
        }

    var lastInstalledVersion: Int
        get() = Hawk.get(LAST_INSTALLED_VERSION, 0)
        set(value) {
            Hawk.put(LAST_INSTALLED_VERSION, value)
        }

    var referrerInfo: ReferrerInfo?
        get() = Hawk.get(REFERRER_INFO)
        set(value) {
            Hawk.put(REFERRER_INFO, value)
        }

    var voicemailConfigured: Boolean
        get() = Hawk.get(VOICEMAIL_CONFIGURED, false)
        set(value) {
            Hawk.put(VOICEMAIL_CONFIGURED, value)
        }

    var mutedStreams: HashMap<Long, Long>
        get() = Hawk.get(MUTED_STREAMS)
        set(value) {
            Hawk.put(MUTED_STREAMS, value)
        }

    var seenAttachments: HashSet<Long>?
        get() = Hawk.get(SEEN_ATTACHMENTS_LIST)
        set(value) {
            Hawk.put(SEEN_ATTACHMENTS_LIST, value)
        }

    /**
     * Clears all the data from the persisted storage
     */
    fun clearAllData() {
        Hawk.clear()
    }

    fun clearActiveContactData() {
        Hawk.remove(ACTIVE_CONTACTS_MAP)
    }


    /**
     * CHECK PERSISTED STORAGE STATE
     */

    fun hasActiveContactsMap(): Boolean {
        return Hawk.contains(ACTIVE_CONTACTS_MAP)
    }

    fun hasContactsMap(): Boolean {
        return Hawk.contains(ROGER_TO_DEVICE_CONTACTS_MAP)
    }

    fun hasMutedStreams(): Boolean {
        return Hawk.contains(MUTED_STREAMS)
    }

}