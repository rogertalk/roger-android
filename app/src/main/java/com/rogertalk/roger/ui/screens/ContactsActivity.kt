package com.rogertalk.roger.ui.screens

import android.Manifest
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.createPulseAnimation
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.android.tasks.DeviceContactQueryTask
import com.rogertalk.roger.event.broadcasts.ActiveContactUpdatedEvent
import com.rogertalk.roger.event.broadcasts.DeviceContactsResultEvent
import com.rogertalk.roger.event.failure.CreateStreamFailEvent
import com.rogertalk.roger.event.failure.ImmediateConvoFailEvent
import com.rogertalk.roger.event.failure.ProfileFailEvent
import com.rogertalk.roger.event.failure.StreamAddParticipantsFailEvent
import com.rogertalk.roger.event.success.CreateStreamSuccessEvent
import com.rogertalk.roger.event.success.ImmediateConvoSuccessEvent
import com.rogertalk.roger.event.success.ProfileSuccessEvent
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.EventTrackingManager.InvitationMethod.MASS_INVITE
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.manager.LocalInviteManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.data.InviteContact
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.*
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.repo.ContactsRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.adapters.ContactPickerAdapter
import com.rogertalk.roger.ui.adapters.listener.ContactPicker
import com.rogertalk.roger.ui.chips.ChipsView
import com.rogertalk.roger.ui.chips.views.ChipLineChangeListener
import com.rogertalk.roger.ui.cta.doneToast
import com.rogertalk.roger.ui.cta.requestFailureToast
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.ui.screens.behaviors.WhiteToolbar
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.android.KeyboardUtils
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.*
import com.rogertalk.roger.utils.log.*
import kotlinx.android.synthetic.main.contacts_screen.*
import kotlinx.android.synthetic.main.white_toolbar_contacts.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.below
import org.jetbrains.anko.toast
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class ContactsActivity : EventAppCompatActivity(true),
        ContactPicker,
        View.OnTouchListener,
        ChipLineChangeListener,
        PermissionListener,
        WhiteToolbar {

    override val _toolbar: Toolbar
        get() = toolbar
    override val _context: AppCompatActivity
        get() = this
    override val _toolbarRightActionAnimation: AnimatorSet?
        get() = rightTopButton.createPulseAnimation()

    companion object {

        private val EXTRA_GROUP_EDITING = "groupEditing"
        private val EXTRA_GROUP_STREAM_ID = "groupId"
        private val EXTRA_SEARCH_BY_HANDLE = "searchingByHandle"
        private val EXTRA_PRE_SELECTED_PARTICIPANTS = "preSelectedParticipants"
        private val EXTRA_FROM_TALK_SCREEN = "fromTalkScreen"

        val RESULT_SELECT_STREAM = 1

        fun startFromTalkScreen(activity: Activity): Intent {
            val startIntent = Intent(activity, ContactsActivity::class.java)
            startIntent.putExtra(EXTRA_GROUP_EDITING, false)
            startIntent.putExtra(EXTRA_GROUP_STREAM_ID, NO_ID)
            startIntent.putExtra(EXTRA_SEARCH_BY_HANDLE, false)
            startIntent.putExtra(EXTRA_PRE_SELECTED_PARTICIPANTS, emptyArray<Long>())
            startIntent.putExtra(EXTRA_FROM_TALK_SCREEN, true)
            return startIntent
        }

        fun startEditGroup(activity: Activity, streamId: Long, participantsAccountIdList: ArrayList<Long>): Intent {
            val startIntent = Intent(activity, ContactsActivity::class.java)
            startIntent.putExtra(EXTRA_GROUP_EDITING, true)
            startIntent.putExtra(EXTRA_GROUP_STREAM_ID, streamId)
            startIntent.putExtra(EXTRA_SEARCH_BY_HANDLE, false)
            startIntent.putExtra(EXTRA_PRE_SELECTED_PARTICIPANTS, participantsAccountIdList.toLongArray())
            return startIntent
        }

        fun startSearchByHandle(activity: Activity, streamId: Long?): Intent {
            val startIntent = Intent(activity, ContactsActivity::class.java)
            val usingStream = streamId != null
            startIntent.putExtra(EXTRA_GROUP_EDITING, usingStream)
            startIntent.putExtra(EXTRA_GROUP_STREAM_ID, streamId ?: NO_ID)
            startIntent.putExtra(EXTRA_SEARCH_BY_HANDLE, true)
            startIntent.putExtra(EXTRA_PRE_SELECTED_PARTICIPANTS, emptyArray<Long>())
            return startIntent
        }
    }

    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }
    private val layoutManager: LinearLayoutManager by lazy(NONE) { LinearLayoutManager(this) }
    private var contactsAdapter: ContactPickerAdapter? = null
    private var pressedContact: DeviceContactInfo? = null

    // Extras
    private val isGroupEditing: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_GROUP_EDITING, false) }
    private val groupStreamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_GROUP_STREAM_ID, NO_ID) }
    private val searchingByHandle: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_SEARCH_BY_HANDLE, false) }
    private val groupPreSelectedParticipants: LongArray by lazy(NONE) { getPreSelectedContacts() }
    private val cameFromTalkScreen: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_FROM_TALK_SCREEN, false) }

    private var firstRun = true
    private var contactsPermissionListener: PermissionListener? = null

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_screen)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        if (!searchingByHandle) {
            if (hasContactsPermission() || firstRun) {
                firstRun = false
                initContactsLists()
                if (contactsAdapter == null) {
                    progressDialogHelper.showWaiting()
                }
            }
        } else {
            initContactsLists()
        }
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }

        progressDialogHelper.showWaiting()
        if (permissionName == Manifest.permission.READ_CONTACTS) {
            requestContacts(true)
        }
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        token?.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        val permissionName = response?.permissionName ?: ""
        val permanentlyDenied = response?.isPermanentlyDenied ?: true
        logDebug { "Denied permission: $permissionName, permanently: $permanentlyDenied" }
        loadContactList(mutableListOf())
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (v != null) {
            // hide keyboard when scrolling trough the list
            KeyboardUtils.hideKeyboard(this)
        }
        return false
    }

    override fun pressedRequestPermission() {
        if (!hasContactsPermission()) {
            // We need the actual contact permission first
            requestContactPermission()
            return
        }

        val currentAccount = UserAccountRepo.current() ?: return
        if (!currentAccount.hasContactInfo) {
            startActivity(AddContactInfoActivity.startFromContacts(this))
            return
        }

        // Actually everything seems in order, just display the contacts then
        requestContacts(true)
    }

    override fun selectionBegun() {
        rightTopButton.setTextColor(R.color.s_blue.colorResource())
        rightTopButton.isEnabled = true
        updateToolbarDisplay()
    }

    override fun selectionCleared() {
        rightTopButton.isEnabled = false
        rightTopButton.setTextColor(R.color.s_medium_grey.colorResource())
        updateToolbarDisplay()
    }

    override fun selectedContact(contact: DeviceContactInfo) {
        // hide keyboard when scrolling trough the list
        KeyboardUtils.hideKeyboard(this)

        if (searchingByHandle || cameFromTalkScreen) {
            // If on-boarding and NOT group editing, pick contact immediately
            startConversationPressed()
        }

        chipsView.addChip(contact)
    }

    override fun unSelectedContact(contact: DeviceContactInfo) {
        chipsView.removeChipBy(contact)
    }

    override fun linesChanged(numLines: Int) {
        val shorten = numLines < 2
        changeSearchAreaSize(shorten)
    }

    override fun onBackPressed() {
        handleGoBack()
    }


    //
    // PUBLIC METHODS
    //

    fun chipsEnterPressed() {
        contactsAdapter?.selectFirstContact()
    }

    //
    // PRIVATE METHODS
    //

    private fun getPreSelectedContacts(): LongArray {
        val possibleArray = intent.getLongArrayExtra(EXTRA_PRE_SELECTED_PARTICIPANTS) ?: return LongArray(0)
        return possibleArray
    }

    /**
     * Pressed start a conversation (or create group) button
     */
    private fun startConversationPressed() {
        if (cameFromTalkScreen) {
            creatingNewConversation()
            return
        }

        if (isGroupEditing) {
            editGroupAction()
            return
        }

        val selectedContacts = contactsAdapter?.getSelectedContacts() ?: return

        progressDialogHelper.showWaiting()

        // Update ghost identifier
        if (selectedContacts.size == 1) {
            pressedContact = selectedContacts.first()
        }

        // Update temporary invited contacts so we can use them in the app
        GlobalManager.invitedContacts = ArrayList<InviteContact>(selectedContacts.size)
        for (contact in selectedContacts) {
            // Only add non-active contacts
            if (!contact.activeOnRoger) {
                val possibleNumber = contact.aliases.first().value
                if (possibleNumber.startsWith("+")) {
                    GlobalManager.invitedContacts.add(InviteContact(contact.displayName, possibleNumber))
                }
            }
        }

        val listOfAliases = ArrayList<String>(selectedContacts.size)

        for (contact in selectedContacts) {
            listOfAliases.add(contact.aliases.first().value)
        }

        // Check if we are creating a group with un-active users
        CreateStreamRequest(listOfAliases, null, "", selectedContacts, showInRecents = true).enqueueRequest()

        // For now we wait for the reply, and dismiss this screen once we have a stream ID
    }

    private fun updateToolbarDisplay() {
        if (cameFromTalkScreen) {
            return
        }
        val selectedContactsSize = contactsAdapter?.getSelectedContacts()?.size ?: 0

        if (searchingByHandle || cameFromTalkScreen) {
            setToolbarText(R.string.contacts_search_title.stringResource())
            rightTopButton.beGone()
            // Don't go further
            return
        } else {
            if (isGroupEditing) {
                val totalPeople = selectedContactsSize + groupPreSelectedParticipants.size
                val groupEditTitle = if (totalPeople > 0) {
                    "${getString(R.string.contacts_title_group_edit)} ($totalPeople)"
                } else {
                    getString(R.string.contacts_title_group_edit)
                }

                setToolbarText(groupEditTitle)
                return
            }
        }

        // Group Conversation
        if (selectedContactsSize > 1) {
            val groupTitle = "${getString(R.string.contacts_title_group)} ($selectedContactsSize)"
            setToolbarText(groupTitle)
        } else {
            // Single Conversation
            setToolbarText(R.string.contacts_title_conversation.stringResource())
        }
    }

    /**
     * We're actually creating a new singular conversation
     */
    private fun creatingNewConversation() {
        logMethodCall()
        val selectedContacts = contactsAdapter?.getSelectedContacts()
        if (selectedContacts == null) {
            failedToAddNewConversation()
            return
        }

        val contactToAdd = selectedContacts.firstOrNull()
        if (contactToAdd == null) {
            failedToAddNewConversation()
            return
        }

        if (contactToAdd.activeOnRoger) {
            logDebug { "This contact is already active on Roger" }
            val rogerId = ContactMapRepo.getRogerIdForDeviceId(contactToAdd.internalId)
            if (rogerId == null) {
                failedToAddNewConversation()
                return
            }

            ImmediateConversationRequest(rogerId, contactToAdd.displayName,
                    listOf(contactToAdd)).enqueueRequest()

            progressDialogHelper.showWaiting()
        } else {
            logDebug { "This is a new NON active contact" }

            pressedContact = selectedContacts.first()
            val listOfAliases = ArrayList<String>(selectedContacts.size)
            for (contact in selectedContacts) {
                listOfAliases.add(contact.aliases.first().value)
            }

            // Check if we are creating a group with un-active users
            CreateStreamRequest(listOfAliases, null, "", selectedContacts, showInRecents = true).enqueueRequest()

            // For now we wait for the reply, and dismiss this screen once we have a stream ID
            progressDialogHelper.showWaiting()
        }
    }

    private fun massInviteSelectedContact(stream: Stream) {
        // Send mass invite if not yet on Roger
        val selectedContacts = contactsAdapter?.getSelectedContacts()
        selectedContacts?.let {
            val newParticipants = ArrayList(selectedContacts.filter { it.activeOnRoger == false })
            for (invitedParticipant in newParticipants) {
                logDebug { "Sending invite to ${invitedParticipant.displayName}" }
                val identifiersList = invitedParticipant.aliases.map { it.value }
                InviteIdentifierRequest(invitedParticipant.displayName, identifiersList, stream.inviteToken).enqueueRequest()

                // Track this invite
                EventTrackingManager.invitation(MASS_INVITE)
            }
        }
    }

    private fun failedToAddNewConversation() {
        progressDialogHelper.dismiss()
        // TODO: change this
        toast("Failed to add contact")
    }

    private fun editGroupAction() {
        logMethodCall()
        val selectedContacts = contactsAdapter?.getSelectedContacts() ?: return

        val listOfContacts = selectedContacts.map { it.aliases.first().value.toString() }

        // Update local invite manager
        val newParticipants = ArrayList(selectedContacts.filter { it.activeOnRoger == false })
        LocalInviteManager.newlyInvitedParticipants = newParticipants

        // Immediately add 'fake' participants to the stream
        for (participant in newParticipants) {
            logDebug { "Adding temporary account" }
            val temporaryAccount = Account.temporaryStreamParticipant(participant.displayName, participant.aliases.map { it.value })
            StreamCacheRepo.addParticipantToStream(groupStreamId, temporaryAccount)
        }

        // Add them to the group request
        // TODO: is this needed?
        StreamAddParticipantsRequest(groupStreamId, listOfContacts, selectedContacts).enqueueRequest()

        // Mass inviter requests
        val stream = StreamCacheRepo.getStream(groupStreamId)
        stream?.let {
            for (invitedParticipant in newParticipants) {
                val identifiersList = invitedParticipant.aliases.map { it.value }
                InviteIdentifierRequest(invitedParticipant.displayName, identifiersList, it.inviteToken).enqueueRequest()
            }
        }

        // Clear newly invited participants
        LocalInviteManager.newlyInvitedParticipants = ArrayList()

        // Display toast with 'done' confirmation
        doneToast()

        // Finish now, and let the remaining tasks finish on the background
        finish()
    }

    private fun hasContactInfo(): Boolean {
        val currentAccount = UserAccountRepo.current() ?: return false
        return currentAccount.hasContactInfo
    }

    private fun initContactsLists() {
        if (searchingByHandle) {
            loadContactList(mutableListOf())
            return
        }

        // If the user doesn't have details, start with an empty list
        if (!hasContactInfo()) {
            loadContactList(mutableListOf())
            return
        }

        // Request contacts immediately if permission is granted.
        if (hasContactsPermission()) {
            if (ContactMapRepo.isDeviceContactMapCached()) {
                // Device contacts are cached, use that
                handleDeviceContactMap(ContactMapRepo.getDeviceContactMap())
            } else {
                requestContacts(true)
            }
        } else {
            requestContactPermission()
        }
    }

    private fun requestContactPermission() {
        val dialogOnDeniedPermissionListener =
                DialogOnDeniedPermissionListener.Builder.withContext(this)
                        .withTitle(R.string.perm_read_contact_title)
                        .withMessage(R.string.perm_read_contact_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        contactsPermissionListener = CompositePermissionListener(this,
                dialogOnDeniedPermissionListener)

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(contactsPermissionListener, Manifest.permission.READ_CONTACTS)
        }
    }

    private fun setupUI() {
        contactList.layoutManager = layoutManager

        chipsView.addChipLineListener(this)

        setupToolbar()

        setupChips()

        // Different label depending on search type
        if (searchingByHandle) {
            chipsView.setHint(R.string.search_contact_menu_handle)

            // Define account explanation labels
            val currentAccount = UserAccountRepo.current()
            currentAccount?.let {
                displayNameExplanationLabel.text = R.string.search_display_name_explanation.stringResource(it.displayName)
                usernameExplanationLabel.text = R.string.search_username_explanation.stringResource("@${it.username}")
                changeAccountExplanationVisibility(true)
            }

        } else {
            chipsView.setHint(R.string.search_contact_menu_name)
        }

        // Start conversation button setup
        rightTopButton.setTextColor(R.color.s_medium_grey.colorResource())
        rightTopButton.setOnClickListener { startConversationPressed() }
        rightTopButton.isEnabled = false
    }

    private fun changeAccountExplanationVisibility(visible: Boolean) {
        if (visible) {
            displayNameExplanationLabel.makeVisible(true)
            usernameExplanationLabel.makeVisible(true)
        } else {
            displayNameExplanationLabel.beGone()
            usernameExplanationLabel.beGone()
        }
    }

    private fun handleGoBack() {
        finish()
    }

    private fun setupToolbar() {
        if (cameFromTalkScreen) {
            // Hide Toolbar
            toolbar.beGone()

            // Add padding to search bar
            val searchAreaParams = searchArea.layoutParams as RelativeLayout.LayoutParams
            searchAreaParams.setMargins(0, getStatusBarHeight(), 0, 0)
            searchArea.layoutParams = searchAreaParams
            return
        }

        setSupportActionBar(toolbar)
        toolbar.statusBarPadding(this)
        toolbar.title = ""
        supportActionBar?.title = ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener({
            handleGoBack()
        })

        // Refresh Toolbar display
        updateToolbarDisplay()
    }

    private fun setupChips() {
        chipsView.setChipsListener(object : ChipsView.ChipsListener {
            override fun onChipAdded(chip: ChipsView.Chip) {
            }

            override fun onChipDeleted(chip: ChipsView.Chip) {
                contactsAdapter?.unSelectedContact(chip.contact)
            }

            override fun onTextChanged(text: CharSequence) {
                contactsAdapter?.filterContacts(text.toString())
                // Query contact for picture
                if (searchingByHandle || cameFromTalkScreen) {
                    // Name explanation display
                    if (text.length > 0) {
                        changeAccountExplanationVisibility(false)
                    } else {
                        changeAccountExplanationVisibility(true)
                    }

                    if (text.length > 2) {
                        ProfileRequest(text.toString()).enqueueRequest()
                    }

                }
            }
        })

        // Set for enter key callback
        chipsView.contactsActivity = this
    }

    private fun requestContacts(updateActiveContactsAfter: Boolean) {
        DeviceContactQueryTask(updateActiveContactsAfter).execute()
    }

    private fun loadContactList(deviceContacts: MutableCollection<DeviceContactInfo>) {
        val displayContactPermissionElement = !hasContactsPermission() || !hasContactInfo()
        progressDialogHelper.dismiss()

        if (contactsAdapter == null) {
            contactsAdapter = ContactPickerAdapter(deviceContacts.toList(), this,
                    displayContactPermissionElement, searchingByHandle || cameFromTalkScreen)

            contactsAdapter?.setHasStableIds(true)
            contactList.adapter = contactsAdapter

            // Pass pre-selected participants if group editing
            if (isGroupEditing) {
                contactsAdapter?.updatePreSelectedContacts(groupPreSelectedParticipants)
            }

            contactList.setOnTouchListener(this)
        } else {
            // Don't even bother processing anything related to contacts if only searching by handle
            if (searchingByHandle) {
                return
            }
            contactsAdapter?.updateContacts(deviceContacts.toList(), displayContactPermissionElement)
        }

    }

    private fun handleDeviceContactMap(deviceContacts: HashMap<Long, DeviceContactInfo>) {
        if (!hasContactInfo()) {
            logVerbose { "no contact info yet" }
            return
        }

        val explodedContacts = ContactsRepo.explodeContacts(deviceContacts.values)

        loadContactList(explodedContacts)
    }

    private fun changeSearchAreaSize(collapse: Boolean) {
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        if (collapse) {
            params.height = resources.getDimension(R.dimen.contacts_search_collapsed_size).toInt()
        } else {
            params.height = resources.getDimension(R.dimen.contacts_search_expanded_size).toInt()
        }
        params.below(toolbar)
        searchArea.layoutParams = params
    }

    private fun matchRemoteWithDeviceContacts() {
        logMethodCall()
        val contactsToMap = contactsAdapter?.getSelectedContacts()
        if (contactsToMap == null) {
            logWarn { "Contacts to map were null, simply exiting the screen as the user probably intended" }
            finish()
            return
        }

        // Try to match the selected contacts
        ActiveContactsRequest(ArrayList(contactsToMap)).enqueueRequest()
    }

    //
    // EVENT METHODS
    //

    /**
     * This event gets called after searching for a username
     */
    @Subscribe(threadMode = MAIN)
    fun onProfileSuccess(event: ProfileSuccessEvent) {
        logEvent(event)
        contactsAdapter?.updateSearchProfile(event.profile)
    }

    @Subscribe(threadMode = MAIN)
    fun onProfileFailure(event: ProfileFailEvent) {
        logEvent(event)
        contactsAdapter?.updateSearchProfile(null)
    }

    @Subscribe(threadMode = MAIN)
    fun onGroupEditFail(event: StreamAddParticipantsFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        CommonDialog.simpleMessage(this,
                getString(R.string.error_generic_title),
                getString(R.string.create_stream_error_message))
    }

    @Subscribe(threadMode = MAIN)
    fun onActiveContactsUpdated(event: ActiveContactUpdatedEvent) {
        logEvent(event)
        if (hasContactsPermission()) {
            requestContacts(false)
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onDeviceContacts(event: DeviceContactsResultEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        handleDeviceContactMap(event.deviceContacts)
    }

    @Subscribe(threadMode = MAIN)
    fun onCreateStreamFail(event: CreateStreamFailEvent) {
        // Failed to get stream, present failure message
        logEvent(event)
        progressDialogHelper.dismiss()
        CommonDialog.simpleMessage(this,
                getString(R.string.error_generic_title),
                getString(R.string.create_stream_error_message))
    }

    /**
     * We issued create a new stream with success, that means we have everything and our job here is
     * done.
     */
    @Subscribe(threadMode = MAIN)
    fun onCreateStreamsSuccess(event: CreateStreamSuccessEvent) {
        logEvent(event)

        val stream = event.stream
        // Check if this contact already exists on the stream
        if (StreamCacheRepo.containsStreamId(stream.id)) {
            logDebug { "newly created stream was already part of stream cache" }
            StreamCacheRepo.temporaryStream = null

            // Update internal stream
            StreamCacheRepo.updateStreamInStreams(stream)
        } else {
            // Check if this stream timestamp is older than last element in cached streams
            val lastStreamTimestamp = StreamCacheRepo.getCached().lastOrNull()?.lastInteraction
            if (lastStreamTimestamp == null) {
                StreamCacheRepo.temporaryStream = event.stream
            } else {
                // Use temporary stream in either case, if it becomes available trough request it
                // will get automatically dismissed but this way we see the stream immediately
                StreamCacheRepo.temporaryStream = event.stream
            }
        }

        // Update Roger to Device Contact mapping
        if (event.stream.othersOrEmpty.size == 1) {
            // Map single contact
            val temporaryPressedContact = pressedContact
            if (temporaryPressedContact != null) {
                val participant = event.stream.othersOrEmpty.first()
                ContactMapRepo.updateRogerToDeviceContactMap(participant.id, temporaryPressedContact.internalId)
            }
        }

        // Make this the new selected stream
        StreamManager.selectedStreamId = stream.id

        matchRemoteWithDeviceContacts()

        // Further handling if creating new conversation from TalkScreen
        if (cameFromTalkScreen) {
            progressDialogHelper.dismiss()
            massInviteSelectedContact(stream)

            finish()
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onImmediateConversationSuccess(event: ImmediateConvoSuccessEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()

        // Select this conversation as the primary one
        startActivity(TalkActivityUtils.startWithStream(this, event.stream.id))
        finish()
    }

    @Subscribe(threadMode = MAIN)
    fun onImmediateConversationFailure(event: ImmediateConvoFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        requestFailureToast()
    }
}