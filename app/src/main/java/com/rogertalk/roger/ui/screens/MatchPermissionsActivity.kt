package com.rogertalk.roger.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.models.state.ContactsScreenHolder
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.utils.extensions.hasContactsPermission
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logMethodCall
import kotlinx.android.synthetic.main.contacts_matching_screen.*
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

class MatchPermissionsActivity : EventAppCompatActivity(logOutIfUnauthorized = true),
        PermissionListener {

    companion object {

        private val EXTRA_FROM_LOBBY = "fromLobby"
        private val EXTRA_FROM_TALK_SCREEN = "fromTalkScreen"

        fun startLobbyScreen(context: Context, stream: Stream): Intent {
            // Save stream state for this screen
            ContactsScreenHolder.stream = stream
            val startIntent = Intent(context, MatchPermissionsActivity::class.java)
            startIntent.putExtra(EXTRA_FROM_LOBBY, true)
            return startIntent
        }

        fun startTalkScreen(context: Context): Intent {
            val startIntent = Intent(context, MatchPermissionsActivity::class.java)
            startIntent.putExtra(EXTRA_FROM_TALK_SCREEN, true)
            return startIntent
        }
    }

    // Extras
    private val cameFromLobby: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_FROM_LOBBY, false) }
    private val cameFromTalkScreen: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_FROM_TALK_SCREEN, false) }

    private var contactsPermissionListener: PermissionListener? = null

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_matching_screen)
        setupUI()
    }


    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }
        proceedToContactsScreen()
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        logMethodCall()
        token?.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        val permissionName = response?.permissionName ?: ""
        val permanentlyDenied = response?.isPermanentlyDenied ?: true
        logDebug { "Denied permission: $permissionName, permanently: $permanentlyDenied" }
        finish()
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun setupUI() {
        // Button presses
        givePermissionButton.setOnClickListener { giveMatchPermissionPressed() }
        doNotGivePermissionButton.setOnClickListener { doNotGiveMatchPermissionPressed() }
    }

    private fun proceedToContactsScreen() {
        // Track user choice
        EventTrackingManager.permissionToMatchContacts(PrefRepo.permissionToMatchContacts)

        if (cameFromLobby) {
            val stream = ContactsScreenHolder.stream
            if (stream == null) {
                logError(Exception("Could not find stream"))
                finish()
                return
            }

            // Construct the data necessary to move to contacts screen
            val participantIds = stream.othersOrEmpty.map { it.id }
            val currentMembersList = ArrayList<Long>(participantIds.size)
            currentMembersList.addAll(participantIds)
            startActivity(ContactsActivity.startEditGroup(this, stream.id, currentMembersList))
        } else {
            startActivity(ContactsActivity.startFromTalkScreen(this))
        }

        finish()
    }

    private fun giveMatchPermissionPressed() {
        PrefRepo.permissionToMatchContacts = true

        // Ask for the real contacts permission if necessary
        if (hasContactsPermission()) {
            proceedToContactsScreen()
        } else {
            // Ask for contact permission on Android M and above
            requestContactPermission()
        }
    }

    private fun doNotGiveMatchPermissionPressed() {
        finish()
    }


    private fun requestContactPermission() {
        val dialogOnDeniedPermissionListener =
                DialogOnDeniedPermissionListener.Builder.withContext(this)
                        .withTitle(R.string.perm_read_contact_title)
                        .withMessage(R.string.perm_read_contact_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        contactsPermissionListener = CompositePermissionListener(this,
                dialogOnDeniedPermissionListener)

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(contactsPermissionListener, Manifest.permission.READ_CONTACTS)
        }
    }
}
