package com.rogertalk.roger.models.json

import java.io.Serializable

class WeatherCondition(val temperature: Double,
                       val weather: String,
                       val cloudiness: Double,
                       val precipitation: Double,
                       val wind: Double) : Serializable
