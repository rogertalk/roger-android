package com.rogertalk.roger.ui.dialog

import android.content.Context
import android.text.InputType
import com.afollestad.materialdialogs.AlertDialogWrapper
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.android.notification.NotificationMuteManager
import com.rogertalk.roger.models.data.ConversationOption
import com.rogertalk.roger.models.data.ConversationOption.*
import com.rogertalk.roger.models.data.MuteDuration.HOURS_8
import com.rogertalk.roger.models.data.MuteDuration.WEEK_1
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.LeaveStreamRequest
import com.rogertalk.roger.network.request.StreamUpdateTitleRequest
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.dialog.listeners.ContactOptionsListener
import com.rogertalk.roger.ui.dialog.listeners.LeaveStreamListener
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.misc.TimeUtils
import java.util.*

object ConversationDialogs {

    private val OPTION_8_HOURS = 0
    private val OPTION_1_WEEK = 1


    /**
     * Options to display when long-pressing conversations.
     */
    fun contactLongPressOptions(context: Context, listener: ContactOptionsListener, stream: Stream) {
        val emojiTitle = if (NotificationMuteManager.isStreamMuted(stream.id)) {
            EmojiUtils.mute
        } else {
            EmojiUtils.speechBubble
        }

        val timeSpoken = "$emojiTitle ${TimeUtils.getDurationLong(stream.totalDuration)}"
        val optionsArray = ArrayList<String>(3)
        val displayedOptions = ArrayList<ConversationOption>(3)

        optionsArray.add(R.string.contact_options_set_name_title.stringResource(context = context))
        displayedOptions.add(SET_NAME)

        // Group options
        if (stream.isGroup) {
            optionsArray.add(R.string.contact_options_members.stringResource(context = context))
            displayedOptions.add(MEMBERS)
        }

        if (!AccessibilityUtils.isScreenReaderActive(appController()) && !stream.isEmptyConversation) {
            optionsArray.add(R.string.contact_options_start_talkhead.stringResource(context = context))
            displayedOptions.add(TALKHEAD)
        }

        // Negative-actions go into the 'more' dialog
        optionsArray.add(R.string.contact_options_more.stringResource(context = context))
        displayedOptions.add(MORE)

        MaterialDialog.Builder(context)
                .title(timeSpoken)
                .items(optionsArray)
                .itemsCallback { materialDialog, view, position, charSequence ->

                    when (displayedOptions[position]) {
                        SET_NAME -> listener.setName(stream)
                        MEMBERS -> listener.members(stream)
                        TALKHEAD -> listener.talkHead(stream)
                        MORE -> moreConversationOptions(context, listener, stream)
                    }
                }
                .show()
    }



    /**
     * 'More' set of options to display when long-pressing conversations
     */
    fun moreConversationOptions(context: Context, listener: ContactOptionsListener, stream: Stream) {
        val optionsArray = ArrayList<String>(3)
        val displayedOptions = ArrayList<ConversationOption>(3)

        // Option to leave group
        optionsArray.add(R.string.contact_options_leave_group.stringResource(context = context))
        displayedOptions.add(LEAVE_GROUP)

        // Option to mute conversation
        val isMuted = NotificationMuteManager.isStreamMuted(stream.id)
        if (isMuted) {
            optionsArray.add(R.string.contact_options_unmute_group.stringResource(context = context))
            displayedOptions.add(UNMUTE_CONVERSATION)
        } else {
            optionsArray.add(R.string.contact_options_mute_group.stringResource(context = context))
            displayedOptions.add(MUTE_CONVERSATION)
        }

        MaterialDialog.Builder(context)
                .items(optionsArray)
                .itemsCallback { materialDialog, view, position, charSequence ->

                    when (displayedOptions[position]) {
                        UNMUTE_CONVERSATION -> listener.unMuteConversation(stream)
                        MUTE_CONVERSATION -> listener.muteConversation(stream)
                        LEAVE_GROUP -> listener.leaveGroup(stream)
                    }
                }
                .show()
    }

    /**
     * Ask about title change for a Stream
     */
    fun changeStreamTitleDialog(context: Context, stream: Stream) {
        val currentGroupName = stream.customTitle
        val builder = MaterialDialog.Builder(context)
                .title(context.getString(R.string.change_stream_title_dialog_title))
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
                .input(context.getString(R.string.change_stream_name_hint), currentGroupName,
                        { materialDialog, charSequence ->
                            if (charSequence.toString().isNotBlank()) {
                                StreamUpdateTitleRequest(stream.id, charSequence.toString()).enqueueRequest()
                            }
                            materialDialog.dismiss()
                        })
        builder.show()
    }

    /**
     * Confirmation dialog regarding leaving a group
     */
    fun confirmLeaveStream(context: Context, stream: Stream, leaveStreamListener: LeaveStreamListener? = null) {
        val groupName = stream.title
        val title = R.string.contact_options_leave_group_title.stringResource(groupName)

        val builder = AlertDialogWrapper.Builder(context)
                .setTitle(title)
                .autoDismiss(true)
                .setPositiveButton(android.R.string.yes,
                        { dialogInterface, i ->
                            LeaveStreamRequest(stream.id).enqueueRequest()

                            // Leave group immediately, if request fails, it will just resurface on it's own
                            StreamCacheRepo.removeStream(stream.id)

                            // Notify listener if one was provided
                            leaveStreamListener?.let(LeaveStreamListener::leftStream)
                        })
                .setNegativeButton(android.R.string.no,
                        { dialogInterface, i -> })

        builder.show()
    }

    /**
     * Choose mute duration
     */
    fun pickMuteDuration(context: Context, stream: Stream) {
        val optionsArray = ArrayList<String>(2)

        optionsArray.add(R.string.contact_options_mute_duration_8_hours.stringResource(context = context))
        optionsArray.add(R.string.contact_options_mute_duration_week.stringResource(context = context))

        MaterialDialog.Builder(context)
                .title(R.string.contact_options_pick_mute_duration_title)
                .items(optionsArray)
                .itemsCallback { materialDialog, view, position, charSequence ->
                    when (position) {
                        OPTION_8_HOURS -> {
                            NotificationMuteManager.muteStreamForDuration(stream, HOURS_8)
                        }

                        OPTION_1_WEEK -> {
                            NotificationMuteManager.muteStreamForDuration(stream, WEEK_1)
                        }
                    }
                }
                .show()
    }
}