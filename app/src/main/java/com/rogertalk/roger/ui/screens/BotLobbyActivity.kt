package com.rogertalk.roger.ui.screens

import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.roger.R
import com.rogertalk.roger.event.failure.BotQueryFailEvent
import com.rogertalk.roger.event.success.BotQuerySuccessEvent
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Bot
import com.rogertalk.roger.models.sections.LobbyListSourcesSection
import com.rogertalk.roger.network.request.BotsRequest
import com.rogertalk.roger.network.request.StreamAddParticipantsRequest
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.adapters.BotLobbyAdapter
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.ui.screens.behaviors.WhiteToolbar
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.log.logMethodCall
import kotlinx.android.synthetic.main.bot_lobby_screen.*
import kotlinx.android.synthetic.main.white_toolbar.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.longToast
import kotlin.LazyThreadSafetyMode.NONE

class BotLobbyActivity : EventAppCompatActivity(logOutIfUnauthorized = true),
        WhiteToolbar {

    override val _toolbarRightActionAnimation: AnimatorSet?
        get() = null

    override val _toolbar: Toolbar
        get() = toolbar
    override val _context: AppCompatActivity
        get() = this

    companion object {

        private val EXTRA_STREAM_ID = "streamId"

        fun start(context: Context, streamId: Long): Intent {
            val startIntent = Intent(context, BotLobbyActivity::class.java)
            startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            return startIntent
        }
    }

    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }
    private var adapter: BotLobbyAdapter? = null

    private val streamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_STREAM_ID, NO_ID) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bot_lobby_screen)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        val cachedBots = BotCacheManager.cachedBotList
        if (cachedBots.isNotEmpty()) {
            displayFeatureGroupsList(cachedBots)
        } else {
            progressDialogHelper.showWaiting()
            BotsRequest().enqueueRequest()
        }
    }

    //
    // PUBLIC METHODS
    //

    fun botSelectedPressed(bot: Bot) {
        // Add bot temporarily to the stream so we can exit immediately
        val temporaryBotAccount = Account.temporaryStreamBotParticipant(bot.title, bot.imageURL)
        StreamCacheRepo.addParticipantToStream(streamId, temporaryBotAccount)

        // Make request for adding the bot
        StreamAddParticipantsRequest(streamId, listOf(bot.nameId), null).enqueueRequest()

        finish()
    }

    fun contactSourcePressed(contactsSource: LobbyListSourcesSection.ContactsSource) {
        // Right now there's just 1 contact source on this screen, we're reusing existing code
        logMethodCall()
        startActivity(BotPickActivity.start(this))
    }

    //
    // PRIVATE METHODS
    //

    private fun setupUI() {
        val title = R.string.bots_lobby_title.stringResource()
        setupToolbar(title)
        rightTopButton.beGone()
        toolbarHasBackAction { finish() }
    }

    private fun displayFeatureGroupsList(botList: List<Bot>) {
        val connectedBots = botList.filter(Bot::connected)

        if (adapter == null) {
            val layoutManager = LinearLayoutManager(this)
            botLobbyList.layoutManager = layoutManager

            adapter = BotLobbyAdapter(connectedBots, this)
            botLobbyList.adapter = adapter
        } else {
            adapter?.updateBots(connectedBots)
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onBotsQuerySuccess(event: BotQuerySuccessEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()

        displayFeatureGroupsList(event.bots)
    }

    @Subscribe(threadMode = MAIN)
    fun onBotQueryFailure(event: BotQueryFailEvent) {
        logEvent(event)
        progressDialogHelper.dismiss()
        longToast(R.string.ob_failed_request)
        finish()
    }

}