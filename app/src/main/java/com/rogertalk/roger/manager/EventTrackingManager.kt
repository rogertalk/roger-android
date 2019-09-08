package com.rogertalk.roger.manager

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.ShareEvent
import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.models.data.SoundojiType

class EventTrackingManager {

    enum class PlaybackSource(val description: String) {
        TAP_AVATAR("tappedAvatar"),
        TAP_TALKHEAD("tappedTalkHead"),
        RAISE_TO_EAR("raiseToEar"),
        RAISE_TO_EAR_SECONDARY("raiseToEarSecondary"), // Playback was started from elsewhere, but switched to raise to ear
        PRESS_MEDIA_KEY("pressMediaKey"),
        AUTOPLAY_INSIDE("autoPlayInsideApp"),
        AUTOPLAY_TALKHEAD("autoPlayTalkHead")
    }

    enum class PlaybackStopReason(val description: String) {
        TAP_AVATAR_INSIDE("tappedAvatarInsideApp"),
        TAP_AVATAR_TALKHEAD("tappedAvatarTalkHead"),
        TAP_NOTIFICATION("tappedNotification"),
        FINISHED_SUCCESS("finishedSuccess"),
        FINISHED_ERROR("finishedError"),
        FINISHED_AUDIO_LOSS("finishedAudioLoss"),
        PRESS_MEDIA_KEY("pressMediaKey"),
        NOT_SPECIFIED("notSpecified")
    }

    enum class RecordingReason(val description: String) {
        TAP_MIC("tapMic"),
        TAP_TALK_HEAD("tapTalkHead"),
        TAP_MIC_ATTACHMENTS("tapMicOnAttachments"),
        TAP_NOTIFICATION("tapNotification"),
        PRESS_MEDIA_KEY("pressMediaKey"),
        NOT_SPECIFIED("notSpecified")
    }

    enum class InvitationMethod(val description: String) {
        SETTINGS_GENERIC_INVITATION("settingsGenericInvitation"),
        ONE_ON_ONE_SMS("sms"),
        ONE_ON_ONE_OTHER("oneOnOneOther"),
        MASS_INVITE("massInvite"),
        INVITE_ALL("inviteAll")
    }

    enum class NotificationType(val description: String) {
        EXPIRING_AUDIO("expiringAudio"),
        ATTACHMENTS("attachments"),
    }


