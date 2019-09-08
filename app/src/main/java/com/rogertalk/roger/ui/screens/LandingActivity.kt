package com.rogertalk.roger.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import com.rogertalk.kotlinjubatus.openWebpageIntent
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.log.logDebug
import kotlinx.android.synthetic.main.landing_screen.*

class LandingActivity : Activity() {

    companion object {

        fun start(context: Context): Intent {
            val startIntent = Intent(context, LandingActivity::class.java)
            return startIntent
        }
    }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        probeAccessibility()

        if (handleAppInitialization()) {
            // All redirected to new screen, don't do anything else
            return
        }

        setContentView(R.layout.landing_screen)

        setupUI()
    }

    //
    // PUBLIC METHODS
    //


    //
    // PRIVATE METHODS
    //

    private fun setupUI() {
        termsAndServicesButton.paintFlags = termsAndServicesButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        termsAndServicesButton.setOnClickListener { openWebpageIntent(RogerConstants.LEGAL_WEBPAGE_URL) }

        getStartedButton.setOnClickListener {
            startActivity(AddContactInfoActivity.startOnBoarding(this))
            finish()
        }
    }

    /**
     * @return True if handled and redirected to a different screen. False otherwise
     */
    private fun handleAppInitialization(): Boolean {
        if (SessionRepo.loggedIn()) {

            if (PrefRepo.pendingChooseNameNewUser) {
                startActivity(NameSetupActivity.startOnBoarding(this))
                finish()
                return true
            }

            // if already logged in, skip on-boarding
            startActivity(TalkActivityUtils.getStartTalkScreen(this))

            // stop loading this screen
            finish()
            return true
        } else {
            if (PrefRepo.loggedIn) {
                // User was logged in, but lost access token. stay put but reset flag
                PrefRepo.loggedIn = false
            }
        }
        return false
    }

    private fun probeAccessibility() {
        if (AccessibilityUtils.isScreenReaderActive(this)) {
            logDebug { "User is using accessibility" }
            EventTrackingManager.usingScreenReader()
        } else {
            logDebug { "User is NOT using accessibility" }
        }
    }
}