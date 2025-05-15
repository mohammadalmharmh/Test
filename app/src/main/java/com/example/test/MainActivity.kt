package com.example.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import android.graphics.PorterDuff
import android.content.res.ColorStateList
import androidx.constraintlayout.widget.ConstraintLayout
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
        Log.d("MainActivity", "Before setContentView")
        try {
            setContentView(R.layout.activity_main)
            Log.d("MainActivity", "After setContentView")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error inflating layout: ${e.message}", e)
            throw e
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseHelper = DatabaseHelper(this)
        setupBottomNavigation()
        setupRefreshButton()
        setupThemeToggle()
        observeWeatherData()
        requestLocationPermissions()
        applyManualThemeChanges() // Apply initial theme
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_explore -> {
                    startActivity(Intent(this, ExploreActivity::class.java))
                    true
                }
                R.id.nav_favorites -> {
                    showFavoritesDialog()
                    false
                }
                else -> false
            }
        }
    }

    private fun setupRefreshButton() {
        findViewById<ImageButton>(R.id.refreshButton)?.setOnClickListener {
            requestLocationPermissions()
        }
    }

    private fun setupThemeToggle() {
        val themeToggle = findViewById<ImageButton>(R.id.themeToggleButton)
        if (themeToggle == null) {
            Log.e("MainActivity", "themeToggleButton is null")
            return
        }
        updateThemeToggleIcon()
        themeToggle.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            updateThemeToggleIcon()
            applyManualThemeChanges() // Apply theme changes after toggle
        }
    }

    private fun updateThemeToggleIcon() {
        val themeToggle = findViewById<ImageButton>(R.id.themeToggleButton)
        themeToggle?.setImageResource(
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                R.drawable.ic_sun else R.drawable.ic_moon
        )
    }

    private fun applyManualThemeChanges() {
        Log.d("MainActivity", "Applying theme changes")
        val mainLayout = findViewById<ConstraintLayout>(R.id.mainLayout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val weatherCard = findViewById<MaterialCardView>(R.id.weatherCard)
        val detailsCard = findViewById<MaterialCardView>(R.id.detailsCard)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)
        val themeToggleButton = findViewById<ImageButton>(R.id.themeToggleButton)
        val weatherIcon = findViewById<ImageView>(R.id.weatherIcon)

        if (mainLayout == null || toolbar == null || weatherCard == null || detailsCard == null ||
            bottomNav == null || refreshButton == null || themeToggleButton == null || weatherIcon == null) {
            Log.e("MainActivity", "One or more views are null")
            return
        }

        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val textColor = ContextCompat.getColor(this, if (isDarkMode) R.color.text_primary_dark else R.color.text_primary)
        val iconTint = ContextCompat.getColor(this, if (isDarkMode) R.color.icon_tint_dark else R.color.icon_tint)
        val navItemTint = ContextCompat.getColor(this, if (isDarkMode) R.color.nav_item_tint_dark else R.color.nav_item_tint)
        val surfaceColor = ContextCompat.getColor(this, if (isDarkMode) R.color.surface_dark else R.color.surface)

        // Update backgrounds
        mainLayout.setBackgroundResource(if (isDarkMode) R.drawable.gradient_background_dark else R.drawable.gradient_background)
        toolbar.setBackgroundResource(if (isDarkMode) R.drawable.bar_dark else R.drawable.bar)
        val weatherCardContent = weatherCard.findViewById<ConstraintLayout>(R.id.weatherCardContent)
        if (weatherCardContent != null) {
            weatherCardContent.setBackgroundResource(if (isDarkMode) R.drawable.hero_gradient_dark else R.drawable.hero_gradient)
        } else {
            Log.e("MainActivity", "weatherCardContent is null")
        }
        detailsCard.setCardBackgroundColor(surfaceColor)
        bottomNav.setBackgroundResource(if (isDarkMode) R.drawable.bottom_nav_background_dark else R.drawable.bottom_nav_background)

        // Update text colors in weatherCard (kept white for contrast)
        listOf(R.id.currentCityText, R.id.currentWeatherText, R.id.weatherDescription).forEach { id ->
            weatherCard.findViewById<TextView>(id)?.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        // Update text and icon colors in detailsCard
        detailsCard.findViewById<LinearLayout>(R.id.detailsCardContent)?.children?.forEach { view ->
            if (view is LinearLayout) {
                view.children.forEach { child ->
                    if (child is TextView) child.setTextColor(textColor)
                    if (child is ImageView) child.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
                }
            }
        }

        // Update icon tints
        refreshButton.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
        themeToggleButton.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
        weatherIcon.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)

        // Update BottomNavigationView
        bottomNav.itemIconTintList = ColorStateList.valueOf(navItemTint)
        bottomNav.itemTextColor = ColorStateList.valueOf(navItemTint)
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
            findViewById<MaterialCardView>(R.id.weatherCard).visibility =
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