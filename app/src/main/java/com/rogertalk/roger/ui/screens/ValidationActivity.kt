package com.rogertalk.roger.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.event.broadcasts.ConnectivityChangedEvent
import com.rogertalk.roger.event.failure.ChallengeSecretFailEvent
import com.rogertalk.roger.event.success.ChallengeSecretCodeSuccessEvent
import com.rogertalk.roger.event.success.SecretCodeEvent
import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.network.request.ChallengeRequest
import com.rogertalk.roger.network.request.ChallengeSecretRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.dialog.listeners.ChangeAccountListener
import com.rogertalk.roger.ui.screens.base.EventSnackActivity
import com.rogertalk.roger.ui.screens.talk.TalkActivityUtils
import com.rogertalk.roger.utils.android.KeyboardUtils
import com.rogertalk.roger.utils.extensions.materialize
import com.rogertalk.roger.utils.extensions.onEnterKey
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logEvent
import kotlinx.android.synthetic.main.validation_screen.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.longToast
import org.jetbrains.anko.onLayoutChange
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.net.HttpURLConnection
import kotlin.LazyThreadSafetyMode.NONE

class ValidationActivity : EventSnackActivity(),
        ChangeAccountListener,
        TextView.OnEditorActionListener {


    companion object {
        private val EXTRA_ADDRESS = "identifier"
        private val EXTRA_ON_BOARDING = "isOnBoarding"
        private val EXTRA_RETURN_TO_CONTACTS = "returnToContacts"

        private val RETRY_SECONDS = 90

        fun start(context: Context, contactAddress: String, isOnBoarding: Boolean): Intent {
            val startIntent = Intent(context, ValidationActivity::class.java)
            startIntent.putExtra(EXTRA_ADDRESS, contactAddress)
            startIntent.putExtra(EXTRA_ON_BOARDING, isOnBoarding)
            startIntent.putExtra(EXTRA_RETURN_TO_CONTACTS, false)
            return startIntent
        }

        fun startFromContacts(context: Context, contactAddress: String): Intent {
            val startIntent = Intent(context, ValidationActivity::class.java)
            startIntent.putExtra(EXTRA_ADDRESS, contactAddress)
            startIntent.putExtra(EXTRA_ON_BOARDING, false)
            startIntent.putExtra(EXTRA_RETURN_TO_CONTACTS, true)
            return startIntent
        }
    }

    private var startTime: Long = 0

    // Extras
    private val identifier: String by lazy(NONE) { intent.getStringExtra(EXTRA_ADDRESS) }
    private val isOnBoarding: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_ON_BOARDING, false) }
    private val returnToContacts: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_RETURN_TO_CONTACTS, false) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.validation_screen)
        startTime = System.currentTimeMillis() / 1000

        handleIntent()
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            confirmNameButton.performClick()
            return true
        }
        return false
    }

    /**
     * This is where the actual Account-Switching happens
     */
    override fun changeAccount(newSession: Session) {
        // Remove previous streams
        StreamCacheRepo.deleteLocalCache()

        SessionRepo.session = newSession

        proceedWithSession(newSession)
    }

    override fun keepCurrentAccount() {
        // If keeping current account, do not update anything, but still move back or to the proper
        // screen
        if (returnToContacts) {
            finish()
            return
        }

        if (isOnBoarding) {
            PrefRepo.pendingChooseNameNewUser = true
            startActivity(TalkActivityUtils.getStartTalkScreen(this, justOnBoarded = true))
            finish()
        } else {
            finish()
        }
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun handleIntent() {
        if (intent == null) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // set material animation for previous versions of Android
        confirmNameButton.materialize(Color.BLACK)

        // display title
        titleText.text = R.string.verification_code_description.stringResource(identifier)

        confirmNameButton.setOnClickListener { submitPressed() }

        codeField.setOnEditorActionListener(this)
        codeField.onEnterKey { submitPressed() }

        callMeTextView.setOnClickListener {
            val difference = System.currentTimeMillis() / 1000 - startTime
            if (difference < RETRY_SECONDS) {
                val message = String.format(getString(R.string.wait_before_call), "${RETRY_SECONDS - difference}")
                CommonDialog.simpleMessage(this, getString(R.string.contact_info_invalid_title), message)
            } else {
                ChallengeRequest(identifier, true).enqueueRequest()
                callMeTextView.textColor = R.color.ob_medium_grey
                callMeTextView.isClickable = false
                callMeTextView.text = getString(R.string.wait_for_call)
            }
        }

        // Scroll view automatically if screen dimensions change
        parentScroll.onLayoutChange { view, a, b, c, d, e, f, g, h ->
            parentScroll.smoothScrollBy(0, 100)
        }

    }

    private fun submitPressed() {
        val secretCode = codeField.text.toString()
        if (secretCode.length == 3) {
            setLoading(true)
            ChallengeSecretRequest(identifier, secretCode).enqueueRequest()
            KeyboardUtils.hideKeyboard(this)
        } else {
            toast(R.string.ob_wrong_code)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        confirmNameButton.setLoadingState(isLoading)
        if (isLoading) {
            progressWheel.makeVisible()
        } else {
            progressWheel.beGone()
        }
    }

    /**
     * Called if registered via `instant signup` and got here because had no contact info yet
     */
    private fun handleOnBoardingFromContactsSuccess(session: Session) {
        logDebug { "Added contact info with success!" }

        // Update Session and Streams
        UserAccountRepo.updateAccount(session.account)
        StreamCacheRepo.updateCache(session.streams)

        finish()
    }

    /**
     * Called after getting a new session data
     */
    private fun proceedWithSession(session: Session) {
        if (returnToContacts) {
            handleOnBoardingFromContactsSuccess(session)
            return
        }

        // We are OnBoarding then
        SessionRepo.session = session
        StreamCacheRepo.updateCache(session.streams)

        // Update pending choose name flag so that we resume into that screen if the user exists the app
        if (!PrefRepo.completedOnboarding) {
            PrefRepo.pendingChooseNameNewUser = true
            startActivity(NameSetupActivity.startOnBoarding(this))
        }
        finish()
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onConnectivityChanged(event: ConnectivityChangedEvent) {
        logEvent(event)
        evaluateConnectivityState()
    }

    @Subscribe(threadMode = MAIN)
    fun onChallengeSecretCodeSuccess(event: ChallengeSecretCodeSuccessEvent) {
        logEvent(event)
        val newSession = event.session
        val oldAccountId = UserAccountRepo.id()

        // There was no old account set
        if (oldAccountId == null) {
            proceedWithSession(newSession)
            return
        }

        if (oldAccountId != newSession.account.id) {
            // This is a new account, as user to switch accounts
            CommonDialog.changeAccountDialog(this, this, newSession, identifier)
            return
        }

        proceedWithSession(newSession)
    }

    @Subscribe(threadMode = MAIN)
    fun onSecretCodeReceived(event: SecretCodeEvent) {
        codeField.setText(event.secretCode)
        codeField.setSelection(event.secretCode.length)
        confirmNameButton.callOnClick()
    }

    @Subscribe(threadMode = MAIN)
    fun onChallengeSecretFail(event: ChallengeSecretFailEvent) {
        if (event.code == HttpURLConnection.HTTP_FORBIDDEN) {
            longToast(R.string.error_generic_403)
        } else if (event.code == HttpURLConnection.HTTP_BAD_REQUEST) {
            longToast(R.string.error_wrong_code)
        } else {
            longToast(R.string.ob_failed_request)
        }
        setLoading(false)
    }
}