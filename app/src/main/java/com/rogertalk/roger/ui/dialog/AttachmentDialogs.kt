package com.rogertalk.roger.ui.dialog

import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.ConversationOption
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.ui.screens.AttachmentsActivity
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.stringResource
import org.jetbrains.anko.clipboardManager
import java.util.*

object AttachmentDialogs {

    /**
     * 'More' set of options to display when long-pressing conversations
     */
    fun attachmentOptions(attachmentsActivity: AttachmentsActivity, stream: Stream) {
        val optionsArray = ArrayList<String>(2)
        val displayedOptions = ArrayList<ConversationOption>(2)

        optionsArray.add(R.string.attachments_photo.stringResource(context = attachmentsActivity))
        displayedOptions.add(ConversationOption.ATTACHMENT_PICTURE)

        // Clipboard link
        val clipboard = appController().clipboardManager
        val clipboardContent = if (clipboard.hasText()) {
            clipboard.text.toString()
        } else {
            ""
        }

        optionsArray.add(R.string.attachments_media_link_from_clipboard.stringResource(context = attachmentsActivity))
        displayedOptions.add(ConversationOption.ATTACHMENT_LINK_FROM_CLIPBOARD)

        var menuContent = ""

        if (clipboardContent.isNotEmpty()) {
            menuContent = "${R.string.attachments_clipboard_content.stringResource()}\n$clipboardContent"
        }

        val dialog = MaterialDialog.Builder(attachmentsActivity)
                .items(optionsArray)
                .title(R.string.attachments_send_media_title)
                .contentColor(R.color.s_light_grey.colorResource())
                .itemsColor(R.color.s_medium_grey.colorResource())
                .itemsCallback { materialDialog, view, position, charSequence ->

                    when (displayedOptions[position]) {

                        ConversationOption.ATTACHMENT_PICTURE -> {
                            attachmentsActivity.setAttachmentPhoto()
                        }

                        ConversationOption.ATTACHMENT_LINK_FROM_CLIPBOARD -> {
                            if (clipboardContent.isNotEmpty()) {
                                attachmentsActivity.setAttachmentLink(stream, clipboardContent.toString())
                            } else {
                                clipboardContentsDialog(attachmentsActivity, stream)
                            }
                        }
                    }
                }
                .build()

        if (menuContent.isNotEmpty()) {
            dialog.setContent(menuContent)
        }

        dialog.show()
    }

    fun clipboardContentsDialog(attachmentsActivity: AttachmentsActivity, stream: Stream) {
        MaterialDialog.Builder(attachmentsActivity)
                .content(R.string.attachments_clipboard_empty)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", "") {
                    dialog, input ->
                    val textToShare = input.toString()
                    if (textToShare.isNotBlank()) {
                        attachmentsActivity.setAttachmentLink(stream, textToShare)
                    }
                }
                .show()
    }
}