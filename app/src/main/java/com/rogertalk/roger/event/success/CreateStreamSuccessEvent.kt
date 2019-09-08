package com.rogertalk.roger.event.success

import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Stream

class CreateStreamSuccessEvent(val stream: Stream, val deviceContactList: List<DeviceContactInfo>?)
