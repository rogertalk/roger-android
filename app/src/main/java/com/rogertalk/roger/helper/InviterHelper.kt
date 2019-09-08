package com.rogertalk.roger.helper

import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.EventTrackingManager.InvitationMethod.MASS_INVITE
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.network.request.InviteIdentifierRequest
import com.rogertalk.roger.network.request.StreamAddParticipantsRequest
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.ui.screens.LobbyActivity
import java.util.*

class InviterHelper(val lobbyActivity: LobbyActivity) {


    var deviceContacts: ArrayList<DeviceContactInfo>

    var previousContact: DeviceContactInfo? = null
    var selectedContact: DeviceContactInfo? = null
    val random = Random()

    init {
        deviceContacts = getNonActiveContacts()
    }

    //
    // PUBLIC METHODS
    //

    init {
        nextPersonToInvite()
    }

    /**
     * Call this when creating new conversations, and Stream arrives asynchronously
     */
    fun refreshWhenStream() {
        if (selectedContact == null) {
            nextPersonToInvite()
        }
    }

    //
    // PRIVATE METHODS
    //

    fun invitePressed() {
        val streamId = lobbyActivity.streamId ?: return

        selectedContact?.let {
            val identifiersToInvite = it.aliases.mapTo(ArrayList<String>()) { it.value }
            val currentStream = lobbyActivity.stream ?: return

            // Send invite from the backend
            InviteIdentifierRequest(it.displayName, identifiersToInvite, currentStream.inviteToken).enqueueRequest()

            // Add participant to group as normal
            StreamAddParticipantsRequest(streamId, listOf(identifiersToInvite.first()),
                    listOf(it)).enqueueRequest()

            // Don't show this contact again
            deviceContacts.add(it)

            // Clear previous as it was just invited
            previousContact = null

            // Move to the next one
            nextPersonToInvite()

            // Track this event
            EventTrackingManager.invitation(MASS_INVITE)
        }
    }

    fun skipPressed() {
        // Don't show this contact again
        selectedContact?.let {
            deviceContacts.remove(it)

            // Make this the new previous
            previousContact = it
        }

        nextPersonToInvite()
        EventTrackingManager.dismissedInvite(true)
    }

    private fun getNonActiveContacts(): ArrayList<DeviceContactInfo> {
        return ArrayList(ContactMapRepo.getDeviceContactMap().values.filter { it.activeOnRoger == false })
    }

    private fun nextPersonToInvite() {
        val currentStream = lobbyActivity.stream
        if (currentStream == null) {
            selectedContact = null
            return
        }

        // No more contacts
        if (deviceContacts.isEmpty()) {
            selectedContact = null
            return
        }

        val chosenDeviceContact: DeviceContactInfo?
        val index = random.nextInt(deviceContacts.size)
        chosenDeviceContact = deviceContacts[index]

        // Don't present it the next time
        deviceContacts.remove(chosenDeviceContact)

        // Add to selected contact
        selectedContact = chosenDeviceContact

        // Circle the list if reached its end
        if (deviceContacts.isEmpty()) {
            deviceContacts = getNonActiveContacts()
        }
    }

    /**
     * @return DeviceContactInfo of the next person to invite.
     * Null if there are no further people to invite.
     */
    fun personToInvite(): DeviceContactInfo? {
        return selectedContact
    }

    fun hasPrevious(): Boolean {
        return previousContact != null
    }

    fun pressedPreviousContact() {
        selectedContact = previousContact
        previousContact = null
    }
}