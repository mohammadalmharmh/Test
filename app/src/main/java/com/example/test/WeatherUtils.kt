package com.example.test

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for weather-related data processing.
 */
object WeatherUtils {
    /**
     * Converts a weather icon code to a drawable resource ID.
     */
    fun String.toWeatherIcon(): Int = when (this) {
        "01d" -> R.drawable.ic_sunny
        "01n" -> R.drawable.ic_clear_night
        "02d" -> R.drawable.ic_partly_cloudy
        "02n" -> R.drawable.ic_partly_cloudy_night
        "03d", "03n" -> R.drawable.ic_cloudy
        "04d", "04n" -> R.drawable.ic_broken_clouds
        "09d", "09n" -> R.drawable.ic_shower_rain
        "10d" -> R.drawable.ic_rain_day
        "10n" -> R.drawable.ic_rain_night
        "11d", "11n" -> R.drawable.ic_thunderstorm
        "13d", "13n" -> R.drawable.ic_snow
        "50d", "50n" -> R.drawable.ic_mist
        else -> R.drawable.ic_default_weather
    }

    /**
     * Converts a weather icon code to a background color resource ID for ConstraintLayout.
     */
    fun String.toWeatherBackgroundColor(): Int = when (this) {
        "01d", "01n" -> R.color.weather_sunny
        "02d", "02n", "03d", "03n", "04d", "04n" -> R.color.weather_cloudy
        "09d", "09n", "10d", "10n" -> R.color.weather_rainy
        "11d", "11n" -> R.color.weather_thunderstorm
        "13d", "13n" -> R.color.weather_snowy
        "50d", "50n" -> R.color.weather_misty
        else -> R.color.weather_default
    }

    /**
     * Formats a Unix timestamp (in seconds) to a date string like "MMM dd, EEE".
     */
    fun Long.toFormattedDate(): String =
        SimpleDateFormat("MMM dd, EEE", Locale.getDefault()).format(Date(this * 1000L))
}