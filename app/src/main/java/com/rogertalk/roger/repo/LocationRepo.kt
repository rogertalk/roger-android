package com.rogertalk.roger.repo

import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.hasCoarseLocationPermission

object LocationRepo {

    val locationEnabled: Boolean
        get() {
            if (!locationAtSessionLevel) return false
            if (!locationAtSystemLevel) return false
            return true
        }

    val locationAtSystemLevel: Boolean
        get() {
            return appController().hasCoarseLocationPermission()
        }

    val locationAtSessionLevel: Boolean
        get() {
            // See if we got location enabled at Session level
            val session = SessionRepo.session ?: return false
            return session.account.shareLocation
        }
}
