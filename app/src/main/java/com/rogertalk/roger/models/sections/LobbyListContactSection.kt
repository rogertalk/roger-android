package com.rogertalk.roger.models.sections

import com.rogertalk.roger.models.json.Account

class LobbyListContactSection(elementsType: Int, var members: List<Account>, val active: Boolean) :
        LobbyListSection("", elementsType) {

    override fun getSectionSize(): Int {
        if (shouldRender()) {
            return super.getSectionSize() + members.size
        }
        return 0
    }

    override fun getElementForPosition(givenPosition: Int): Any {
        // Shift 1 position on account of the section
        return members[givenPosition - 1]
    }

    override fun shouldRender(): Boolean {
        return members.isNotEmpty()
    }
}
