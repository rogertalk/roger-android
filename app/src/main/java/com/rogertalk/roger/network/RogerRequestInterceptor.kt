package com.rogertalk.roger.network

import com.rogertalk.kotlinjubatus.utils.DeviceUtils
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.utils.android.AccessibilityUtils
import com.rogertalk.roger.utils.extensions.appHelper
import okhttp3.Interceptor
import okhttp3.Response
import java.util.*


class RogerRequestInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response? {
        val loggedIn = PrefRepo.loggedIn

        val request = chain.request()

        // modified request
        val modifiedRequest = request.newBuilder()
                .header("User-Agent", "RogerAndroid/${appHelper().appVersion} TalkBack/${AccessibilityUtils.cachedScreenReaderActive}")


        // Add i18n to headers
        val lang = Locale.getDefault().language // "en"
        val langComplex = Locale.getDefault().toString() // "en_US"
        modifiedRequest.addHeader("Accept-Language", "$langComplex, $lang;q=0.9")

        // Add device info to headers
        modifiedRequest.addHeader("X-Device", DeviceUtils.deviceName)

        // add authorization header if available!
        if (loggedIn) {
            val accessToken = SessionRepo.accessToken()

            modifiedRequest.addHeader("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(modifiedRequest.build())
    }
}