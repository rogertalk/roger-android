package com.rogertalk.roger.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.R
import com.rogertalk.roger.android.tasks.DeviceContactQueryTask
import com.rogertalk.roger.event.broadcasts.ConnectivityChangedEvent
import com.rogertalk.roger.event.broadcasts.ReferrerInfoUpdatedEvent
import com.rogertalk.roger.event.failure.ChallengeFailEvent
import com.rogertalk.roger.event.success.ChallengeSuccessEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.models.holder.OnBoardingDataHolder
import com.rogertalk.roger.network.request.ChallengeRequest
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.screens.base.EventSnackActivity
import com.rogertalk.roger.utils.android.KeyboardUtils
import com.rogertalk.roger.utils.extensions.hasAudioRecordPermission
import com.rogertalk.roger.utils.extensions.hasContactsPermission
import com.rogertalk.roger.utils.extensions.materialize
import com.rogertalk.roger.utils.extensions.onEnterKey
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.phone.PhoneUtils
import kotlinx.android.synthetic.main.contact_info_screen.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.onLayoutChange
import org.jetbrains.anko.toast
import kotlin.LazyThreadSafetyMode.NONE


class AddContactInfoActivity : EventSnackActivity(),
        TextView.OnEditorActionListener,
        MultiplePermissionsListener {

    enum class ContactType {
        PHONE, EMAIL
    }

    companion object {

        private val EXTRA_CONTACT_TYPE = "contactType"
        private val EXTRA_IS_ON_BOARDING = "isOnBoarding"
        private val EXTRA_PREFILL_IDENTIFIER = "preFillIdentifier"
        private val EXTRA_RETURN_TO_CONTACTS = "returnToContacts"

        fun start(context: Context, contactType: ContactType): Intent {
            val startIntent = Intent(context, AddContactInfoActivity::class.java)
            startIntent.putExtra(EXTRA_CONTACT_TYPE, contactType)
            startIntent.putExtra(EXTRA_IS_ON_BOARDING, false)
            return startIntent
        }

        fun startFromContacts(context: Context): Intent {
            val startIntent = Intent(context, AddContactInfoActivity::class.java)
            startIntent.putExtra(EXTRA_CONTACT_TYPE, ContactType.PHONE)
            startIntent.putExtra(EXTRA_RETURN_TO_CONTACTS, true)
            startIntent.putExtra(EXTRA_IS_ON_BOARDING, false)
            return startIntent
        }

        /**
         * This should be used if we want the user to login again
         */
        fun startOnBoarding(context: Context): Intent {
            val startIntent = Intent(context, AddContactInfoActivity::class.java)
            startIntent.putExtra(EXTRA_IS_ON_BOARDING, true)
            return startIntent
        }
    }

    // Permissions
    private var multiplePermissionsListener: MultiplePermissionsListener? = null

    private val contactType: ContactType by lazy(NONE) { intent.getSerializableExtra(EXTRA_CONTACT_TYPE) as? ContactType ?: ContactType.PHONE }
    private val isOnBoarding: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_IS_ON_BOARDING, true) }
    private val returnToContacts: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_RETURN_TO_CONTACTS, false) }
    private val preFillIdentifier: String by lazy(NONE) { intent.getStringExtra(EXTRA_PREFILL_IDENTIFIER) ?: "" }

    private var contactInfoValue = ""
    private var currentMechanism = ContactType.PHONE

    private var phoneNumberId = ""
    private var formattedPhoneNumber = ""

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.contact_info_screen)

        handleIntent()
        warmUpContactQuery()
    }

    override fun onResume() {
        super.onResume()

        if (isOnBoarding) {
            handlePermissions()
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            KeyboardUtils.hideKeyboard(this)
            confirmNameButton.performClick()
            return true
        }
        return false
    }

    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
        if (permissions == null) {
            return
        }
        for (permission in permissions) {
            if (permission.name == Manifest.permission.RECORD_AUDIO) {
                token?.continuePermissionRequest()
            }
        }
    }

    /**
     * This is called after the user is done with selecting permissions.
     * The user might reach this state via 2 separate buttons, so logic is in place to know the
     * origin of user action.
     */
    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
        if (report == null) {
            return
        }

        val permissions = report.grantedPermissionResponses
        var microPermissionEnabled = false
        var smsPermissionEnabled = false
        var callPermissionEnabled = false
        var permissionsCount = 0

        for (permission in permissions) {
            val permissionName = permission.permissionName
            logDebug { "Enabled permission: $permissionName" }
            when (permissionName) {
                Manifest.permission.RECORD_AUDIO -> {
                    microPermissionEnabled = true
                    permissionsCount++
                }

                Manifest.permission.READ_PHONE_STATE -> {
                    permissionsCount++
                    callPermissionEnabled = true

                    tryToPrefillPhoneNumber()
                }

                Manifest.permission.RECEIVE_SMS -> {
                    permissionsCount++
                    smsPermissionEnabled = true
                }
            }
        }

        // Track what kind of permissions this user has agreed to
        EventTrackingManager.permissionsOnBoarding(permissionsCount,
                microPermissionEnabled, smsPermissionEnabled, callPermissionEnabled)
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun handlePermissions() {
        if (!hasAudioRecordPermission()) {
            requestPermissions()
        }
    }

    /**
     * If read contacts permission exists, begin to probe contacts immediately
     */
    private fun warmUpContactQuery() {
        if (hasContactsPermission()) {
            logDebug { "Contact permission granted, warming up contact cache" }
            DeviceContactQueryTask().execute()
        } else {
            logDebug { "No contact permission" }
        }
    }

    private fun handleIntent() {
        if (intent == null) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // pop-up keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // set material animation for previous versions of Android
        confirmNameButton.materialize(Color.BLACK)

        // Update current mechanism
        currentMechanism = contactType

        refreshLoginMechanismUI()

        if (isOnBoarding) {
            toggleEmailLogin.setOnClickListener { toggleLoginMechanism() }
        } else {
            toggleEmailLogin.beGone()
        }

        // Scroll view automatically if screen dimensions change
        parentScroll.onLayoutChange { view, a, b, c, d, e, f, g, h ->
            parentScroll.smoothScrollBy(0, 100)
        }

    }

    private fun refreshLoginMechanismUI() {
        if (currentMechanism == ContactType.EMAIL) {
            // We are trying to add a new E-mail address
            titleLabel.text = getString(R.string.contact_info_email_title)
            confirmNameButton.text = getString(R.string.contact_info_confirm_email)
            emailField.makeVisible()
            phoneNumberField.beGone()
            emailField.setOnEditorActionListener(this)
            emailField.onEnterKey { validateEmailField() }
            confirmNameButton.setOnClickListener { validateEmailField() }
            toggleEmailLogin.setText(R.string.toggle_login_use_phone)
        } else {
            // We are adding a new Phone Number
            titleLabel.text = getString(R.string.get_started_title)
            confirmNameButton.text = getString(R.string.button_confirm_number)
            emailField.beGone()
            phoneNumberField.makeVisible()

            // Pre-fill identifier if one is found
            if (preFillIdentifier.isNotBlank()) {
                phoneNumberField.setText(preFillIdentifier)
            } else {
                // Otherwise use either phone number or country code
                val ownPhoneNumber = PhoneUtils.getOwnPhoneNumber(this)
                if (ownPhoneNumber.isNotBlank()) {
                    phoneNumberField.setText(ownPhoneNumber)
                } else {
                    phoneNumberField.hint = PhoneUtils.getCountryExamplePhoneNumber(this)
                    phoneNumberField.setText(PhoneUtils.getCountryPrefix(this))
                    // pop-up keyboard if number was not auto-filled
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                }
            }

            // Put the input cursor at the end of the phone number field.
            phoneNumberField.setSelection(phoneNumberField.text.length)

            phoneNumberField.setOnEditorActionListener(this)
            phoneNumberField.onEnterKey { validatePhoneNumberField() }
            confirmNameButton.setOnClickListener { validatePhoneNumberField() }
            toggleEmailLogin.setText(R.string.toggle_login_use_email)
        }
    }

    private fun tryToPrefillPhoneNumber() {
        // Otherwise use either phone number or country code
        val ownPhoneNumber = PhoneUtils.getOwnPhoneNumber(this)
        if (ownPhoneNumber.isNotBlank()) {
            phoneNumberField.setText(ownPhoneNumber)
        }
    }

    private fun validateEmailField() {
        emailField.isEnabled = false

        val emailText = emailField.editableText.toString()
        if (emailText.isNullOrBlank()) {
            displayInvalidEmail()
            return
        }

        if (!emailText.contains("@")) {
            displayInvalidEmail()
            return
        }

        addContactInfo(emailText)
    }

    private fun validatePhoneNumberField() {
        phoneNumberId = phoneNumberField.text.toString()
        if (phoneNumberId.isNotBlank()) {

            // if this is a debug-build just go ahead. this is a quick way to unsure we can use
            // invalid numbers during testing
            if (BuildConfig.DEBUG) {
                formattedPhoneNumber = phoneNumberId
                addContactInfo(formattedPhoneNumber)
                return
            }

            formattedPhoneNumber = PhoneUtils.getProperPhoneNumber(phoneNumberId)

            if (formattedPhoneNumber.isEmpty()) {
                CommonDialog.simpleMessageWithButton(this,
                        getString(R.string.contact_info_invalid_title),
                        getString(R.string.contact_info_invalid_number),
                        getString(R.string.contact_info_invalid_button))
                return
            }

            phoneNumberField.isEnabled = false
            addContactInfo(formattedPhoneNumber)
        } else {
            toast(R.string.ob_phone_number_empty)
        }
    }

    private fun displayInvalidEmail() {
        emailField.isEnabled = true
        CommonDialog.simpleMessageWithButton(this,
                getString(R.string.contact_info_invalid_title),
                getString(R.string.contact_info_invalid_email),
                getString(R.string.contact_info_invalid_button))
    }

    private fun addContactInfo(contact: String) {
        setLoading(true)
        contactInfoValue = contact
        ChallengeRequest(contact).enqueueRequest()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            confirmNameButton.isEnabled = false
            confirmNameButton.text = ""
            progressWheel.makeVisible()
        } else {
            confirmNameButton.isEnabled = true
            confirmNameButton.text = getString(R.string.button_confirm_number)
            progressWheel.beGone()
        }
    }

    private fun toggleLoginMechanism() {
        if (currentMechanism == ContactType.PHONE) {
            currentMechanism = ContactType.EMAIL
        } else {
            currentMechanism = ContactType.PHONE
        }
        refreshLoginMechanismUI()
    }

    private fun requestPermissions() {
        val dialogOnDeniedPermissionListener =
                DialogOnAnyDeniedMultiplePermissionsListener.Builder.withContext(this)
                        .withTitle(R.string.perm_start_permissions_title)
                        .withMessage(R.string.perm_start_permissions_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        multiplePermissionsListener = CompositeMultiplePermissionsListener(this, dialogOnDeniedPermissionListener)

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermissions(multiplePermissionsListener,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO)
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onConnectivityChanged(event: ConnectivityChangedEvent) {
        evaluateConnectivityState()
    }

    @Subscribe(threadMode = MAIN)
    fun onChallengeSuccess(event: ChallengeSuccessEvent) {
        logEvent(event)
        if (returnToContacts) {
            logDebug { "Return to contact after" }
            startActivity(ValidationActivity.startFromContacts(this, contactInfoValue))
        } else {
            // We reached here either from Settings or OnBoarding (new and existing users)
            startActivity(ValidationActivity.start(this, contactInfoValue, isOnBoarding))
        }

        // This screen is no longer needed
        finish()
    }

    @Subscribe(threadMode = MAIN)
    fun onChallengeFail(event: ChallengeFailEvent) {
        setLoading(false)

        phoneNumberField.isEnabled = true
        emailField.isEnabled = true

        CommonDialog.simpleMessage(this,
                getString(R.string.challenge_request_failed_title),
                getString(R.string.check_internet_connection_message))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReferrerInfo(event: ReferrerInfoUpdatedEvent) {
        logEvent(event)
        OnBoardingDataHolder.possibleInviteToken = event.referrerInfo.inviteToken
        OnBoardingDataHolder.possibleParticipant = event.referrerInfo.publicProfile
    }

}