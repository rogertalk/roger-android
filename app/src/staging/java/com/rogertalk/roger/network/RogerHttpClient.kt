package com.rogertalk.roger.network

import com.rogertalk.roger.utils.constant.RuntimeConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class RogerHttpClient() {
    companion object {
        fun buildHttpClient(): OkHttpClient {
            // Add logging module
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC

            // Build request interceptor - adds authentication header when available
            val requestInterceptor = RogerRequestInterceptor()

            return OkHttpClient.Builder()
                    .addInterceptor(requestInterceptor)
                    .addInterceptor(logging)
                    .readTimeout(RuntimeConstants.NETWORK_READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                    .build()
        }
    }
}