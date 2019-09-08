package com.rogertalk.roger.ui.screens

import android.os.Bundle
import com.rogertalk.roger.R
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.thirdparty.alexa.AlexaConnectionListener
import com.rogertalk.roger.thirdparty.alexa.ServiceWebViewClient
import com.rogertalk.roger.ui.screens.base.BaseAppCompatActivity
import com.rogertalk.roger.utils.constant.ALEXA_CONNECT_URL_PREFIX
import kotlinx.android.synthetic.main.webview_screen.*


class AlexaConnectScreen : BaseAppCompatActivity(),
        AlexaConnectionListener {

    companion object {
        val SUCCESS_RESULT = 1
        val FAIL_RESULT = 0
    }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_screen)
        loadWebpage()
        setResult(FAIL_RESULT)
    }

    override fun urlLoaded(url: String) {
        if (url.contains("code=") && url.contains("alexa")) {
            PrefRepo.alexaConnected = true
            setResult(SUCCESS_RESULT)
            finish()
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun loadWebpage() {
        val refreshToken = SessionRepo.session?.refreshToken ?: ""
        val urlToLoad = "$ALEXA_CONNECT_URL_PREFIX$refreshToken"

        webview.loadUrl(urlToLoad)

        setupUI()
        setupWebView()
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
        supportActionBar?.title = getString(R.string.alexa_connect_page_title)

        toolbar.setNavigationOnClickListener({
            finish()
        })
    }


}