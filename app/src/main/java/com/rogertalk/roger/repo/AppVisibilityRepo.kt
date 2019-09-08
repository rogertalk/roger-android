package com.rogertalk.roger.repo

import com.rogertalk.roger.utils.extensions.appController

/**
 * This class can be used a quick and simple way to know if TalkScreen is visible
 */
object AppVisibilityRepo {

    var chatIsForeground: Boolean = false

    val appIsBackground: Boolean
        get() {
            return appController().backgroundManager.isInBackground
        }

}