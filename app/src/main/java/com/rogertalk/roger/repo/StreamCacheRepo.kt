package com.rogertalk.roger.repo

import android.os.SystemClock
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.internal.LinkedTreeMap
import com.rogertalk.roger.event.broadcasts.streams.StreamsChangedEvent
import com.rogertalk.roger.manager.RuntimeVarsManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.manager.audio.AutoplayManager
import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Chunk
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.network.request.SingleStreamRequest
import com.rogertalk.roger.utils.cache.StreamCache
import com.rogertalk.roger.utils.constant.AttachmentConstants
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.*
import java.util.*

object StreamCacheRepo {

    // TODO: Remove and maintain temporary stream purely in UI code
    var temporaryStream: Stream? = null

    // Time for which stream data should be considered fresh (to avoid unnecessary calls)
    private val STREAM_FRESHNESS_TIMESPAN = 5000
    private var lastStreamRefresh = NO_TIME

    // Hidden variable (only updated internally)
    private var reachedListEndValue = false
    private var gotCursorOnceValue = false

    /**
     * @return True if stream cache is still fresh, ie, no need to refresh it explicitly
     */
    val streamCacheFresh: Boolean
        get() {
            val now = SystemClock.elapsedRealtime()
            val elapsedTime = now - lastStreamRefresh
            return elapsedTime < STREAM_FRESHNESS_TIMESPAN
        }

    // Read-only value
    val reachedListEnd: Boolean
        get() = reachedListEndValue

    val gotCursorOnce: Boolean
        get() = gotCursorOnceValue

    var nextCursor: String? = null
        set(value) {
            // We never want the value to become null again
            if (value == null) {
                // Never nullify the value, this also symbolizes we reached the end of the list
                reachedListEndValue = true
                logWarn { "REACHED END OF LIST" }
                return
            }

            gotCursorOnceValue = true

            // We have a cursor, so definitely this is not the end of the list
            reachedListEndValue = false
            field = value
        }

    /**
     * Check if ghost stream already exists on StreamCache, if so, remove ghost stream
     */
    fun evaluateGhostAvailability() {
        val ghost = temporaryStream
        if (ghost != null) {
            val stream = getStreamRaw(ghost.id)
            if (stream != null) {
                temporaryStream = null
            }
        }
    }

    //
    // PUBLIC METHODS
    //


    /**
     * Hides ghost and removes the same instance from cached streams
     */
    fun properlyHideGhost() {
        synchronized(StreamCache.streamsList) {
            val tmpStream = temporaryStream
            if (tmpStream != null) {
                // Remove stream from cache as well
                removeStream(tmpStream.id)

                temporaryStream = null
            }
        }
    }

    fun deleteLocalCache() {
        synchronized(StreamCache.streamsList) {
            temporaryStream = null
            StreamCache.deleteStreams()
        }
    }

    /**
     * Removes a participant from a stream locally, and persists it
     */
    fun removeParticipant(accountId: Long, streamId: Long) {
        synchronized(StreamCache.streamsList) {
            val copyOfStreams = getCachedCopy()
            for (stream in copyOfStreams) {
                if (stream.id == streamId) {
                    logDebug { "REMOVED!!!!" }
                    val newOthers = stream.othersOrEmpty.filter { it.id != accountId }
                    stream.others = newOthers
                }
            }

            updateStreams(copyOfStreams, true)
        }
    }

    /**
     * Remove a given stream from cache
     */
    fun removeStream(streamId: Long) {
        synchronized(StreamCache.streamsList) {
            getCached().removeAll { it.id == streamId }
            StreamCache.persistStreams()

            // Remove current stream from Stream Manager
            StreamManager.removeStream(streamId)

            // Tell app to update content display
            postEvent(StreamsChangedEvent())
        }
    }

    /**
     * Get cached streams
     */
    fun getCached(): LinkedList<Stream> {
        synchronized(StreamCache.streamsList) {
            return StreamCache.streamsList
        }
    }

    /**
     * Get copy of cached streams. [getCached] returns a pointer to the actual cache
     */
    fun getCachedCopy(): LinkedList<Stream> {
        synchronized(StreamCache.streamsList) {
            return LinkedList(StreamCache.streamsList)
        }
    }

