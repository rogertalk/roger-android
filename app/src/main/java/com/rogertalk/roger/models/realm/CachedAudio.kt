package com.rogertalk.roger.models.realm

import com.rogertalk.roger.utils.constant.NO_ID
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import java.util.*


@RealmClass
open class CachedAudio(
        open var chunkID: Long = NO_ID,
        open var streamID: Long = NO_ID,
        open var audioURL: String = "",
        open var persistedAt: Date = Date(),
        open var persistPath: String = "") : RealmObject()