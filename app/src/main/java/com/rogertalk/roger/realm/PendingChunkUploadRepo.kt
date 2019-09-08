package com.rogertalk.roger.realm

import com.rogertalk.roger.models.realm.PendingChunkUpload
import com.rogertalk.roger.models.realm.PendingChunkUpload.Companion.COLUMN_ACCOUNT_ID
import com.rogertalk.roger.models.realm.PendingChunkUpload.Companion.COLUMN_CREATED_AT
import com.rogertalk.roger.models.realm.PendingChunkUpload.Companion.COLUMN_SAVED_PATH
import com.rogertalk.roger.models.realm.PendingChunkUpload.Companion.COLUMN_STREAM_ID
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.extensions.getRealm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*


object PendingChunkUploadRepo {

    fun createEntry(streamId: Long, duration: Int, savePath: String): PendingChunkUpload {
        getRealm().beginTransaction()
        val pendingChunkUpload = getRealm().createObject(PendingChunkUpload::class.java)
        pendingChunkUpload.accountId = SessionRepo.sessionId()
        pendingChunkUpload.createdAt = Date().time
        pendingChunkUpload.duration = duration
        pendingChunkUpload.streamId = streamId
        pendingChunkUpload.savedPath = savePath
        getRealm().commitTransaction()
        return pendingChunkUpload
    }

    /**
     * Validation if there are other pending audio uploads for this same stream
     */
    fun isFirstForStream(streamId: Long, path: String): Boolean {
        val chunksToUpload = getAllForStreamId(streamId)
        val firstChunk = chunksToUpload.first() ?: return true
        if (firstChunk.savedPath == path) {
            // It is indeed the first chunk for this stream
            return true
        }
        return false
    }

    fun incrementRetries(path: String) {
        val entryToUpdate = getRecord(path)
        if (entryToUpdate != null) {
            getRealm().beginTransaction()
            entryToUpdate.retries = entryToUpdate.retries + 1
            getRealm().copyToRealmOrUpdate(entryToUpdate)
            getRealm().commitTransaction()
        }
    }

    fun deleteRecord(path: String) {
        val entryToRemove = getRecord(path)
        if (entryToRemove != null) {
            getRealm().beginTransaction()
            entryToRemove.deleteFromRealm()
            getRealm().commitTransaction()
        }
    }

    fun deleteRecord(pendingUpload: PendingChunkUpload) {
        getRealm().beginTransaction()
        pendingUpload.deleteFromRealm()
        getRealm().commitTransaction()
    }

    fun getRecord(path: String): PendingChunkUpload? {
        return getRealm().where(PendingChunkUpload::class.java).equalTo(COLUMN_SAVED_PATH, path).findFirst()
    }

    /**
     * Get all pending chunk uploads for stream, ordered by date
     */
    fun getAllForStreamId(streamId: Long): RealmResults<PendingChunkUpload> {
        return getRealm().where(PendingChunkUpload::class.java)
                .equalTo(COLUMN_STREAM_ID, streamId)
                .findAllSorted(COLUMN_CREATED_AT, Sort.ASCENDING)
    }

    fun getAllRecords(): RealmResults<PendingChunkUpload> {
        return getRealm().where(PendingChunkUpload::class.java).findAll()
    }

    /**
     * Get the oldest record for the current active account
     */
    fun popRecord(): PendingChunkUpload? {
        val currentAccountId = UserAccountRepo.id() ?: return null

        return getRealm().where(PendingChunkUpload::class.java)
                .equalTo(COLUMN_ACCOUNT_ID, currentAccountId)
                .findAllSorted(COLUMN_CREATED_AT, Sort.ASCENDING).firstOrNull()
    }
}