package com.example.test

import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.test.WeatherUtils.toFormattedDate
import com.example.test.WeatherUtils.toWeatherBackgroundColor
import com.example.test.WeatherUtils.toWeatherIcon

/**
 * Adapter for displaying weather forecast items in a RecyclerView.
 */
class ForecastAdapter(
    private val forecasts: List<ForecastItem>,
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        with(holder) {
            dateText.text = forecast.dt.toFormattedDate()
            tempText.text = "${forecast.main.temp}°C"
            descriptionText.text = forecast.weather.firstOrNull()?.description.orEmpty()
            icon.setImageResource(
                forecast.weather.firstOrNull()?.icon?.toWeatherIcon() ?: R.drawable.ic_default_weather
            )
            humidityText.text = "Humidity: ${forecast.main.humidity}%"

            // Set background color based on weather
            val backgroundColor = ContextCompat.getColor(
                itemView.context,
                forecast.weather.firstOrNull()?.icon?.toWeatherBackgroundColor() ?: R.color.weather_default
            )
            container.background = ColorDrawable(backgroundColor)
            Log.d("ForecastAdapter", "Set background color for ${forecast.weather.firstOrNull()?.description}: $backgroundColor")
            Log.d("ForecastAdapter", "Bound item at position $position, date: ${dateText.text}, temp: ${tempText.text}")
        }
    }

    override fun getItemCount(): Int = forecasts.size
}