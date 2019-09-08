package com.rogertalk.roger.models.sections


class LobbyListInviterSection() : LobbyListSection("", LobbySectionType.TYPE_INVITER.ordinal) {

    override fun getSectionSize(): Int {
        if (shouldRender()) {
            return 1
        }
        return 0
    }

    override fun shouldRender(): Boolean {
        return true
    }

    override fun shouldRenderSectionHeader(): Boolean {
        return false
    }
}