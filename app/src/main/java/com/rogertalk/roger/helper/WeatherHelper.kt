package com.rogertalk.roger.helper

import android.content.Context
import android.view.View
import com.rogertalk.kotlinjubatus.fadeIn
import com.rogertalk.kotlinjubatus.fadeOut
import com.rogertalk.roger.R
import com.rogertalk.roger.helper.WeatherHelper.BgWeatherState.*
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.repo.WeatherRepo
import com.rogertalk.roger.utils.extensions.isDawn
import com.rogertalk.roger.utils.extensions.isDaytime
import com.rogertalk.roger.utils.extensions.isDusk
import com.rogertalk.roger.utils.extensions.isNight
import java.util.*

class WeatherHelper(val cardNightBackgroundView: View,
                    val cardSunsetBackgroundView: View,
                    val cardDayBackgroundView: View,
                    val cardSunriseBackgroundView: View,
                    val smileBackgroundView: View) {

    enum class BgWeatherState {
        NONE, DAY, SUNRISE, SUNSET, NIGHT
    }

    companion object {

        fun getWeatherIconText(participant: Account, participantTime: Calendar): String {
            if (!WeatherRepo.participantHasWeather(participant.id)) {
                return ""
            }

            return when (WeatherRepo.weatherForParticipant(participant.id)) {
                "cloudy" -> " C "
                "fog" -> " O "
                "partly-cloudy" -> if (participantTime.isNight) " J " else " C "
                "rain" -> if (participantTime.isNight) " K" else " R "
                "sleet" -> " X "
                "snow" -> " W "
                "wind" -> " b "
                else ->
                    // Clear
                    if (participantTime.isNight) " J " else " A "
            }
        }

        fun getWeatherTextRepresentation(context: Context, participant: Account,
                                         participantTime: Calendar): String {
            if (!WeatherRepo.participantHasWeather(participant.id)) {
                return ""
            }

            return when (WeatherRepo.weatherForParticipant(participant.id)) {
                "cloudy" -> context.getString(R.string.glimpses_cloudy)
                "fog" -> context.getString(R.string.glimpses_fog)
                "partly-cloudy" -> context.getString(R.string.glimpses_partly_cloudy)
                "rain" -> context.getString(R.string.glimpses_rain)
                "sleet" -> context.getString(R.string.glimpses_sleet)
                "snow" -> context.getString(R.string.glimpses_snow)
                "wind" -> context.getString(R.string.glimpses_wind)
                else -> if (participantTime.isNight) context.getString(R.string.glimpses_night) else context.getString(R.string.glimpses_clear)
            }
        }

        fun getWeatherTemperature(context: Context, participant: Account): String {
            if (!WeatherRepo.participantHasWeather(participant.id)) {
                return ""
            }

            var temperatureValue = WeatherRepo.temperatureForParticipant(participant.id)

            val useCelcius = context.resources.getBoolean(R.bool.use_celcius)
            if (!useCelcius) {
                // Convert to Fahrenheit
                temperatureValue = temperatureValue * 9 / 5 + 32
            }

            return " • " + temperatureValue.toInt().toString() + "°"
        }
    }


    private var currentBackgroundState = NONE

    fun updateBackground(participantTime: Calendar) {
        var newState = DAY
        if (participantTime.isDaytime) {
            newState = DAY
        } else if (participantTime.isNight) {
            newState = NIGHT
        } else if (participantTime.isDawn) {
            newState = SUNRISE
        } else if (participantTime.isDusk) {
            newState = SUNSET
        }

        if (currentBackgroundState == newState) {
            // All set really
            return
        }

        if (newState == SUNRISE) {
            smileBackgroundView.setBackgroundResource(R.drawable.sunrise_background)
            cardSunriseBackgroundView.fadeIn()
            cardDayBackgroundView.fadeOut()
            cardSunsetBackgroundView.fadeOut()
            cardNightBackgroundView.fadeOut()
        }

        if (newState == DAY) {
            smileBackgroundView.setBackgroundResource(R.drawable.day_background)
            cardSunriseBackgroundView.fadeOut()
            cardDayBackgroundView.fadeIn()
            cardSunsetBackgroundView.fadeOut()
            cardNightBackgroundView.fadeOut()
        }

        if (newState == SUNSET) {
            smileBackgroundView.setBackgroundResource(R.drawable.sunset_background)
            cardSunriseBackgroundView.fadeOut()
            cardDayBackgroundView.fadeOut()
            cardSunsetBackgroundView.fadeIn()
            cardNightBackgroundView.fadeOut()
        }

        if (newState == NIGHT) {
            smileBackgroundView.setBackgroundResource(R.drawable.night_background)
            cardSunriseBackgroundView.fadeOut()
            cardDayBackgroundView.fadeOut()
            cardSunsetBackgroundView.fadeOut()
            cardNightBackgroundView.fadeIn()
        }
    }

    fun showDayBackground() {
        val dayTime = Calendar.getInstance()
        dayTime.set(Calendar.HOUR_OF_DAY, 12)
        updateBackground(dayTime)
    }

}
