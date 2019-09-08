package com.rogertalk.roger.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.AlertDialogWrapper
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.ui.dialog.listeners.ChangeAccountListener
import com.rogertalk.roger.ui.dialog.listeners.ContactInfoSelectionListener
import com.rogertalk.roger.ui.dialog.listeners.SettingsDataCheckListener
import com.rogertalk.roger.ui.dialog.listeners.ShareRankListener
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.extensions.stringResource

/**
 * This class contains all the dialogs used within the app.
 */
object CommonDialog {

    /**
     * Generic dialog for displaying a dialog with title and message
     */
    fun simpleMessage(context: Context, title: String, message: String) {
        val builder = AlertDialogWrapper.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }


    /**
     * Generic dialog for displaying a dialog with title and message with a dismiss button (with NO callback)
     */
    fun simpleMessageWithButton(context: Context, title: String, message: String, button: String) {
        val builder = AlertDialogWrapper.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.autoDismiss(true)
        builder.setPositiveButton(button,
                { dialogInterface, i -> })
        builder.show()
    }

    /**
     * Generic dialog for displaying a dialog with title and message with a dismiss button
     * and with a callback anonymous function
     * */
    fun simpleMessageWithButton(context: Context, title: String, message: String,
                                button: String, func: () -> Any) {
        val builder = AlertDialogWrapper.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.autoDismiss(true)
        builder.setPositiveButton(button,
                { dialogInterface, i -> func() })
        builder.show()
    }

    fun settingsCTADialog(context: Context, buttonText: String,
                          mainAction: () -> Any, helpAction: () -> Any) {
        val builder = AlertDialogWrapper.Builder(context)
        builder.setMessage(R.string.settings_contact_cta)
        builder.autoDismiss(true)
        builder.setNegativeButton(buttonText,
                { dialogInterface, i -> mainAction() })
        builder.setPositiveButton(R.string.settings_help, {
            dialogInterface, i ->
            helpAction()
        }
        )
        builder.show()
    }


    /**
     * Choose between the type of contact info to add (phone, email)
     */
    fun contactInfoChoice(context: Context, listener: ContactInfoSelectionListener) {
        MaterialDialog.Builder(context)
                .title(context.getString(R.string.settings_add_contact_info_title))
                .positiveText(R.string.settings_add_phone)
                .onPositive { materialDialog, dialogAction -> listener.chooseNewNumber() }
                .negativeText(R.string.settings_add_email)
                .onNegative { materialDialog, dialogAction -> listener.chooseNewEmail() }
                .show()
    }


    /**
     * Display TopTalker dialog
     */
    fun shareTopTalkerDialog(context: Context, listener: ShareRankListener, rankPosition: Int) {
        var description = R.string.top_talker_dialog_description.stringResource(rankPosition.toString())
        description = "${EmojiUtils.trophy} $description"

        MaterialDialog.Builder(context)
                .title(context.getString(R.string.notification_top_talker_title))
                .content(description)
                .positiveText(R.string.share_top_talker_cta)
                .onPositive { materialDialog, dialogAction -> listener.shareRank(rankPosition) }
                .show()
    }


    /**
     * Username edited
     */
    fun usernameEditedDialog(context: Context, newUsername: String, listener: SettingsDataCheckListener) {
        MaterialDialog.Builder(context)
                .title(context.getString(R.string.settings_changed_username))
                .content("\"$newUsername\"")
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { materialDialog, dialogAction ->
                    listener.saveUsernameChange()
                }
                .show()
    }


    /**
     * Username edited
     */
    fun displayNameEditedDialog(context: Context, newDisplayName: String, listener: SettingsDataCheckListener) {
        MaterialDialog.Builder(context)
                .title(context.getString(R.string.settings_changed_display_name))
                .content("\"$newDisplayName\"")
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { materialDialog, dialogAction ->
                    listener.saveDisplayNameChange()
                }
                .show()
    }


    /**
     * Offer user with the option to change account
     */
    fun changeAccountDialog(context: Context, listener: ChangeAccountListener, newSession: Session, currentIdentifier: String) {
        MaterialDialog.Builder(context)
                .title(currentIdentifier)
                .content(R.string.change_account_description)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.yes)
                .onPositive { materialDialog, dialogAction -> listener.changeAccount(newSession) }
                .negativeText(android.R.string.no)
                .onNegative { materialDialog, dialogAction -> listener.keepCurrentAccount() }
                .show()
    }
}