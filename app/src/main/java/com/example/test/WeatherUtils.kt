package com.example.test

import androidx.appcompat.app.AppCompatDelegate
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
     * Returns a background color resource ID for ConstraintLayout based on theme mode.
     */
    fun String.toWeatherBackgroundColor(): Int {
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        return if (isDarkMode) R.color.surface_dark else R.color.surface
    }

    /**
     * Formats a Unix timestamp (in seconds) to a date string like "MMM dd, EEE".
     */
    fun Long.toFormattedDate(): String =
        SimpleDateFormat("MMM dd, EEE", Locale.getDefault()).format(Date(this * 1000L))
}