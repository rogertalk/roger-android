package com.rogertalk.roger.network.request

import com.rogertalk.roger.event.broadcasts.RecordingUploadFailEvent
import com.rogertalk.roger.event.success.RecordingUploadSuccessEvent
import com.rogertalk.roger.manager.PendingUploads
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.models.realm.PendingChunkUpload
import com.rogertalk.roger.realm.PendingChunkUploadRepo
import com.rogertalk.roger.utils.constant.RuntimeConstants.Companion.MAX_NETWORK_RETRIES
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logVerbose
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.io.File
import java.util.*

class SendAudioChunkRequest(val streamId: Long, val file: File, val duration: Int,
                            val persist: Boolean = false) : BaseRequest() {

    var chunkToken: String? = null

    //
    // OVERRIDE METHODS
    //

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)

        // TODO : if error is related to no-internet, don't increment retry count!

        removeFromCurrentlyUploading()

        if (persist) {
            // If this is a persist audio, don't try to send it again
            removePersistedRequest()
            return
        }

        // Certain request codes tell us this request won't ever succeed
        if (responseCode >= 400 && responseCode < 500 && responseCode != 401) {
            removePersistedRequest()
            // Give up any further processing
            return
        }

        val existingRequest = PendingChunkUploadRepo.getRecord(file.absolutePath)
        // Update retry count
        if (existingRequest != null) {
            handleExistingRequest(existingRequest)
        }

        // Inform the rest of the app
        postEvent(RecordingUploadFailEvent())
    }

    override fun <T : Any> handleSuccess(t: T) {
        removeFromCurrentlyUploading()

        // Delete any pending upload entry if available
        removePersistedRequest()

        // Update the stream
        val stream = t as? Stream ?: return
        postEvent(RecordingUploadSuccessEvent(stream))
    }

    override fun enqueueRequest() {
        persistRequest()
        val firstForThisStream = PendingChunkUploadRepo.isFirstForStream(streamId, file.absolutePath)

        if (firstForThisStream) {
            proceedWithUpload()
        } else {
            // This is not the first of the stream,so make sure the other uploads have finished first
            if (PendingUploads.chunkPathList.isEmpty()) {
                proceedWithUpload()
            } else {
                logDebug { "No uploading this audio since there's another one pending for this stream" }
            }
        }
    }

    //
    // PUBLIC METHODS
    //

    fun enqueueRequestForToken(): String {
        generateChunkToken()
        enqueueRequest()
        return chunkToken ?: ""
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Here is where the actual logic for uploading happens.
     */
    private fun proceedWithUpload() {
        val callback = getCallback(Stream::class.java)
        val filePart = RequestBody.create(MediaType.parse("audio/mp4"), file)
        addToCurrentlyUploading()

        getRogerAPI().sendAudioChunkWithToken(
                streamId.toString(),
                duration.toString(),
                chunkToken,
                filePart,
                persist = persist).enqueue(callback)
    }

    private fun removePersistedRequest() {
        val pendingUpload = PendingChunkUploadRepo.getRecord(file.absolutePath)
        if (pendingUpload != null) {
            PendingChunkUploadRepo.deleteRecord(pendingUpload)
        }

        // Delete the file once we've successfully uploaded it
        file.delete()
    }

    private fun persistRequest() {
        val path = file.absolutePath
        val existentRequest = PendingChunkUploadRepo.getRecord(path)
        if (existentRequest == null) {
            PendingChunkUploadRepo.createEntry(streamId, duration, path)
        }
    }

    /**
     * Remove from currently uploading list.
     */
    private fun removeFromCurrentlyUploading() {
        PendingUploads.chunkPathList.remove(file.absolutePath)
    }

    /**
     * Remove from currently uploading list.
     */
    private fun addToCurrentlyUploading() {
        PendingUploads.chunkPathList.add(file.absolutePath)
    }

    private fun handleExistingRequest(existingRequest: PendingChunkUpload) {
        // Increment retry attempts
        val retriesCount = existingRequest.retries + 1

        // If we retried to many times, just discard this request
        if ((retriesCount + 1) > MAX_NETWORK_RETRIES) {
            removePersistedRequest()
            return
        }

        PendingChunkUploadRepo.incrementRetries(file.absolutePath)
    }

    /**
     * Generate a unique chunk token.
     */
    private fun generateChunkToken() {
        val base62Spectrum = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        var stringBuffer = ""
        repeat(4) {
            stringBuffer += base62Spectrum[random.nextInt(base62Spectrum.length)]
        }
        chunkToken = stringBuffer

        logVerbose { "Generated chunk token: $chunkToken" }
    }
}