    companion object {

        // REUSABLE KEYS
        private val KEY_DEVICE_FEATURES_EVENT = "Device features"
        private val KEY_NOTIFICATIONS_EVENTS = "Notifications"
        private val KEY_AUDIO_CONTROL = "Audio Control"
        private val KEY_ATTACHMENTS = "Attachments"
        private val KEY_PLAYBACK_STARTED = "Playback started"
        private val KEY_NOTIFICATION_SELECTED = "Notification Selected"

        private fun logCustomEvent(event: CustomEvent) {
            if (!BuildConfig.DEBUG) {
                Answers.getInstance().logCustom(event)
            }
        }

        /**
         * Started Playback Event
         * @param source what let to playback starting
         * @param duration the duration of this playback
         * @param cached tell us if the content for playback was cached offline prior to playback
         * @param unplayed tell us if the content was brand new, or being replayed
         */
        fun playbackStart(duration: Double,
                          unplayed: Boolean,
                          cached: Boolean,
                          usingHeadphones: Boolean,
                          usingBluetooth: Boolean) {
            val event = CustomEvent(KEY_PLAYBACK_STARTED)
            event.putCustomAttribute("duration", duration)
            event.putCustomAttribute("unplayed", unplayed.toString())
            event.putCustomAttribute("cached", cached.toString())
            event.putCustomAttribute("usingHeadphones", usingHeadphones.toString())
            event.putCustomAttribute("usingBluetooth", usingBluetooth.toString())
            logCustomEvent(event)
        }

        /**
         * Started Playback Event
         * @param source what let to playback starting
         * */
        fun playbackStartReason(source: PlaybackSource) {
            val event = CustomEvent(KEY_PLAYBACK_STARTED)
            event.putCustomAttribute("reason", source.description)
            logCustomEvent(event)
        }

        /**
         * Stopped playback event
         * @param reason what let to playback being stopped
         */
        fun playbackStop(reason: PlaybackStopReason) {
            val event = CustomEvent("Playback stopped")
            event.putCustomAttribute("reason", reason.description)
            logCustomEvent(event)
        }

        /**
         * Recording started
         * @param reason what let to recording starting
         */
        fun recordingStart(reason: RecordingReason) {
            val event = CustomEvent("Recording completed")
            event.putCustomAttribute("reason", reason.description)
            logCustomEvent(event)
        }

        /**
         * Recording finished (either successfully or not)
         * @param reason what let to recording being completed
         */
        fun recordingCompleted(reason: RecordingReason) {
            val event = CustomEvent("Recording completed")
            event.putCustomAttribute("reason", reason.description)
            logCustomEvent(event)
        }

        /**
         * Track what kind of permissions user's enable when onboarding on MM and newer devices
         */
        fun permissionsOnBoarding(permissionsEnabled: Int, micro: Boolean,
                                  sms: Boolean, calls: Boolean) {
            val event = CustomEvent("Onboarding Permissions")
            event.putCustomAttribute("permissionsCount", permissionsEnabled)
            event.putCustomAttribute("microphonePermission", micro.toString())
            event.putCustomAttribute("callsPermission", calls.toString())
            event.putCustomAttribute("smsPermission", sms.toString())
            logCustomEvent(event)
        }

        /**
         * Invite or share content from the application
         */
        fun invitation(method: InvitationMethod) {
            val event = ShareEvent()
            event.putMethod(method.description)
            if (!BuildConfig.DEBUG) {
                Answers.getInstance().logShare(event)
            }
        }


        // Track usage of specific actions

        /**
         * User made use of Android beam to receive content.
         * In practice means another person had to send the content, so the actual usage of NFC
         * might be double of this number.
         */
        fun androidBeam() {
            val event = CustomEvent("Use Android Beam")
            logCustomEvent(event)
        }

        /**
         * User made use of Media Keys on the device to navigate the app
         */
        fun pressMediaKey() {
            val event = CustomEvent("Use Media Key")
            logCustomEvent(event)
        }

        /**
         * User made is using Google's talkback with the app
         */
        fun usingScreenReader() {
            val event = CustomEvent("Use Screen Reader")
            logCustomEvent(event)
        }

        /**
         * User buzzed someone
         */
        fun buzzedSomeone() {
            val event = CustomEvent("Buzzed someone")
            logCustomEvent(event)
        }

        /**
         * User paused playback
         */
        fun pausedPlayback(secondsIn: Long, remainingSeconds: Long) {
            val event = CustomEvent("Paused Playback")
            event.putCustomAttribute("secondsIn", secondsIn)
            event.putCustomAttribute("remainingSeconds", remainingSeconds)
            logCustomEvent(event)
        }

        /**
         * User resume playback
         */
        fun resumePlayback() {
            val event = CustomEvent("Resume Playback")
            logCustomEvent(event)
        }

        /**
         * Pressed credits
         */
        fun pressedCredits() {
            val event = CustomEvent("Pressed Credits")
            logCustomEvent(event)
        }

        /**
         * Pressed changelog history
         */
        fun pressedChangelogHistory() {
            val event = CustomEvent("Pressed changelog history")
            logCustomEvent(event)
        }

        /**
         * Info about device NFC availability
         */
        fun deviceHasNFC(hasNFC: Boolean) {
            val event = CustomEvent(KEY_DEVICE_FEATURES_EVENT)
            event.putCustomAttribute("hasNFC", hasNFC.toString())
            logCustomEvent(event)
        }

        /**
         * Pressed Skip on Invitation
         */
        fun dismissedInvite(pressSkip: Boolean) {
            val event = CustomEvent("Invite Ignore")
            event.putCustomAttribute("pressSkip", pressSkip.toString())
            logCustomEvent(event)
        }

        fun mutedFor8Hours(isGroup: Boolean) {
            val event = CustomEvent(KEY_NOTIFICATIONS_EVENTS)
            event.putCustomAttribute("muted 8 hours", "true")
            event.putCustomAttribute("is_group", isGroup.toString())
            logCustomEvent(event)
        }

        fun mutedFor1Week(isGroup: Boolean) {
            val event = CustomEvent(KEY_NOTIFICATIONS_EVENTS)
            event.putCustomAttribute("muted 1 week", "true")
            event.putCustomAttribute("is_group", isGroup.toString())
            logCustomEvent(event)
        }

        fun liveModeChange(changedLiveTo: Boolean) {
            val event = CustomEvent("Changed Live Mode")
            event.putCustomAttribute("enabled", changedLiveTo.toString())
            logCustomEvent(event)
        }

        fun liveModeStartState(liveModeStartSetting: Boolean) {
            val event = CustomEvent("Live Mode start state")
            event.putCustomAttribute("enabled", liveModeStartSetting.toString())
            logCustomEvent(event)
        }

        fun talkHeadStartState(startSetting: Boolean) {
            val event = CustomEvent("TalkHead start state")
            event.putCustomAttribute("enabled", startSetting.toString())
            logCustomEvent(event)
        }

        fun talkHeadChange(changedTo: Boolean) {
            val event = CustomEvent("Changed TalkHead Mode")
            event.putCustomAttribute("enabled", changedTo.toString())
            logCustomEvent(event)
        }

        fun enabledExperimentalMode() {
            val event = CustomEvent("Enabled experimental mode")
            logCustomEvent(event)
        }

        fun pressedRewind() {
            val event = CustomEvent(KEY_AUDIO_CONTROL)
            event.putCustomAttribute("pressed_rewind", "true")
            logCustomEvent(event)
        }

        fun pressedForward() {
            val event = CustomEvent(KEY_AUDIO_CONTROL)
            event.putCustomAttribute("pressed_forward", "true")
            logCustomEvent(event)
        }

        fun toggledSpeed() {
            val event = CustomEvent(KEY_AUDIO_CONTROL)
            event.putCustomAttribute("toggled_speed", "true")
            logCustomEvent(event)
        }

        fun permissionToMatchContacts(gavePermission: Boolean) {
            val event = CustomEvent("Contact Matching")
            event.putCustomAttribute("gavePermission", gavePermission.toString())
            logCustomEvent(event)
        }


        fun seenAttachment(isPhoto: Boolean) {
            val event = CustomEvent(KEY_ATTACHMENTS)
            event.putCustomAttribute("is_photo", isPhoto.toString())
            logCustomEvent(event)
        }

        fun setAttachmentPhoto() {
            val event = CustomEvent(KEY_ATTACHMENTS)
            event.putCustomAttribute("sendPhoto", true.toString())
            logCustomEvent(event)
        }

        fun setAttachmentLink() {
            val event = CustomEvent(KEY_ATTACHMENTS)
            event.putCustomAttribute("sendLink", true.toString())
            logCustomEvent(event)
        }

        fun usedSoundoji(type: SoundojiType) {
            val event = CustomEvent("Playback Soundoji")
            event.putCustomAttribute("type", type.name)
            logCustomEvent(event)
        }

        fun notificationPressed(notificationType: NotificationType) {
            val event = CustomEvent(KEY_NOTIFICATION_SELECTED)
            event.putCustomAttribute("type", notificationType.description)
            logCustomEvent(event)
        }
    }

}