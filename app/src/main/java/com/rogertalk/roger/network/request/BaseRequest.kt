package com.rogertalk.roger.network.request

import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.android.AppController
import com.rogertalk.roger.event.broadcasts.ReAuthenticateEvent
import com.rogertalk.roger.network.RequestImpl
import com.rogertalk.roger.network.api.RogerAPI
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.NO_RESULT_CODE
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logError
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpURLConnection

open class BaseRequest : RequestImpl {

    override fun enqueueRequest() {
        throw UnsupportedOperationException()
    }

    fun getRestAdapter(): Retrofit {
        return AppController.instance.helper.retrofit
    }

    fun getRogerAPI(): RogerAPI {
        val requestHandler = getRestAdapter()
        return requestHandler.create(RogerAPI::class.java)
    }

    open protected fun handleFailure(error: ResponseBody?, responseCode: Int) {
        if (BuildConfig.DEBUG) {
            logError { "Failed request ($responseCode): ${error?.charStream().toString()}" }
        }
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            // Access token might have expired or became corrupt somehow
            // Reset logged in info
            PrefRepo.loggedIn = false


            // Fire re-auth event
            postEvent(ReAuthenticateEvent())
        }

    }

    open protected fun <T : Any> handleSuccess(t: T) {
        // empty
    }

    open protected fun <T : Any> getCallback(t: Class<T>): RequestCallback<T> {
        return RequestCallback(this)
    }

    protected class RequestCallback<T : Any>(val baseRequest: BaseRequest) : Callback<T> {

        override fun onResponse(call: Call<T>?, response: Response<T>?) {
            // response might be un-successful
            if (response != null) {
                if (response.isSuccessful) {
                    baseRequest.handleSuccess(response.body())
                } else {
                    baseRequest.handleFailure(response.errorBody(), response.code())
                }
            }
        }

        override fun onFailure(call: Call<T>?, t: Throwable?) {
            baseRequest.handleFailure(null, NO_RESULT_CODE)

            // Don't log network problems on production builds
            if (BuildConfig.DEBUG) {
                if (t != null) {
                    logError(t) { "Request error" }
                }
            }
        }

    }
}