package com.rogertalk.roger.manager

import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.AvatarSize.*
import com.rogertalk.roger.utils.extensions.appController
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Hold runtime variables, that are device dependant but otherwise remain constant throughout
 * the lifetime on the application.
 */
object RuntimeVarsManager {

    private val bigAvatarSize: Int by lazy(NONE) { appController().resources.getDimensionPixelSize(R.dimen.contact_biggest_circle_diameter) }
    private val contactCircleSize: Int by lazy(NONE) { appController().resources.getDimensionPixelSize(R.dimen.contact_circle_diameter) }
    private val cornerAvatarSize: Int by lazy(NONE) { appController().resources.getDimensionPixelSize(R.dimen.navigation_contact_diameter) }
    private val groupAvatarSize: Int by lazy(NONE) { appController().resources.getDimensionPixelSize(R.dimen.ribbon_participant_diameter) }

    fun getDimensionForAvatarSize(avatarSize: AvatarSize): Int {
        when (avatarSize) {
            BIG -> return bigAvatarSize
            CONTACT -> return contactCircleSize
            TOP_CORNER -> return cornerAvatarSize
            GROUP_PARTICIPANT -> return groupAvatarSize
        }
    }
}