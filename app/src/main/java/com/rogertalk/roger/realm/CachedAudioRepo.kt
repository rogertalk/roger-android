package com.rogertalk.roger.realm

import com.rogertalk.roger.models.realm.CachedAudio
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appHelper
import com.rogertalk.roger.utils.extensions.getRealm
import io.realm.RealmResults
import java.util.*

object CachedAudioRepo {

    private val EXPIRATION_MILLIS = 48 * 60 * 60 * 1000 // 48 hours

    private val CHUNK_ID = "chunkID"
    private val STREAM_ID = "streamID"
    private val AUDIO_URL = "audioURL"
    private val PERSISTED_AT = "persistedAt"

    fun createEntry(chunkId: Long, streamId: Long, audioURL: String, path: String): CachedAudio {
        if (!hasRecord(chunkId, streamId)) {
            appHelper().getRealm().beginTransaction()
            val cachedAudio = getRealm().createObject(CachedAudio::class.java)
            cachedAudio.chunkID = chunkId
            cachedAudio.streamID = streamId
            cachedAudio.audioURL = audioURL
            cachedAudio.persistPath = path
            cachedAudio.persistedAt = Date()
            appHelper().getRealm().commitTransaction()
            return cachedAudio
        } else {
            return getRecord(chunkId, streamId) ?: CachedAudio()
        }
    }

    fun deleteRecord(chunkId: Long, streamId: Long, audioURL: String) {
        val entryToRemove = getRecord(chunkId, streamId, audioURL)
        if (entryToRemove != null) {
            getRealm().beginTransaction()
            entryToRemove.deleteFromRealm()
            getRealm().commitTransaction()
        }
    }

    private fun getRecord(chunkId: Long, streamId: Long, audioURL: String): CachedAudio? {
        if (chunkId == NO_ID || streamId == NO_ID) {
            return getRecord(audioURL)
        }
        return getRecord(chunkId, streamId)
    }

    fun getRecord(chunkId: Long, streamId: Long): CachedAudio? {
        return getRealm().where(CachedAudio::class.java).
                equalTo(CHUNK_ID, chunkId).
                equalTo(STREAM_ID, streamId).
                findFirst()
    }

    fun getRecord(audioURL: String): CachedAudio? {
        return getRealm().where(CachedAudio::class.java).
                equalTo(AUDIO_URL, audioURL).
                findFirst()
    }

    fun hasRecord(audioURL: String): Boolean {
        return getRealm()
                .where(CachedAudio::class.java)
                .equalTo(AUDIO_URL, audioURL)
                .findFirst() != null
    }

    fun hasRecord(chunkId: Long, streamId: Long): Boolean {
        return getRealm()
                .where(CachedAudio::class.java)
                .equalTo(CHUNK_ID, chunkId)
                .equalTo(STREAM_ID, streamId)
                .findFirst() != null
    }

    fun getExpiredCachedAudioList(): RealmResults<CachedAudio>? {
        val currentTime = Date().time
        val expirationDate = Date(currentTime - EXPIRATION_MILLIS)
        return getRealm().where(CachedAudio::class.java).lessThan(PERSISTED_AT, expirationDate).findAll()
    }

    fun getAll(): RealmResults<CachedAudio>? {
        return getRealm().where(CachedAudio::class.java).findAll()
    }
}
