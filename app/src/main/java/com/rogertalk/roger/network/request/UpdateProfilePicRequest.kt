package com.rogertalk.roger.network.request

import android.graphics.Bitmap
import com.rogertalk.roger.event.failure.ChangeDisplayPicFailEvent
import com.rogertalk.roger.event.success.ChangeDisplayPicSuccessEvent
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.utils.constant.RuntimeConstants
import com.rogertalk.roger.utils.extensions.postEvent
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream


class UpdateProfilePicRequest(val bitmap: Bitmap) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(Account::class.java)

        // convert to byte array
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, RuntimeConstants.PROFILE_PIC_JPG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()

        val filePart = RequestBody.create(MediaType.parse("image/jpg"), byteArray)

        getRogerAPI().changeUserImage(filePart).enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val account = t as? Account ?: return
        postEvent(ChangeDisplayPicSuccessEvent(account))
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        postEvent(ChangeDisplayPicFailEvent())
    }
}