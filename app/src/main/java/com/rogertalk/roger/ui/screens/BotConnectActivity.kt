package com.rogertalk.roger.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.BotCacheManager
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.thirdparty.alexa.AlexaConnectionListener
import com.rogertalk.roger.thirdparty.alexa.ServiceWebViewClient
import com.rogertalk.roger.ui.screens.base.BaseAppCompatActivity
import com.rogertalk.roger.utils.log.logDebug
import kotlinx.android.synthetic.main.webview_screen.*
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE


class BotConnectActivity : BaseAppCompatActivity(),
        AlexaConnectionListener {

    companion object {
        val SUCCESS_RESULT = 1
        val FAIL_RESULT = 0

        private val EXTRA_URL = "url"
        private val EXTRA_BOT_NAME = "botName"
        private val EXTRA_NAME_ID = "nameId"
        private val EXTRA_FINISH_PATTERN = "finishPattern"

        fun start(context: Context, url: String, finishPattern: String,
                  botName: String, nameId: String): Intent {
            val startIntent = Intent(context, BotConnectActivity::class.java)
            startIntent.putExtra(EXTRA_URL, url)
            startIntent.putExtra(EXTRA_FINISH_PATTERN, finishPattern)
            startIntent.putExtra(EXTRA_BOT_NAME, botName)
            startIntent.putExtra(EXTRA_NAME_ID, nameId)
            return startIntent
        }
    }

    private val urlToLoad: String by lazy(NONE) { intent.getStringExtra(EXTRA_URL) }
    private val finishPattern: String by lazy(NONE) { intent.getStringExtra(EXTRA_FINISH_PATTERN) }
    private val botName: String by lazy(NONE) { intent.getStringExtra(EXTRA_BOT_NAME) }
    private val botNameId: String by lazy(NONE) { intent.getStringExtra(EXTRA_BOT_NAME) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_screen)
        loadWebpage()
        setResult(FAIL_RESULT)

        logDebug { "FinishPattern: $finishPattern" }
        // TODO : Add progress bar to title
    }

    override fun urlLoaded(url: String) {
        logDebug { "URL: $url" }
        if (url.contains(finishPattern)) {
            logDebug { "Finish pattern detected" }
            setResult(SUCCESS_RESULT)

            // Add this bot to available services
            BotCacheManager.markBotAsConnected(botNameId)

            // End this screen
            finish()
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun loadWebpage() {
        val refreshToken = SessionRepo.session?.refreshToken ?: ""
        val headers = HashMap<String, String>()
        headers.put("X-RefreshToken", refreshToken)

        //TODO : Remove the need for javascript on Roger
        webview.settings.javaScriptEnabled = true
        webview.settings.javaScriptCanOpenWindowsAutomatically = true

        setupUI()
        setupWebView()

        webview.loadUrl(urlToLoad, headers)
    }

    private fun setupUI() {
        setupToolbar()
    }

    private fun setupWebView() {
        webview.setWebViewClient(ServiceWebViewClient(this))
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = botName
        toolbar.setNavigationOnClickListener({
            finish()
        })
    }


}