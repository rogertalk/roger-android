package com.rogertalk.roger.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeInvisible
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.GroupAvatarManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.PlaceholderType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.adapters.listener.ConversationsListener
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.anim.Shaker
import com.rogertalk.roger.utils.constant.MaterialIcon.CONVERSATION_HEARD
import com.rogertalk.roger.utils.constant.MaterialIcon.CONVERSATION_SENT
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.initial
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import kotlinx.android.synthetic.main.contact_circle_part.view.*
import kotlinx.android.synthetic.main.conversation_placeholder_elem.view.*
import kotlinx.android.synthetic.main.conversations_elem.view.*
import java.util.*

class ConversationsAdapter(private val talkActivity: TalkActivity,
                           private var streams: LinkedList<Stream>,
                           private val listener: ConversationsListener) :
        RecyclerView.Adapter<BaseViewHolder>(),
        View.OnClickListener,
        View.OnLongClickListener {


    companion object {
        private val INVITE_ELEM_UID = -2000
        private val CONTACT_ADD_ELEM_UID = -3000

        const val TYPE_CONVERSATION = 1
        const val TYPE_LOADING = 2
        const val TYPE_CONTACT_SCREEN = 4
        const val TYPE_CONVERSATION_PLACEHOLDER = 5
    }

    /** The index of the currently selected stream in the list. Only use this for visual reference. */
    val selectedPosition: Int
        get() {
            val index = streams.indexOfFirst { it.id == StreamManager.selectedStreamId }
            if (index == -1) {
                return 0
            }
            return index
        }

    private val colorNormal: Int
    private val colorSelected: Int
    private val peopleLabel: String
    private val settingsLabel: String

    private var placeholderList: List<PlaceholderType>


    // Variable that controls the display of the loading element
    private val displayLoading: Boolean
        get() = !StreamCacheRepo.reachedListEnd

    init {
        colorNormal = R.color.contact_light.colorResource(talkActivity)
        colorSelected = R.color.opaque_white.colorResource(talkActivity)
        peopleLabel = talkActivity.getString(R.string.people_button)
        settingsLabel = talkActivity.getString(R.string.settings_button)

        placeholderList = updatedPlaceholders()
    }

    //
    // OVERRIDE METHODS
    //

    override fun onClick(v: View?) {
        if (v != null) {
            pressedDownEvent(v)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        val pos = v?.tag as? Int ?: return false

        if (pos < 0) {
            // Invalid position, can happen if pressed a ghost contact for example
            return true
        }

        // Hide ghost contact if clicked on another contact
        if (pos > -1) {
            hideGhost()
        }

        // Report to listener, but not for People or Settings icons.
        if (pos < (streams.size)) {
            listener.conversationsItemLongPressed(streams[pos].id)
        }
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            TYPE_CONVERSATION -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.conversations_elem, parent, false)
                val vh = ConversationVH(v, colorNormal)
                v.contactCirclePart.setOnClickListener(this)
                v.contactCirclePart.setOnLongClickListener(this)
                return vh
            }

            TYPE_CONTACT_SCREEN -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.contacts_add_elem, parent, false)
                val vh = ContactAddVH(v)
                v.contactCirclePart.setOnClickListener(this)
                return vh
            }

            TYPE_CONVERSATION_PLACEHOLDER -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.conversation_placeholder_elem, parent, false)
                val vh = PlaceholderVH(v)
                return vh
            }

            else -> {
                // This is a loading item
                val v = LayoutInflater.from(parent.context).inflate(R.layout.loading_conversations_elem, parent, false)
                return LoadingVH(v)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder?) {
        super.onViewDetachedFromWindow(holder)
        if (holder != null && holder is ConversationVH) {
            val shaker = holder.itemView.blueDot.tag as Shaker?
            shaker?.stop()
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder?) {
        if (holder != null && holder is ConversationVH) {
            val shaker = holder.itemView.blueDot.tag as Shaker?
            shaker?.stop()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int {
        var conversationListRealSize = streams.size
        if (StreamCacheRepo.temporaryStream != null) {
            conversationListRealSize++
        }
        return when (position) {
            0 -> TYPE_CONTACT_SCREEN
            in 1..conversationListRealSize -> TYPE_CONVERSATION
            in conversationListRealSize..(conversationListRealSize + placeholderList.size) -> TYPE_CONVERSATION_PLACEHOLDER
            else -> TYPE_LOADING
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder?, position: Int) {
        if (holder == null) {
            return
        }

        // Draw the respective ViewHolder
        when (holder) {
            is InviteVH -> holder.itemView.contactCirclePart.tag = INVITE_ELEM_UID
            is ContactAddVH -> holder.itemView.contactCirclePart.tag = CONTACT_ADD_ELEM_UID
            is LoadingVH -> {
            }
            is PlaceholderVH -> renderPlaceholder(holder, position)

            is ConversationVH -> {
                var pos = position - 1

                // Offset position if there is a temporary stream
                StreamCacheRepo.temporaryStream?.let { pos-- }
                holder.pos = pos
                holder.itemView.contactCirclePart.tag = pos

                // Reset the item and prepare for redrawing.
                holder.resetView(talkActivity)

                when (pos) {
                    -1 -> renderContactConversation(holder, stream = StreamCacheRepo.temporaryStream!!)
                    else -> renderContactConversation(holder, stream = streams[pos])
                }
            }

        }
    }

    override fun getItemCount(): Int {
        var count = streams.size
        if (StreamCacheRepo.temporaryStream != null) {
            count++
        }
        if (displayLoading) {
            count++
        }

        // Add contact add element
        count++

        // Add Placeholder count
        count += placeholderList.size

        return count
    }

    override fun getItemId(position: Int): Long {
        // TODO : This whole thing needs to be refactored
        var pos = position
        var conversationsStartShift = 0

        // Offset position if there is a temporary stream
        StreamCacheRepo.temporaryStream?.let {
            conversationsStartShift++
            pos--
        }

        // Position always offsets for the add contact
        pos--

        val numPlaceholders = placeholderList.size

        val maxConversations = streams.size

        return when (position) {
            -1 -> StreamCacheRepo.temporaryStream?.id ?: NO_ID

            0 -> -11L

            in (1 + conversationsStartShift)..(maxConversations + conversationsStartShift) -> streams[pos].id

            in (maxConversations + conversationsStartShift)..(conversationsStartShift + maxConversations + numPlaceholders) -> -1000L * pos

            else -> {
                // This is likely the loading element
                -10L
            }
        }
    }

    //
    // PUBLIC METHODS
    //

    fun updateItems(newStreams: LinkedList<Stream>) {
        // Update data
        streams = newStreams
        notifyDataSetChanged()
    }

    fun groupAvatarReady(streamId: Long) {
        val streamsCopy = LinkedList<Stream>(streams)
        for (i in 0..streamsCopy.size) {
            if (streamsCopy[i].id == streamId) {
                logDebug { "Updating item. Stream name: ${streamsCopy[i].shortTitle}" }
                notifyItemChanged(i)
                return
            }
        }
    }

    /**
     * Should call this every time we make a change in the underlying data
     */
    fun updateCurrentlySelected() {
        notifyDataSetChanged()
    }

    //
    // PRIVATE METHODS
    //

    private fun updateSelectionState(itemView: View, stream: Stream) {
        val selected = stream.id == StreamManager.selectedStreamId

        // Update the shake animation.
        var shaker = itemView.blueDot.tag as Shaker?
        if (stream.unplayed) {
            itemView.blueDot.makeVisible()
            itemView.blueDotBorder.makeVisible()
        }

        if (stream.unplayed && !selected) {
            if (shaker == null) {
                val shakeContainer = itemView.blueDot
                shaker = Shaker(distance = 3.0, view = shakeContainer)
                shakeContainer.tag = shaker
            }
            shaker.start()
        } else {
            shaker?.stop()
            if (!stream.unplayed) {
                itemView.selectorView.setImageResource(R.drawable.circumference_white)
                itemView.blueDot.makeInvisible()
                itemView.blueDotBorder.makeInvisible()
            } else {
                itemView.selectorView.setImageResource(R.drawable.circumference_blue)
            }
        }

        if (!selected) {
            itemView.contactName.setTextColor(colorNormal)
            itemView.selectorView.makeInvisible()
            return
        }

        // Show selection UI.
        itemView.contactName.setTextColor(colorSelected)
        val selectorView = itemView.selectorView
        selectorView.makeVisible()

        // Configure Spring System for selection animation
        val springSystem = SpringSystem.create()
        val selectorSpring = springSystem.createSpring()
        selectorSpring.springConfig = SpringConfig.fromBouncinessAndSpeed(14.0, 50.0)

        selectorSpring.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.
                val scale = 0.85f + (spring.currentValue.toFloat() * 0.15f)
                selectorView.scaleX = scale
                selectorView.scaleY = scale
            }
        })

        // Set spring to go from 0.0 to to 1.0
        selectorSpring.currentValue = 0.0
        selectorSpring.endValue = 1.0
    }

    private fun pressedDownEvent(v: View) {
        val pos = (v.tag as Int)

        // Hide ghost contact if clicked on another contact
        if (pos > -1) {
            hideGhost()

            // If there are no streams, don't send pressed down event
            if (!streams.isEmpty()) {
                listener.conversationsItemPressed(streams[pos].id)
            }
        } else {
            when (pos) {
                CONTACT_ADD_ELEM_UID -> listener.pressedAddContacts()
            }
        }

        notifyDataSetChanged()
    }

    private fun hideGhost() {
        // TODO: Once we only care about selected stream id we don't need to do this.
        StreamCacheRepo.properlyHideGhost()
    }

    private fun renderContactConversation(holder: ConversationVH, stream: Stream) {
        val itemView = holder.itemView
        itemView.initials.text = stream.title.initial
        itemView.contactName.text = stream.shortTitle

        // Update description for TalkBack
        if (stream.unplayed) {
            itemView.contactCirclePart.contentDescription = appController().getString(R.string.ac_unheard, stream.title)
        } else {
            itemView.contactCirclePart.contentDescription = stream.title
        }

        // Check if this is a temporary stream
        if (!streams.contains(stream)) {
            itemView.whiteOverlay.makeVisible()
            itemView.dateTimeTV.text = "——"
        } else {
            itemView.dateTimeTV.text = stream.lastInteractionLabelShort

            // What icon to display
            if (stream.currentUserHasReplied) {
                // has some icon
                itemView.statusIcon.makeVisible()
                if (stream.otherListenedTime == null) {
                    itemView.statusIcon.text = CONVERSATION_SENT.text
                } else {
                    itemView.statusIcon.text = CONVERSATION_HEARD.text
                }
            }
        }

        // Display avatar if available
        if (stream.isEmptyGroup) {
            itemView.initials.text = ""
            itemView.blackCircle.beGone()
            Glide.with(talkActivity).load(R.drawable.party_popper_circle).dontAnimate().into(itemView.contactPhoto)
        } else {
            val avatarURI = stream.imageURL
            if (avatarURI != null) {
                RoundImageUtils.createRoundImage(talkActivity, itemView.contactPhoto,
                        avatarURI,
                        AvatarSize.CONTACT)
                itemView.initials.text = ""
            } else {
                if (stream.reachableParticipants.size >= 2) {
                    itemView.initials.text = ""
                    GroupAvatarManager.loadGroupAvatarInto(itemView.contactPhoto, stream, AvatarSize.CONTACT)
                } else {
                    itemView.blackCircle.makeVisible()
                    Glide.with(talkActivity).load(R.color.transparent).dontAnimate().into(itemView.contactPhoto)
                }
            }
        }

        // Change selection state
        updateSelectionState(holder.itemView, stream)
    }

    private fun renderPlaceholder(holder: PlaceholderVH, listPosition: Int) {
        val realPosition = realPositionForPlaceholder(listPosition)

        holder.itemView.emojiIcon.setImageResource(getImageForPlaceholder(realPosition))
        holder.itemView.placeholderLabel.setText(getLabelForPlaceholder(realPosition))
        holder.itemView.placeholderParent.contentDescription = appController().getString(getLabelForPlaceholder(realPosition))
    }

    private fun realPositionForPlaceholder(adapterPosition: Int): Int {
        var realPosition = adapterPosition

        // Consider add contact
        realPosition--

        // Consider conversation elements
        realPosition -= streams.size
        return realPosition
    }

    private fun getImageForPlaceholder(index: Int): Int {
        val type = placeholderList[index]
        return when (type) {

            PlaceholderType.FAMILY -> R.drawable.family
            PlaceholderType.FRIENDS -> R.drawable.friends
            PlaceholderType.TEAM -> R.drawable.team
        }
    }

    private fun getLabelForPlaceholder(index: Int): Int {
        val type = placeholderList[index]
        return when (type) {

            PlaceholderType.FAMILY -> R.string.placeholder_family
            PlaceholderType.FRIENDS -> R.string.placeholder_friends
            PlaceholderType.TEAM -> R.string.placeholder_team
        }
    }

    private fun updatedPlaceholders(): List<PlaceholderType> {
        val placeholderTmpList = ArrayList<PlaceholderType>(3)
        if (PrefRepo.showAddFamily) {
            placeholderTmpList.add(PlaceholderType.FAMILY)
        }
        if (PrefRepo.showAddFriends) {
            placeholderTmpList.add(PlaceholderType.FRIENDS)
        }
        if (PrefRepo.showAddTeam) {
            placeholderTmpList.add(PlaceholderType.TEAM)
        }
        return placeholderTmpList
    }

    //
    // ViewHolders
    //

    class ConversationVH(v: View, val colorNormal: Int) : BaseViewHolder(v) {
        var pos: Int = 0

        fun resetView(context: Context) {
            val shaker = itemView.blueDot.tag as Shaker?
            shaker?.stop()

            itemView.whiteOverlay.beGone()

            itemView.blueDot.makeInvisible()
            itemView.blueDotBorder.makeInvisible()

            Glide.with(context).load(R.color.transparent).dontAnimate().into(itemView.contactPhoto)
            itemView.greyCircumference.makeInvisible()

            itemView.initials.text = ""
            itemView.contactName.text = ""
            itemView.dateTimeTV.text = ""

            // Reset other possible states.
            itemView.selectorView.makeInvisible()
            itemView.blackCircle.makeInvisible()
            itemView.statusIcon.beGone()
            itemView.contactName.setTextColor(colorNormal)
        }
    }

    /**
     *  SectionView Holder
     */
    inner class LoadingVH(v: View) : BaseViewHolder(v) {
        val pos: Int = -1000 // this value can be ignored
    }

    inner class InviteVH(v: View) : BaseViewHolder(v) {
        val pos: Int = INVITE_ELEM_UID
    }

    inner class ContactAddVH(v: View) : BaseViewHolder(v) {
        val pos: Int = CONTACT_ADD_ELEM_UID
    }

    inner class PlaceholderVH(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            if (view == null) {
                return
            }

            when (view.id) {
                R.id.placeholderSelectorView -> {
                    val pos = adapterPosition
                    val realPosition = realPositionForPlaceholder(pos)
                    talkActivity.conversationPlaceholderPressed(placeholderList[realPosition])

                    // Re-evaluate placeholders
                    placeholderList = updatedPlaceholders()
                    notifyDataSetChanged()
                }
            }
        }

        init {
            v.findViewById(R.id.placeholderSelectorView).setOnClickListener(this)
        }
    }
}