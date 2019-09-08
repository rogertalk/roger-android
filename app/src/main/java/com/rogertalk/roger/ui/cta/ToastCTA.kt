package com.rogertalk.roger.ui.cta

import android.content.Context
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.android.EmojiUtils.checkmark
import com.rogertalk.roger.utils.android.EmojiUtils.sentRandomEmoji
import com.rogertalk.roger.utils.extensions.stringResource
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast

fun Context.doneToast() {
    val sentText = R.string.lobby_sent_feedback.stringResource()
    this.longToast("$sentText $checkmark")
}

fun Context.sentToast() {
    val sentText = R.string.lobby_sent_feedback.stringResource()
    this.toast("$sentText $sentRandomEmoji")
}

fun Context.buzzToast() {
    this.toast(EmojiUtils.bee)
}

fun Context.requestFailureToast() {
    val failText = R.string.settings_upload_pic_failed.stringResource()
    this.longToast(failText)
}