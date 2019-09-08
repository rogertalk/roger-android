package com.rogertalk.roger.ui.screens

import android.os.Bundle
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.ui.screens.base.BaseAppCompatActivity

class IFTTTDeepLinkActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Clear current bot cache, if any
        BotCacheManager.cachedBotList = emptyList()

        // Navigate back to where the user previously was on the app
        finish()
    }
}