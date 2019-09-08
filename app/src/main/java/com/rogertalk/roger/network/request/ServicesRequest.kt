package com.rogertalk.roger.network.request

import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.models.json.BotList
import com.rogertalk.roger.utils.extensions.runOnUiThread

class ServicesRequest() : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(BotList::class.java)
        getRogerAPI().services().enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val botList = t as? BotList ?: return

        runOnUiThread {
            // Update BotCache
            BotCacheManager.cachedServicesList = botList.bots
        }
    }
}