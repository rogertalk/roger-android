package com.rogertalk.roger.helper

import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.StreamStatusRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.screens.AttachmentsActivity
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.constant.MaterialIcon
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.image.RoundImageUtils
import kotlinx.android.synthetic.main.talk_screen_idle_controls.*

class AttachmentsHelper(val talkActivity: TalkActivity) {

    //
    // PUBLIC METHODS
    //

    /**
     * Render attachments for a given stream.
     */
    fun renderAttachments(stream: Stream) {
        val rawAttachments = stream.attachments
        if (stream.isEmptyGroup) {
            talkActivity.attachmentsArea.beGone(true)
            return
        }

        talkActivity.attachmentsArea.makeVisible(true)
        if (rawAttachments.isEmpty()) {
            // Default look
            renderEmptyAttachment(stream)
            return
        }

        // Try to find type
        val firstKey = stream.attachmentType

        if (firstKey == AttachmentType.LINK || firstKey == AttachmentType.IMAGE) {
            if (firstKey == AttachmentType.IMAGE) {
                talkActivity.attachmentIcon.text = MaterialIcon.PHOTO.text

                // Load preview
                stream.attachmentLink?.let {
                    talkActivity.attachmentPreview.makeVisible()
                    RoundImageUtils.createRoundImageNoPlaceholder(appController(), talkActivity.attachmentPreview, it, AvatarSize.CONTACT)
                }
            } else {
                talkActivity.attachmentIcon.text = MaterialIcon.LINK.text
                talkActivity.attachmentPreview.beGone()
            }

            // Seen state
            if (GlobalManager.seenAttachments.contains(stream.id)) {
                talkActivity.attachmentsCTA.beGone()
                talkActivity.attachmentIcon.setTextColor(R.color.white_60.colorResource())
            } else {
                talkActivity.attachmentIcon.setTextColor(R.color.s_light_red.colorResource())
                talkActivity.attachmentsCTA.makeVisible(true)
            }
        } else {
            // Type not matched, change to default look
            renderEmptyAttachment(stream)
        }
    }

    fun attachmentPressed(streamId: Long) {
        // On the first time we're seeing this photo, report to server
        if (!GlobalManager.seenAttachments.contains(streamId)) {
            // This attachment has never been seen before
            val stream = StreamCacheRepo.getStream(streamId)
            stream?.let {
                if (it.attachmentType == AttachmentType.IMAGE) {
                    StreamStatusRequest(streamId, StreamStatus.VIEWING_ATTACHMENT).enqueueRequest()
                }
            }

        }

        talkActivity.startActivity(AttachmentsActivity.start(talkActivity, streamId))
    }

    //
    // PRIVATE METHODS
    //

    private fun renderEmptyAttachment(stream: Stream) {
        talkActivity.attachmentsArea.setBackgroundResource(R.drawable.circumference_white)
        talkActivity.attachmentIcon.setTextColor(R.color.white_60.colorResource())
        talkActivity.attachmentPreview.beGone()
        talkActivity.attachmentIcon.text = MaterialIcon.ATTACH_FILE.text
        if (stream.isEmptyConversation) {
            talkActivity.attachmentsCTA.beGone()
        } else {
            if (PrefRepo.didSeeAttachmentsScreen) {
                talkActivity.attachmentsCTA.beGone()
            } else {
                talkActivity.attachmentsCTA.makeVisible(true)
            }
        }
    }

}
