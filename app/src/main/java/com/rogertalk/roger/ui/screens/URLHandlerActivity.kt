package com.rogertalk.roger.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.gson.Gson
import com.rogertalk.roger.R
import com.rogertalk.roger.event.failure.CreateStreamFailEvent
import com.rogertalk.roger.event.success.CreateStreamSuccessEvent
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.models.data.ProfileFromURI
import com.rogertalk.roger.network.request.CreateStreamRequest
import com.rogertalk.roger.network.request.JoinGroupRequestV1
import com.rogertalk.roger.network.request.JoinGroupRequestV2
import com.rogertalk.roger.repo.ContactsRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.screens.base.EventActivity
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.toast
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

/**
 * This screen handles URI of type __rogertalk://__
 */
class URLHandlerActivity : EventActivity() {

    companion object {

        private val EXTRA_HANDLE = "extra_handle"

        /**
         * Re-use this screen and the logic inside it for adding new users from handle (username)
         */
        fun startConversationInternal(context: Context, userHandle: String): Intent {
            val startIntent = Intent(context, URLHandlerActivity::class.java)
            startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startIntent.putExtra(EXTRA_HANDLE, userHandle)
            return startIntent
        }
    }

    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logMethodCall()

        // is the current user logged in?
        if (!SessionRepo.loggedIn()) {
            startActivity(LandingActivity.start(this))
            finish()
            return
        }

        setContentView(R.layout.url_handler_screen)

        if (intent.hasExtra(EXTRA_HANDLE)) {
            handleUserFromHandle(intent.getStringExtra(EXTRA_HANDLE) ?: "")
            return
        } else {
            val data = intent.data
            handleURI(data)
        }
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun handleURI(data: Uri) {
        logDebug { "Data: $data." }

        when (data.authority) {
            "v2" -> parseUriV2(data)
            "v3" -> parseUriV3(data)
            else -> {
                logError { "Unrecognised URI version" }

                // Still, try to use v3 since it's the latest
                parseUriV3(data)
            }
        }
    }

    private fun parseUriV2(data: Uri) {
        logInfo { "Parsing as V2 URI formatForDisplay" }
        val inviteToken = data.getQueryParameters("invite_token")
        if (isParamValid(inviteToken)) {
            handleInviteToken(inviteToken.first())
            return
        }

        val userId = data.getQueryParameters("id")

        if (!isParamValid(userId)) {
            toast("Could not handle user id.")
            finish()
            return
        }

        handleUserSelected(userId[0].toLong())
    }

    private fun parseUriV3(data: Uri) {
        logInfo { "Parsing as V3 URI formatForDisplay" }
        val inviteToken = data.getQueryParameters("invite_token")
        if (isParamValid(inviteToken)) {
            handleInviteToken(inviteToken.first())
            return
        }

        val profileRaw = data.getQueryParameters("profile")

        if (!isParamValid(profileRaw)) {
            toast("Could not obtain raw profile")
            finish()
            return
        } else {
            handleV3Profile(data)
        }
    }

    private fun handleInviteToken(inviteToken: String) {
        logDebug { "InviteToken: $inviteToken" }
        JoinGroupRequestV2(inviteToken).enqueueRequest()
    }

    private fun handleV3Profile(data: Uri) {
        val profileRaw = data.getQueryParameters("profile")
        logDebug { "Profile: $profileRaw" }
        try {
            val profile = Gson().fromJson(profileRaw.first(), ProfileFromURI::class.java)

            // Try to join an open group
            if (profile.chunkToken != null) {
                JoinGroupRequestV1(profile.chunkToken, profile.id.toString()).enqueueRequest()
            }

            // Add user as well
            handleUserSelected(profile.id)

        } catch(e: Exception) {
            logError { "Error converting profile" }
            finish()
        }
    }

    private fun isParamValid(param: MutableList<String>?): Boolean {
        if (param == null || param.isEmpty()) {
            return false
        }
        return true
    }

    private fun handleUserFromHandle(handle: String) {
        // create new stream with this user
        progressDialogHelper.showWaiting()
        val contactList = ArrayList<String>(1)
        contactList.add(handle)
        CreateStreamRequest(contactList, null, "", null, true).enqueueRequest()
    }

    private fun handleUserSelected(userId: Long) {
        val possibleStream = ContactsRepo.streamWithUser(userId)
        if (possibleStream != null) {
            logDebug { "Stream with user already exists" }
            // We already have this user in streams

            // Pre-select this stream
            StreamManager.selectedStreamId = possibleStream.id
            startActivity(TalkActivityUtils.getStartTalkScreen(this))
            return
        }

        // Create new stream with this user
        logDebug { "This is a new user" }
        progressDialogHelper.showWaiting()
        CreateStreamRequest(null, userId, "", null, true).enqueueRequest()
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onCreateStreamFail(event: CreateStreamFailEvent) {
        // Failed to get stream, present failure message
        logEvent(event)
        progressDialogHelper.dismiss()
        CommonDialog.simpleMessageWithButton(this,
                R.string.error_generic_title.stringResource(),
                R.string.check_internet_connection_message.stringResource(),
                android.R.string.ok.stringResource(),
                { finish() })
    }

    /**
     * We issued create a new stream with success, that means we have everything and our job here is
     * done.
     */
    @Subscribe(threadMode = MAIN)
    fun onCreateStreamSuccess(event: CreateStreamSuccessEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()

        val stream = event.stream

        // Add to streams
        StreamCacheRepo.updateStreamInStreams(stream)

        // Pre-select this stream
        StreamManager.selectedStreamId = stream.id
        startActivity(TalkActivityUtils.getStartTalkScreen(this))
        finish()
    }

}