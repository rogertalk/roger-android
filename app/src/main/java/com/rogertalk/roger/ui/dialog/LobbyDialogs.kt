package com.rogertalk.roger.ui.dialog

import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.ui.screens.LobbyActivity
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.stringResource
import java.util.*

object LobbyDialogs {

    enum class LobbyOptions {
        REMOVE_FROM_CONVERSATION,
        SHARE_PROFILE,
        START_NEW_CONVERSATION
    }

    /**
     * Options to display when long-pressing conversations.
     */
    fun memberOptions(lobby: LobbyActivity, account: Account) {
        val memberUsername = if (account.username != null) {
            "@${account.username}"
        } else {
            ""
        }
        val timeSpoken = account.displayName
        val optionsArray = ArrayList<String>(4)
        val displayedOptions = ArrayList<LobbyOptions>(4)

        optionsArray.add(R.string.lobby_option_start_conversation.stringResource(context = lobby))
        displayedOptions.add(LobbyOptions.START_NEW_CONVERSATION)

        optionsArray.add(R.string.lobby_option_share.stringResource(context = lobby))
        displayedOptions.add(LobbyOptions.SHARE_PROFILE)

        optionsArray.add(R.string.lobby_option_remove.stringResource(context = lobby))
        displayedOptions.add(LobbyOptions.REMOVE_FROM_CONVERSATION)


        MaterialDialog.Builder(lobby)
                .title(timeSpoken)
                .content(memberUsername)
                .contentColor(R.color.s_green.colorResource())
                .itemsColor(R.color.s_medium_grey.colorResource())
                .items(optionsArray)
                .itemsCallback { materialDialog, view, position, charSequence ->

                    when (displayedOptions[position]) {
                        LobbyOptions.START_NEW_CONVERSATION -> lobby.startConversationWithContact(account)
                        LobbyOptions.SHARE_PROFILE -> lobby.shareProfile(account)
                        LobbyOptions.REMOVE_FROM_CONVERSATION -> lobby.displayRemovalConfirmationDialog(account.id)
                    }
                }
                .show()
    }

}