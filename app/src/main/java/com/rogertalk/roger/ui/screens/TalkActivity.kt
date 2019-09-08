package com.rogertalk.roger.ui.screens

import android.Manifest.permission
import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.isInvisibleOrGone
import com.rogertalk.kotlinjubatus.isVisible
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.android.handlers.DelayedActionListener
import com.rogertalk.roger.android.handlers.DelayedActionsHandler
import com.rogertalk.roger.android.notification.NotificationsHandler
import com.rogertalk.roger.android.services.*
import com.rogertalk.roger.android.tasks.DeviceContactQueryTask
import com.rogertalk.roger.event.broadcasts.AudioTokenEvent
import com.rogertalk.roger.event.broadcasts.ConnectivityChangedEvent
import com.rogertalk.roger.event.broadcasts.GroupAvatarReadyEvent
import com.rogertalk.roger.event.broadcasts.audio.*
import com.rogertalk.roger.event.broadcasts.media.MediaKeyPlayStopEvent
import com.rogertalk.roger.event.broadcasts.streams.NextStreamsRequestFinishedEvent
import com.rogertalk.roger.event.broadcasts.streams.RefreshStreamsEvent
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.event.success.ParticipantRemovedSuccessEvent
import com.rogertalk.roger.event.success.RecordingUploadSuccessEvent
import com.rogertalk.roger.event.success.SingleStreamSuccessEvent
import com.rogertalk.roger.event.success.StreamsSuccessEvent
import com.rogertalk.roger.helper.*
import com.rogertalk.roger.helper.audio.MediaKeysHelper
import com.rogertalk.roger.manager.*
import com.rogertalk.roger.manager.EventTrackingManager.*
import com.rogertalk.roger.manager.EventTrackingManager.PlaybackSource.*
import com.rogertalk.roger.manager.EventTrackingManager.RecordingReason.TAP_MIC
import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager.bufferingOrPlaying
import com.rogertalk.roger.manager.audio.PlaybackStateManager.doingAudioIO
import com.rogertalk.roger.manager.audio.PlaybackStateManager.recording
import com.rogertalk.roger.models.data.*
import com.rogertalk.roger.models.data.AudioState.*
import com.rogertalk.roger.models.holder.OnBoardingDataHolder
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.network.request.*
import com.rogertalk.roger.network.request.reporting.StereoUnavailableRequest
import com.rogertalk.roger.repo.*
import com.rogertalk.roger.ui.adapters.ConversationsAdapter
import com.rogertalk.roger.ui.adapters.listener.ConversationsListener
import com.rogertalk.roger.ui.cta.buzzToast
import com.rogertalk.roger.ui.dialog.CTADialogs
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.dialog.ConversationDialogs
import com.rogertalk.roger.ui.dialog.listeners.ShareRankListener
import com.rogertalk.roger.ui.screens.base.PlayerBaseScreen
import com.rogertalk.roger.ui.screens.behaviors.GroupImageGenerator
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils.EXTRA_CAME_FROM_NOTIFICATION
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils.EXTRA_JUST_ONBOARDED
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils.EXTRA_PRE_SELECT_STREAM
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils.EXTRA_RANKING_VALUE
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.android.ShareUtils
import com.rogertalk.roger.utils.changelog.ChangeLogHistory
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.extensions.*
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.*
import com.rogertalk.roger.utils.phone.Vibes
import kotlinx.android.synthetic.main.talk_avatar_empty_group.*
import kotlinx.android.synthetic.main.talk_avatar_generic.*
import kotlinx.android.synthetic.main.talk_screen.*
import kotlinx.android.synthetic.main.talk_screen_idle_controls.*
import me.grantland.widget.AutofitHelper
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.startActivity
import java.nio.charset.Charset
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class TalkActivity : PlayerBaseScreen(true),
        ConversationsListener,
        DelayedActionListener,
        PermissionListener,
        ShareRankListener,
        NfcAdapter.CreateNdefMessageCallback {


    // Conversations List
    private var layoutManager = LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
    private val endlessContactLoadingHelper: ContactEndlessLoadingHelper by lazy(NONE) { ContactEndlessLoadingHelper(conversationsList, layoutManager) }
    private val conversationsAdapter: ConversationsAdapter by lazy(NONE) { createConversationsAdapter() }

    private var recordAudioPermissionListener: PermissionListener? = null
    private val mediaKeyClient: MediaKeysHelper by lazy(NONE) { MediaKeysHelper(this) }
    private val weatherHelper: WeatherHelper by lazy(NONE) { initGlimpsesManager() }
    private val groupDisplayHelper: GroupDisplayHelper by lazy(NONE) { GroupDisplayHelper(this) }
    private val conversationOptionsHelper: ConversationOptionsHelper by lazy(NONE) { ConversationOptionsHelper(this) }
    private val conversationStatusHelper: ConversationStatusHelper by lazy(NONE) { ConversationStatusHelper(listeningCircleFeedback) }
    private val attachmentsHelper: AttachmentsHelper by lazy(NONE) { AttachmentsHelper(this) }
    private val experimentalHelper: ExperimentalHelper by lazy(NONE) { ExperimentalHelper(this) }

    // Playback and Recording Audio Visualizer values
    private var audioLevel = 0.0
    private var audioLevelPrevious = 0.0

    // This manager controls the various tutorial overlays that appear on this screen
    private val overlayHelper: OverlayHelper by lazy(NONE) { OverlayHelper(this) }

    // This class is responsible for generation the split images for groups avatars
    private val groupImageGenManager: GroupImageGenerator by lazy(NONE) { GroupImageGenerator(this) }

    // Timers for count-up and periodic UI refresh
    private var refreshUITimer = DelayedActionsHandler(this, frequencyMillis = TalkActivityUtils.UI_REFRESH_RATE_MILLIS,
            actionCode = TalkActivityUtils.TIMER_REFRESH_UI)
    private var refreshStreamsTimer = DelayedActionsHandler(this, frequencyMillis = 30000,
            actionCode = TalkActivityUtils.TIMER_REFRESH_STREAMS)

    private val currentStream: Stream?
        get() {
            var stream = StreamManager.selectedStream
            if (stream == null) {
                stream = StreamCacheRepo.getCached().firstOrNull() ?: return null

                // Make this the new selected one
                StreamManager.selectedStreamId = stream.id
            }
            return stream
        }


    private var scrollToContact = true

    // OVERRIDE METHODS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.talk_screen)

        // Load player UI
        postLayoutSetup()

        handleIntent(intent)

        // Scan contacts if possible as soon as this screens starts so we get a head start from the user.
        preScanContacts()

        // Allows the system to equate cells and animate data changes for us
        conversationsAdapter.setHasStableIds(true)

        setupUI()

        setupNFC()

        // Reset OnBoarding temp state
        OnBoardingDataHolder.possibleInviteToken = null
        OnBoardingDataHolder.possibleParticipant = null

        // Mark onboarded as fully complete
        PrefRepo.completedOnboarding = true

        // Track TalkHead state
        EventTrackingManager.talkHeadStartState(PrefRepo.talkHeads)
    }


    override fun onStop() {
        super.onStop()

        // Clear layout manager so we try to avoid memory leaks.
        layoutManager.recycleChildrenOnDetach = true
        layoutManager.removeAllViews()

        refreshUITimer.removeMessages()
        refreshStreamsTimer.removeMessages()

        // Clear cache list
        AudioDownloadManager.clearEntireList()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logMethodCall()

        if (intent != null) {
            handleIntent(intent)
        }

        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            handleNFCmessage(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        logMethodCall()

        // Reset scrolling to contact
        scrollToContact = true

        // Select correct contact from global setting
        StreamManager.selectedStream?.let {
            updateStreamId(it.id)
        }


        PushNotificationManager.initPushNotifications()

        refreshSessionToken()

        // Re-assert performance mode
        PerformanceManager.reassertPerformanceMode()

        // Change connectivity display based on current state
        evaluateConnectivityState()

        // Retry uploading previously failed chunks
        UploadRetryManager.run()

        // Upload new location to server
        startService(GlimpsesService.start(this))

        // Do a network request for new data.
        refreshStream()

        // Refresh UI every once in a while.
        refreshUITimer.startNow()

        // Refresh streams once in a while.
        refreshStreamsTimer.startNow()

        // Clear any pending notifications.
        NotificationsHandler.clearNotifications()

        updateWithAudioState()

        // Register for media key events.
        mediaKeyClient.registerMediaKeysReceiver()

        // Start the cache clean-up
        CleanupCacheManager.runCleanup()

        // Weather update. The manager will control how often the request actually happens.
        WeatherManager.updateWeather()

        resetOnBoardingFlags()

        // Show tutorial if applicable
        handleTutorialShowcase()

        refreshServicesCache()

        if (currentStream != null) {
            hideConversationsList()
        }
    }

    override fun handleAudioStateChange(oldState: AudioState, newState: AudioState) {
        super.handleAudioStateChange(oldState, newState)
        // React to completion of playback/recording.
        if (newState == IDLE && oldState == RECORDING) {
            recordingComplete()
        }

        // Not playing anymore, but was playing
        if (oldState == PLAYING && newState != PLAYING) {
            // Reset visualizer
            resetAudioLevel()

            // Reset message
            raiseToListenDisplay(false)
        }

        // if playback started use new volume slider control
        if (newState == PLAYING || newState == BUFFERING) {
            freezeUIListening()
        }

        updatePlaybackControlsUI()

        // Update unplayed counter display
        currentStream?.let {
            updateUnplayedUI(it.unplayed)
        }

        if (newState == IDLE) {
            unFreezeUI()
        }

        updateUserCardDisplay()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        logInfo { "resultCode: $resultCode, RequestCode: $requestCode" }
        when (resultCode) {
            ContactsActivity.RESULT_SELECT_STREAM -> {
                conversationsAdapter.updateCurrentlySelected()
                updateConversationsView()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()

        // If playing, show a notification.
        if (bufferingOrPlaying) {
            val currentStream = StreamManager.selectedStream ?: return
            val senderId = currentStream.participants[0].id

            // Gather the necessary data for building the notification.
            val notificationData = NotificationData(senderId, currentStream.title,
                    "", currentStream.imageURL, currentStream.id, "")

            // Start the service that will load the image in background and that will then handle
            // displaying the notification to the proper controller.
            startService(NotificationsService.loadAvatarForPlaying(this, notificationData))
        }

        if (recording) {
            NotificationsHandler.updateRecordingNotification(this)
        }

        // Un-register of media key events.
        mediaKeyClient.unregisterMediaKeysReceiver()

        // Hide snackbar on pause, it will get re-checked once we get back
        // This prevents the screen to get stuck for some cases
        hideMessageBar()
    }

    /**
     * We use this to periodically refresh the UI when this screen is visible.
     * Let's try to keep this lightweight (performance wise).
     */
    override fun delayedAction(actionCode: Int) {
        when (actionCode) {
            TalkActivityUtils.TIMER_REFRESH_UI -> {
                // Update UI every UI_REFRESH_RATE_MILLIS milliseconds.
                if (StreamCacheRepo.getCached().isEmpty()) {
                    // If there are no streams, do nothing.
                    return
                }

                // If not playing nor recording, unfreeze UI
                if (PlaybackStateManager.notPlayingNorBuffering && !recording) {
                    unFreezeUI()
                }

                // Revisit network connectivity
                evaluateConnectivityState()

                // Update Card
                updateUserCardDisplay()
            }

            TalkActivityUtils.TIMER_REFRESH_STREAMS -> {
                // Don't forcibly update stream data if playback or recording is in progress
                if (doingAudioIO) {
                    return
                }

                // If loading next stream content, don't issue a stream update now
                if (endlessContactLoadingHelper.loading) {
                    return
                }

                // Do a network request for new data if connected to the internet.
                runIfConnected(this) {
                    refreshStream()
                }
            }
        }
    }

    override fun conversationsItemLongPressed(streamId: Long) {
        logMethodCall()
        if (lockedOnContact()) {
            // Can't react to long press right now.
            return
        }

        updateStreamId(streamId)
        updateUserCardDisplay()

        val stream = StreamManager.selectedStream ?: return
        ConversationDialogs.contactLongPressOptions(this, conversationOptionsHelper, stream)
    }

    override fun scaleRecordingVisualizer(scaleLevel: Float) {
        super.scaleRecordingVisualizer(scaleLevel)
        recordingVisualizer.scaleX = scaleLevel
        recordingVisualizer.scaleY = scaleLevel
    }

    // Called when a list item was clicked
    override fun conversationsItemPressed(streamId: Long) {
        hideConversationsList()

        // Update selection
        updateStreamId(streamId)

        val stream = StreamCacheRepo.getStream(streamId)
        if (stream != null) {
            if (stream.isGroup && stream.isEmptyGroup) {
                startActivity(LobbyActivity.start(this, streamId))
                return
            }
        }

        if (lockedOnContact()) {
            return
        }

        if (!PlaybackStateManager.doingAudioIO && stream != null) {
            PlaybackStateManager.currentStream = stream

            updateUnplayedCounter(stream)
        }

        // TODO: Call updates via an event pipeline.
        updateUserCardDisplay()
    }

    override fun pressedAddContacts() {
        if (!PrefRepo.permissionToMatchContacts || !hasContactsPermission()) {
            startActivity(MatchPermissionsActivity.startTalkScreen(this))
        } else {
            startActivity(ContactsActivity.startFromTalkScreen(this))
        }
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }
        recordingButton.displayAsNotRecording()
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        token?.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        val permissionName = response?.permissionName ?: ""
        val permanentlyDenied = response?.isPermanentlyDenied ?: true
        logDebug { "Denied permission: $permissionName, permanently: $permanentlyDenied" }
        recordingButton.displayAsNotRecording()
    }

    override fun createNdefMessage(event: NfcEvent?): NdefMessage? {
        // Send either username or phone number to another device trough Android Beam
        var usernameVal = UserAccountRepo.current()?.username ?: ""
        if (usernameVal.isEmpty()) {
            usernameVal = UserAccountRepo.current()?.phoneNumber ?: ""
        }

        val msg = NdefMessage(
                arrayOf(createMimeRecord(
                        "application/${RogerConstants.DEFAULT_PACKAGE_NAME}", usernameVal.toByteArray()))
        )
        return msg
    }

    override fun shareRank(rankPosition: Int) {
        ShareUtils.shareTopTalker(this, rankPosition)
    }

    /**
     * Content description varies on Avatar Container to give better action info for TalkBack users.
     */
    override fun updateAvatarContainerContentDescription() {
        super.updateAvatarContainerContentDescription()

        val currentStream = StreamManager.selectedStream ?: return
        if (currentStream.unplayed) {
            // Display listen CTA, along with un-listened seconds
            var secondsValue = audioService?.getChunksToPlay(currentStream)?.duration ?: 0
            secondsValue = Math.ceil(secondsValue.toFloat() / 1000.0).toInt()
            if (secondsValue < 120) {
                val secondsPrefix = resources.getQuantityString(R.plurals.second, secondsValue)
                avatarContainer.contentDescription = getString(R.string.ac_tap_to_listen_unplayed, "$secondsValue $secondsPrefix")
            } else {
                val minutesValue = secondsValue / 60
                val minutesPrefix = resources.getQuantityString(R.plurals.minutes, minutesValue)
                avatarContainer.contentDescription = getString(R.string.ac_tap_to_listen_unplayed, "$minutesValue $minutesPrefix")
            }
        } else {
            if (currentStream.showGreet) {
                avatarContainer.contentDescription = getString(R.string.ac_tap_to_listen_no_messages)
            } else {
                avatarContainer.contentDescription = getString(R.string.ac_tap_to_listen_re_listen)
            }
        }
    }

    override fun playSelectedStream(playbackSource: PlaybackSource) {
        if (lockedOnContact()) {
            logWarn { "Locked on contact, cannot play stream." }
            return
        }
        val stream = currentStream ?: return

        if (stream.playableChunks().isEmpty()) {
            logDebug { "No playable chunks on this stream" }
            return
        }

        EventTrackingManager.playbackStartReason(playbackSource)

        userInitiatedPlayback()

        raiseToListenDisplay(true)

        PlaybackStateManager.currentStream = stream
        PlaybackStateManager.usingLoudspeaker = true
        audioService?.playStream()
    }

    override fun afterAudioServiceBind() {
        super.afterAudioServiceBind()
        if (PlaybackStateManager.playing) {
            // Resume visualizer
            playbackVisualizer.makeVisible(true)
        }

        if (recording) {
            // Resume visualizer
            recordingVisualizer.makeVisible(true)
        }
    }

    override fun updatePlaybackControlsUI() {
        super.updatePlaybackControlsUI()

        val stream = currentStream
        val showIdleControls = if (stream == null) {
            false
        } else if (stream.isEmptyConversation) {
            false
        } else if (bufferingOrPlaying) {
            false
        } else {
            true
        }

        if (showIdleControls) {
            idleControlsRibbon.makeVisible()
        } else {
            idleControlsRibbon.beGone()
        }
    }

    override fun onBackPressed() {
        if (conversationsList.isVisible()) {
            hideConversationsList()
        } else {
            super.onBackPressed()
        }
    }

    //
    // PUBLIC METHODS
    //

    fun conversationPlaceholderPressed(type: PlaceholderType) {
        hideConversationsList()
        val conversationTitle: String
        when (type) {

            PlaceholderType.FAMILY -> {
                PrefRepo.showAddFamily = false
                conversationTitle = R.string.placeholder_family.stringResource()
            }
            PlaceholderType.FRIENDS -> {
                PrefRepo.showAddFriends = false
                conversationTitle = R.string.placeholder_friends.stringResource()
            }
            PlaceholderType.TEAM -> {
                PrefRepo.showAddTeam = false
                conversationTitle = R.string.placeholder_team.stringResource()
            }
        }
        createConversation(conversationTitle)
    }

    fun microphoneTutorialPressed() {
        recordingButton.buttonPressed()
    }

    fun tapToListenTutorialPressed() {
        playSelectedStream(TAP_AVATAR)
    }

    fun tapManageMembers() {
        editGroupPressed()
    }

    fun groupImageFullyLoaded(bitmap: Bitmap, streamId: Long) {
        val stream = currentStream ?: return
        if (stream.id == streamId) {
            logDebug { "will load image onto avatar now" }
            RoundImageUtils.createRoundImage(this, userProfileImage, bitmap, AvatarSize.BIG)
        }
    }

    fun editGroupPressed() {
        val stream = currentStream
        stream?.let {
            // Reset flag so that this overlay doesn't show again
            PrefRepo.didTapManagerConversation = true

            startActivity(LobbyActivity.start(this, stream.id))
        }

    }

    fun settingsPressed() {
        startActivity(SettingsActivity.start(this))
    }

    //
    // PRIVATE METHODS
    //

    private fun createConversationsAdapter(): ConversationsAdapter {
        logMethodCall()
        return ConversationsAdapter(this, LinkedList<Stream>(), this)
    }

    private fun updateStreamId(streamId: Long) {
        StreamManager.selectedStreamId = streamId

        // Notify adapter to redraw contents
        conversationsAdapter.updateCurrentlySelected()
    }

    private fun refreshServicesCache() {
        val cachedBots = BotCacheManager.cachedBotList
        if (cachedBots.isEmpty() && ConnectivityHelper.isConnected(this)) {
            ServicesRequest().enqueueRequest()
        }
    }

    private fun raiseToListenDisplay(visible: Boolean) {
        if (visible) {
            if (!PlaybackStateManager.usingAlternateOutput) {
                bottomMessage.makeVisible(true)
                bottomMessage.text = R.string.raise_to_listen_private.stringResource() + " {gmd-hearing}"
            }
        } else {
            bottomMessage.beGone(true)
        }
    }

    private fun resetAudioLevel() {
        playbackVisualizer.beGone()
        audioLevelPrevious = 0.0
        audioLevel = 0.0
    }

    private fun redrawVisualizer(visualizerType: VisualizerType) {
        when (visualizerType) {
        // Playback visualizer
            VisualizerType.PLAYBACK -> {
                val playbackLevel = {
                    // Smooth out changes in the audio level
                    if (audioLevel < audioLevelPrevious) {
                        (audioLevelPrevious * 13 + audioLevel) / 14
                    } else {
                        audioLevel
                    }
                }()

                audioLevelPrevious = playbackLevel
                val visualizerScale = (0.75 + playbackLevel / Math.sqrt(playbackLevel + 0.01f)).toFloat()
                playbackVisualizer.scaleX = visualizerScale
                playbackVisualizer.scaleY = visualizerScale

            }

        // Recording visualizer
            VisualizerType.RECORDING -> {
                val recordingLevel = {
                    // Smooth out changes in the audio level
                    if (audioLevel < audioLevelPrevious) {
                        (audioLevelPrevious * 5 + audioLevel) / 6
                    } else {
                        audioLevel
                    }
                }()

                audioLevelPrevious = recordingLevel
                val visualizerScale = (1 + recordingLevel / Math.sqrt(recordingLevel + 0.01f)).toFloat()
                scaleRecordingVisualizer(visualizerScale)
            }
        }
    }

    /**
     * Updates the UI regarding the current audio state
     */
    private fun updateWithAudioState() {
        // Update stream display immediately according to latest streams content
        if (StreamCacheRepo.getCached().isEmpty()) {
            logWarn { "Stream cache is empty, nothing to update" }
            updateUserCardDisplay()
            return
        }

        // If not playing nor recording, unfreeze UI.
        if (PlaybackStateManager.notPlayingNorBuffering && !recording) {
            unFreezeUI()

            // Re-calculate the remaining seconds
            PlaybackCounterManager.showInitialRemainingTime()

            // Update Conversations view immediately
            updateConversationsView()

            // We don't need to go any further
            return
        }

        val playbackStream = PlaybackStateManager.currentStream
        if (playbackStream == null) {
            logError { "Playback state is ${PlaybackStateManager.state.name} but no stream defined!" }
            return
        }

        // Decision for when to freeze/unfreeze based on listening status
        if (PlaybackStateManager.notPlayingNorBuffering) {
            unFreezeUI()
        } else {
            freezeUIListening()
        }

        // Handle recording state
        if (!recording) {
            // Reset UI state of recording
            recordingVisualizer.beGone()
            recordingButton.displayAsNotRecording()
        } else {
            // We're in currently recording
            freezeUIRecording()

            // Update RecordButton display
            recordingButton.displayAsRecording()
        }

        // Update selected stream
        if (StreamManager.selectedStreamId != playbackStream.id) {
            val ex = Exception("Playback stream ID and Selected stream don't match")
            logError(ex)
        }
        logDebug { "Playback id: ${playbackStream.id}" }
        updateConversationsView()
    }

    private fun createConversation(title: String?) {
        // Clear previous data from LobbyManager
        LobbyManager.clearLobbyState()
        CreateConversationRequest(title).enqueueRequest()
        startActivity(LobbyActivity.start(this))
    }

    private fun handleTutorialShowcase() {
        val stream = currentStream

        // Don't try to show tutorial when audio I/O exists
        if (doingAudioIO) {
            return
        }

        // Display tap to listen overlay
        if (!PrefRepo.didTapToListen) {
            if (stream != null && stream.unplayed) {
                overlayHelper.displayTapToListenOverlay()
                return
            }
        }

        // Display tap to talk overlay
        if (!PrefRepo.didTapToTalk && stream != null) {
            overlayHelper.displayMicrophoneOverlay()
            return
        }

        // Display manage conversations overlay
        if (!PrefRepo.didTapManagerConversation && stream != null) {
            overlayHelper.displayManageMembersOverlay()
            return
        }

        // Only show changelog if done with all the tutorials
        handleChangelog()
    }

    /**
     * @return True if shown, False otherwise
     */
    private fun handleChangelog(): Boolean {
        // Show change's dialog?
        if (ChangeLogHistory.shouldShowChangelog(this)) {
            // Don't show this again
            ChangeLogHistory.changelogDisplayHandled(this)
            CTADialogs.showLatestChangeDialog(this)
            return true
        }

        // Don't show this again
        ChangeLogHistory.changelogDisplayHandled(this)

        return false
    }

    private fun handleNFCmessage(intent: Intent) {
        EventTrackingManager.androidBeam()
        val rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        // only one message sent during the beam
        val msg = rawMessage[0] as NdefMessage
        // record 0 contains the MIME type, record 1 is the AAR, if present
        val text = String(msg.records[0].payload)

        logInfo { "Got username from NFC: $text" }
        // Will try to add this user
        if (text.isNotBlank()) {
            startActivity(URLHandlerActivity.startConversationInternal(this, text))
        }
    }

    /**
     * Reset some of the onboarding flags, a safety measure to make sure there's no stuck conditions
     * after having properly initialized the app.
     */
    private fun resetOnBoardingFlags() {
        if (PrefRepo.loggedIn) {
            PrefRepo.pendingChooseNameNewUser = false
        }
    }

    /**
     * Used for encoding NFC raw messages
     */
    private fun createMimeRecord(mimeType: String, payload: ByteArray): NdefRecord {
        val mimeBytes = mimeType.toByteArray(Charset.forName("UTF-8"))
        val mimeRecord = NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, ByteArray(0), payload)
        return mimeRecord
    }

    private fun setupNFC() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            EventTrackingManager.deviceHasNFC(false)
            return
        }
        nfcAdapter.setNdefPushMessageCallback(this, this)

        EventTrackingManager.deviceHasNFC(true)
    }

    private fun refreshSessionToken() {
        if (SessionRepo.accessTokenExpired) {
            val refreshToken = SessionRepo.session?.refreshToken ?: ""
            if (refreshToken.isNotEmpty()) {
                logDebug { "will refresh session" }
                RefreshTokenRequest(refreshToken).enqueueRequest()
            } else {
                logWarn { "Refresh token is empty!" }
            }
        }
    }

    private fun initGlimpsesManager(): WeatherHelper {
        return WeatherHelper(cardNightBackgroundView, cardSunsetBackgroundView,
                cardDayBackgroundView, cardSunriseBackgroundView, smileBackground)
    }

    private fun requestAudioRecordPermission() {
        // TODO : review copy and use proper icon
        val dialogOnDeniedPermissionListener =
                DialogOnDeniedPermissionListener.Builder.withContext(this)
                        .withTitle(R.string.perm_audio_record_title)
                        .withMessage(R.string.perm_audio_record_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        recordAudioPermissionListener = CompositePermissionListener(this,
                dialogOnDeniedPermissionListener)

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(recordAudioPermissionListener, RECORD_AUDIO)
        }
    }

    private fun preScanContacts() {
        if (hasPermission(this, permission.READ_CONTACTS)) {
            // Has permission to scan
            DeviceContactQueryTask().execute()
        }
    }

    private fun startRecordingAction() {
        if (lockedOnContact()) {
            logWarn { "Locked on contact, cannot record." }
            return
        }

        val stream = StreamManager.selectedStream ?: return

        // Keep screen on while recording
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Reset visualizer
        recordingVisualizer.makeVisible()
        recordingVisualizer.scaleX = 1f
        recordingVisualizer.scaleY = 1f

        audioService?.startRecording(stream, createChunkToken = !stream.otherIsActive)

        freezeUIRecording()
    }

    private fun stopRecordingAction() {
        audioService?.stopRecording()
        unFreezeUI()
    }

    private fun recordButtonAreaPressed() {
        if (!hasAudioRecordPermission()) {
            requestAudioRecordPermission()
            recordingButton.displayAsNotRecording()
            return
        }
        val recordingStream = currentStream
        if (recordingStream == null) {
            logWarn { "There is no current stream" }
            recordingButton.displayAsNotRecording()
            return
        }

        PlaybackStateManager.currentStream = recordingStream

        // Check if user is Alexa and needs connecting
        val currentContact = StreamManager.selectedStream?.others?.firstOrNull() ?: null
        if (currentContact != null && currentContact.id == RogerConstants.ALEXA_ACCOUNT_ID) {
            if (!PrefRepo.alexaConnected) {
                startActivity<AlexaConnectScreen>()
                return
            }
        }

        if (recording) {
            PrefRepo.didSendChunk = true
            EventTrackingManager.recordingCompleted(TAP_MIC)
            stopRecordingAction()
        } else {
            PrefRepo.didTapToTalk = true
            EventTrackingManager.recordingStart(TAP_MIC)
            startRecordingAction()
        }
    }

    private fun displayMessageBar(stringRef: Int) {
        // Don't re-display if it is already displaying
        topMessageLabel.setText(stringRef)
        topMessageSpaceGuide.makeVisible()
        topMessageContainer.makeVisible(true)
        topNavigationControls.removeStatusBarPadding()
    }

    private fun hideMessageBar() {
        topMessageContainer.beGone(true)
        topMessageSpaceGuide.beGone()
        topNavigationControls.statusBarPadding(this)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_JUST_ONBOARDED, false)) {
            logDebug { "user just onboarded" }
            // Don't display anything else
            return
        }

        // Track interactions from expiring audio notification
        if (intent.hasExtra(EXTRA_CAME_FROM_NOTIFICATION)) {
            val cameFromNotification = intent.getBooleanExtra(EXTRA_CAME_FROM_NOTIFICATION, false)
            if (cameFromNotification) {
                EventTrackingManager.notificationPressed(EventTrackingManager.NotificationType.EXPIRING_AUDIO)
            }
        }
        if (intent.hasExtra(EXTRA_RANKING_VALUE)) {
            showShareRankDialog(intent.getIntExtra(EXTRA_RANKING_VALUE, -1))
        }

        if (intent.hasExtra(EXTRA_PRE_SELECT_STREAM)) {
            val streamIdToPreselect = intent.getLongExtra(EXTRA_PRE_SELECT_STREAM, NO_ID)
            if (streamIdToPreselect != NO_ID) {
                logDebug { "Will try to preselect stream: $streamIdToPreselect" }
                updateStreamId(streamIdToPreselect)
            }
        }
    }

    private fun setupUI() {
        // Add padding for top navigation bar
        topNavigationControls.statusBarPadding(this)
        userTitleAccessibility.statusBarPadding(this)
        topMessageContainer.statusBarPadding(this)

        // TalkBack label for group management
        groupManagementElement.contentDescription = R.string.ac_manage_conversation.stringResource()

        // Assign shadow to recording button
        recordingButton.shadow = smileShadow2

        // Audio feedback bubbles should be hidden.
        recordingVisualizer.beGone()

        enableGlimpsesLabel.setOnClickListener { startActivity<WeatherEnableActivity>() }

        // State for UI freezing
        //conversationsOverlay.beGone(true)
        //conversationsOverlay.isClickable = false

        // Setup conversations list
        conversationsList.layoutManager = layoutManager
        conversationsList.adapter = conversationsAdapter

        // Initialize Endless contact manager
        endlessContactLoadingHelper

        handleUIClicks()

        // Track initial state of auto-play
        EventTrackingManager.liveModeStartState(PrefRepo.livePlayback)

        // Auto-fit text for Title and talked seconds
        AutofitHelper.create(userTitleLabel)
        AutofitHelper.create(unplayedTimeLabel)
    }

    private fun showShareRankDialog(rankPosition: Int) {
        if (rankPosition < 1) {
            return
        }
        CommonDialog.shareTopTalkerDialog(this, this, rankPosition)
    }

    private fun handleUIClicks() {
        emptyAddMembersButton.materialize(rippleColor = Color.DKGRAY, useOnNewerAPI = true)
        recordingButton.setToggleAction { recordButtonAreaPressed() }

        groupParticipantsContainer.setOnClickListener {
            groupRibbonPressed()
        }
        groupManagementElement.setOnClickListener {
            groupRibbonPressed()
        }

        emptyAddMembersButton.setOnClickListener { editGroupPressed() }

        cityLabel.setOnClickListener {
            if (PrefRepo.godMode) {
                experimentalHelper.displayExperimentalUI()
            }
        }

        buzzContainer.setOnClickListener {
            currentStream?.let {
                BuzzStreamRequest(it.id).enqueueRequest()

                // Track user buzzes
                EventTrackingManager.buzzedSomeone()

                buzzToast()
                Vibes.mediumVibration()
            }
        }

        conversationsButton.setOnClickListener {
            conversationsButtonPressed()
        }

        recordingDramaticBackground.setOnClickListener {
            dramaticBackgroundPressed()
        }

        settingsButton.setOnClickListener {
            settingsPressed()
        }

        attachmentsArea.setOnClickListener {
            currentStream?.let {
                attachmentsHelper.attachmentPressed(it.id)
            }

        }

        // Soundojis
        soundojiLaughing.setOnClickListener {
            postEvent(SoundojiPlayEvent(SoundojiType.LAUGHING))
        }

        soundojiAwkwardCricket.setOnClickListener {
            postEvent(SoundojiPlayEvent(SoundojiType.AWKWARD_CRICKET))
        }

        soundojiRimshot.setOnClickListener {
            postEvent(SoundojiPlayEvent(SoundojiType.RIMSHOT))
        }
    }

    private fun refreshStream() {
        if (StreamCacheRepo.streamCacheFresh) {
            logVerbose { "StreamCache still fresh, not refreshing yet" }
            return
        }
        StreamsRequest().enqueueRequest()
    }


    private fun groupRibbonPressed() {
        val currentStream = StreamManager.selectedStream ?: return
        startActivity(LobbyActivity.start(this, currentStream.id))
    }

    /**
     * Freezes UI so that user cannot interact with recent contact list or microphone.
     */
    private fun freezeUI() {
        //conversationsOverlay.makeVisible(true)
        //conversationsOverlay.isClickable = true

        groupManagementElement.isClickable = false
        groupParticipantsContainer.isClickable = false

        // Overlay visibility
        val stream = currentStream ?: return
        val unheardContentPresent = stream.unplayed
        if (!unheardContentPresent) {
            if (!PrefRepo.didTapToTalk) {
                overlayHelper.displayTapToListenOverlay()
            }
        }
    }

    private fun freezeUIRecording() {
        recordingDramaticBackground.makeVisible(true)

        // Only display sound effects if playing trough loudspeaker
        if (!PlaybackStateManager.usingAlternateOutput) {
            soundojiRow1.makeVisible(true)
        }

        talkNearMicrophoneLabel.makeVisible(true)
        val recordingStream = PlaybackStateManager.currentStream
        recordingStream?.let {
            talkingWithLabel.makeVisible(true)
            talkingWithLabel.text = R.string.talk_screen_speaking_with.stringResource(it.title)
        }

        accessibilityFreezeRecording()

        freezeUI()
    }

    /**
     * Specialized UI freezing for when listening to content
     */
    private fun freezeUIListening() {
        if (!recording) {
            recordingButton.freeze()
            recordingButton.isEnabled = false
        }

        // Freeze settings button as well
        settingsButton.isEnabled = false
        conversationsButton.isEnabled = false

        accessibilityFreezeListening()

        // Shared freezing logic
        freezeUI()
    }

    private fun showConversationsList() {
        conversationsList.makeVisible(true)
        conversationsList.requestFocus()
        conversationsList.requestFocusFromTouch()
        recordingButton.beGone()
        recordingDramaticBackground.makeVisible()
        accessibilityFreeze()
    }

    private fun hideConversationsList() {
        // Only hide if there is at least 1 stream
        if (currentStream != null) {
            conversationsList.beGone()
            if (!doingAudioIO) {
                recordingButton.makeVisible()
                recordingDramaticBackground.beGone()
            }
            accessibilityUnFreeze()
        }
    }

    private fun accessibilityFreezeRecording() {
        accessibilityFreeze()

        // Unfreeze the components we still need
        recordingButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun accessibilityFreezeListening() {
        accessibilityFreeze()

        // Unfreeze the components we still need
        playbackControlsRibbon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun accessibilityFreeze() {
        settingsButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        conversationsLabelCTA.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        conversationsButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        recordingButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        cardInfoAccessibility.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        groupManagementElement.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        idleControlsRibbon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        playbackControlsRibbon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        avatarGenericContent.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    private fun accessibilityUnFreeze() {
        settingsButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        conversationsLabelCTA.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        conversationsButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        recordingButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        cardInfoAccessibility.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        groupManagementElement.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        idleControlsRibbon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        playbackControlsRibbon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        avatarGenericContent.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /**
     * Un-Freezes the UI, so the user can interact with recent contacts list and microphone
     */
    private fun unFreezeUI() {
        if (!doingAudioIO && conversationsList.isInvisibleOrGone()) {
            recordingDramaticBackground.beGone()
        }
        soundojiRow1.beGone()
        talkingWithLabel.beGone()
        talkNearMicrophoneLabel.beGone()

        if (currentStream != null) {
            recordingButton.unFreeze()
            recordingButton.isEnabled = true
        }

        // Unfreeze top elements
        groupManagementElement.isClickable = true
        groupParticipantsContainer.isClickable = true

        // Unfreeze settings button
        settingsButton.isEnabled = true
        conversationsButton.isEnabled = true

        handleTutorialShowcase()

        // Reset message
        raiseToListenDisplay(false)

        accessibilityUnFreeze()
    }

    private fun dramaticBackgroundPressed() {
        hideConversationsList()
    }

    private fun conversationsButtonPressed() {
        showConversationsList()
    }

    private fun recordingComplete() {
        logMethodCall()

        // Update internal state.
        resetAudioLevel()
        recordingVisualizer.beGone()
        recordingButton.displayAsNotRecording()

        // Release Keep Screen ON
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun evaluateConnectivityState() {
        if (ConnectivityHelper.isConnected(this)) {
            hideMessageBar()
        } else {
            displayMessageBar(R.string.offline_message_talk_screen)
        }
    }

    /**
     * Will update the list of conversations, and its state
     */
    private fun updateConversationsView() {
        logMethodCall()
        val streams = StreamCacheRepo.getCachedCopy()
        conversationsAdapter.updateItems(streams)

        // Pre-select a contact if required.
        handlePreSelectContact()

        // Update card UI.
        updateUserCardDisplay()

        currentStream?.let {
            updateUnplayedCounter(it)
        }

        updateConversationsCTA()
    }

    /**
     * Update the CTA that displays the number of unheard conversations
     */
    private fun updateConversationsCTA() {
        val streams = StreamCacheRepo.getCachedCopy()
        val unheardStreams = streams.filter { it.unplayed }
        if (unheardStreams.isEmpty()) {
            conversationsCTA.beGone()
        } else {
            conversationsCTA.makeVisible(true)
            conversationsLabelCTA.setText(unheardStreams.size.toString())
        }
    }

    /**
     * @return True is display is locked to a given contact (because playback is active for example).
     */
    private fun lockedOnContact(): Boolean {
        return (doingAudioIO)
    }

    private fun handlePreSelectContact() {
        if (lockedOnContact()) {
            return
        }

        // Scroll there if needed
        conversationsAdapter.updateCurrentlySelected()

        // Only scroll automatically if currently NOT loading more
        if (!endlessContactLoadingHelper.loading && scrollToContact) {
            scrollToContact = false
            layoutManager.scrollToPosition(conversationsAdapter.selectedPosition)
        }
    }

    /**
     * Main method that should be called for updating the card contents.
     */
    private fun updateUserCardDisplay() {
        handleTutorialShowcase()
        updatePlaybackControlsUI()

        // Clear view if there is no stream
        val stream = currentStream
        if (stream == null) {
            renderNoStream()
            return
        }
        updateAvatarContainerContentDescription()
        updateCentralContent(stream)
        updateCardAvatar(stream)
        updateCardText(stream)
        updateUnplayedCounter(stream)
        updateUnplayedUI(stream.unplayed)
        updateCardBackground(stream)
        updateRealTimeStatusUI(stream)
        attachmentsHelper.renderAttachments(stream)
        groupDisplayHelper.renderParticipants(stream)
    }

    /**
     * Update User Card display when there is no stream
     */
    private fun renderNoStream() {
        groupDisplayHelper.clearParticipants()
        avatarGenericContent.beGone()
        avatarEmptyGroupContent.beGone()
        groupManagementElement.beGone()
        attachmentsArea.beGone()
        attachmentsCTA.beGone()
        membersLabels.beGone()

        // Hide title
        userTitleLabel.text = ""
        statusMsgLabel.text = ""

        // Reset weather look
        resetWeatherDisplay()
        weatherHelper.showDayBackground()

        // Display Conversations list so the user is forced to add a conversation
        showConversationsList()
    }

    private fun resetWeatherDisplay() {
        cityLabel.beGone()
        localTimeLabel.beGone()
        weatherConditionLabel.beGone()
        weatherIconLabel.beGone()
        enableGlimpsesLabel.beGone()
        // Clear description for talkback
        cardInfoAccessibility.contentDescription = ""
    }

    /**
     * This is where we change the display of the central area of the screen.
     * Amongst the variations is for example the 'empty group' display.
     */
    private fun updateCentralContent(stream: Stream) {
        if (stream.isEmptyGroup && avatarEmptyGroupContent.isInvisibleOrGone()) {
            avatarGenericContent.beGone()
            avatarEmptyGroupContent.makeVisible()
        } else if (!stream.isEmptyGroup && avatarGenericContent.isInvisibleOrGone()) {
            avatarGenericContent.makeVisible()
            avatarEmptyGroupContent.beGone()
        }
    }

    private fun updateCardAvatar(stream: Stream) {
        // During playback, display photo of person talking
        val participantTalking = PlaybackStateManager.participantCurrentChunk
        if (participantTalking != null) {
            if (participantTalking.imageURL != null) {
                RoundImageUtils.createRoundImageMainAvatar(this, userProfileImage,
                        participantTalking.imageURL)
                return
            }
        }

        // Select image destination
        val otherParticipantPhoto = stream.imageURL

        if (otherParticipantPhoto != null) {
            RoundImageUtils.createRoundImageMainAvatar(this, userProfileImage,
                    otherParticipantPhoto)
        } else {
            // If there is no photo, present Mr. Pee
            userProfileImage.setImageResource(R.drawable.pee)

            // For groups with more than 2 participants let's display a split view
            if (stream.isGroup) {
                // Load images in the background. Once ready @groupImageFullyLoaded is called
                groupImageGenManager.loadImages(stream)
            }
        }
    }

    /**
     * Update card Title and description
     */
    private fun updateCardText(stream: Stream) {
        var cardTitle = stream.title
        if (cardTitle.length > TalkActivityUtils.MAX_TITLE_CHARACTERS) {
            cardTitle = "${cardTitle.take(40)}…"
        }
        userTitleLabel.text = cardTitle

        if (stream.isEmptyGroup) {
            statusMsgLabel.text = ""
            userTitleAccessibility.contentDescription = "$cardTitle . ${getString(R.string.ac_empty_group_description)}"
        } else {
            val statusMessage = getStatusMessage(stream)
            statusMsgLabel.text = statusMessage

            // Update title for talkback users
            userTitleAccessibility.contentDescription = "$cardTitle $statusMessage"
        }
    }

    private fun getStatusMessage(stream: Stream): String {
        // On playback, state who is talking at a particular moment
        val participantTalking = PlaybackStateManager.participantCurrentChunk
        if (participantTalking != null) {
            return R.string.real_time_currently_listening_to.stringResource(participantTalking.shortName)
        }

        // Check RT conversation status
        if (stream.isGroup) {
            val participantToShow = stream.participantForLastStatus

            // We got a participant for status of this group, proceed
            if (participantToShow != null) {
                when (stream.statusForStream) {
                    StreamStatus.TALKING -> {
                        return R.string.real_time_status_talking_to_you_group.stringResource(participantToShow.shortName)
                    }
                    StreamStatus.LISTENING -> {
                        return R.string.real_time_status_listening_group.stringResource(participantToShow.shortName)
                    }
                }
            }
        } else {
            // 1 on 1 RT status
            when (stream.statusForStream) {
                StreamStatus.TALKING -> {
                    return R.string.real_time_status_talking_to_you_single.stringResource()
                }
                StreamStatus.LISTENING -> {
                    return R.string.real_time_status_listening_single.stringResource()
                }
            }
        }

        // The usual interaction label
        return stream.lastInteractionLabel
    }

    private fun updateUnplayedCounter(stream: Stream) {
        if (stream.isEmptyGroup) {
            return
        }
        if (stream == PlaybackStateManager.pausedStream) {
            val duration = PlaybackStateManager.pauseRemainingMillis
            renderPlaybackCounter(Math.ceil(duration / 1000.0).toLong())
            return
        }

        renderPlaybackCounter(PlaybackCounterManager.remainingSeconds)
    }

    private fun updateCardBackground(stream: Stream) {
        if (stream.isEmptyGroup) {
            resetWeatherDisplay()
            return
        }

        val participant = stream.participants.firstOrNull() ?: return
        val participantTime: Calendar
        var location: String? = null
        if (!WeatherRepo.participantHasWeather(participant.id)) {
            // default to clear day then
            participantTime = Calendar.getInstance().dayExample()
            weatherIconLabel.beGone()
        } else {
            participantTime = participant.getUserTime()
            location = participant.location
        }

        // Update weather background
        weatherHelper.updateBackground(participantTime)

        if (LocationRepo.locationEnabled) {
            enableGlimpsesLabel.beGone()
            weatherConditionLabel.makeVisible()
            cityLabel.makeVisible()
            localTimeLabel.makeVisible()
        } else {
            cityLabel.beGone()
            localTimeLabel.beGone()
            weatherConditionLabel.beGone()
            weatherIconLabel.beGone()
            enableGlimpsesLabel.makeVisible()

            // Clear description for talkback
            cardInfoAccessibility.contentDescription = ""
            return
        }

        // Display User time.
        if (participant.timezone == null) {
            localTimeLabel.text = ""
            cityLabel.text = ""
        } else {
            localTimeLabel.text = participantTime.shortFormat()
            cityLabel.text = location
        }

        weatherIconLabel.makeVisible()

        // Find appropriate letter for the icon font and text representation
        weatherIconLabel.text = WeatherHelper.getWeatherIconText(participant, participantTime)
        weatherConditionLabel.text = WeatherHelper.getWeatherTemperature(this, participant)

        // Display user info for talkback
        val temperature = weatherConditionLabel.text
        if (location != null && location.isNotEmpty() && temperature.isNotEmpty()) {
            val userName = currentStream?.title ?: ""
            val weatherDescription = WeatherHelper.getWeatherTextRepresentation(this, participant, participantTime)
            val userTime = localTimeLabel.text

            val cardTextualInfo = resources.getString(R.string.ac_card_state_description, userName, location, userTime, temperature, weatherDescription)

            cardInfoAccessibility.contentDescription = cardTextualInfo
        } else {
            cardInfoAccessibility.contentDescription = ""
        }

    }

    private fun updateRealTimeStatusUI(stream: Stream) {
        conversationStatusHelper.updateAnimations(stream)
    }

    /**
     * This method is used to handle common actions to be taken in
     * the event of a user initiating playback.
     */
    private fun userInitiatedPlayback() {
        //Reset flag for listening
        PrefRepo.didTapToListen = true
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onGroupAvatarReady(event: GroupAvatarReadyEvent) {
        logDebug { "New group avatar ready for stream: ${event.streamId}" }
        conversationsAdapter.groupAvatarReady(event.streamId)
    }

    @Subscribe(threadMode = MAIN)
    fun audioRouteChanged(event: AudioRouteChangedEvent) {
        logDebug { "New audio route: ${event.newAudioStreamType.name}" }
        volumeControlStream = event.newAudioStreamType.intValue
    }

    @Subscribe(threadMode = MAIN)
    fun onShouldAutoplayStream(event: SwitchAndPlayStreamEvent) {
        logEvent(event)

        // Update UI
        updateStreamId(event.stream.id)
        updateConversationsView()

        // Play stream
        playSelectedStream(AUTOPLAY_INSIDE)
    }

    /**
     * This method is called when MediaKey Play/Stop is pressed.
     * Here we only initialize Playback.
     */
    @Subscribe(threadMode = MAIN)
    fun onMediaKeyPlayStopEvent(event: MediaKeyPlayStopEvent) {
        EventTrackingManager.pressMediaKey()
        val stream = StreamManager.selectedStream ?: return

        if (PlaybackStateManager.notPlayingNorBuffering && !recording) {
            // App is standing still
            if (stream.unplayed) {
                // Play un-played content
                playSelectedStream(PRESS_MEDIA_KEY)
            } else {
                // Start recording
                recordingButton.buttonPressed()
            }
            return
        }

        // If recording, stop
        if (recording) {
            EventTrackingManager.recordingCompleted(RecordingReason.PRESS_MEDIA_KEY)
            recordingButton.buttonPressed()
            return
        }

        // If playing, stop
        if (bufferingOrPlaying) {
            EventTrackingManager.playbackStop(PlaybackStopReason.PRESS_MEDIA_KEY)
            stopPlaybackAction()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onRefreshStreams(event: RefreshStreamsEvent) {
        logEvent(event)
        refreshStream()
    }

    @Subscribe(threadMode = MAIN)
    fun onStreamsChanged(event: StreamsChangedEvent) {
        logEvent(event)
        updateConversationsView()
        StreamCacheRepo.evaluateGhostAvailability()
    }

    @Subscribe(threadMode = MAIN)
    fun onParticipantRemovedSuccess(event: ParticipantRemovedSuccessEvent) {
        logEvent(event)
        handleTutorialShowcase()
    }

    /**
     * Callback to first page of streams
     */
    @Subscribe(threadMode = MAIN)
    fun onNewStreams(event: StreamsSuccessEvent) {
        logEvent(event)
        StreamCacheRepo.updateCache(event.streams)

        // Inform the infinite loading manager that the regular stream refresh just happened
        // to it can manage it's state based on that
        if (StreamCacheRepo.nextCursor != null) {
            endlessContactLoadingHelper.handleStreamsLoaded()
        } else {
            // Nothing else to load, stop loading from appearing
            endlessContactLoadingHelper.finishedLoadingNext()
        }

        StreamCacheRepo.evaluateGhostAvailability()

        // If streams are empty, display primer again
        if (event.streams.isEmpty()) {
            if (StreamCacheRepo.getCached().isEmpty()) {
                PrefRepo.pendingPrimer = true
            }
            handleTutorialShowcase()
        }

        updateConversationsView()
    }

    /**
     * Callback to subsequent pages of streams
     */
    @Subscribe(threadMode = MAIN)
    fun onNewNextStreams(event: NextStreamsRequestFinishedEvent) {
        endlessContactLoadingHelper.finishedLoadingNext()
        StreamCacheRepo.evaluateGhostAvailability()
    }

    /**
     * Called when we got a single stream from remote server.
     * Can happen after playing a stream.
     */
    @Subscribe(threadMode = MAIN)
    fun onNewSingleStream(event: SingleStreamSuccessEvent) {
        logEvent(event)
        StreamCacheRepo.updateStreamInStreams(event.singleStream)
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioTokenEvent(event: AudioTokenEvent) {
        logEvent(event)
        val chunkToken = event.chunkToken ?: return
        val stream = StreamCacheRepo.getStream(event.streamId) ?: return

        val participant = stream.othersOrEmpty.firstOrNull()
        if (participant != null && chunkToken.isNotEmpty()) {
            if (participant.id == RogerConstants.SHARE_ACCOUNT_ID) {
                InviteActivity.shareViaOther(this, chunkToken, isOpenGroup = false)
                return
            }
        }
    }

    /**
     * Recording was just uploaded successfully to webservice
     */
    @Subscribe(threadMode = MAIN)
    fun onRecordingUploadFinish(event: RecordingUploadSuccessEvent) {
        logEvent(event)

        // TODO: Should we update stream here or upon network callback?
        StreamCacheRepo.updateStreamInStreams(event.stream)

        // update the UI just now
        updateConversationsView()
    }

    @Subscribe(threadMode = MAIN)
    fun onConnectivityChange(event: ConnectivityChangedEvent) {
        logEvent(event)
        evaluateConnectivityState()
    }

    @Subscribe(threadMode = MAIN)
    fun onProximityChange(event: ProximityEvent) {
        logEvent(event)
        val service = audioService ?: return
        if (recording) {
            return
        }
        if (bufferingOrPlaying) {
            if (event.againstEar) {
                service.switchToEarpiece()

                // Reset message display
                raiseToListenDisplay(false)
            }
            return
        }
        if (AccessibilityUtils.isScreenReaderActive(this)) {
            logWarn { "Accessibility is enabled, don't start playback based on proximity trigger" }
            return
        }
        if (!event.againstEar || PlaybackStateManager.usingAlternateOutput) {
            return
        }
        val stream = currentStream ?: return
        // Empty groups should not try to play
        if (stream.isEmptyGroup) {
            return
        }
        if (stream.playableChunks().isEmpty()) {
            logDebug { "No playable chunks on this stream" }
            return
        }

        // Hide tap to listen, in case the user had listened to it based on proximity
        overlayHelper.forceDismissTapToListen()

        userInitiatedPlayback()

        if (!PlaybackStateManager.bufferingOrPlaying) {
            EventTrackingManager.playbackStartReason(RAISE_TO_EAR)
        } else {
            EventTrackingManager.playbackStartReason(RAISE_TO_EAR_SECONDARY)
        }

        PlaybackStateManager.currentStream = stream
        PlaybackStateManager.usingLoudspeaker = false
        service.playStream()
    }

    @Subscribe(threadMode = MAIN)
    fun onConversationStatusEvent(event: ConversationStatusChangeEvent) {
        logEvent(event)
        val currentStream = currentStream ?: return
        conversationStatusHelper.updateAnimations(currentStream)
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioFileSaved(event: SavedAudioFileEvent) {
        ShareUtils.shareAudioToExternalApp(this, event.filename)
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioRecordAdjusted(event: AudioRecordAdjustmentEvent) {
        logEvent(event)
        CommonDialog.simpleMessageWithButton(this, getString(R.string.error_generic_title),
                getString(R.string.recording_failed_adjusting),
                getString(android.R.string.ok))

        // Send this info to server so we can prevent similar devices to go trough this fail in the future
        StereoUnavailableRequest().enqueueRequest()
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioAmplitude(event: AudioAmplitudeEvent) {
        // Convert into our own leveling metric
        audioLevel = Math.pow(10.0, event.amplitude / 40)
        redrawVisualizer(event.visualizerType)
    }

    @Subscribe(threadMode = MAIN)
    fun onChunksSwapped(event: ChunksSwappedEvent) {
        logEvent(event)
        currentStream?.let {
            updateCardText(it)
            updateCardAvatar(it)
        }

    }

}