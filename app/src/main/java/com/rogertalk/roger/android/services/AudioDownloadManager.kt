package com.rogertalk.roger.android.services

import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.network.request.ChunkDownloadRequest
import com.rogertalk.roger.realm.CachedAudioRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logVerbose
import com.rogertalk.roger.utils.log.logWarn
import java.util.*

/**
 * This class manages background audio caching. It makes sure there is only 1 audio
 * being downloaded at any given time.
 * It supports adding audio to the beginning or end of the list for finer control over priority.
 */
object AudioDownloadManager {

    private val REDUCED_STRAIN_SIZE = 3

    class PendingAudioDownload(val audioURL: String, val chunkId: Long?, val streamId: Long?)

    private var audioPendingDownload = LinkedList<PendingAudioDownload>()

    fun cacheRemoteAudioLowPriority(pendingAudioDownload: PendingAudioDownload) {
        val sizeBefore = audioPendingDownload.size
        audioPendingDownload.add(pendingAudioDownload)
        if (sizeBefore == 0) {
            downloadNext()
        }
    }

    fun cacheAudioHighPriority(pendingAudioDownload: PendingAudioDownload) {
        val sizeBefore = audioPendingDownload.size
        audioPendingDownload.push(pendingAudioDownload)
        if (sizeBefore == 0) {
            downloadNext()
        }
    }

    fun cacheEntireStreamList(streamList: List<Stream>) {
        if (ConnectivityHelper.isConnectedToWiFi(appController())) {
            val currentlyCached = CachedAudioRepo.getAll()?.toList() ?: emptyList()

            for (stream in streamList) {
                innerFor@for (chunk in stream.chunks) {
                    // Should NOT add if already in the download list
                    if (audioPendingDownload.any { it.chunkId == chunk.id && it.streamId == stream.id }) {
                        continue@innerFor
                    }

                    // Should NOT be cached already
                    if (currentlyCached.none { it.chunkID == chunk.id && it.streamID == stream.id }) {
                        // Add to list of downloads
                        cacheRemoteAudioLowPriority(PendingAudioDownload(chunk.audioURL, chunk.id, stream.id))
                    }
                }
            }
        } else {
            logInfo { "Not connected to WiFi" }
        }
    }

    /**
     * We call this method from time to time to make sure if a deadlock occurs it doesn't
     * invalidate the whole cache mechanism.
     */
    fun clearEntireList() {
        audioPendingDownload = LinkedList<PendingAudioDownload>()
    }

    fun downloadNext() {
        if (audioPendingDownload.isEmpty()) {
            logDebug { "Empty list" }
            return
        }

        // Re-assert WiFi connectivity
        if (!ConnectivityHelper.isConnectedToWiFi(appController())) {
            if (audioPendingDownload.size > REDUCED_STRAIN_SIZE) {
                logDebug { "Not connected to WiFi anymore, reducing queue for cached audio" }

                val slicedList = audioPendingDownload.take(REDUCED_STRAIN_SIZE)
                audioPendingDownload = LinkedList<PendingAudioDownload>()
                audioPendingDownload.addAll(slicedList)
            }
        }

        try {
            val pendingDownload = audioPendingDownload.removeFirst()

            val audioURL = pendingDownload.audioURL
            val chunkId = pendingDownload.chunkId
            val streamId = pendingDownload.streamId

            if (chunkId != null && streamId != null) {
                cacheAudio(audioURL, chunkId, streamId)
            } else {
                cacheAudio(audioURL)
            }

        } catch (e: NoSuchElementException) {
            logWarn { "No element found. Stopping." }
            return
        }
    }


    private fun cacheAudio(audioURL: String) {
        // Check if entry already exists in DB
        if (CachedAudioRepo.getRecord(audioURL) != null) {
            downloadNext()
            // Already have this record, give up
            return
        }

        // Actually download it
        logVerbose { "Will download audio in background: $audioURL" }
        ChunkDownloadRequest(null, audioURL, null).enqueueRequest()
    }

    private fun cacheAudio(audioURL: String, chunkId: Long, streamId: Long) {
        // Check if entry already exists in DB
        if (CachedAudioRepo.getRecord(chunkId, streamId) != null) {
            downloadNext()
            // Already have this record, give up
            return
        }

        // Actually download it
        logVerbose { "Will download audio in background: $audioURL" }
        ChunkDownloadRequest(chunkId, audioURL, streamId).enqueueRequest()
    }

}