package com.rogertalk.roger.helper

import android.widget.RelativeLayout
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.isInvisibleOrGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.constant.MaterialIcon
import com.rogertalk.roger.utils.extensions.drawableResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import kotlinx.android.synthetic.main.participant_ribbon_elem.view.*
import kotlinx.android.synthetic.main.talk_screen.*
import org.jetbrains.anko.alignParentRight
import java.util.*

class GroupDisplayHelper(val talkScreen: TalkActivity) {

    // Pre-loaded resources
    private val parent = talkScreen.groupParticipantsContainer
    private val mrPeeDrawable = R.drawable.pee.drawableResource(talkScreen)

    private val participantLayoutList = ArrayList<RelativeLayout>()
    private val inflater = talkScreen.layoutInflater
    private val participantShift = talkScreen.resources.getDimensionPixelSize(R.dimen.ribbon_participant_shift)


    companion object {
        // TODO : Adjust this dynamically according to the available space
        private val PARTICIPANT_DISPLAY_LIMIT = 6
    }

    init {
        talkScreen.groupManagementElement.mrPeeAvatar.beGone()
        talkScreen.groupManagementElement.contactPhoto.beGone()
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Render participants for groups
     */
    fun renderParticipants(stream: Stream) {
        clearParticipants()
        if (talkScreen.groupManagementElement.isInvisibleOrGone()) {
            talkScreen.groupManagementElement.makeVisible()
        }
        if (stream.isEmptyGroup) {
            // Don't draw participants for empty groups
            talkScreen.membersLabels.beGone()
            talkScreen.groupManagementElement.circleCenterLabel.text = ""
            talkScreen.groupManagementElement.circleCenterIcon.text = MaterialIcon.PERSON_ADD.text
            return
        }

        talkScreen.membersLabels.makeVisible()

        // First one is the one who spoke most recently
        val activeParticipants = stream.othersOrEmpty.filter { it.active == true }
        val participants = activeParticipants.take(PARTICIPANT_DISPLAY_LIMIT).reversed()
        val numParticipants = activeParticipants.size
        var position = 0

        for (participant in participants) {
            val participantView = inflater.inflate(R.layout.participant_ribbon_elem, parent, false)

            // Add Margin display this element apart from the rest
            val layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

            //layoutParams.leftMargin = -20
            layoutParams.rightMargin = participantShift * position
            layoutParams.alignParentRight()

            // Display this participant's info
            val avatarURI = participant.userAvatar
            if (avatarURI != null) {
                // Set MR. Pee temporarily
                participantView.contactPhoto.setImageDrawable(mrPeeDrawable)

                RoundImageUtils.createRoundImage(talkScreen, participantView.contactPhoto,
                        avatarURI, AvatarSize.GROUP_PARTICIPANT)
                participantView.circleCenterLabel.text = ""
                participantView.mrPeeAvatar.beGone()
            } else {
                participantView.mrPeeAvatar.makeVisible()
            }

            // Hide white translucent circle
            participantView.whiteCircle.beGone()

            // Add to parent
            parent.addView(participantView, layoutParams)

            // Increment position
            position++
        }

        if (numParticipants > PARTICIPANT_DISPLAY_LIMIT) {
            val remainder = numParticipants - PARTICIPANT_DISPLAY_LIMIT
            updateGroupManagementView(remainder, true)
        } else {
            updateGroupManagementView(0, true)
        }
    }

    fun clearParticipants() {
        for (participantLayout in participantLayoutList) {
            participantLayout.removeAllViews()
        }
        participantLayoutList.clear()
        parent.removeAllViews()
    }

    //
    // PRIVATE METHODS
    //

    private fun updateGroupManagementView(remainder: Int, isGroup: Boolean) {
        if (!isGroup) {
            talkScreen.groupManagementElement.circleCenterLabel.text = ""
            talkScreen.groupManagementElement.circleCenterIcon.text = MaterialIcon.GROUP_ADD.text
            // Don't proceed any further
            return
        }

        if (remainder == 0) {
            talkScreen.groupManagementElement.circleCenterLabel.text = ""
            talkScreen.groupManagementElement.circleCenterIcon.text = MaterialIcon.PERSON_ADD.text
        } else {
            talkScreen.groupManagementElement.circleCenterLabel.text = "+$remainder"
            talkScreen.groupManagementElement.circleCenterIcon.text = ""
        }
    }

}
