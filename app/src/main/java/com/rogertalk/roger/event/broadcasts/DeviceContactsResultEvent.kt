package com.rogertalk.roger.event.broadcasts

import com.rogertalk.roger.models.data.DeviceContactInfo
import java.util.*


class DeviceContactsResultEvent(val deviceContacts: HashMap<Long, DeviceContactInfo>)