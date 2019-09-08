package com.rogertalk.roger.repo

import com.rogertalk.roger.manager.WeatherManager
import com.rogertalk.roger.models.json.WeatherCondition
import com.rogertalk.roger.models.json.WeatherList
import java.util.*

object WeatherRepo {

    fun participantHasWeather(accountId: Long): Boolean {
        return WeatherManager.weatherMap.containsKey(accountId)
    }

    fun weatherForParticipant(accountId: Long): String {
        return WeatherManager.weatherMap[accountId]?.weather ?: ""
    }

    fun temperatureForParticipant(accountId: Long): Double {
        return WeatherManager.weatherMap[accountId]?.temperature ?: 0.0
    }

    fun updateWeatherFromIdentifiersList(weatherList: WeatherList, identifiersList: List<Long>) {
        // Create a map with the weather info mapped to the identifiers
        val weatherMap = HashMap<Long, WeatherCondition>(identifiersList.size)
        val actualWeatherList = weatherList.data
        for (i in 0..identifiersList.size - 1) {
            val weatherElem = actualWeatherList[i]
            if (weatherElem != null) {
                weatherMap.put(identifiersList[i], weatherElem)
            }
        }
        WeatherManager.updateWeatherData(weatherMap)
    }
}
