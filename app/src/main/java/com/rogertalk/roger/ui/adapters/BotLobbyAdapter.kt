package com.rogertalk.roger.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.SectionPosition
import com.rogertalk.roger.models.json.Bot
import com.rogertalk.roger.models.sections.ConnectedBotSection
import com.rogertalk.roger.models.sections.LobbyListSection
import com.rogertalk.roger.models.sections.LobbyListSourcesSection
import com.rogertalk.roger.models.sections.LobbyListSourcesSection.ContactsSource.BOTS
import com.rogertalk.roger.models.sections.LobbySectionType
import com.rogertalk.roger.ui.screens.BotLobbyActivity
import com.rogertalk.roger.utils.extensions.*
import com.rogertalk.roger.utils.image.RoundImageUtils
import kotlinx.android.synthetic.main.contact_source_elem.view.*
import kotlinx.android.synthetic.main.contacts_section_elem.view.*
import kotlinx.android.synthetic.main.device_contact_elem.view.*
import java.util.*

class BotLobbyAdapter(var connectedBots: List<Bot>,
                      val lobbyActivity: BotLobbyActivity) : RecyclerView.Adapter<BaseViewHolder>() {


    private var sectionsList: List<LobbyListSection>
    private val sourcesList = listOf(BOTS)

    // Pre-loaded resources
    private val descriptionColor: Int

    init {
        sectionsList = updatedSectionsList()

        // Load reusable resources
        descriptionColor = R.color.s_light_grey.colorResource(lobbyActivity)
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
            val contactSource = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as LobbyListSourcesSection.ContactsSource
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

    inner class ConnectBotViewHolder(v: View) : BaseViewHolder(v), View.OnClickListener {

        override fun onClick(view: View?) {
            val pos = adapterPosition
            val sp = sectionPosition(pos)
            val account = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Bot
            lobbyActivity.botSelectedPressed(account)
        }

        init {
            v.findViewById(R.id.top_elem).setOnClickListener(this)
        }
    }

    //
    // OVERRIDE METHODS
    //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            LobbySectionType.TYPE_SECTION.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contacts_section_elem, parent, false)
                return SectionViewHolder(v)
            }

            LobbySectionType.TYPE_CONNECTED_BOT.ordinal -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.device_contact_elem, parent, false)
                return ConnectBotViewHolder(v.materializeRecyclerElement())
            }

        // ContactSource
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
            is ConnectBotViewHolder -> {
                renderConnectBot(holder, position)
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

    fun updateBots(newBots: List<Bot>) {
        connectedBots = newBots

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
        val newSectionsList = ArrayList<LobbyListSection>(1)

        val sourcesSection = LobbyListSourcesSection(
                R.string.lobby_connect_new_bots.stringResource(),
                sourcesList)


        val connectedBotsSection = ConnectedBotSection(connectedBots)

        // Add all the sections to the list in the order that we want them displayed
        newSectionsList.add(connectedBotsSection)
        newSectionsList.add(sourcesSection)

        return newSectionsList
    }

    private fun renderSection(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val contentType = sectionsList[sp.sectionIndex].elementsType

        val sectionLabel = when (contentType) {

            LobbySectionType.TYPE_CONNECTED_BOT.ordinal -> {
                holder.itemView.sectionName.contentDescription = ""
                R.string.lobby_connect_action.stringResource()
            }

            else -> {
                holder.itemView.sectionName.contentDescription = ""
                sectionsList[sp.sectionIndex].sectionName
            }
        }

        holder.itemView.sectionName.text = sectionLabel
    }

    private fun renderConnectBot(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val bot = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as Bot

        holder.itemView.contactNameLabel.text = bot.title

        // Load avatar
        if (bot.imageURL != null) {
            RoundImageUtils.createRoundImage(appController(), holder.itemView.contactPhoto, bot.imageURL)
            holder.itemView.contactInitialsLabel.text = ""
        } else {
            holder.itemView.contactPhoto.setImageResource(R.color.transparent)
            holder.itemView.contactInitialsLabel.text = bot.title.initial
        }

        holder.itemView.contactOriginLabel.text = bot.description
        holder.itemView.contactOriginLabel.setTextColor(descriptionColor)
        holder.itemView.infoButton.beGone()
    }

    private fun renderContactSource(holder: BaseViewHolder, position: Int) {
        val sp = sectionPosition(position)
        val elem = sectionsList[sp.sectionIndex].getElementForPosition(sp.realPosition) as LobbyListSourcesSection.ContactsSource
        val label: String
        val materialIcon: String
        val backgroundResource: Int
        label = R.string.lobby_connect_action.stringResource().capitalize()
        backgroundResource = R.drawable.bots
        materialIcon = ""
        holder.itemView.sourceLabel.text = label
        holder.itemView.avatarBackground.setImageResource(backgroundResource)
        holder.itemView.avatarIcon.text = materialIcon
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