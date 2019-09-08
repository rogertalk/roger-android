package com.rogertalk.roger.manager

import com.rogertalk.roger.models.json.WeatherCondition
import com.rogertalk.roger.network.request.WeatherRequest
import com.rogertalk.roger.repo.LocationRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import java.util.*

/**
 * Weather manager that takes care of requesting updates for weather and keeping data cached and up-to-date.
 * Do not instantiate, use @AppControllerHelper to get the instance for this manager if needed.
 */
object WeatherManager {

    // Map accountID to a specific weather condition
    val weatherMap: HashMap<Long, WeatherCondition>

    init {
        // Recover weather map from cache, if available
        weatherMap = PrefRepo.weatherMap
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Update in-memory and cached weather data
     */
    fun updateWeatherData(newWeatherData: HashMap<Long, WeatherCondition>) {
        weatherMap.putAll(newWeatherData)

        // Store it persistently in cache
        PrefRepo.weatherMap = weatherMap
    }

    /**
     * Call this to issue an update for the weather data (will contact server and update data).
     */
    fun updateWeather() {
        if (!LocationRepo.locationEnabled) {
            // Weather is disabled, do NOT proceed with request
            return
        }
        // TODO : control how often it issues the request
        //Get the list of the account of each stream all the
        val identifiersList = StreamCacheRepo.getCached().map { it.participants.first().id }

        WeatherRequest(identifiersList).enqueueRequest()
    }
}
