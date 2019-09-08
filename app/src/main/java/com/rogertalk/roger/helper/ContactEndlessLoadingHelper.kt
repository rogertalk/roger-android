package com.rogertalk.roger.helper

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.rogertalk.roger.network.request.NextStreamsRequest
import com.rogertalk.roger.ui.view.EndlessRecyclerOnScrollListener
import com.rogertalk.roger.utils.log.logDebug

class ContactEndlessLoadingHelper(conversationList: RecyclerView, val layoutManager: LinearLayoutManager) :
        EndlessRecyclerOnScrollListener(layoutManager) {


    init {
        conversationList.addOnScrollListener(this)
    }

    override fun onLoadMore(nextCursor: String) {
        logDebug { "Will load cursor: $nextCursor" }
        NextStreamsRequest(nextCursor).enqueueRequest()
    }

}
