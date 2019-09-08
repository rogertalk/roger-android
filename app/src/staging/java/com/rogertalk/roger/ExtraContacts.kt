package com.rogertalk.roger

import com.rogertalk.roger.models.data.ContactLabel
import com.rogertalk.roger.models.data.DeviceContactInfo
import java.util.*


class ExtraContacts {

    companion object {
        fun getExtraContacts(): HashMap<Long, DeviceContactInfo> {
            val fakeContacts = HashMap<Long, DeviceContactInfo>(2)

            // add a couple of fake contacts
            val phoneNumbersAramis = HashSet<ContactLabel>(1)
            val phoneNumbersAthos = HashSet<ContactLabel>(1)

            val randomNum = Random().nextInt(800) + 100
            val randomNumPrefix = Random().nextInt(89) + 10

            phoneNumbersAramis.add(ContactLabel("+1${randomNum}55501$randomNumPrefix", "mobile"))
            phoneNumbersAthos.add(ContactLabel("+19345550167", "mobile"))

            fakeContacts.put(-1L, DeviceContactInfo(-1L, "Aramis (randomized)", null, phoneNumbersAramis))

            fakeContacts.put(-2L, DeviceContactInfo(-2L, "Athos", null, phoneNumbersAthos))

            return fakeContacts
        }
    }

}