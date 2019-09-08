package com.rogertalk.roger.ui.adapters.listener

interface ConversationsListener {
    fun conversationsItemPressed(streamId: Long)
    fun conversationsItemLongPressed(streamId: Long)
    fun pressedAddContacts()
}
