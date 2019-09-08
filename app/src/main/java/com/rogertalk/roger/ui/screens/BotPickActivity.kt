package com.rogertalk.roger.ui.screens

import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.models.json.Bot
import com.rogertalk.roger.ui.adapters.BotPickAdapter
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.ui.screens.behaviors.WhiteToolbar
import com.rogertalk.roger.ui.view.GridSpacingItemDecoration
import com.rogertalk.roger.utils.extensions.stringResource
import kotlinx.android.synthetic.main.bot_pick_screen.*
import kotlinx.android.synthetic.main.white_toolbar.*

class BotPickActivity : EventAppCompatActivity(logOutIfUnauthorized = true),
        WhiteToolbar {
    override val _toolbarRightActionAnimation: AnimatorSet?
        get() = null

    override val _toolbar: Toolbar
        get() = toolbar
    override val _context: AppCompatActivity
        get() = this

    companion object {

        fun start(context: Context): Intent {
            val startIntent = Intent(context, BotPickActivity::class.java)
            return startIntent
        }
    }

    private var adapter: BotPickAdapter? = null

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bot_pick_screen)

        setupUI()
        displayBotList(BotCacheManager.cachedBotList)
    }

    //
    // PUBLIC METHODS
    //

    fun botSelected(bot: Bot) {
        // TODO : Refactor this once BE finishes IFTTT applets implementation
        // Special case for ifttt for now
        if (bot.nameId == "ifttt") {
            val uri = Uri.Builder()
                    .scheme("https")
                    .authority("ifttt.com")
                    .appendPath("roger")
                    .appendQueryParameter("embed", "true")
                    .build()

            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)


            finish()
            return
        }

        val connectURL = bot.connectURL ?: return
        val finishPattern = bot.finishPattern ?: return
        val botName = bot.title

        startActivity(BotConnectActivity.start(this, connectURL, finishPattern, botName,
                bot.nameId))

        // Fall trough to previous screen when returning
        finish()
    }

    //
    // PRIVATE METHODS
    //

    private fun setupUI() {
        val title = R.string.bots_picker_title.stringResource()
        setupToolbar(title)
        rightTopButton.beGone()
        toolbarHasBackAction { finish() }
    }

    private fun displayBotList(botList: List<Bot>) {
        // Select only services that have not been connected yet
        val unconnectedBots = botList.filterNot(Bot::connected)

        if (adapter == null) {
            val layoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
            botRecyclerView.layoutManager = layoutManager

            val spacing = resources.getDimensionPixelSize(R.dimen.default_side_padding) / 2
            val includeEdge = true
            botRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacing, includeEdge))

            adapter = BotPickAdapter(unconnectedBots, this)
            botRecyclerView.adapter = adapter

            // This controls the span size for items
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
        }
    }

    //
    // EVENT METHODS
    //

}
