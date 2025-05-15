package com.example.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val weatherViewModel: WeatherViewModel by lazy {
        ViewModelProvider(this)[WeatherViewModel::class.java]
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseHelper: DatabaseHelper

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) getLastKnownLocation()
        else showToast("Location permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseHelper = DatabaseHelper(this)

        setupBottomNavigation()
        setupRefreshButton()
        observeWeatherData()
        requestLocationPermissions()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_explore -> {
                    startActivity(Intent(this, ExploreActivity::class.java))
                    true
                }
                R.id.nav_favorites -> {
                    showFavoritesDialog()
                    false // Keep home selected
                }
                else -> false
            }
        }
    }

    private fun setupRefreshButton() {
        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            requestLocationPermissions()
        }
    }

    private fun showFavoritesDialog() {
        val favorites = databaseHelper.getAllFavorites().toTypedArray()
        if (favorites.isEmpty()) {
            showToast("No favorite cities added")
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Favorite City")
            .setItems(favorites) { _, which ->
                val city = favorites[which]
                weatherViewModel.fetchWeatherByCity(city)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun observeWeatherData() {
        weatherViewModel.isLoading.observe(this) { isLoading ->
            findViewById<ProgressBar>(R.id.loadingIndicator).visibility =
                if (isLoading) View.VISIBLE else View.GONE
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.weatherCard).visibility =
                if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        weatherViewModel.weatherData.observe(this) { weather ->
            weather?.let {
                findViewById<TextView>(R.id.currentCityText).text = "${it.name}, ${it.sys.country}"
                findViewById<TextView>(R.id.currentWeatherText).text = "${it.main.temp}°C"
                findViewById<TextView>(R.id.weatherDescription).text = it.weather[0].description
                findViewById<TextView>(R.id.feelsLikeText).text = "Feels like: ${it.main.feels_like}°C"
                findViewById<TextView>(R.id.tempRangeText).text = "Min: ${it.main.temp_min}°C | Max: ${it.main.temp_max}°C"
                findViewById<TextView>(R.id.humidityText).text = "Humidity: ${it.main.humidity}%"
                findViewById<TextView>(R.id.windSpeedText).text = "Wind Speed: ${it.wind.speed} m/s"
                findViewById<TextView>(R.id.pressureText).text = "Pressure: ${it.main.pressure} hPa"
                findViewById<TextView>(R.id.lastUpdatedText).text = "Last Updated: ${
                    SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(it.dt * 1000L))
                }"
                findViewById<ImageView>(R.id.weatherIcon).setImageResource(getIconResourceForWeather(it.weather[0].icon))
            }
        }

        weatherViewModel.errorData.observe(this) { error ->
            showToast(error)
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showToast("Location permissions are not granted")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                weatherViewModel.fetchWeatherByCoordinates(it.latitude, it.longitude)
            } ?: showToast("Could not get location")
        }.addOnFailureListener {
            showToast("Failed to get location: ${it.localizedMessage}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getIconResourceForWeather(iconCode: String): Int {
        return when (iconCode) {
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
    }

    data class WeatherResponse(
        val name: String,
        val sys: Sys,
        val weather: List<Weather>,
        val main: Main,
        val wind: Wind,
        val dt: Long
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
    data class Wind(val speed: Float)

    interface WeatherService {
        @GET("weather")
        fun getWeatherByCoordinates(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): Call<WeatherResponse>

        @GET("weather")
        fun getWeatherByCity(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): Call<WeatherResponse>
    }

    object RetrofitInstance {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        fun getRetrofit(): Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    class WeatherViewModel : ViewModel() {
        val weatherData = MutableLiveData<WeatherResponse>()
        val errorData = MutableLiveData<String>()
        val isLoading = MutableLiveData<Boolean>()

        private val apiKey = "8e0ab1c934049f6c847323891aa9b241"
        private val service = RetrofitInstance.getRetrofit().create(WeatherService::class.java)

        fun fetchWeatherByCoordinates(lat: Double, lon: Double) {
            isLoading.postValue(true)
            service.getWeatherByCoordinates(lat, lon, apiKey).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    isLoading.postValue(false)
                    if (response.isSuccessful && response.body() != null) {
                        weatherData.postValue(response.body())
                    } else {
                        errorData.postValue("Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    isLoading.postValue(false)
                    errorData.postValue("Network error: ${t.localizedMessage}")
                }
            })

        }

        fun fetchWeatherByCity(city: String) {
            isLoading.postValue(true)
            service.getWeatherByCity(city, apiKey).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    isLoading.postValue(false)
                    if (response.isSuccessful && response.body() != null) {
                        weatherData.postValue(response.body())
                    } else {
                        errorData.postValue("Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    isLoading.postValue(false)
                    errorData.postValue("Network error: ${t.localizedMessage}")
                }
            })
        }
    }
}