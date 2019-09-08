package com.rogertalk.roger.ui.dialog.listeners

import com.rogertalk.roger.models.json.Stream

interface ContactOptionsListener {

    fun setName(stream: Stream)
    fun members(stream: Stream)
    fun talkHead(stream: Stream)
    fun muteConversation(stream: Stream)
    fun unMuteConversation(stream: Stream)
    fun leaveGroup(stream: Stream)
}
