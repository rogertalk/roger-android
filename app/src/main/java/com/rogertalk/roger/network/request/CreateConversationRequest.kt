package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.failure.CreateGroupFailEvent
import com.rogertalk.roger.event.success.CreateGroupSuccessEvent
import com.rogertalk.roger.manager.LobbyManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import okhttp3.ResponseBody

class CreateConversationRequest(val title: String?) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)
        getRogerAPI().createConversation(title).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return
        runOnUiThread {
            // Update manager
            LobbyManager.stream = stream

            postEvent(CreateGroupSuccessEvent(stream))
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(CreateGroupFailEvent(title))
    }
}