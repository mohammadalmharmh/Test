package com.example.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * ViewModel for managing weather and forecast data.
 */
class ExploreViewModel : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>()
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _forecastState = MutableLiveData<ForecastState>()
    val forecastState: LiveData<ForecastState> = _forecastState

    private val weatherService = RetrofitInstance.retrofit.create(WeatherService::class.java)

    fun fetchWeather(city: String) {
        _weatherState.value = WeatherState.Loading
        weatherService.getWeatherByCity(city, Constants.WEATHER_API_KEY, Constants.TEMPERATURE_UNIT)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        _weatherState.value = WeatherState.Success(response.body()!!)
                    } else {
                        _weatherState.value = WeatherState.Error("Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    _weatherState.value = WeatherState.Error("Network error: ${t.localizedMessage}")
                }
            })
    }

    fun fetchForecast(city: String) {
        _forecastState.value = ForecastState.Loading
        weatherService.getForecastByCity(city, Constants.WEATHER_API_KEY, Constants.TEMPERATURE_UNIT)
            .enqueue(object : Callback<ForecastResponse> {
                override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        _forecastState.value = ForecastState.Success(response.body()!!)
                    } else {
                        _forecastState.value = ForecastState.Error("Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    _forecastState.value = ForecastState.Error("Network error: ${t.localizedMessage}")
                }
            })
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val data: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

sealed class ForecastState {
    object Loading : ForecastState()
    data class Success(val data: ForecastResponse) : ForecastState()
    data class Error(val message: String) : ForecastState()
}

interface WeatherService {
    @GET("weather")
    fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Call<WeatherResponse>

    @GET("forecast")
    fun getForecastByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Call<ForecastResponse>
}

object RetrofitInstance {
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.WEATHER_API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

data class WeatherResponse(
    val name: String,
    val sys: Sys,
    val weather: List<Weather>,
    val main: Main
)

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>
)

data class Sys(val country: String)
data class Weather(val description: String, val icon: String)
data class Main(
    val temp: Float,
    val feels_like: Float,
    val temp_min: Float,
    val temp_max: Float,
    val humidity: Int,
    val pressure: Int
)