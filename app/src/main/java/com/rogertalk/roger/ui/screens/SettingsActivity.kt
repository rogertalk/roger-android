package com.rogertalk.roger.ui.screens

import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.isSafeChooser
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.kotlinjubatus.utils.AppStatsUtils
import com.rogertalk.kotlinjubatus.utils.DeviceUtils
import com.rogertalk.roger.R
import com.rogertalk.roger.event.failure.DisplayNameUpdateFailEvent
import com.rogertalk.roger.event.failure.ShareLocationFailEvent
import com.rogertalk.roger.event.failure.UsernameUpdateFailEvent
import com.rogertalk.roger.event.success.DisplayNameUpdateSuccessEvent
import com.rogertalk.roger.event.success.ShareLocationSuccessEvent
import com.rogertalk.roger.event.success.UsernameUpdateSuccessEvent
import com.rogertalk.roger.helper.PhotoSettingUIHelper
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.holder.ImagePickHolder
import com.rogertalk.roger.models.json.Bot
import com.rogertalk.roger.network.request.UpdateDisplayNameRequest
import com.rogertalk.roger.network.request.UpdateShareLocationRequest
import com.rogertalk.roger.network.request.UpdateUsernameRequest
import com.rogertalk.roger.repo.*
import com.rogertalk.roger.ui.dialog.CommonDialog
import com.rogertalk.roger.ui.dialog.VoicemailDialog
import com.rogertalk.roger.ui.dialog.listeners.ContactInfoSelectionListener
import com.rogertalk.roger.ui.dialog.listeners.SettingsDataCheckListener
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.android.KeyboardUtils
import com.rogertalk.roger.utils.android.ShareUtils
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.extensions.getStatusBarHeight
import com.rogertalk.roger.utils.extensions.onEnterKey
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.log.logWarn
import kotlinx.android.synthetic.main.settings_screen.*
import kotlinx.android.synthetic.main.settings_screen_content.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.*
import java.net.HttpURLConnection.HTTP_CONFLICT
import kotlin.LazyThreadSafetyMode.NONE

