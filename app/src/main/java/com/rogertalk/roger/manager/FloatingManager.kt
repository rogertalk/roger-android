package com.rogertalk.roger.manager

import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.repo.PrefRepo

/**
 * Manager class for Floating UI implementation
 */
object FloatingManager {

    /**
     * This method determines if new content push notifications should
     * prefer updating the floating UI rather than posting a new android notification.
     */
    fun notificationsPreferFloating(): Boolean {
        if (AppVisibilityRepo.appIsBackground && PrefRepo.talkHeads && PrefRepo.completedOnboarding) {
            return true
        }
        return false
    }
}