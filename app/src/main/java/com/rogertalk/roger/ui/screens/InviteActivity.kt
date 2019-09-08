package com.rogertalk.roger.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rogertalk.kotlinjubatus.LONG_ANIM_DURATION
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.fadeIn
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.LocalInviteManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.screens.base.BaseAppCompatActivity
import com.rogertalk.roger.utils.android.ShareUtils
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.phone.PhoneUtils
import kotlinx.android.synthetic.main.invite_after_talking_screen.*
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class InviteActivity : BaseAppCompatActivity() {

    companion object {

        private val EXTRA_TOKEN = "token"
        private val EXTRA_STREAM_ID = "streamId"
        private val EXTRA_PARTICIPANT_NAME = "participantName"
        private val EXTRA_PHONE_LIST = "phoneList"
        private val EXTRA_OPEN_GROUP = "openGroup"

        fun start(context: Context, token: String, participantName: String,
                  phoneList: ArrayList<String>, streamId: Long, isOpenGroup: Boolean): Intent {
            val startIntent = Intent(context, InviteActivity::class.java)
            startIntent.putExtra(EXTRA_TOKEN, token)
            startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            startIntent.putExtra(EXTRA_PARTICIPANT_NAME, participantName)
            startIntent.putExtra(EXTRA_PHONE_LIST, phoneList)
            startIntent.putExtra(EXTRA_OPEN_GROUP, isOpenGroup)
            return startIntent
        }

        fun shareViaOther(context: Context, token: String, isOpenGroup: Boolean,
                          groupName: String? = null) {
            val userAccount = UserAccountRepo.current() ?: return

            EventTrackingManager.invitation(EventTrackingManager.InvitationMethod.ONE_ON_ONE_OTHER)
            val message: String
            val link = " ${ShareUtils.getCompleteShareUrl(userAccount, token, isOpenGroup)}"
            if (isOpenGroup) {
                val group = groupName ?: ""
                message = R.string.invitation_send_email_chooser.stringResource(group) + link

            } else {
                message = R.string.invitation_message.stringResource() + link
            }

            ShareUtils.shareAudioLink(message, context)
        }
    }

    // Extras
    private val token: String by lazy(NONE) { intent.getStringExtra(EXTRA_TOKEN) }
    private val streamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_STREAM_ID, NO_ID) }
    private val participantName: String by lazy(NONE) { intent.getStringExtra(EXTRA_PARTICIPANT_NAME) }
    private val phoneList: ArrayList<String> by lazy(NONE) { intent.getStringArrayListExtra(EXTRA_PHONE_LIST) }
    private val isOpenGroup: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_OPEN_GROUP, false) }

    private val stream: Stream?
        get() {
            return StreamCacheRepo.getStream(streamId)
        }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.invite_after_talking_screen)
        setupUI()

        // Clear memory
        LocalInviteManager.newlyInvitedParticipants = ArrayList()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFinishAfterTransition()
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun setupUI() {
        // Load Avatar
        val stream = StreamCacheRepo.getStream(streamId)
        stream?.let {
            val photo = stream.imageURL
            if (photo != null) {
                RoundImageUtils.createRoundImageMainAvatar(this, contactPhoto,
                        photo)
            }
        }

        logDebug { "Phone numbers list: $phoneList" }

        // Deal with each invite flow case separately
        if (isOpenGroup) {
            setupUIForOpenGroup()
        } else {
            setupUIForNormalInvite()
        }

        // Make other components appear
        outerCircle.fadeIn(LONG_ANIM_DURATION) {
            startConversationDescription.fadeIn()
        }
    }

    private fun setupUIForOpenGroup() {
        val groupName = stream?.title ?: return
        startConversationDescription.text = R.string.invite_open_group_main.stringResource(groupName)

        // For open groups, always share via multiple options
        otherSharingOptionsButton.beGone()
        sendButton.setOnClickListener { shareViaOther(true) }
    }

    private fun setupUIForNormalInvite() {
        startConversationDescription.text = R.string.invite_start_conversation_description.stringResource(participantName)
        val canSendSms = PhoneUtils.canSendSms()
        val isGroup = phoneList.size > 1

        if (canSendSms) {
            sendButton.setText(R.string.invite_send_via_sms_button_label)
            sendButton.setOnClickListener { shareViaSMS() }
            if (isGroup) {
                otherSharingOptionsButton.beGone()
            } else {
                otherSharingOptionsButton.setOnClickListener { shareViaOther(false) }
            }
        } else {
            // This device cannot send SMS!
            otherSharingOptionsButton.beGone()
            sendButton.setOnClickListener { shareViaOther(false) }
        }
    }

    private fun shareViaSMS() {
        val userAccount = UserAccountRepo.current() ?: return
        val message = R.string.invitation_message.stringResource() +
                " ${ShareUtils.getCompleteShareUrl(userAccount, token, isOpenGroup = false)}"
        EventTrackingManager.invitation(EventTrackingManager.InvitationMethod.ONE_ON_ONE_SMS)
        ShareUtils.shareViaSMS(message, phoneList, this)

        finish()
    }

    private fun shareViaOther(isOpenGroup: Boolean) {
        shareViaOther(this, token, isOpenGroup, participantName)
        finish()
    }

}