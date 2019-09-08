package com.rogertalk.roger.ui.dialog

import android.content.Context
import android.text.util.Linkify
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.utils.changelog.ChangeLogHistory

object CTADialogs {
    /**
     * Display latest changelog
     */
    fun showLatestChangeDialog(context: Context) {
        val dialogBuild = MaterialDialog.Builder(context)
                .title(context.getString(R.string.settings_whats_new))
                .content(ChangeLogHistory.getLatestChangeLogText())
                .positiveText(android.R.string.ok)
                .onPositive { materialDialog, dialogAction ->
                    // Nothing
                }
                .build()

        Linkify.addLinks(dialogBuild.contentView, Linkify.ALL)
        dialogBuild.show()
    }

}
