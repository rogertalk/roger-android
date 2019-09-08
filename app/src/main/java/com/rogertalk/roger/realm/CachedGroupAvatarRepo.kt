package com.rogertalk.roger.realm

import com.rogertalk.roger.models.realm.CachedGroupAvatar
import com.rogertalk.roger.utils.extensions.getRealm
import io.realm.RealmResults
import java.util.*


object CachedGroupAvatarRepo {

    fun createEntry(streamId: Long, groupHash: Int, path: String): CachedGroupAvatar {
        getRealm().beginTransaction()
        val cachedGroupAvatar = getRealm().createObject(CachedGroupAvatar::class.java)
        cachedGroupAvatar.streamId = streamId
        cachedGroupAvatar.persistedAt = Date()
        cachedGroupAvatar.groupHash = groupHash
        cachedGroupAvatar.persistPath = path
        getRealm().commitTransaction()
        return cachedGroupAvatar
    }

    fun deleteRecord(streamId: Long, groupHash: Int) {
        val entryToRemove = getRecord(streamId, groupHash)
        if (entryToRemove != null) {
            getRealm().beginTransaction()
            entryToRemove.deleteFromRealm()
            getRealm().commitTransaction()
        }
    }


    fun getRecord(streamId: Long, groupHash: Int): CachedGroupAvatar? {
        return getRealm()
                .where(CachedGroupAvatar::class.java)
                .equalTo(CachedGroupAvatar.COLUMN_STREAM_ID, streamId)
                .equalTo(CachedGroupAvatar.COLUMN_GROUP_HASH, groupHash)
                .findFirst()
    }

    fun getAllRecords(): RealmResults<CachedGroupAvatar> {
        return getRealm().where(CachedGroupAvatar::class.java).findAll()
    }

}