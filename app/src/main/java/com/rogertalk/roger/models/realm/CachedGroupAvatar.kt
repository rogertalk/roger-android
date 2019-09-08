package com.rogertalk.roger.models.realm

import com.rogertalk.roger.utils.constant.NO_ID
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class CachedGroupAvatar(
        open var streamId: Long = NO_ID,
        open var groupHash: Int = -1,
        open var persistedAt: Date = Date(),
        open var persistPath: String = "") : RealmObject() {

    companion object {
        val COLUMN_STREAM_ID = "streamId"
        val COLUMN_GROUP_HASH = "groupHash"
    }

}