package com.rogertalk.roger.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.afollestad.materialdialogs.AlertDialogWrapper
import com.rogertalk.roger.R
import com.rogertalk.roger.android.tasks.DeviceContactQueryTask
import com.rogertalk.roger.event.broadcasts.DeviceContactsResultEvent
import com.rogertalk.roger.event.failure.DisplayNameUpdateFailEvent
import com.rogertalk.roger.event.success.DisplayNameUpdateSuccessEvent
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.holder.ImagePickHolder
import com.rogertalk.roger.models.holder.OnBoardingDataHolder
import com.rogertalk.roger.network.request.CreateStreamRequest
import com.rogertalk.roger.network.request.JoinGroupRequestV2
import com.rogertalk.roger.network.request.UpdateDisplayNameRequest
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.screens.base.NameChangeBase
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.contact.UserAccountsHelper
import com.rogertalk.roger.utils.extensions.hasDeviceAccountPermission
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logEvent
import kotlinx.android.synthetic.main.name_query_screen.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.toast
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class NameSetupActivity() : NameChangeBase() {

    companion object {
        // Use this extra if you want TalkScreen to be explicitly started after name change.
        // Otherwise, previous screen will be resumed as this one finishes.
        private val EXTRA_IS_ON_BOARDING = "isOnBoarding"

        fun startOnBoarding(activity: Activity): Intent {
            val startIntent = Intent(activity, NameSetupActivity::class.java)
            startIntent.putExtra(EXTRA_IS_ON_BOARDING, true)
            return startIntent
        }
    }

    // Init flags
    private val isOnBoarding: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_IS_ON_BOARDING, false) }


    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for device contacts in advance
        DeviceContactQueryTask().execute()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        loadPictureFromMemory()
        refreshUI()
    }

    override fun onBackPressed() {
        // Ignore back key press
    }

    override fun confirmNamePressed() {
        super.confirmNamePressed()
        if (setPhotoOnce) {
            makeRequest()
        } else {
            displaySetPictureDialog()
        }
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun makeRequest() {
        val textName = nameInput.text.toString()
        setLoading(true)
        UpdateDisplayNameRequest(textName.capitalize()).enqueueRequest()
    }

    private fun displaySetPictureDialog() {
        val builder = AlertDialogWrapper.Builder(this)
        builder.setTitle(title)
        builder.setMessage(R.string.name_change_dialog_text)
        builder.autoDismiss(true)
        builder.setPositiveButton(android.R.string.ok,
                { dialogInterface, i ->
                    photoUiHelper.choosePhotoSourcePressed()
                })
        builder.setNegativeButton(R.string.name_change_dialog_later,
                { dialogInterface, i ->
                    makeRequest()
                })
        builder.show()
    }

    private fun clearInstantSignUpReferrer() {
        PrefRepo.referrerInfo = null
    }

    /**
     * Load avatar picture from memory so it gets displayed immediately
     */
    private fun loadPictureFromMemory() {
        val image = ImagePickHolder.croppedImage ?: return
        RoundImageUtils.createRoundImage(this, userPhoto, image,
                AvatarSize.CONTACT)
    }

    private fun refreshUI() {
        // Refresh avatar
        val profileImage = UserAccountRepo.current()?.imageURL
        profileImage?.let {
            RoundImageUtils.createRoundImage(this, userPhoto, profileImage,
                    AvatarSize.CONTACT)
        }
    }

    private fun setupUI() {
        handleInfoPreFill()
    }

    private fun handleInfoPreFill() {
        if (isOnBoarding) {
            if (hasDeviceAccountPermission()) {
                if (!preFillDisplayNameInferred()) {
                    preFillNameWithAccount()
                }
            } else {
                preFillNameWithAccount()
            }
            return
        }

        preFillNameWithAccount()
    }

    /**
     * @return True if succeeded, False otherwise
     */
    private fun preFillNameWithAccount(): Boolean {
        if (nameInput.text.isNotBlank()) {
            return false
        }

        val currentAccount = UserAccountRepo.current() ?: return false

        if (!(currentAccount.displayNameSet ?: false)) {
            logDebug { "Display name has not been set yet, don't pre-fill with account info" }
            return false
        }

        val preExistingName = currentAccount.customDisplayName ?: ""

        nameInput.setText(preExistingName)
        nameInput.setSelection(preExistingName.length)
        return preExistingName.isNotBlank()
    }

    private fun goToTalkScreen() {
        // Update flags that control on-boarding state
        val showConversationsOverlay = UserAccountRepo.isBrandNewUser
        PrefRepo.didTapToTalk = !showConversationsOverlay
        PrefRepo.didTapManagerConversation = !showConversationsOverlay
        PrefRepo.didTapToListen = !showConversationsOverlay
        PrefRepo.pendingPrimer = showConversationsOverlay

        // Should display placeholders
        PrefRepo.showAddFamily = showConversationsOverlay
        PrefRepo.showAddFriends = showConversationsOverlay
        PrefRepo.showAddTeam = showConversationsOverlay

        startActivity(TalkActivityUtils.getStartTalkScreen(this, justOnBoarded = true))
        finish()
    }

    /**
     * @return True if succeed, False otherwise
     */
    private fun preFillDisplayNameInferred(): Boolean {
        val deviceContacts = ContactMapRepo.getDeviceContactMap().values
        // Try to auto-fill the name of the user
        val displayName = UserAccountsHelper.getDisplayName(this, deviceContacts.toList())
        if (displayName.isNotBlank()) {
            // Only auto-fill if user hasn't filled something yet
            if (nameInput.text.isBlank()) {
                nameInput.setText(displayName)
                nameInput.setSelection(displayName.length)
                return true
            }
        }
        return false
    }

    private fun joinInvitedConversations() {
        // Redeem invitation (group or user)
        val inviteToken = OnBoardingDataHolder.possibleInviteToken
        if (inviteToken != null) {
            JoinGroupRequestV2(inviteToken).enqueueRequest()
        } else {
            val participant = OnBoardingDataHolder.possibleParticipant
            if (participant != null) {
                logDebug { "Going to add person to user's streams: $participant" }
                val participantList = ArrayList<String>(1)
                participantList.add(participant)
                CreateStreamRequest(participantList, null, "", null, true,
                        updateImmediately = true).enqueueRequest()
            }
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onDeviceContacts(event: DeviceContactsResultEvent) {
        logEvent(event)
        preFillDisplayNameInferred()
    }

    @Subscribe(threadMode = MAIN)
    fun onDisplayNameUpdated(event: DisplayNameUpdateSuccessEvent) {
        logEvent(event)

        clearInstantSignUpReferrer()

        // Update account info
        UserAccountRepo.updateAccount(event.account)

        // Join conversations if user has been invited to one
        joinInvitedConversations()

        goToTalkScreen()
    }

    @Subscribe(threadMode = MAIN)
    fun onDisplayNameFail(event: DisplayNameUpdateFailEvent) {
        logEvent(event)
        setLoading(false)
        toast(R.string.error_failed_to_update_name)
    }
}
