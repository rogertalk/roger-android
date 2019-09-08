package com.rogertalk.roger.ui.frags

import android.os.Bundle
import android.preference.PreferenceFragment
import com.rogertalk.roger.R

class AdvancedPreferencesFrag : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.advanced_preferences)
    }
}