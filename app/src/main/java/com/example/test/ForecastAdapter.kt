package com.example.test

import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.test.WeatherUtils.toFormattedDate
import com.example.test.WeatherUtils.toWeatherBackgroundColor
import com.example.test.WeatherUtils.toWeatherIcon
import com.google.android.material.card.MaterialCardView

class ForecastAdapter(
    private val forecasts: List<ForecastItem>,
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView? = itemView.findViewById(R.id.cardView)
        val container: ConstraintLayout = itemView.findViewById(R.id.forecastContainer)
        val dateText: TextView = itemView.findViewById(R.id.forecastDate)
        val tempText: TextView = itemView.findViewById(R.id.forecastTemp)
        val descriptionText: TextView = itemView.findViewById(R.id.forecastDescription)
        val icon: ImageView = itemView.findViewById(R.id.forecastIcon)
        val humidityText: TextView = itemView.findViewById(R.id.forecastHumidity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        Log.d("ForecastAdapter", "ViewHolder created")
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecasts[position]
        val context = holder.itemView.context
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val textColor = ContextCompat.getColor(context, if (isDarkMode) R.color.text_primary_dark else R.color.text_primary)
        val secondaryTextColor = ContextCompat.getColor(context, if (isDarkMode) R.color.text_secondary_dark else R.color.text_secondary)
        val iconTint = ContextCompat.getColor(context, if (isDarkMode) R.color.icon_tint_dark else R.color.icon_tint)
        val cardBackgroundColor = try {
            ContextCompat.getColor(context, if (isDarkMode) R.color.surface_dark else R.color.surface)
        } catch (e: Exception) {
            Log.e("ForecastAdapter", "Error loading card background color: ${e.message}")
            0xFF1E3A8A.toInt() // Hardcoded surface_dark (#1E3A8A)
        }

        with(holder) {
            if (cardView != null) {
                cardView.setCardBackgroundColor(cardBackgroundColor)
                Log.d("ForecastAdapter", "Set card background to ${if (isDarkMode) "surface_dark (#1E3A8A, $cardBackgroundColor)" else "surface ($cardBackgroundColor)"} at position $position")
            } else {
                Log.e("ForecastAdapter", "cardView is null at position $position, skipping background set")
            }

            dateText.text = forecast.dt.toFormattedDate()
            dateText.setTextColor(textColor)
            tempText.text = "${forecast.main.temp}°C"
            tempText.setTextColor(ContextCompat.getColor(context, if (isDarkMode) R.color.primary_dark else R.color.primary))
            descriptionText.text = forecast.weather.firstOrNull()?.description.orEmpty()
            descriptionText.setTextColor(secondaryTextColor)
            humidityText.text = "Humidity: ${forecast.main.humidity}%"
            humidityText.setTextColor(secondaryTextColor)
            try {
                icon.setImageResource(
                    forecast.weather.firstOrNull()?.icon?.toWeatherIcon() ?: R.drawable.ic_default_weather
                )
                icon.clearColorFilter() // Skip tinting to preserve original icon color
                Log.d("ForecastAdapter", "Preserved original color for forecastIcon at position $position")
            } catch (e: Exception) {
                Log.e("ForecastAdapter", "Error setting icon: ${e.message}")
                icon.setImageResource(R.drawable.ic_default_weather)
                icon.clearColorFilter()
            }

            try {
                val weatherBackgroundColor = ContextCompat.getColor(
                    context,
                    forecast.weather.firstOrNull()?.icon?.toWeatherBackgroundColor() ?: (if (isDarkMode) R.color.surface_dark else R.color.primary)
                )
                container.background = ColorDrawable(weatherBackgroundColor)
                Log.d("ForecastAdapter", "Set container background for ${forecast.weather.firstOrNull()?.description} (icon: ${forecast.weather.firstOrNull()?.icon}): $weatherBackgroundColor at position $position")
            } catch (e: Exception) {
                Log.e("ForecastAdapter", "Error setting container background: ${e.message}")
                container.background = ColorDrawable(ContextCompat.getColor(context, if (isDarkMode) R.color.surface_dark else R.color.primary))
            }
            Log.d("ForecastAdapter", "Bound item at position $position, date: ${dateText.text}, temp: ${tempText.text}")
        }
    }

    override fun getItemCount(): Int = forecasts.size
}