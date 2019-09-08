package com.rogertalk.roger.models.realm

import com.rogertalk.roger.utils.constant.NO_DURATION
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.NO_TIME
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass


@RealmClass
open class PendingChunkUpload(
        open var streamId: Long = NO_ID,
        open var accountId: Long = NO_ID,
        open var duration: Int = NO_DURATION,
        open var retries: Int = 0,
        open var createdAt: Long = NO_TIME,
        @PrimaryKey open var savedPath: String = "") : RealmObject() {

    companion object {
        val COLUMN_STREAM_ID = "streamId"
        val COLUMN_ACCOUNT_ID = "accountId"
        val COLUMN_DURATION = "duration"
        val COLUMN_RETRIES = "retries"
        val COLUMN_CREATED_AT = "createdAt"
        val COLUMN_SAVED_PATH = "savedPath"
    }
}