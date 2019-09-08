package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.NumberToActiveContactMapContainer
import com.rogertalk.roger.repo.ActiveContactsRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread
import java.util.*

class InviteIdentifierRequest(val name: String, val identifiers: List<String>, val inviteToken: String?) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(NumberToActiveContactMapContainer::class.java)

        // Make a list with same amount of identifiers
        val names = identifiers.map { name }

        getRogerAPI().inviteViaSMS(identifiers, names, inviteToken).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val activeContactsMap = t as? NumberToActiveContactMapContainer? ?: return

        runOnUiThread {
            // Match and persist active contacts
            val contactsMap = HashMap(activeContactsMap.activeContactsMap)
            ActiveContactsRepo.matchActiveContactsWithContacts(contactsMap)
        }
    }
}