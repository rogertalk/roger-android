package com.rogertalk.roger.utils.cache

import com.rogertalk.roger.manager.audio.PlaybackCounterManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logWarn
import java.io.*
import java.util.*
import kotlin.properties.Delegates

object StreamCache {

    private val PERSIST_FILE = "stream.z" // just because it sounds cool

    var previousStreamsList = LinkedList<Stream>()
    var streamsList: LinkedList<Stream> by Delegates.observable(LinkedList()) {
        prop, old, new ->
        previousStreamsList = old.clone() as LinkedList<Stream>

        // For all chunks, add stream id.
        new.map(Stream::mapChunksToStream)
    }

    init {
        // Init stream cache
        val recoveredStreams = recoverFromPersistedStorage() ?: LinkedList<Stream>()
        StreamCacheRepo.updateCacheMemoryOnly(recoveredStreams)
    }

    @Synchronized fun persistStreams(streamsList: LinkedList<Stream>) {
        // Persist only a part of the streams
        val listToPersist = LinkedList<Stream>(streamsList.take(RogerConstants.FIXED_STREAM_PAGE_SIZE))

        val oos: ObjectOutputStream
        val file = File(appController().cacheDir, PERSIST_FILE)
        val fileOutputStream = FileOutputStream(file)
        oos = ObjectOutputStream(fileOutputStream)
        oos.use {
            try {
                oos.writeObject(listToPersist)
                oos.flush()
            } catch (e: Exception) {
                logError(e) { "Failed to persist stream." }
            }
        }
    }

    @Synchronized fun recoverFromPersistedStorage(): LinkedList<Stream>? {
        val file: File
        val fin: FileInputStream
        try {
            file = File(appController().cacheDir, PERSIST_FILE)
            fin = FileInputStream(file)
            val ois = ObjectInputStream(fin)
            val streams = ois.readObject() as? LinkedList<Stream>
            streams?.let {
                return streams
            }

        } catch (e: FileNotFoundException) {
            // File not found, just give up
            return null
        } catch (e: Exception) {
            logWarn { "Stream cache file Exception thrown" }
            // File might have gone corrupt somehow
            return null
        } catch (e: InvalidClassException) {
            logWarn { "Stream class changed ID somehow, so cache was invalidated" }
        } catch (e: InvalidClassException) {
            logWarn { "Stream class changed ID somehow, so cache was invalidated" }
        }

        return null

    }

    /**
     * Call this every time the contents of Streams change!
     */
    fun persistStreams() {
        persistStreams(streamsList)

        // Re-calculate stream counter
        PlaybackCounterManager.showInitialRemainingTime()
    }

    fun deleteStreams() {
        streamsList = LinkedList<Stream>()
        persistStreams()
    }
}