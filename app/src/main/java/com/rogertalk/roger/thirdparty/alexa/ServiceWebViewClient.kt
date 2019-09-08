package com.rogertalk.roger.thirdparty.alexa

import android.webkit.WebView
import android.webkit.WebViewClient

class ServiceWebViewClient(val listener: AlexaConnectionListener) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url != null) {
            listener.urlLoaded(url)
        }
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        view?.loadUrl(url)
        return false
    }
}
