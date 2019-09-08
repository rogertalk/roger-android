package com.rogertalk.roger.helper

import com.rogertalk.roger.android.notification.NotificationMuteManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.ClearTextPrefRepo
import com.rogertalk.roger.ui.dialog.ConversationDialogs
import com.rogertalk.roger.ui.dialog.listeners.ContactOptionsListener
import com.rogertalk.roger.ui.screens.TalkActivity

/**
 * This manager handles all the options the user can perform in a conversation.
 * (Set name, photo, view group participants, etc)
 */
class ConversationOptionsHelper(val talkScreen: TalkActivity) :
        ContactOptionsListener {

    //
    // OVERRIDE METHODS
    //

    override fun talkHead(stream: Stream) {
        ClearTextPrefRepo.dismissedTalkHeads = false
        talkScreen.finish()
    }

    override fun setName(stream: Stream) {
        ConversationDialogs.changeStreamTitleDialog(talkScreen, stream)
    }

    override fun members(stream: Stream) {
        talkScreen.editGroupPressed()
    }

    override fun muteConversation(stream: Stream) {
        ConversationDialogs.pickMuteDuration(talkScreen, stream)
    }

    override fun unMuteConversation(stream: Stream) {
        NotificationMuteManager.unMuteStream(stream)
    }

    override fun leaveGroup(stream: Stream) {
        ConversationDialogs.confirmLeaveStream(talkScreen, stream)
    }

}
