package com.rogertalk.roger.ui.screens

import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.createPulseAnimation
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.event.broadcasts.ActiveContactUpdatedEvent
import com.rogertalk.roger.event.broadcasts.streams.RefreshStreamsEvent
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.event.failure.CreateGroupFailEvent
import com.rogertalk.roger.event.failure.ParticipantRemovedFailEvent
import com.rogertalk.roger.event.failure.StreamAddParticipantsFailEvent
import com.rogertalk.roger.event.failure.StreamShareableFailEvent
import com.rogertalk.roger.event.success.CreateGroupSuccessEvent
import com.rogertalk.roger.event.success.ParticipantRemovedSuccessEvent
import com.rogertalk.roger.event.success.StreamShareableSuccessEvent
import com.rogertalk.roger.helper.InviterHelper
import com.rogertalk.roger.helper.LobbyAnimationsHelper
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.manager.LobbyManager
import com.rogertalk.roger.manager.LocalInviteManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.models.sections.LobbyListSourcesSection.ContactsSource
import com.rogertalk.roger.models.sections.LobbyListSourcesSection.ContactsSource.*
import com.rogertalk.roger.network.request.*
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.adapters.LobbyAdapter
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.dialog.ConversationDialogs
import com.rogertalk.roger.ui.dialog.LobbyDialogs
import com.rogertalk.roger.ui.dialog.listeners.LeaveStreamListener
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.ui.screens.behaviors.WhiteToolbar
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.android.ShareUtils
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.hasContactsPermission
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.log.logWarn
import kotlinx.android.synthetic.main.lobby_screen.*
import kotlinx.android.synthetic.main.white_toolbar.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.longToast
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class LobbyActivity : EventAppCompatActivity(logOutIfUnauthorized = true),
        WhiteToolbar, LeaveStreamListener {

    override val _toolbar: Toolbar
        get() = toolbar
    override val _context: AppCompatActivity
        get() = this
    override val _toolbarRightActionAnimation: AnimatorSet?
        get() = rightTopButton.createPulseAnimation()

    val inviterHelper: InviterHelper by lazy(NONE) { InviterHelper(this) }

    companion object {
        private val EXTRA_STREAM_ID = "streamId"
        private val EXTRA_CREATING_STREAM = "creatingStreams"

        fun start(context: Context, streamId: Long): Intent {
            val startIntent = Intent(context, LobbyActivity::class.java)
            startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            startIntent.putExtra(EXTRA_CREATING_STREAM, false)
            return startIntent
        }

        fun start(context: Context): Intent {
            val startIntent = Intent(context, LobbyActivity::class.java)
            startIntent.putExtra(EXTRA_CREATING_STREAM, true)
            return startIntent
        }

    }

    private val lobbyAnimationsHelper: LobbyAnimationsHelper by lazy(NONE) { LobbyAnimationsHelper(outerCircle) }
    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }

    private val intentStreamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_STREAM_ID, NO_ID) }

    private val creatingStream: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_CREATING_STREAM, false) }

    private var accountToRemoveId = NO_ID

    var stream: Stream? = null
    val streamId: Long?
        get() {
            val currentStream = stream
            if (currentStream != null) {
                return currentStream.id
            }
            return intentStreamId
        }

    private var adapter: LobbyAdapter? = null

    private var shouldDismissOnNewMembers = false

    // Starting this value as true make is that when editing and existing group
    // doesn't display the animation right away.
    private var displayedActiveAnimation = true

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renewStream()
        if (!creatingStream && stream == null) {
            logError { "Stream not found while in edit mode" }
            finish()
            return
        }

        setContentView(R.layout.lobby_screen)
        setupUI()

        // Mark further behavior for the lobby and app
        if (PrefRepo.pendingPrimer) {
            shouldDismissOnNewMembers = true
            PrefRepo.pendingPrimer = false
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()

        // Start pulsing animation
        lobbyAnimationsHelper.startAnimations()
    }

    override fun onPause() {
        super.onPause()
        lobbyAnimationsHelper.stopAnimations()
    }

    override fun onBackPressed() {
        handleBackPressed()
    }

    override fun leftStream() {
        finish()
    }

    override fun onStop() {
        super.onStop()
        stopToolbarAnimations()
    }

    //
    // PUBLIC METHODS
    //

    fun contactSourcePressed(contactsSource: ContactsSource) {
        val currentStream = stream ?: return

        // Conversation should become visible as soon as the user tries to add new participants
        makeStreamVisible()

        when (contactsSource) {

            ADDRESS_BOOK -> {
                if (!PrefRepo.permissionToMatchContacts || !hasContactsPermission()) {
                    startActivity(MatchPermissionsActivity.startLobbyScreen(this, currentStream))
                } else {
                    val participantIds = currentStream.othersOrEmpty.map { it.id }
                    val currentMembersList = ArrayList<Long>(participantIds.size)
                    currentMembersList.addAll(participantIds)
                    startActivity(ContactsActivity.startEditGroup(this, currentStream.id, currentMembersList))
                }
            }

            HANDLE -> {
                startActivity(ContactsActivity.startSearchByHandle(this, currentStream.id))
            }

            GROUP_SHARE_LINK -> {
                inviteToConversation()
            }

            BOTS -> {
                startActivity(BotLobbyActivity.start(this, currentStream.id))
            }
        }
    }

    fun contactInfoPressed(account: Account) {
        LobbyDialogs.memberOptions(this, account)
    }

    fun invitedContactPressed(account: Account) {
        val deviceAccount = ContactMapRepo.getDeviceContactFromAccountId(account.id)
        if (deviceAccount == null) {
            logWarn { "Could not find device account to remind" }
            return
        }
        // Add to memory so it gets picked up by the Invite screen
        LocalInviteManager.newlyInvitedParticipants = ArrayList<DeviceContactInfo>(1)
        LocalInviteManager.newlyInvitedParticipants.add(deviceAccount)

        inviteToConversation()
    }

    fun activeContactPressed(account: Account) {
        startConversationWithContact(account)
    }

    fun startConversationWithContact(account: Account) {
        val userAccount = UserAccountRepo.current() ?: return
        if (userAccount.id == account.id) {
            logDebug { "Trying to start a conversation with self, can't do that" }
            return
        }
        startActivity(URLHandlerActivity.startConversationInternal(this, account.id.toString()))
    }

    fun shareProfile(account: Account) {
        ShareUtils.shareUserProfile(this, account)
    }

    fun displayRemovalConfirmationDialog(accountId: Long) {
        val userAccount = UserAccountRepo.current() ?: return
        if (userAccount.id == accountId) {
            displayRemoveSelfDialog()
            return
        }

        val currentStream = stream ?: return
        accountToRemoveId = accountId
        val participant = currentStream.participants.firstOrNull { it.id == accountToRemoveId } ?: return
        val personName = participant.displayName
        val contentMessage = R.string.dialog_group_removal_description.stringResource(personName)

        MaterialDialog.Builder(this)
                .title(getString(R.string.dialog_group_removal_title))
                .content(contentMessage)
                .positiveText(android.R.string.ok)
                .onPositive { materialDialog, dialogAction -> removeSelectedPerson() }
                .negativeText(android.R.string.cancel)
                .onNegative { materialDialog, dialogAction -> }
                .show()
    }

    fun displayRemoveSelfDialog() {
        val currentStream = stream ?: return
        ConversationDialogs.confirmLeaveStream(this, currentStream, leaveStreamListener = this)
    }

    //
    // PRIVATE METHODS
    //

    private fun inviteToConversation() {
        val currentStream = stream ?: return
        if (currentStream.isOpenGroup) {
            val token = currentStream.inviteToken
            if (token == null) {
                logWarn { "Stream invite token was null!" }
                return
            }
            InviteActivity.shareViaOther(this, token, true, currentStream.title)
        } else {
            convertToOpenGroup()
        }
    }

    private fun makeStreamVisible() {
        val currentStream = stream
        if (currentStream != null && currentStream.visible == false) {
            StreamVisibleRequest(currentStream.id).enqueueRequest()
        }
    }

    private fun convertToOpenGroup() {
        val currentStream = stream ?: return
        progressDialogHelper.showWaiting()
        MakeStreamShareableRequest(currentStream.id).enqueueRequest()
    }

    private fun refreshUI() {
        // Refresh stream if gone out of this screen
        renewStream()

        // Update state for mass inviter
        inviterHelper.refreshWhenStream()

        updateList()
        updateHeader()
        updateTopRightIcon()

        // Update toolbar title
        setToolbarText(stream?.title ?: "")
    }

    private fun removeSelectedPerson() {
        val currentStream = stream ?: return
        StreamRemoveParticipantsRequest(currentStream.id, listOf(accountToRemoveId.toString())).enqueueRequest()

        // Remove from the local stream immediately
        StreamCacheRepo.removeParticipant(accountToRemoveId, currentStream.id)

        // Update UI immediately
        refreshUI()
    }

    private fun renewStream() {
        if (!creatingStream) {
            stream = StreamCacheRepo.getStream(intentStreamId)
        } else {
            // When creating a stream, it might get updated if user adds someone to it
            val possibleStream = StreamCacheRepo.getStream(streamId)
            if (possibleStream != null) {
                stream = possibleStream

                // Clear manager as it is no longer needed
                LobbyManager.clearLobbyState()
            } else {
                stream = LobbyManager.stream
            }

        }

        // If lobby is been marked as dismissible check number of participants and act on it
        stream?.let {
            if (shouldDismissOnNewMembers) {
                if (it.othersOrEmpty.isNotEmpty()) {
                    onBackPressed()
                }
            }
        }
    }

    private fun setupUI() {
        // Toolbar stuff
        setupToolbar(stream?.title ?: "")
        toolbarHasBackAction { handleBackPressed() }
        rightTopButton.contentDescription = R.string.ac_start_conversation.stringResource()

        rightTopButton.setOnClickListener {
            donePressed()
        }

        updateList()
    }

    private fun updateHeader() {
        val currentStream = stream ?: return
        if (currentStream.hasInvitedMembers) {
            // We are waiting for invited members
            header.makeVisible()
            header.setBackgroundResource(R.drawable.lobby_blue_background)
            headerMessage.setText(R.string.lobby_waiting)
        } else {
            if (currentStream.hasActiveMembers) {
                // We have active members, and no more invited. Clear header
                header.beGone()
            } else {
                // All invited members are currently here
                header.makeVisible()
                header.setBackgroundResource(R.drawable.lobby_red_background)
                headerMessage.setText(R.string.lobby_empty_conversation)
            }

        }
    }

    private fun updateTopRightIcon() {
        val currentStream = stream ?: return
        if (currentStream.othersOrEmpty.any { it.active == true }) {
            // There are other active users, we can allow the user to move to the next screen
            setRightActionColor(R.color.s_blue.colorResource(this))
            if (!displayedActiveAnimation) {
                startToolbarRightActionAnimation()
                displayedActiveAnimation = true
            }
        } else {
            displayedActiveAnimation = false
            setRightActionColor(R.color.s_medium_grey.colorResource(this))
        }
    }

    private fun donePressed() {
        val currentStream = stream ?: return
        if (currentStream.othersOrEmpty.any { it.active == true }) {
            preFinishScreen()
            finish()
        } else {
            CommonDialog.simpleMessageWithButton(this,
                    EmojiUtils.thinkingFace,
                    R.string.lobby_conversation_not_ready.stringResource(),
                    android.R.string.ok.stringResource())
        }
    }

    private fun updateList() {
        if (adapter == null) {
            val currentStream = stream
            val participants = if (currentStream != null) {
                currentStream.participants.filter { it.accountReachable == true }
            } else {
                emptyList()
            }
            lobbyList.layoutManager = LinearLayoutManager(this)
            adapter = LobbyAdapter(participants, this)
            adapter?.setHasStableIds(true)
            lobbyList.adapter = adapter
        } else {
            val currentStream = stream ?: return
            adapter?.updateMembers(currentStream.participants.filter { it.accountReachable == true })
        }
    }

    private fun handleBackPressed() {
        // Clear manager to start fresh the next time
        LobbyManager.clearLobbyState()

        preFinishScreen()

        finish()
    }

    /**
     * Common code to execute before finishing this screen
     */
    private fun preFinishScreen() {
        // If this stream has members, select it upon returning to last activity
        stream?.let {
            if (it.othersOrEmpty.isNotEmpty()) {
                StreamManager.selectedStream = it
            }
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onConversationCreated(event: CreateGroupSuccessEvent) {
        logEvent(event)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onConversationCreatedFailed(event: CreateGroupFailEvent) {
        logEvent(event)
        if (creatingStream) {
            if (stream == null) {
                // Make the request again!
                logWarn { "Create Stream request failed. Try again!" }
                CreateConversationRequest(event.title).enqueueRequest()
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onShouldRefreshStream(event: RefreshStreamsEvent) {
        logEvent(event)
        val currentStream = stream ?: return
        SingleStreamRequest(currentStream.id).enqueueRequest()
    }

    @Subscribe(threadMode = MAIN)
    fun onParticipantAddFailed(event: StreamAddParticipantsFailEvent) {
        logEvent(event)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onActiveContactsUpdated(event: ActiveContactUpdatedEvent) {
        logEvent(event)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onStreamsChanged(event: StreamsChangedEvent) {
        logEvent(event)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onParticipantRemovedSuccess(event: ParticipantRemovedSuccessEvent) {
        logEvent(event)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onParticipantRemovedFailed(event: ParticipantRemovedFailEvent) {
        logEvent(event)
        CommonDialog.simpleMessageWithButton(this, "",
                R.string.error_could_not_remove_user.stringResource(),
                android.R.string.ok.stringResource())
    }

    @Subscribe(threadMode = MAIN)
    fun onStreamMadeSharable(event: StreamShareableSuccessEvent) {
        logEvent(event)
        refreshUI()
        inviteToConversation()
        progressDialogHelper.dismiss()

    }

    @Subscribe(threadMode = MAIN)
    fun onStreamMadeSharableFailure(event: StreamShareableFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        longToast(R.string.ob_failed_request)
    }
}
