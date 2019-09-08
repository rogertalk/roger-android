package com.rogertalk.roger.android.tasks

import android.os.AsyncTask
import com.rogertalk.roger.event.broadcasts.DeviceContactsResultEvent
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.data.PersistedContacts
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.TaskStateRepo
import com.rogertalk.roger.utils.contact.ContactManager
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.state.TaskStates
import java.util.*


class DeviceContactQueryTask(val updateActiveContactsAfter: Boolean = true) : AsyncTask<Unit, Unit, HashMap<Long, DeviceContactInfo>>() {

    private val taskUID = TaskStates.PROBING_DEVICE_CONTACTS

    override fun doInBackground(vararg params: Unit?): HashMap<Long, DeviceContactInfo>? {
        if (TaskStateRepo.get(taskUID)) {
            // task is already execution, give up
            logDebug { "Already processing this task, giving up" }
            return null
        }

        TaskStateRepo.set(taskUID, true)
        val contactManager = ContactManager()
        return contactManager.queryContacts(appController())
    }

    override fun onPostExecute(result: HashMap<Long, DeviceContactInfo>?) {
        TaskStateRepo.set(taskUID, false)

        if (result == null) {
            return
        }

        if (result.isNotEmpty()) {
            val orderedDeviceContacts = LinkedHashMap<Long, DeviceContactInfo>()
            orderedDeviceContacts.putAll(result)
            val persistedContact = PersistedContacts()
            persistedContact.deviceContacts = orderedDeviceContacts
            PrefRepo.deviceContactsMap = persistedContact
        }

        // Start active contact query
        if (updateActiveContactsAfter) {
            ActiveContactsTask.updateActiveContacts(result.values)
        }

        postEvent(DeviceContactsResultEvent(result))
    }
}