package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.BotQueryFailEvent
import com.rogertalk.roger.event.success.BotQuerySuccessEvent
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.models.json.BotList
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import okhttp3.ResponseBody

class BotsRequest() : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(BotList::class.java)
        getRogerAPI().bots().enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val botList = t as? BotList ?: return

        runOnUiThread {
            // Update BotCache
            BotCacheManager.cachedBotList = botList.bots

            // Inform app
            postEvent(BotQuerySuccessEvent(botList.bots))
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        postEvent(BotQueryFailEvent())
    }
}