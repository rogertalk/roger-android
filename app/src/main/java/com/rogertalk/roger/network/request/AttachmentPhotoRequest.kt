package com.rogertalk.roger.network.request

import android.graphics.Bitmap
import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.AttachmentConstants
import com.rogertalk.roger.utils.constant.RuntimeConstants
import com.rogertalk.roger.utils.extensions.runOnUiThread
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class AttachmentPhotoRequest(val streamId: Long, val bitmap: Bitmap) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Stream::class.java)

        // Convert image to byte array
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, RuntimeConstants.PROFILE_PIC_JPG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()

        val requestBody = RequestBody.create(MediaType.parse("image/jpg"), byteArray)
        val body = MultipartBody.Part.createFormData("url", "${Date().time}.jpg", requestBody)

        // Create data content
        val data = JSONObject()
        data.put(AttachmentConstants.ATTACHMENT_TYPE_FIELD, AttachmentType.IMAGE.type)

        getRogerAPI().sendImageAttachment(streamId,
                AttachmentConstants.ATTACHMENT_KEY,
                data,
                body).enqueue(callback)

    }

    override fun <T : Any> handleSuccess(t: T) {
        val stream = t as? Stream ?: return

        // Update local stream
        runOnUiThread {
            StreamCacheRepo.updateStreamInStreams(stream)
        }
    }
}