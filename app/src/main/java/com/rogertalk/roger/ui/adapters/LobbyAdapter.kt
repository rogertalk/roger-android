package com.rogertalk.roger.ui.adapters

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeInvisible
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.android.AppController
import com.rogertalk.roger.models.data.SectionPosition
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.sections.LobbyListContactSection
import com.rogertalk.roger.models.sections.LobbyListInviterSection
import com.rogertalk.roger.models.sections.LobbyListSection
import com.rogertalk.roger.models.sections.LobbyListSourcesSection
import com.rogertalk.roger.models.sections.LobbyListSourcesSection.ContactsSource
import com.rogertalk.roger.models.sections.LobbyListSourcesSection.ContactsSource.*
import com.rogertalk.roger.models.sections.LobbySectionType.*
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.screens.LobbyActivity
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.constant.MaterialIcon
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.initial
import com.rogertalk.roger.utils.extensions.materializeRecyclerElement
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import kotlinx.android.synthetic.main.contact_source_elem.view.*
import kotlinx.android.synthetic.main.contacts_section_elem.view.*
import kotlinx.android.synthetic.main.device_contact_elem.view.*
import kotlinx.android.synthetic.main.invite_elem.view.*
import java.util.*

class LobbyAdapter(var members: List<Account>,
                   val lobbyActivity: LobbyActivity) : RecyclerView.Adapter<BaseViewHolder>() {

    private var invitedMembers: List<Account>
    private var activeMembers: List<Account>
    private var botMembers: List<Account>

    private var sectionsList: List<LobbyListSection>
    private val sourcesList = listOf(ADDRESS_BOOK, HANDLE, GROUP_SHARE_LINK, BOTS)

    // Pre-loaded resources
    private val onRogerText: String
    private val onRogerColor: Int
    private val hasNotJoinedColor: Int
    private val hasNotJoinedText: String
    private val remindText: String
    private val remindTextAccessibility: String
    private val contactActionsAccessibility: String

    init {
        invitedMembers = members.filter { it.active == false }
        activeMembers = members.filter { it.active == true && it.bot == false }
        botMembers = members.filter { it.bot == true }

        // Initialized sections list
        sectionsList = updatedSectionsList()

        // Load reusable resources
        onRogerText = R.string.on_roger.stringResource()
        remindText = R.string.lobby_remind_action.stringResource()
        remindTextAccessibility = R.string.ac_remind_text.stringResource()
        contactActionsAccessibility = R.string.ac_contact_action_description_text.stringResource()
        hasNotJoinedText = R.string.hasnt_joined.stringResource()
        onRogerColor = R.color.s_green.colorResource(lobbyActivity)
        hasNotJoinedColor = R.color.s_light_grey.colorResource(lobbyActivity)
    }

    //
    // INNER CLASSES
    //

    /**
     *  Enable contact permission holder
     */
    inner class ContactSourcesViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val contactSource = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as ContactsSource
            lobbyActivity.contactSourcePressed(contactSource)
        }

        init {
            v.findViewById(R.id.topElem).setOnClickListener(this)
        }
    }

    /**
     *  SectionView Holder
     */
    inner class SectionViewHolder(v: View) : BaseViewHolder(v)

    inner class InvitedContactViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener,
            View.OnLongClickListener {

        override fun onClick(view: View?) {
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val account = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
            lobbyActivity.invitedContactPressed(account)
        }

        override fun onLongClick(view: View?): Boolean {
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val account = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
            lobbyActivity.displayRemovalConfirmationDialog(account.id)
            return true
        }

        init {
            v.findViewById(R.id.top_elem).setOnClickListener(this)
            v.findViewById(R.id.top_elem).setOnLongClickListener(this)
        }
    }

    inner class InviterViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {

        override fun onClick(view: View?) {
            if (view == null) {
                return
            }
            when (view.id) {
                R.id.skipButton -> {
                    lobbyActivity.inviterHelper.skipPressed()

                    // Issue a redraw
                    notifyDataSetChanged()
                }

                R.id.inviteButton -> {
                    lobbyActivity.inviterHelper.invitePressed()

                    // Issue a redraw
                    notifyDataSetChanged()
                }

                R.id.previousContact -> {
                    lobbyActivity.inviterHelper.pressedPreviousContact()

                    // Issue a redraw
                    notifyDataSetChanged()
                }
            }
        }

        init {
            v.findViewById(R.id.skipButton).setOnClickListener(this)
            v.findViewById(R.id.inviteButton).setOnClickListener(this)
            v.findViewById(R.id.previousContact).setOnClickListener(this)
        }
    }

    inner class ActiveContactViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            if (view == null) {
                return
            }
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val account = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
            when (view.id) {
                R.id.top_elem -> {
                    lobbyActivity.activeContactPressed(account)
                }

                R.id.infoButton -> {
                    lobbyActivity.contactInfoPressed(account)
                }
            }
        }

        init {
            v.findViewById(R.id.top_elem).setOnClickListener(this)
            v.findViewById(R.id.infoButton).setOnClickListener(this)
        }
    }

    inner class BotViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            if (view == null) {
                return
            }
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val account = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
            when (view.id) {
                R.id.top_elem -> {
                    lobbyActivity.activeContactPressed(account)
                }

                R.id.infoButton -> {
                    lobbyActivity.contactInfoPressed(account)
                }
            }
        }

        init {
            v.findViewById(R.id.top_elem).setOnClickListener(this)
            v.findViewById(R.id.infoButton).setOnClickListener(this)
        }
    }

    //
    // OVERRIDE METHODS
    //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            TYPE_SECTION.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contacts_section_elem, parent, false)
                return SectionViewHolder(v)
            }

            TYPE_ACTIVE_MEMBERS.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.device_contact_elem, parent, false)
                return ActiveContactViewHolder(v.materializeRecyclerElement())
            }

            TYPE_BOT.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.device_contact_elem, parent, false)
                return BotViewHolder(v.materializeRecyclerElement())
            }

            TYPE_INVITED.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.device_contact_elem, parent, false)
                return InvitedContactViewHolder(v.materializeRecyclerElement())
            }

            TYPE_INVITER.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.invite_elem, parent, false)
                return InviterViewHolder(v)
            }

            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contact_source_elem, parent, false)
                val vh = ContactSourcesViewHolder(v.materializeRecyclerElement())
                return vh
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder?, position: Int) {
        if (holder == null) {
            return
        }
        when (holder) {
            is ContactSourcesViewHolder -> {
                renderContactSource(holder, position)
            }
            is InvitedContactViewHolder -> {
                renderInvitedContact(holder, position)
            }
            is ActiveContactViewHolder -> {
                renderActiveContact(holder, position)
            }
            is BotViewHolder -> {
                renderBotContact(holder, position)
            }
            is InviterViewHolder -> {
                renderInviter(holder)
            }
            is SectionViewHolder -> renderSection(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return sectionsList.sumBy { it.getSectionSize() }
    }

    /**
     * Get a unique item ID, that must be retained during item updates
     */
    override fun getItemId(position: Int): Long {
        val sp = sectionPosition(position)
        return sectionsList[sp.sectionIndex].getUniqueElementId(sp.realPosition, sp.sectionIndex)
    }

    override fun getItemViewType(position: Int): Int {
        val sp = sectionPosition(position)
        return sectionsList[sp.sectionIndex].getItemType(sp.realPosition)
    }

    //
    // PUBLIC METHODS
    //

    fun updateMembers(newMembers: List<Account>) {
        members = newMembers
        invitedMembers = members.filter { it.active == false }
        activeMembers = members.filter { it.active == true && it.bot == false }
        botMembers = members.filter { it.bot == true }

        // Refresh sections list
        sectionsList = updatedSectionsList()

        // Refresh content display
        notifyDataSetChanged()
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Build an updated list of the sections
     */
    private fun updatedSectionsList(): List<LobbyListSection> {
        val newSectionsList = ArrayList<LobbyListSection>(3)

        val inviterSection = LobbyListInviterSection()

        val sourcesSection = LobbyListSourcesSection(
                R.string.lobby_add_members_via.stringResource(),
                sourcesList)

        val botContactsSection = LobbyListContactSection(TYPE_BOT.ordinal, botMembers, active = true)

        val invitedContactsSection = LobbyListContactSection(TYPE_INVITED.ordinal, invitedMembers, active = false)

        val membersContactsSection = LobbyListContactSection(TYPE_ACTIVE_MEMBERS.ordinal, activeMembers, active = true)

        // Add all the sections to the list in the order that we want them displayed
        newSectionsList.add(botContactsSection)
        newSectionsList.add(invitedContactsSection)
        newSectionsList.add(membersContactsSection)
        newSectionsList.add(sourcesSection)
        newSectionsList.add(inviterSection)

        return newSectionsList
    }

    private fun renderSection(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val contentType = sectionsList[sp.sectionIndex].elementsType

        val sectionLabel = when (contentType) {
            TYPE_BOT.ordinal -> {
                R.string.lobby_bot_section.stringResource()
            }

            TYPE_INVITED.ordinal -> {
                val numInvitedMembers = invitedMembers.size
                holder.itemView.sectionName.contentDescription = ""
                R.string.lobby_invited_section.stringResource(numInvitedMembers)
            }

            TYPE_ACTIVE_MEMBERS.ordinal -> {
                val numActiveMembers = activeMembers.size
                val memberLabel = lobbyActivity.resources.getQuantityString(R.plurals.member, numActiveMembers)
                val sectionTitle = "$numActiveMembers $memberLabel"
                holder.itemView.sectionName.contentDescription = "$sectionTitle. $contactActionsAccessibility"
                sectionTitle
            }

            else -> {
                holder.itemView.sectionName.contentDescription = ""
                sectionsList[sp.sectionIndex].sectionName
            }
        }

        holder.itemView.sectionName.text = sectionLabel
    }

    private fun renderInviter(holder: BaseViewHolder) {
        val contactToInvite = lobbyActivity.inviterHelper.personToInvite()
        if (contactToInvite == null) {
            // There's no contacts left to invite
            holder.itemView.doneLabel.makeVisible()
            val allDoneText = R.string.no_more_invites.stringResource()
            holder.itemView.doneLabel.text = "${EmojiUtils.raiseHands} ${EmojiUtils.partyPopper} $allDoneText ${EmojiUtils.balloon} ${EmojiUtils.relievedFace}"
            holder.itemView.shareTitle.makeInvisible()
            holder.itemView.inviteButton.makeInvisible()
            holder.itemView.skipButton.makeInvisible()
            holder.itemView.previousContact.makeInvisible()
            return
        }

        // Reset state first
        holder.itemView.doneLabel.beGone()
        holder.itemView.shareTitle.makeVisible()
        holder.itemView.inviteButton.makeVisible()
        holder.itemView.skipButton.makeVisible()
        holder.itemView.previousContact.makeVisible()

        val suffix = R.string.invite_title_cta.stringResource("<font color='#000000'>${contactToInvite.displayName}</font>")

        // Format text for different colors
        val shareTitleText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml("${EmojiUtils.randomFaceEmoji} $suffix", Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml("${EmojiUtils.randomFaceEmoji} $suffix")
        }
        holder.itemView.shareTitle.text = shareTitleText

        if (lobbyActivity.inviterHelper.hasPrevious()) {
            holder.itemView.previousContact.makeVisible(true)
        } else {
            holder.itemView.previousContact.beGone()
        }
    }

    private fun renderBotContact(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val member = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
        renderGenericContact(holder, member)
        holder.itemView.contactOriginLabel.text = "@${member.username}"
        holder.itemView.contactOriginLabel.setTextColor(onRogerColor)
        holder.itemView.linkLabel.beGone()
        holder.itemView.infoButton.makeVisible()
        holder.itemView.infoButton.contentDescription = R.string.ac_contact_info_action.stringResource(member.displayName)
    }

    private fun renderActiveContact(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val member = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
        renderGenericContact(holder, member)
        holder.itemView.contactOriginLabel.text = "@${member.username}"
        holder.itemView.contactOriginLabel.setTextColor(onRogerColor)
        holder.itemView.linkLabel.beGone()
        holder.itemView.infoButton.makeVisible()
        holder.itemView.infoButton.contentDescription = R.string.ac_contact_info_action.stringResource(member.displayName)
    }

    private fun renderInvitedContact(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val member = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Account
        renderGenericContact(holder, member)
        holder.itemView.contactOriginLabel.text = hasNotJoinedText
        holder.itemView.contactOriginLabel.setTextColor(hasNotJoinedColor)
        holder.itemView.infoButton.beGone()
        holder.itemView.linkLabel.makeVisible()
        holder.itemView.linkLabel.text = remindText
        holder.itemView.linkLabel.contentDescription = remindTextAccessibility
    }

    private fun renderGenericContact(holder: BaseViewHolder, member: Account) {
        val memberName = if (member.bot && member.ownerId != null) {
            // Display bot ownership
            val botName = member.displayName
            val owner = StreamCacheRepo.getAccountById(member.ownerId)
            if (owner != null) {
                R.string.bots_ownership.stringResource(owner.displayName, botName)
            } else {
                member.displayName
            }

        } else {
            member.displayName
        }
        holder.itemView.contactNameLabel.text = memberName

        // Load avatar
        if (member.imageURL != null) {
            RoundImageUtils.createRoundImage(AppController.instance, holder.itemView.contactPhoto, member.imageURL)
            holder.itemView.contactInitialsLabel.text = ""
        } else {
            holder.itemView.contactPhoto.setImageResource(R.color.transparent)
            holder.itemView.contactInitialsLabel.text = member.displayName.initial
        }
    }

    private fun renderContactSource(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val elem = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as ContactsSource
        val label: String
        val materialIcon: String
        val backgroundResource: Int
        val showTextIcon: Boolean
        when (elem) {
            ADDRESS_BOOK -> {
                label = R.string.lobby_address_book.stringResource()
                backgroundResource = R.drawable.addressbook
                materialIcon = MaterialIcon.ADDRESS_BOOK.text
                showTextIcon = false
            }

            HANDLE -> {
                label = R.string.lobby_handle.stringResource()
                backgroundResource = R.drawable.handle
                materialIcon = ""
                showTextIcon = true
            }

            GROUP_SHARE_LINK -> {
                label = R.string.lobby_group_share_link.stringResource()
                backgroundResource = R.drawable.link
                materialIcon = MaterialIcon.LINK.text
                showTextIcon = false
            }

            BOTS -> {
                label = R.string.lobby_bots_option.stringResource()
                backgroundResource = R.drawable.bots
                materialIcon = ""
                showTextIcon = false
            }
        }
        holder.itemView.sourceLabel.text = label
        holder.itemView.avatarBackground.setImageResource(backgroundResource)
        holder.itemView.avatarIcon.text = materialIcon

        if (showTextIcon) {
            holder.itemView.handleIcon.makeVisible()
        } else {
            holder.itemView.handleIcon.beGone()
        }

    }

    /**
     * Obtain the section index and shift amount for a given global position
     */
    private fun sectionPosition(position: Int): SectionPosition {
        var shiftAmount = 0
        var realPosition: Int
        for (i in 0 until sectionsList.size) {
            realPosition = position - shiftAmount
            if (realPosition < sectionsList[i].getSectionSize()) {
                return SectionPosition(i, realPosition)
            }

            // Increase shift amount by the size if the section size
            shiftAmount += sectionsList[i].getSectionSize()
        }

        throw IndexOutOfBoundsException("Element not found!")
    }

}