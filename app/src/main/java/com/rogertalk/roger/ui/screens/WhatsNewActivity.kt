package com.rogertalk.roger.ui.screens

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.rogertalk.roger.R
import com.rogertalk.roger.ui.adapters.ChangeLogAdapter
import com.rogertalk.roger.ui.screens.base.BaseAppCompatActivity
import com.rogertalk.roger.utils.changelog.ChangeLogHistory
import kotlinx.android.synthetic.main.whats_new_screen.*

class WhatsNewActivity : BaseAppCompatActivity() {


    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.whats_new_screen)
        setupUI()
    }

    //
    // PRIVATE METHODS
    //


    private fun setupUI() {
        setupToolbar()

        // List with the actual changes
        changesList.layoutManager = LinearLayoutManager(this)
        changesList.adapter = ChangeLogAdapter(ChangeLogHistory.changeLogHistory())
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_whats_new)

        toolbar.setNavigationOnClickListener({
            finish()
        })
    }
}