class SettingsActivity : EventAppCompatActivity(true),
        TextView.OnEditorActionListener,
        ContactInfoSelectionListener,
        SettingsDataCheckListener,
        ObservableScrollViewCallbacks {

    companion object {
        fun start(context: Context): Intent {
            val startIntent = Intent(context, SettingsActivity::class.java)
            return startIntent
        }
    }

    private val minHeaderHeight: Int by lazy(NONE) { dimen(R.dimen.settings_header_min_height) }
    private val maxHeaderHeight: Int by lazy(NONE) { dimen(R.dimen.settings_header_height) }
    private val contentTranslateThreshold: Int by lazy(NONE) { dimen(R.dimen.settings_header_height_translate_threshold) }

    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }
    private var firstRun = true
    private val settingsShadowSize: Float by lazy(NONE) { resources.getDimensionPixelSize(R.dimen.elevation_l1).toFloat() }
    private val photoUiHelper: PhotoSettingUIHelper by lazy(NONE) { PhotoSettingUIHelper(this) }

    private var checkedDataOnce = false

    private var experimentalToggleCount = 0
    private var shownCopyNotification = false

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_screen)

        setupUI()
        refreshUI()
    }

    override fun onResume() {
        super.onResume()

        // If resuming from somewhere else, refresh data
        if (!firstRun) {
            loadPictureFromMemory()
            refreshUI()
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (v == null) {
            return false
        }

        if (actionId == EditorInfo.IME_ACTION_DONE) {
            KeyboardUtils.hideKeyboard(this)

            progressDialogHelper.showWaiting()

            when (v.id) {
                R.id.usernameEdit -> saveUsername()
                R.id.displayName -> saveDisplayName()
            }

            return true
        }
        return false
    }

    override fun chooseNewNumber() {
        startActivity(AddContactInfoActivity.start(this, AddContactInfoActivity.ContactType.PHONE))
    }

    override fun chooseNewEmail() {
        startActivity(AddContactInfoActivity.start(this, AddContactInfoActivity.ContactType.EMAIL))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            photoUiHelper.handleOnActivityResult(requestCode, resultCode, data)
        }
    }

    override fun saveUsernameChange() {
        progressDialogHelper.showWaiting()
        saveUsername()
    }

    override fun saveDisplayNameChange() {
        progressDialogHelper.showWaiting()
        saveDisplayName()
    }

    //
    // OBSERVABLE SCROLL VIEW METHODS
    //

    @TargetApi(21)
    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
        val maxScrollY = (maxHeaderHeight - minHeaderHeight).toFloat()
        val scrollThreshold = (maxHeaderHeight - contentTranslateThreshold).toFloat()

        // Configure properties to "animate" based on scroll status
        var displayNameTranslation = 0f
        var headerTranslation = 0f
        val alpha = (maxScrollY - scrollY) / maxScrollY
        if (scrollY >= maxScrollY) {
            headerTranslation = -maxScrollY
            displayNameTranslation = maxScrollY - scrollThreshold
        } else if (scrollY > 0) {
            if (scrollY >= scrollThreshold) {
                displayNameTranslation = scrollY - scrollThreshold
            }

            headerTranslation = -scrollY.toFloat()
        }

        // Set properties that react to scrolling
        avatarLayoutContainer.alpha = alpha
        username.alpha = alpha

        // Draw shadow; becomes more noticeable as the users scrolls down
        if (AndroidVersion.fromApiVal(LOLLIPOP, true)) {
            var headerElevation = headerTranslation / (-280) * settingsShadowSize
            if (headerElevation < 0) {
                headerElevation = 0f
            }
            header.elevation = headerElevation
        }

        header.translationY = headerTranslation
        displayName.translationY = displayNameTranslation

        // Layout everything
        header.requestLayout()
    }

    override fun onDownMotionEvent() {
    }

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {
    }

    override fun onBackPressed() {
        if (handleBackAction()) {
            super.onBackPressed()
        }
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    /**
     * @return True if back will successfully navigate to previous app screen,
     *  False if we still have stuff to do
     */
    private fun handleBackAction(): Boolean {
        if (checkChangedData()) {
            return false
        }

        supportFinishAfterTransition()
        return true
    }

    /**
     * Check if user changed data, and offer to save before exit.
     * Return true if handled, false otherwise.
     */
    private fun checkChangedData(): Boolean {
        // Don't check twice in a row
        if (checkedDataOnce) {
            return false
        }
        checkedDataOnce = true

        // Check Username
        val userAccount = UserAccountRepo.current()
        val newUsername = usernameEdit.text.toString().trim()
        if (userAccount != null && newUsername.isNotBlank()) {
            val savedUsername = userAccount.username ?: ""

            val usernameChanged = newUsername != savedUsername
            if (usernameChanged) {
                CommonDialog.usernameEditedDialog(this, newUsername, this)
                return true
            }
        }

        // Check Display Name
        val newDisplayName = displayName.text.toString().trim()
        if (userAccount != null && newDisplayName.isNotBlank()) {
            val savedDisplayName = userAccount.customDisplayName ?: ""

            val displayNameChanged = newDisplayName != savedDisplayName
            if (displayNameChanged) {
                CommonDialog.displayNameEditedDialog(this, newDisplayName, this)
                return true
            }
        }

        return false
    }

    private fun setupUI() {
        // App version on footer
        val appVersionDescription = getString(R.string.app_name) + " " + AppStatsUtils.getAppVersionCode(this)
        appVersionLabel.text = appVersionDescription
        appVersion.contentDescription = appVersionDescription

        setupToolbar()

        // Account for status bar at the top
        val statusBarHeight = getStatusBarHeight()
        val params = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, statusBarHeight)
        params.topMargin = -statusBarHeight
        statusBarCover.layoutParams = params

        // Scrolling change callbacks
        observableScrollView.setScrollViewCallbacks(this)

        // Click listeners
        rateUs.setOnClickListener { rateUsPressed() }
        emailUs.setOnClickListener { emailUsPressed() }
        appVersion.setOnClickListener { copyAppVersionPressed() }
        addAccountInfo.setOnClickListener { addAccountInfoPressed() }
        userPhotoOverlay.setOnClickListener { photoUiHelper.choosePhotoSourcePressed() }
        infoLabel.setOnClickListener { goToInfoPressed() }
        invitePeople.setOnClickListener { invitePeoplePressed() }
        whatsNewButton.setOnClickListener { whatsNewPressed() }
        faq.setOnClickListener { goToInfoPressed() }
        creditsButton.setOnClickListener { creditsPressed() }

        // List for changes on edit texts
        usernameEdit.setOnEditorActionListener(this)
        usernameEdit.onEnterKey {
            KeyboardUtils.hideKeyboard(this)
            progressDialogHelper.showWaiting()
            saveUsername()
        }
        displayName.setOnEditorActionListener(this)

        // Render Advanced if enabled
        if (PrefRepo.godMode) {
            advancedButton.makeVisible()
            advancedButton.setOnClickListener { advancedPressed() }
        }
    }

    private fun listenForWeatherChanges() {
        weatherToggle.onCheckedChange { compoundButton, newState ->
            if (!newState) {
                progressDialogHelper.showWaiting()
                UpdateShareLocationRequest(false).enqueueRequest()

            } else {
                startActivity<WeatherEnableActivity>()
            }
        }
    }

    private fun listenForTalkHeadsChanges() {
        talkHeadToggle.onCheckedChange { compoundButton, newState ->
            PrefRepo.talkHeads = newState
            EventTrackingManager.talkHeadChange(changedTo = newState)
        }
    }

    private fun listenForAutoplayChanges() {
        liveToggle.onCheckedChange { compoundButton, newState ->
            PrefRepo.livePlayback = newState

            clearTalkHeadsToggleListener()
            if (newState) {
                talkHeadToggle.enabled = false
            } else {
                talkHeadToggle.enabled = true
                listenForTalkHeadsChanges()
            }
            renderTalkHeads()
        }
    }

    private fun clearWeatherToggleListener() {
        weatherToggle.setOnCheckedChangeListener(null)
    }

    private fun clearAutoplayToggleListener() {
        liveToggle.setOnCheckedChangeListener(null)
    }

    private fun clearTalkHeadsToggleListener() {
        talkHeadToggle.setOnCheckedChangeListener(null)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        // This fixes the toolbar displacement on the shitty Samsung Duos
        toolbar.setPadding(0, getStatusBarHeight(), 0, 0)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = ""
        supportActionBar?.title = ""

        toolbar.setNavigationOnClickListener({
            onBackPressed()
        })
    }

    /**
     * Load avatar picture from memory so it gets displayed immediately
     */
    private fun loadPictureFromMemory() {
        val image = ImagePickHolder.croppedImage ?: return
        RoundImageUtils.createRoundImage(this, userPhoto, image,
                AvatarSize.CONTACT)
    }

    private fun refreshUI() {
        // Refresh avatar
        val profileImage = UserAccountRepo.current()?.imageURL ?: ""
        if (profileImage.isNotBlank()) {
            RoundImageUtils.createRoundImage(this, userPhoto, profileImage,
                    AvatarSize.CONTACT)
        }

        // Refresh user's display name and username
        val displayNameVal = UserAccountRepo.current()?.displayName ?: getString(R.string.settings_add_name)
        val usernameVal = UserAccountRepo.current()?.username ?: ""

        if (!displayNameVal.startsWith("+")) {
            displayName.setText(displayNameVal)
        }

        if (usernameVal.isNotBlank()) {
            username.text = "@" + usernameVal
            usernameEdit.setText(usernameVal)
            val userAccount = UserAccountRepo.current()
            userAccount?.let {
                reachUserLabel.text = getString(R.string.username_set_explanation_prefix) + " ${ShareUtils.getShortProfileUrl(it)}"
            }

        } else {
            username.text = UserAccountRepo.current()?.phoneNumber ?: ""
            reachUserLabel.text = getString(R.string.username_unset_explanation_prefix)
        }

        // There can be various account info elements
        fillAccountInfo()

        fillServicesInfo()

        // Past this point, this is not first run anymore
        firstRun = false

        // Update Glimpses toggle display
        clearWeatherToggleListener()
        weatherToggle.isChecked = LocationRepo.locationEnabled
        listenForWeatherChanges()

        // Update TalkHeads toggle
        clearTalkHeadsToggleListener()
        renderTalkHeads()
        listenForTalkHeadsChanges()

        // Update Live toggle
        clearAutoplayToggleListener()
        liveToggle.isChecked = PrefRepo.livePlayback
        listenForAutoplayChanges()
    }

    private fun renderTalkHeads() {
        talkHeadToggle.isChecked = PrefRepo.talkHeads
    }

    private fun fillAccountInfo() {
        // Clear previous elements
        accountInfoContainer.removeAllViewsInLayout()
        val username = UserAccountRepo.current()?.username ?: ""

        // get all aliases except the one that's equal to username
        val aliases = (UserAccountRepo.current()?.identifiers ?: emptyList<String>()).filter { it != username }

        // build list
        for (alias in aliases) {
            val accountElem = layoutInflater.inflate(R.layout.settings_account_elem, accountInfoContainer, false)
            val accountLabel = accountElem.findViewById(R.id.accountLabel) as TextView
            accountLabel.text = alias

            accountInfoContainer.addView(accountElem)
        }
    }

    private fun fillServicesInfo() {
        // Clear previous elements
        servicesContainer.removeAllViewsInLayout()
        val serviceList = BotCacheManager.servicesList

        if (serviceList.isEmpty()) {
            // There are no cached services, don't display the section
            servicesContainer.beGone()
            servicesTitle.beGone()
            return
        }

        // build list
        for (service in serviceList) {
            val serviceElem = layoutInflater.inflate(R.layout.service_entry_elem, servicesContainer, false)
            val serviceNameLabel = serviceElem.findViewById(R.id.serviceNameLabel) as TextView
            val serviceDescriptionLabel = serviceElem.findViewById(R.id.serviceDescriptionLabel) as TextView
            val serviceAvatar = serviceElem.findViewById(R.id.serviceAvatar) as ImageView
            val serviceParentView = serviceElem.findViewById(R.id.serviceTopElem) as LinearLayout

            if (service.imageURL != null) {
                RoundImageUtils.createRoundImage(this, serviceAvatar, service.imageURL)
            }
            serviceNameLabel.text = service.title
            serviceDescriptionLabel.text = service.description

            serviceParentView.setOnClickListener {
                handleServicePressed(service)
            }

            // Add to container
            servicesContainer.addView(serviceElem)
        }
    }

    private fun handleServicePressed(service: Bot) {
        if (service.clientCode) {
            when (service.nameId) {
                "voicemail" -> {
                    VoicemailDialog.show(this)
                }
            }

            return
        }
        service.connectURL?.let {
            ShareUtils.openExternalLink(this, it)
        }
    }

    private fun advancedPressed() {
        val startIntent = Intent(this, AdvancedOptionsActivity::class.java)
        startActivity(startIntent)
    }

    private fun goToInfoPressed() {
        ShareUtils.openExternalLink(this, RogerConstants.HELP_WEBPAGE_URL)
    }

    private fun whatsNewPressed() {
        EventTrackingManager.pressedChangelogHistory()

        startActivity<WhatsNewActivity>()
    }

    private fun creditsPressed() {
        EventTrackingManager.pressedCredits()

        LibsBuilder()
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutIconShown(true)
                .withActivityTitle(getString(R.string.settings_credits))
                .withAboutDescription("Open-Source libraries")
                .withAboutAppName("Roger\nMade with ${EmojiUtils.bigHeart} in NYC")
                .withAboutVersionShown(true)
                .withLicenseShown(true)
                .start(this)
    }

    private fun invitePeoplePressed() {
        val userAccount = UserAccountRepo.current() ?: return
        val shareText = getString(R.string.settings_invitation_text) +
                " ${ShareUtils.getCompleteShareUrl(userAccount, isOpenGroup = false)}"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        if (shareIntent.isSafeChooser(this)) {
            EventTrackingManager.invitation(EventTrackingManager.InvitationMethod.SETTINGS_GENERIC_INVITATION)

            startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_invitation_chooser)))
        } else {
            logWarn { "No apps for sharing" }
        }
    }

    private fun saveUsername() {
        val username = usernameEdit.text.toString().trim()

        // Remove focus from this EditText
        usernameEdit.clearFocus()

        if (username.isBlank()) {
            progressDialogHelper.dismiss()
            toast(R.string.error_username_cannot_be_black)
            return
        }

        UpdateUsernameRequest(username).enqueueRequest()
    }

    private fun saveDisplayName() {
        val textName = displayName.text.toString().trim()

        // remove focus from this EditText
        displayName.clearFocus()

        if (textName.isBlank()) {
            progressDialogHelper.dismiss()
            toast(R.string.error_name_cannot_be_black)
            return
        }

        UpdateDisplayNameRequest(textName).enqueueRequest()
    }

    private fun addAccountInfoPressed() {
        CommonDialog.contactInfoChoice(this, this)
    }

    private fun rateUsPressed() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RogerConstants.PLAY_STORE_LINK))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logError(e) { "Play Store not found" }
            longToast(R.string.play_store_not_found)
        }
    }

    private fun emailUsPressed() {
        val buttonText = getString(R.string.email_us)
        CommonDialog.settingsCTADialog(this, buttonText, { emailUsAction() }, { goToInfoPressed() })
    }

    private fun emailUsAction() {
        val osVersion = Build.VERSION.SDK_INT
        val model = DeviceUtils.deviceName
        val version = AppStatsUtils.getAppVersionCode(this)
        val accountID = SessionRepo.sessionId()

        var emailText = "\n\n\n\n"
        emailText += "--"
        emailText += "\nOS Version: $osVersion"
        emailText += "\nModel: $model"
        emailText += "\nRoger version: $version"
        emailText += "\nAccount Id: $accountID"

        // User has enabled advanced features, let us know about those
        if (PrefRepo.godMode) {
            emailText += "\nStereo Recording: ${ClearTextPrefRepo.stereoRecording}"
            emailText += "\nForce disable NS: ${ClearTextPrefRepo.disableNoiseSuppression}"
        }

        val subject = getString(R.string.settings_email_subject)
        val emailHandled = email(RogerConstants.SUPPORT_EMAIL, subject, emailText)

        if (!emailHandled) {
            // No app to handle e-mail, copy e-mail to clipboard and show toast instead
            longToast(R.string.copied_email)

            val clip = ClipData.newPlainText("Roger Email", RogerConstants.SUPPORT_EMAIL)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = clip
        }
    }

    private fun copyAppVersionPressed() {
        experimentalToggleCount++
        val clip = ClipData.newPlainText("Roger Version", AppStatsUtils.getAppVersionCode(this))
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = clip

        if (experimentalToggleCount != 10) {
            if (!shownCopyNotification) {
                longToast(R.string.copied_app_version)
                shownCopyNotification = true
            }
        } else {
            longToast("Experimental mode ON")
            EventTrackingManager.enabledExperimentalMode()
            PrefRepo.godMode = true
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onDisplayNameUpdated(event: DisplayNameUpdateSuccessEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()

        // Update account info
        UserAccountRepo.updateAccount(event.account)

        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onDisplayNameFail(event: DisplayNameUpdateFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        toast(R.string.ob_failed_request)
    }

    @Subscribe(threadMode = MAIN)
    fun onUsernameSet(event: UsernameUpdateSuccessEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()

        UserAccountRepo.updateAccount(event.account)
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onUsernameSetFail(event: UsernameUpdateFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        if (event.httpCode == HTTP_CONFLICT) {
            CommonDialog.simpleMessage(this,
                    getString(R.string.settings_username_invalid_taken_title),
                    getString(R.string.settings_username_conflict_description))
        } else {
            CommonDialog.simpleMessage(this,
                    getString(R.string.settings_username_invalid_chars_title),
                    getString(R.string.settings_username_invalid_chars_description))
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateShareLocationSuccess(event: ShareLocationSuccessEvent) {
        UserAccountRepo.updateAccount(event.account)
        progressDialogHelper.dismiss()
        refreshUI()
    }

    @Subscribe(threadMode = MAIN)
    fun onUpdateShareLocationFailure(event: ShareLocationFailEvent) {
        progressDialogHelper.dismiss()
        toast(R.string.ob_failed_request)
    }
}