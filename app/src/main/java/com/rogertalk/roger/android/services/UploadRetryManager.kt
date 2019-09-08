package com.rogertalk.roger.android.services

import com.rogertalk.roger.event.broadcasts.RecordingUploadFailEvent
import com.rogertalk.roger.event.success.RecordingUploadSuccessEvent
import com.rogertalk.roger.manager.PendingUploads
import com.rogertalk.roger.models.realm.PendingChunkUpload
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.network.request.SendAudioChunkRequest
import com.rogertalk.roger.realm.PendingChunkUploadRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File


object UploadRetryManager {

    private var running = false

    //
    // PUBLIC METHODS
    //


    fun run() {
        logMethodCall()
        if (running) {
            return
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            //Register for events
            EventBus.getDefault().register(this)
        }
        running = true
        uploadNext()
    }

    //
    // PRIVATE METHODS
    //

    private fun uploadNext() {
        // Give up if not connected to the network
        if (!ConnectivityHelper.isConnected(appController())) {
            logInfo { "Not connected to the internet, not retrying now" }
            running = false
            return
        }

        val pendingUpload = PendingChunkUploadRepo.popRecord()

        if (pendingUpload == null) {
            // No more pending uploads. We can stop execution now.
            running = false
            return
        }

        // Do not proceed if this is blank
        if (pendingUpload.savedPath.isBlank()) {
            logError { "Pending upload path invalid" }

            // Delete it
            removeRecord(pendingUpload)

            uploadNext()
            return
        }

        // Is it already uploading?
        if (PendingUploads.chunkPathList.contains(pendingUpload.savedPath)) {
            logDebug { "Already uploading the current chunk" }

            // Don't continue work, but leave service alive to get the callback from the current request
            running = false
            return
        }

        // Build request again
        val uploadFile = File(pendingUpload.savedPath)

        // Give up if file doesn't exist anymore
        if (!uploadFile.exists()) {
            logWarn { "Recording file does not exist anymore" }

            // Delete it
            removeRecord(pendingUpload)

            uploadNext()
            return
        }

        SendAudioChunkRequest(pendingUpload.streamId, uploadFile, pendingUpload.duration,
                false).enqueueRequest()
    }

    private fun removeRecord(pendingChunk: PendingChunkUpload) {
        PendingUploads.chunkPathList.remove(pendingChunk.savedPath)
        PendingChunkUploadRepo.deleteRecord(pendingChunk)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecordingUploaded(event: RecordingUploadSuccessEvent) {
        logEvent(event)
        uploadNext()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecordingFailedToUpload(event: RecordingUploadFailEvent) {
        logEvent(event)
        uploadNext()
    }
}