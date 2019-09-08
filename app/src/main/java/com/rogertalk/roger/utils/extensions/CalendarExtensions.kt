package com.rogertalk.roger.utils.extensions

import com.rogertalk.roger.R
import java.text.SimpleDateFormat
import java.util.*

fun CalendarWithMillis(millis: Long): GregorianCalendar {
    val calendar = GregorianCalendar()
    calendar.timeInMillis = millis
    return calendar
}

fun Calendar.shortFormat(): String {
    return when {
        this.hoursAgo < 24 -> {
            val amPm = if (this.isAM) {
                R.string.short_time_am
            } else {
                R.string.short_time_pm
            }
            amPm.stringResource(this.formatForDisplay("h:mm"))
        }
        this.daysAgo < 7 -> this.formatForDisplay("EEE")
        else -> this.formatForDisplay("MMM d")
    }
}

fun Calendar.howLongAgoFormat(): String {
    val sec = this.secondsAgo
    val morningHours = 5..11
    val eveningHours = 18..23

    return when {
        sec < 12 -> R.string.long_time_now.stringResource()
        sec < 2 * 60 -> R.string.long_time_recent_minute.stringResource()
        sec < 20 * 60 -> R.string.long_time_recent_minutes.stringResource()
        sec < 40 * 60 -> R.string.long_time_recent_half_hour.stringResource()
        sec < 100 * 60 -> R.string.long_time_recent_hour.stringResource()
        sec < 5 * 60 * 60 -> R.string.long_time_recent_hours.stringResource()
        this.isToday -> when {
            this.hourOfDay in morningHours ->
                R.string.long_time_today_morning.stringResource()
            else ->
                R.string.long_time_today_other.stringResource(this.formattedHour)
        }
        this.isYesterday -> when {
            this.hourOfDay in morningHours ->
                R.string.long_time_yesterday_morning.stringResource()
            this.hourOfDay in eveningHours ->
                R.string.long_time_yesterday_evening.stringResource()
            else ->
                R.string.long_time_yesterday_other.stringResource(this.formattedHour)
        }
        this.daysAgo < 7 ->
            R.string.long_time_past_week.stringResource(this.formatForDisplay("EEEE"))
        else ->
            R.string.long_time_other.stringResource(this.formatForDisplay("MMMM d"))
    }
}


fun Calendar.dayDifference(other: Calendar): Int {
    var a = this.clone() as Calendar
    var b = other.clone() as Calendar

    if (a.year == b.year) {
        return a.dayOfYear - b.dayOfYear
    }

    val swap = a.year < b.year
    if (swap) {
        val c = a
        a = b
        b = c
    }

    var extraDays = 0
    while (a.year > b.year) {
        a.add(Calendar.YEAR, -1)
        // getActualMaximum() is important for leap years.
        extraDays += a.getActualMaximum(Calendar.DAY_OF_YEAR)
    }

    val days = extraDays - b.dayOfYear + a.dayOfYear
    return if (swap) -days else days
}

fun Calendar.formatForDisplay(format: String): String {
    val simpleTime = SimpleDateFormat(format)
    simpleTime.timeZone = this.timeZone
    return simpleTime.format(this.time)
}

val Calendar.formattedTime: String
    get() = this.formatForDisplay("h:mm a")

val Calendar.dayOfMonth: Int
    get() = this.get(Calendar.DAY_OF_MONTH)

val Calendar.dayOfYear: Int
    get() = this.get(Calendar.DAY_OF_YEAR)

val Calendar.daysAgo: Int
    get() = GregorianCalendar().dayDifference(this)

val Calendar.formattedHour: String
    get() {
        val hour = Math.min(this.hourOfDay + (if (this.minute >= 30) 1 else 0), 23)
        val ampm = if (hour < 12) "AM" else "PM"
        val hour12 = hour % 12
        return "${if (hour12 > 0) hour12 else 12} $ampm"
    }

val Calendar.hourOfDay: Int
    get() = this.get(Calendar.HOUR_OF_DAY)

val Calendar.hoursAgo: Int
    get() = this.secondsAgo / 3600

val Calendar.millisAgo: Long
    get() = GregorianCalendar().timeInMillis - this.timeInMillis

val Calendar.minute: Int
    get() = this.get(Calendar.MINUTE)

val Calendar.month: Int
    get() = this.get(Calendar.MONTH)

val Calendar.isAM: Boolean
    get() = this.get(Calendar.AM_PM) == Calendar.AM

val Calendar.isPM: Boolean
    get() = this.get(Calendar.AM_PM) == Calendar.PM

val Calendar.isToday: Boolean
    get() = this.daysAgo == 0

val Calendar.isYesterday: Boolean
    get() = this.daysAgo == 1

val Calendar.second: Int
    get() = this.get(Calendar.SECOND)

val Calendar.secondsAgo: Int
    get() = (this.millisAgo / 1000).toInt()

val Calendar.year: Int
    get() = this.get(Calendar.YEAR)


/**
 * Time of day and weather-related
 */

fun Calendar.dayExample(): Calendar{
    this.set(Calendar.HOUR_OF_DAY, 12)
    return this
}

val Calendar.isDaytime: Boolean
    get() {
        val hourOfTheDay = this.hourOfDay
        return hourOfTheDay >= 8 && hourOfTheDay < 18
    }

val Calendar.isNight: Boolean
    get() {
        val hourOfTheDay = hourOfDay
        return hourOfTheDay >= 20 || hourOfTheDay < 6
    }

val Calendar.isDawn: Boolean
    get() {
        val hourOfTheDay = hourOfDay
        return hourOfTheDay >= 6 && hourOfTheDay < 8
    }

val Calendar.isDusk: Boolean
    get() {
        val hourOfTheDay = hourOfDay
        return hourOfTheDay >= 18 && hourOfTheDay <20
    }