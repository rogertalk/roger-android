package com.rogertalk.roger.models.data

import java.io.Serializable
import java.util.*

class PersistedContacts() : Serializable {
    var deviceContacts = LinkedHashMap<Long, DeviceContactInfo>()
}