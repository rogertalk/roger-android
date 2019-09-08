package com.rogertalk.roger.android.services

import com.rogertalk.roger.realm.CachedAudioRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logInfo
import com.rogertalk.roger.utils.log.logVerbose
import java.io.File
import java.util.*

/**
 * This service is responsible for the maintenance of the app chunks cache.
 */
class CleanupCacheManager {

    companion object {

        private val RUN_INTERVAL = 3600000 // 1 hour in milliseconds

        /**
         * Cleanup expired downloaded audio files
         */
        fun runCleanup() {
            // Only run this scheduled cleanup every so often
            var currentTimestamp = Date().time
            val lastExecution = PrefRepo.lastCleanupTimestamp
            if (lastExecution != NO_TIME
                    && (currentTimestamp - lastExecution) < RUN_INTERVAL) {
                logInfo { "Already ran cleanup not long ago, giving up" }
                return
            }

            logInfo { "Will cleanup expired cached audio files" }

            // Get expired entries
            val expiredEntries = CachedAudioRepo.getExpiredCachedAudioList() ?: return

            // Remove from persisted storage
            for (cachedAudio in expiredEntries) {
                logVerbose { "Removing cached audio: ${cachedAudio.audioURL}" }
                removeFile(cachedAudio.persistPath)
                CachedAudioRepo.deleteRecord(cachedAudio.chunkID, cachedAudio.streamID, cachedAudio.audioURL)
            }

            // Save timestamp of this execution
            currentTimestamp = Date().time
            PrefRepo.lastCleanupTimestamp = currentTimestamp

            // All Done :)
        }

        /**
         * Remove a file from internal storage
         */
        private fun removeFile(absolutePath: String) {
            if (absolutePath.isNullOrBlank()) {
                // Nothing to do, file was probably never downloaded
                return
            }
            val file = File(absolutePath)
            if (file.exists()) {
                try {
                    file.delete()
                } catch(e: Exception) {
                    logError(e) { "Could not delete file: " + absolutePath }
                }
            }
        }
    }
}
