package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.AttachmentConstants
import com.rogertalk.roger.utils.extensions.runOnUiThread
import org.json.JSONObject

class AttachmentLinkRequest(val streamId: Long, val linkURL: String) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)

        val data = JSONObject()
        data.put(AttachmentConstants.ATTACHMENT_TYPE_FIELD, AttachmentType.LINK.type)
        data.put(AttachmentConstants.LINK_FIELD, linkURL)

        getRogerAPI().sendLinkAttachment(streamId,
                AttachmentConstants.ATTACHMENT_KEY,
                data).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        // Update local stream
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
        }
    }
}