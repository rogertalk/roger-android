package com.rogertalk.roger.network

import com.rogertalk.roger.utils.constant.RuntimeConstants
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


class RogerHttpClient() {
    companion object {
        fun buildHttpClient(): OkHttpClient {

            // Build request interceptor - adds authentication header when available
            val requestInterceptor = RogerRequestInterceptor()

            return OkHttpClient.Builder()
                    .addInterceptor(requestInterceptor)
                    .readTimeout(RuntimeConstants.NETWORK_READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                    .build()
        }
    }
}