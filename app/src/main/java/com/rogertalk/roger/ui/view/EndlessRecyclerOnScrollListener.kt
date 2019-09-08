package com.rogertalk.roger.ui.view

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.rogertalk.roger.repo.StreamCacheRepo

abstract class EndlessRecyclerOnScrollListener(private val mLinearLayoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {

    private var previousTotal = 0

    // Start True since we'll wait for the first data to arrive
    private var loadingValue = true
    private var loadedFirstValuesOnce = false

    val loading: Boolean
        get() = loadingValue

    internal var firstVisibleItem: Int = 0
    internal var visibleItemCount: Int = 0
    internal var totalItemCount: Int = 0

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val cursor = StreamCacheRepo.nextCursor
        if (cursor == null) {
            loadingValue = false
            return
        }
        if (StreamCacheRepo.reachedListEnd) {
            return
        }

        visibleItemCount = recyclerView!!.childCount
        totalItemCount = mLinearLayoutManager.itemCount
        firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()

        if (loadingValue) {
            if (totalItemCount > previousTotal) {
                loadingValue = false
                previousTotal = totalItemCount
            }
        }
        if (!loadingValue && totalItemCount - visibleItemCount <= firstVisibleItem) {
            // End has been reached

            onLoadMore(cursor)

            loadingValue = true
        }
    }

    fun finishedLoadingNext() {
        loadingValue = false
    }

    fun handleStreamsLoaded() {
        if (!loadedFirstValuesOnce) {
            loadedFirstValuesOnce = true
            loadingValue = false
        }
    }

    abstract fun onLoadMore(nextCursor: String)
}
