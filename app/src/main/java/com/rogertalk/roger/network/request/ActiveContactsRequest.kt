package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.NumberToActiveContactMapContainer
import com.rogertalk.roger.repo.ActiveContactsRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logWarn
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.util.*

class ActiveContactsRequest(val contactList: List<DeviceContactInfo>) : BaseRequest() {

    private fun getRequestBody(): String {
        val str = StringBuilder(contactList.size * 20)
        for (contact in contactList) {
            for (phone in contact.aliases) {
                str.append('\n')
                str.append(phone.value)
            }
        }
        return str.toString()
    }

    override fun enqueueRequest() {
        if (!PrefRepo.loggedIn) {
            logWarn { "Not logged in, don't send contacts request" }
            return
        }
        if (!PrefRepo.permissionToMatchContacts) {
            logWarn { "We don't have the permission to match contacts" }
            return
        }

        val callback = getCallback(NumberToActiveContactMapContainer::class.java)
        val requestBody = RequestBody.create(MediaType.parse("text/plain"), getRequestBody())
        getRogerAPI().activeContacts(requestBody).enqueue(callback)
    }

    /**
     * Synchronous request execution
     */
    fun executeRequest(): Response<NumberToActiveContactMapContainer>? {
        if (!PrefRepo.permissionToMatchContacts) {
            logWarn { "We don't have the permission to match contacts" }
            return null
        }

        val requestBody = RequestBody.create(MediaType.parse("text/plain"), getRequestBody())
        return getRogerAPI().activeContacts(requestBody).execute()
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