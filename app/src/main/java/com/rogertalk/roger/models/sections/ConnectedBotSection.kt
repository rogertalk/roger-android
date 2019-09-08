package com.rogertalk.roger.models.sections

import com.rogertalk.roger.models.json.Bot

class ConnectedBotSection(var botList: List<Bot>) :
        LobbyListSection("", LobbySectionType.TYPE_CONNECTED_BOT.ordinal) {

    override fun getSectionSize(): Int {
        return super.getSectionSize() + botList.size
    }

    override fun getElementForPosition(givenPosition: Int): Any {
        // Shift 1 position on account of the section
        return botList[givenPosition - 1]
    }
}
