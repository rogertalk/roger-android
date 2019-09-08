package com.rogertalk.roger.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.balysv.materialripple.MaterialRippleLayout
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.android.AppController
import com.rogertalk.roger.models.data.ContactListSection
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.data.SectionPosition
import com.rogertalk.roger.models.json.Profile
import com.rogertalk.roger.repo.ContactMapRepo
import com.rogertalk.roger.ui.adapters.listener.ContactPicker
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.initial
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.misc.NameUtils
import kotlinx.android.synthetic.main.contacts_section_elem.view.*
import kotlinx.android.synthetic.main.device_contact_elem.view.*
import org.jetbrains.anko.textColor
import java.util.*

class ContactPickerAdapter(private var originalContacts: List<DeviceContactInfo>,
                           private val listener: ContactPicker,
                           displayPermissionElement: Boolean,
                           val searchingByHandle: Boolean) :
        RecyclerView.Adapter<BaseViewHolder>() {

    companion object {
        private val SECTION_CONTACTS_IDX = 0
        private val SECTION_SEARCH_IDX = 1
    }

    // We maintain a list of cached profiles for using with the user search
    private var matchedProfile: Profile? = null
    private var sectionsList = LinkedList<ContactListSection>()

    private var selectedDeviceContact = HashSet<DeviceContactInfo>()
    private var preSelectedGroupParticipants = HashSet<DeviceContactInfo>()

    // Reusable Resources
    private val viaStringPrefix: String
    private val pickFirstConversationText: String
    private val nonActiveColor: Int
    private val activeColor: Int
    private val activeBackgroundColor: Int
    private val nonActiveBackgroundColor: Int

    init {
        // Build sections and add them to the list
        var sectionTile: String
        sectionTile = appController().getString(R.string.device_contacts)
        val sectionContactsSection = ContactListSection(originalContacts, sectionTile, SECTION_CONTACTS_IDX,
                displayPermissionElement = displayPermissionElement)

        // Add general contacts section
        sectionsList.addLast(sectionContactsSection)

        if (searchingByHandle) {
            sectionTile = appController().getString(R.string.search_contact)
            val searchSection = ContactListSection(emptyList(), sectionTile, SECTION_SEARCH_IDX, searchSection = true)
            // Add search section
            sectionsList.addLast(searchSection)
        }

        // Pre-load some resources
        nonActiveColor = R.color.s_light_grey.colorResource()
        activeColor = R.color.s_green.colorResource()
        activeBackgroundColor = R.color.s_light_blue.colorResource()
        nonActiveBackgroundColor = R.color.opaque_white.colorResource()
        viaStringPrefix = R.string.contact_via.stringResource()
        pickFirstConversationText = R.string.pick_your_first_conversation.stringResource()
    }


    enum class ContactSectionType() {
        CONTENT,
        SECTION,
        REQUEST_PERMISSION
    }
    /**
     *  ContactsView Holder
     */
    inner class ContactViewHolder(v: View) : BaseViewHolder(v),
            View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val pos = adapterPosition
            if (pos == RecyclerView.NO_POSITION) {
                // TODO : revisit this in the future under Fabric to know if this cause the crashes
                val ex = Exception("NO POSITION PRESSED!")
                logError(ex)
                return
            }
            // Element pressed
            val sp = sectionPosition(pos)
            val contact = sectionsList[sp.sectionIndex].getContactForPosition(sp.realPosition)

            if (contact.isSearchContact) {
                logDebug { "pressed search contact" }
                if (matchedProfile == null) {
                    return
                }
            }
            toggleContactSelection(contact)
        }
    }


    /**
     *  Enable contact permission holder
     */
    inner class ContactPermissionsViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            listener.pressedRequestPermission()
        }

        init {
            v.findViewById(R.id.askContactPermissionButton).setOnClickListener(this)
        }
    }

    /**
     *  SectionView Holder
     */
    inner class SectionViewHolder(v: View) : BaseViewHolder(v)

    //
    // OVERRIDE METHODS
    //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            ContactSectionType.SECTION.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contacts_section_elem, parent, false)
                return SectionViewHolder(v)
            }

            ContactSectionType.REQUEST_PERMISSION.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contact_permission_elem, parent, false)
                return ContactPermissionsViewHolder(v)
            }

            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.device_contact_elem, parent, false)
                val vh = ContactViewHolder(MaterialRippleLayout
                        .on(v)
                        .rippleOverlay(true)
                        .rippleAlpha(0.1f)
                        .rippleDelayClick(false)
                        .create())
                return vh
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder?, position: Int) {
        if (holder == null) {
            return
        }

        if (holder is SectionViewHolder) {
            // Render section
            renderSection(holder, position)
            return
        }

        // Contact permission request element
        if (holder is ContactPermissionsViewHolder) {
            return
        }

        renderContactElement(holder, position)
    }

    override fun getItemCount(): Int {
        return sectionsList.sumBy { it.getSectionSize() }
    }

    /**
     * Get a unique item ID, that must be retained during item updates
     */
    override fun getItemId(position: Int): Long {
        val sp = sectionPosition(position)
        return sectionsList[sp.sectionIndex].getUniqueElementId(sp.realPosition)
    }

    override fun getItemViewType(position: Int): Int {
        val sp = sectionPosition(position)
        return sectionsList[sp.sectionIndex].getItemType(sp.realPosition)
    }

    //
    // PUBLIC METHODS
    //

    fun updatePreSelectedContacts(preSelectedContactsAccountIdList: LongArray) {
        for (preSelectedAccountId in preSelectedContactsAccountIdList) {
            // Try to map it to a device ID
            val deviceId = ContactMapRepo.deviceIdForRogerId(preSelectedAccountId)
            if (deviceId != null) {
                // Find DeviceContact with this ID
                val deviceContact = originalContacts.filter { it.internalId == deviceId }.firstOrNull()
                if (deviceContact != null) {
                    preSelectedGroupParticipants.add(deviceContact)
                }
            }
        }
    }

    fun updateContacts(newContacts: List<DeviceContactInfo>, newDisplayPermissionElement: Boolean) {
        // Build sections and add them to the list
        originalContacts = newContacts

        sectionsList[SECTION_CONTACTS_IDX].updateContacts(originalContacts)
        sectionsList[SECTION_CONTACTS_IDX].updateDisplayPermissionElement(newDisplayPermissionElement)

        if ((sectionsList.size - 1) >= SECTION_SEARCH_IDX) {
            sectionsList[SECTION_SEARCH_IDX].updateContacts(emptyList())
        }

        notifyDataSetChanged()
    }

    fun filterContacts(searchText: String) {
        val comparableName = NameUtils.comparableName(searchText)
        sectionsList.forEach { it.filterContacts(comparableName) }
        notifyDataSetChanged()
    }

    /**
     * Called when getting profile information from the server
     */
    fun updateSearchProfile(profile: Profile?) {
        // Reset profile matched state
        matchedProfile = profile
        notifyDataSetChanged()
    }

    fun getSelectedContacts(): List<DeviceContactInfo> {
        return selectedDeviceContact.toList()
    }

    fun unSelectedContact(contact: DeviceContactInfo) {
        if (selectedDeviceContact.contains(contact)) {
            selectedDeviceContact.remove(contact)
        }
        reassertSelectionState()
        notifyDataSetChanged()
    }

    /**
     * Used as a shortcut when pressing the enter key
     */
    fun selectFirstContact() {
        if (searchingByHandle) {
            val sp = sectionPosition(1)
            val contact = sectionsList[sp.sectionIndex].getContactForPosition(sp.realPosition)
            toggleContactSelection(contact)
        }
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Obtain the section index and shift amount for a given global position
     */
    private fun sectionPosition(position: Int): SectionPosition {
        var shiftAmount = 0
        var realPosition: Int
        for (i in 0..(sectionsList.size - 1)) {
            realPosition = position - shiftAmount
            if (realPosition < sectionsList[i].getSectionSize()) {
                return SectionPosition(i, realPosition)
            }

            // Increase shift amount by the size if the section size
            shiftAmount += sectionsList[i].getSectionSize()
        }

        throw IndexOutOfBoundsException("Element not found!")
    }

    private fun renderContactElement(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val contact = sectionsList[sp.sectionIndex].getContactForPosition(sp.realPosition)

        // Display initials, photo or Mr. Pee, or Selection check
        renderContactPhoto(contact, holder)

        // Optional controls on the element
        holder.itemView.linkLabel.beGone()

        // Display contact origin (eg: 'via 1234')
        val profileMatched = matchedProfile
        if (contact.isSearchContact) {
            holder.itemView.contactOriginLabel.textColor = nonActiveColor
        } else if (contact.activeOnRoger) {
            if (contact.customDescriptionMessage?.isNotEmpty() ?: false) {
                holder.itemView.contactOriginLabel.text = contact.customDescriptionMessage
                holder.itemView.contactOriginLabel.textColor = nonActiveColor
            } else {
                holder.itemView.contactOriginLabel.setText(R.string.active_on_roger)
                holder.itemView.contactOriginLabel.textColor = activeColor
            }
        } else if (contact.aliases.size > 0) {
            val contactLabel = contact.aliases.first()
            holder.itemView.contactOriginLabel.text = "${contactLabel.label.capitalize()} ${contactLabel.value}"
            holder.itemView.contactOriginLabel.textColor = nonActiveColor
        }

        // Display full name
        if (contact.isSearchContact) {
            if (profileMatched == null) {
                holder.itemView.contactNameLabel.text = contact.displayName
            } else {
                holder.itemView.contactNameLabel.text = profileMatched.displayName
                holder.itemView.contactOriginLabel.textColor = activeColor
                holder.itemView.contactOriginLabel.text = "@${profileMatched.username}"
            }
        } else {
            holder.itemView.contactNameLabel.text = contact.displayName
        }
    }

    private fun renderContactPhoto(contact: DeviceContactInfo, holder: BaseViewHolder) {
        // Pre-Selected contacts
        if (preSelectedGroupParticipants.contains(contact)) {
            holder.itemView.contactPhoto.beGone()
            holder.itemView.contactInitialsLabel.text = ""
            holder.itemView.selectionCircle.makeVisible()
            holder.itemView.selectionIcon.makeVisible()
            holder.itemView.top_elem.setBackgroundColor(activeBackgroundColor)
            return
        }

        // User selected contacts
        if (selectedDeviceContact.contains(contact)) {
            holder.itemView.contactPhoto.beGone()
            holder.itemView.contactInitialsLabel.text = ""
            holder.itemView.selectionCircle.makeVisible()
            holder.itemView.selectionIcon.makeVisible()
            holder.itemView.top_elem.setBackgroundColor(activeBackgroundColor)
            return
        } else {
            holder.itemView.top_elem.setBackgroundColor(nonActiveBackgroundColor)
            holder.itemView.contactPhoto.makeVisible()
            holder.itemView.selectionCircle.beGone()
            holder.itemView.selectionIcon.beGone()
        }

        if (contact.isSearchContact) {
            // Initially set photo as mr. pee
            holder.itemView.contactPhoto.setImageResource(R.drawable.pee)

            // Setup username as a question mark initially
            holder.itemView.contactPhoto.setImageResource(R.color.transparent)
            holder.itemView.contactInitialsLabel.text = "?"

            holder.itemView.contactOriginLabel.text = ""

            // Check if we have a photo for this username
            val matchedContact = matchedProfile
            if (matchedContact != null) {
                if (matchedContact.imageURL != null) {
                    RoundImageUtils.createRoundImage(AppController.instance, holder.itemView.contactPhoto, matchedContact.imageURL)
                        holder.itemView.contactInitialsLabel.text = ""

                        // Display description label
                        holder.itemView.contactOriginLabel.setText(R.string.add_by_username)
                    }
            }
        } else if (contact.photoURI != null) {
            RoundImageUtils.createRoundImage(AppController.instance, holder.itemView.contactPhoto, contact.photoURI)
            holder.itemView.contactInitialsLabel.text = ""
        } else {
            holder.itemView.contactPhoto.setImageResource(R.color.transparent)
            holder.itemView.contactInitialsLabel.text = contact.displayName.initial
        }
    }

    private fun toggleContactSelection(contact: DeviceContactInfo) {
        // If belong to pre-selected contacts, don't do anything
        if (preSelectedGroupParticipants.contains(contact)) {
            return
        }

        if (selectedDeviceContact.contains(contact)) {
            selectedDeviceContact.remove(contact)
            listener.unSelectedContact(contact)
        } else {
            selectedDeviceContact.add(contact)
            listener.selectedContact(contact)
        }

        reassertSelectionState()

        // Update data displayed
        notifyDataSetChanged()
    }

    private fun reassertSelectionState() {
        if (selectedDeviceContact.isEmpty()) {
            listener.selectionCleared()
        } else {
            listener.selectionBegun()
        }
    }

    private fun renderSection(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val sectionName = sectionsList[sp.sectionIndex].sectionName
        holder.itemView.sectionName.text = sectionName
    }
}