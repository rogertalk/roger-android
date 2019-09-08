package com.rogertalk.roger.network.request

import com.rogertalk.roger.models.json.WeatherList
import com.rogertalk.roger.network.ConnectivityHelper
import com.rogertalk.roger.repo.WeatherRepo
import com.rogertalk.roger.utils.extensions.appController

class WeatherRequest(val identifiers: List<Long>) : BaseRequest() {

    override fun enqueueRequest() {
        if (identifiers.isEmpty()) {
            // Don't make this request if list is empty
            return
        }
        if (!ConnectivityHelper.isConnected(appController())) {
            // Don't attempt request if offline
            return
        }

        val callback = getCallback(WeatherList::class.java)
        getRogerAPI().weather(getIdentifiersList()).enqueue(callback)
    }

    private fun getIdentifiersList(): String {
        val stringList = identifiers.map(Long::toString)
        return stringList.joinToString(",")
    }

    override fun <T : Any> handleSuccess(t: T) {
        val response = t as? WeatherList ?: return

        // Update local data
        WeatherRepo.updateWeatherFromIdentifiersList(response, identifiers)
    }
}