    /**
     * Update Cached streams
     */
    fun updateCache(streams: List<Stream>) {
        updateStreams(streams, persistAfter = true)
    }

    /**
     * Update Cached streams but only its memory counterpart
     */
    fun updateCacheMemoryOnly(streams: List<Stream>) {
        updateStreams(streams, persistAfter = false)
    }

    /**
     * Check if a given stream id is already part of the cached streams.
     */
    fun containsStreamId(streamId: Long): Boolean {
        synchronized(StreamCache.streamsList) {
            val streamList = getCached()
            for (stream in streamList) {
                if (stream.id == streamId) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Get a a cached stream given its ID
     */
    fun getStream(streamId: Long?): Stream? {
        if (streamId == null) {
            return null
        }
        synchronized(StreamCache.streamsList) {
            // First check for the temporary stream
            temporaryStream?.let { stream ->
                if (stream.id == streamId) {
                    return stream
                }
            }

            // Check cached streams list
            return getStreamRaw(streamId)
        }
    }

    /**
     * Receive a new or updated stream, and update the stream cache we had before
     */
    fun updateStreamInStreams(stream: Stream) {
        if (stream.id == temporaryStream?.id) {
            // The received stream is the same as ghost, so do nothing
            return
        }
        if (stream.id == NO_ID) {
            logError { "Got a NO_ID stream" }
            // The received stream is the same as ghost, so do nothing
            return
        }
        mergeStreamWithStreams(stream, persistAfter = true)
        postEvent(StreamsChangedEvent())
        updateStreamManager(stream)
    }

    private fun updateStreamManager(stream: Stream) {
        if (stream.id == StreamManager.selectedStreamId) {
            StreamManager.selectedStream = stream
        }
    }

    private fun updateStreamManager(streamId: Long) {
        if (streamId == StreamManager.selectedStreamId) {
            StreamManager.selectedStream = getStream(streamId)
        }
    }

    /**
     * This can also be used to insert a temporary (fake) participant into a Stream
     */
    fun addParticipantToStream(streamId: Long, participant: Account) {
        synchronized(StreamCache.streamsList) {
            val streamList = getCached()
            forcycle@for (stream in streamList) {
                if (stream.id == streamId) {
                    logDebug { "Will add new participant to an existing stream" }
                    val savedOthers = ArrayList(stream.others)
                    savedOthers.add(participant)
                    stream.others = savedOthers.toList()
                }
            }
            updateStreamManager(streamId)
        }
    }

    fun updateAttachmentForStream(streamId: Long, newAttachments: HashMap<String, LinkedTreeMap<String, String>>) {
        synchronized(StreamCache.streamsList) {
            val streamList = getCached()
            forcycle@for (stream in streamList) {
                if (stream.id == streamId) {
                    logDebug { "Will add new attachment to an existing stream" }
                    stream.attachments = newAttachments

                    // Preload attachment image for future use (even if offline)
                    if (ConnectivityHelper.canUseExtraData()) {
                        val attachmentLink = stream.attachmentLink
                        val isImage = stream.attachmentType != null && stream.attachmentType == AttachmentType.IMAGE
                        if (isImage && attachmentLink != null)
                            logDebug { "Connected to good network, pre-loading attachment image" }

                        Glide.with(appController())
                                .load(attachmentLink)
                                .override(AttachmentConstants.ATTACHMENT_PHOTO_WIDTH, AttachmentConstants.ATTACHMENT_PHOTO_HEIGHT)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .preload()

                        // Pre-load smaller version as well
                        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.CONTACT)
                        Glide.with(appController())
                                .load(attachmentLink)
                                .override(cropSize, cropSize)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .preload()
                    }

                    break@forcycle
                }
            }
            updateStreamManager(streamId)
            // Inform the app
            postEvent(StreamsChangedEvent())

            StreamCache.persistStreams()
        }
    }

    /**
     * Add a chunk to an existing stream
     * @return True if was able to add this chunk, False otherwise
     */
    fun addChunkToStreams(newChunk: Chunk, streamId: Long) {
        val streamList = getCached()
        var position = 0
        var foundStream = false
        forcycle@for (stream in streamList) {
            if (stream.id == streamId) {
                val newChunkList = ArrayList<Chunk>(stream.chunks.size + 1)

                // Add all previous exiting chunks
                newChunkList.addAll(stream.chunks)
                val chunkIsActuallyNew = (stream.chunks.filter { it.id == newChunk.id }).isEmpty()

                // Make sure new chunk is different
                if (chunkIsActuallyNew) {
                    logDebug { "New chunk is actually new" }
                    newChunkList.add(newChunk)
                    if (stream.lastInteraction >= newChunk.end) {
                        logInfo { "Stream last interaction was NOT updated" }
                    } else {
                        logInfo { "Stream last interaction was updated" }
                        stream.lastInteraction = newChunk.end

                        // Participant order should be adjusted
                        val participantWhoSpoke = stream.othersOrEmpty.firstOrNull() { it.id == newChunk.senderId }
                        if (participantWhoSpoke != null) {
                            val newOthers = ArrayList<Account>(stream.others)

                            // Move that participant to the front
                            newOthers.remove(participantWhoSpoke)
                            newOthers.add(0, participantWhoSpoke)
                            stream.others = newOthers.toList()
                        }
                    }
                } else {
                    logWarn { "New chunk already existed!" }
                }

                stream.chunks = newChunkList

                foundStream = true
                break@forcycle
            }
            position++
        }

        if (foundStream) {
            // Move this stream to the front
            moveStreamToFront(position)

            // Persist this change
            updateStreamFreshness()
            StreamCache.persistStreams()

            // Check autoplay
            if (streamList.isNotEmpty()) {
                handleStreamAutoplay(streamList.first)
            }

            // Update stream manager
            updateStreamManager(streamId)

            // Inform the app
            postEvent(StreamsChangedEvent())
            return
        }

        // If stream doesn't exist, issue a stream update instead
        logDebug { "Stream to add chunk not found. Will refresh the entire stream instead." }
        SingleStreamRequest(streamId).enqueueRequest()
    }

    // TODO: We should update individual Stream timestamps and reorder streams locally instead.
    fun moveStreamToFront(streamPos: Int) {
        if (streamPos == 0) {
            // it already is in the front!
            return
        }

        // safety checks
        if (streamPos >= getCached().size || streamPos < 0) {
            throw ArrayIndexOutOfBoundsException()
        }

        val stream = getCached()[streamPos]
        getCached().removeAt(streamPos)
        getCached().addFirst(stream)

        logDebug { "Added stream to front" }
    }

    /**
     * Get the account for the specified account ID from cache
     */
    fun getAccountById(accountId: Long): Account? {
        // Check if account is from current user first
        val ownId = UserAccountRepo.id()
        if (ownId != null && ownId == accountId) {
            return UserAccountRepo.current()
        }

        synchronized(StreamCache.streamsList) {
            val streams = getCached()
            for (stream in streams) {
                for (participant in stream.othersOrEmpty) {
                    if (participant.id == accountId) {
                        // We found the ACCOUNT we were looking for
                        return participant
                    }
                }
            }

            // Not found
            return null
        }
    }

    /**
     * Update streams with new stream object, merging data and reordering the streams if necessary.
     */
    fun mergeStreamWithStreams(newStream: Stream, persistAfter: Boolean) {
        synchronized(StreamCache.streamsList) {
            val originalStreamIndex = StreamCache.streamsList.indexOfFirst { it.id == newStream.id }
            if (originalStreamIndex != -1) {
                // Stream is already present on the list.
                val oldStream = StreamCache.streamsList[originalStreamIndex]

                // Don't update if the one we got is actually newer
                if (newStream.lastInteraction < oldStream.lastInteraction) {
                    logDebug { "This stream last interaction was older than the current one" }
                    newStream.lastInteraction = oldStream.lastInteraction
                }

                // Merge OTHERS
                if (newStream.others == null && oldStream.others != null) {
                    newStream.others = oldStream.others
                }

                // MERGE Chunks
                val expirationDate = Date().time - (1.728e+8)
                // Preserve the old chunks that are newer than 48 hours and
                // that do not exist in the newer chunk list
                val validChunks = oldStream.chunks.filter { it.end > (expirationDate) && !newStream.chunks.contains(it) }

                val finalChunkList = ArrayList<Chunk>(validChunks.size + newStream.chunks.size)
                finalChunkList.addAll(validChunks)
                finalChunkList.addAll(newStream.chunks)

                // Re-order chunks
                finalChunkList.sortBy { it.end }
                newStream.chunks = finalChunkList

                // MERGE locally-changeable data
                val finalPlayedUntil = Math.max(newStream.playedUntil, oldStream.playedUntil)
                val finalLastInteraction = Math.max(newStream.lastInteraction, oldStream.lastInteraction)
                newStream.playedUntil = finalPlayedUntil
                newStream.lastInteraction = finalLastInteraction

                // Preserve attachments
                if (newStream.attachments.isEmpty() && oldStream.attachments.isNotEmpty()) {
                    newStream.attachments = oldStream.attachments
                }

                // Remove it from current position, we'll re-add it in a new position after
                StreamCache.streamsList.removeAt(originalStreamIndex)
            }

            // Reorder stream
            if (StreamCache.streamsList.isNotEmpty()) {
                // Current stream list is NOT empty, insert the new stream in the appropriate position (ordered by interaction timestamp).
                val newIndex = StreamCache.streamsList.indexOfFirst { it.lastInteraction < newStream.lastInteraction }
                if (newIndex > -1) {
                    StreamCache.streamsList.add(newIndex, newStream)
                } else {
                    // There was no stream older than this one, so put it at the end.
                    StreamCache.streamsList.add(newStream)
                }
            } else {
                // Current stream list was empty, simply insert this stream at the top
                StreamCache.streamsList.add(newStream)
            }

            // Decide if we should persist that data
            if (persistAfter) {
                updateStreamFreshness()
                StreamCache.persistStreams()
            }

            // Update Stream manager
            updateStreamManager(newStream.id)
        }
    }


    //
    // PRIVATE METHODS
    //

    private fun handleStreamAutoplay(stream: Stream) {
        if (PrefRepo.livePlayback && stream.unplayed) {
            logDebug { "Will attempt autoplay of new stream content" }
            AutoplayManager.autoplayStream(stream)
        }
    }

    private fun updateStreams(newStreams: List<Stream>, persistAfter: Boolean = true) {
        synchronized(StreamCache.streamsList) {
            if (persistAfter) {
                sanitizeStreams(newStreams)
            }

            // Merge each stream
            for (stream in newStreams) {
                mergeStreamWithStreams(stream, persistAfter)
            }
        }
    }

    /**
     * Transverses the first streams batch and removes those which are not
     * part of the server's response.
     * This effectively removes streams that were removed remotely.
     */
    private fun sanitizeStreams(newStreams: List<Stream>) {
        logMethodCall()
        val sanitizedList = LinkedList<Stream>()
        val previousList = getCachedCopy()

        val stillFreshTimestamp = Date().time - (600000)

        for (i in 0 until previousList.size) {
            val elem = previousList[i]
            if (i < RogerConstants.FIXED_STREAM_PAGE_SIZE) {
                val stillFresh = elem.lastInteraction > stillFreshTimestamp
                if (newStreams.contains(elem) || stillFresh) {
                    sanitizedList.add(elem)
                }
            } else {
                // Add all subsequent element (from conversations paging)
                sanitizedList.add(elem)
            }
        }

        // Update streams list
        StreamCache.streamsList = sanitizedList
        updateStreamFreshness()
        StreamCache.persistStreams()
    }

    /**
     * Only for internal use, will search within the cache alone.
     */
    private fun getStreamRaw(streamId: Long): Stream? {
        synchronized(StreamCache.streamsList) {
            val streamList = getCached()
            for (stream in streamList) {
                if (stream.id == streamId) {
                    return stream
                }
            }
            return null
        }
    }

    private fun updateStreamFreshness() {
        lastStreamRefresh = SystemClock.elapsedRealtime()
    }
}