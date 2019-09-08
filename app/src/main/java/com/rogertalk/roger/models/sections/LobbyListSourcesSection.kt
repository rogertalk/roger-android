package com.rogertalk.roger.models.sections

class LobbyListSourcesSection(sectionName: String,
                              val contactsSourceList: List<ContactsSource>) :
        LobbyListSection(sectionName, LobbySectionType.TYPE_CONTACT_SOURCE.ordinal) {

    enum class ContactsSource {
        ADDRESS_BOOK,
        HANDLE,
        GROUP_SHARE_LINK,
        BOTS
    }

    override fun getSectionSize(): Int {
        return super.getSectionSize() + contactsSourceList.size
    }

    override fun getElementForPosition(givenPosition: Int): Any {
        // Shift 1 position on account of the section
        return contactsSourceList[givenPosition - 1]
    }
}
