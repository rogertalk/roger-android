package com.rogertalk.roger.realm.explorer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*

class ModelSelectionActivity : Activity(), AdapterView.OnItemClickListener {

    var emptyView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView = FrameLayout(this)
        setContentView(contentView)

        // Set up List Adapter
        val listItemLayout = android.R.layout.simple_list_item_1
        val modelNames = listOf("CachedAudio", "PendingChunkUpload")
        val adapter = ArrayAdapter(this, listItemLayout, modelNames)

        // Set up List View
        val listView = ListView(this)
        listView.adapter = adapter
        listView.emptyView = getEmptyView()
        listView.onItemClickListener = this

        // Add View Elements to Activity Container
        contentView.addView(listView)
        contentView.addView(emptyView)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        startActivity(Intent(this, DisplayModelActivity::class.java))
    }

    private fun getEmptyView(): View? {
        emptyView = TextView(this)
        emptyView?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        emptyView?.text = "Nothing to display"
        return emptyView
    }
}
