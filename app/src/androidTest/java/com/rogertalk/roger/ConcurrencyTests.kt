package com.rogertalk.roger

import android.test.ActivityInstrumentationTestCase2
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.cache.StreamCache
import java.util.*

class ConcurrencyTests : ActivityInstrumentationTestCase2<TalkActivity>(TalkActivity::class.java) {

    private var mainActivity: TalkActivity? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mainActivity = activity
    }

    /**
     * Test concurrent access to streams cache
     */
    fun test_ConcurrentAccessToStreamCache() {
        var newStreamCacheList: ArrayList<Stream>

        // Update stream from multiple threads at the same time
        val threadsList = ArrayList<Thread>(20)
        for (i in 0..20) {
            newStreamCacheList = ArrayList<Stream>(10)
            for (j in 0..9) {
                newStreamCacheList.add(Stream())
            }

            val tt = Thread()
            tt.run {
                StreamCacheRepo.updateCache(newStreamCacheList)
                newStreamCacheList.add(Stream())
                val recoveredStuff = StreamCache.recoverFromPersistedStorage()
                val size = recoveredStuff?.size ?: 0
                assertEquals(size, 10)
            }

            tt.start()

            threadsList.add(tt)
        }

        // Wait for them to complete
        for (thread in threadsList) {
            thread.join()
        }

    }

}