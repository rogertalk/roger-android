package com.rogertalk.roger.ui.screens

import android.app.Activity
import android.os.Bundle
import com.rogertalk.roger.ui.frags.AdvancedPreferencesFrag

class AdvancedOptionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        fragmentManager.beginTransaction().replace(android.R.id.content, AdvancedPreferencesFrag()).commit()
    }
}