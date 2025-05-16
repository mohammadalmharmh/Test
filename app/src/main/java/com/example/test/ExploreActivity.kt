package com.example.test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.WeatherUtils.toWeatherIcon
import com.example.test.databinding.ActivityExploreBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Activity for exploring weather by city search, displaying current weather and forecast.
 */
class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding
    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityExploreBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ExploreActivity", "Layout inflated successfully")
        } catch (e: Exception) {
            Log.e("ExploreActivity", "Error inflating layout: ${e.message}", e)
            throw e
        }

        databaseHelper = DatabaseHelper(this)

        setupSearch()
        setupBottomNavigation()
        setupRecyclerView()
        setupFavoriteButton()
        observeViewModel()
        applyThemeSafely()
    }

    override fun onResume() {
        super.onResume()
        // Only re-apply theme if necessary (e.g., after theme toggle)
        applyThemeSafely()
        Log.d("ExploreActivity", "Theme checked in onResume")
    }

    private fun applyThemeSafely() {
        val rootView = binding.root
        if (rootView != null) {
            try {
                ThemeUtils.applyTheme(this, rootView)
                Log.d("ExploreActivity", "Theme applied successfully")
            } catch (e: Exception) {
                Log.e("ExploreActivity", "Error applying theme: ${e.message}", e)
            }
        } else {
            Log.e("ExploreActivity", "Root view is null, skipping theme application")
        }
    }

    private fun setupSearch() {
        binding.cityInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val city = binding.cityInput.text.toString().trim()
                if (city.isNotEmpty()) {
                    viewModel.fetchWeather(city)
                    viewModel.fetchForecast(city)
                    updateFavoriteButton(city)
                } else {
                    showToast("Please enter a city name")
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupBottomNavigation() {
        with(binding.bottomNav) {
            selectedItemId = R.id.nav_explore
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(Intent(this@ExploreActivity, MainActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_explore -> true
                    R.id.nav_favorites -> {
                        showFavoritesDialog()
                        false
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.forecastRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExploreActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = ForecastAdapter(emptyList())
            addItemDecoration(HorizontalSpacingItemDecoration(spacing = 8))
        }
    }

    private fun setupFavoriteButton() {
        binding.favoriteButton.setOnClickListener {
            val city = binding.cityName.text.toString().split(",")[0].trim()
            if (city.isNotEmpty()) {
                if (databaseHelper.isFavorite(city)) {
                    databaseHelper.removeFavorite(city)
                    binding.favoriteButton.setImageResource(R.drawable.ic_star_outline)
                    showToast("$city removed from favorites")
                } else {
                    databaseHelper.addFavorite(city)
                    binding.favoriteButton.setImageResource(R.drawable.ic_star_filled)
                    showToast("$city added to favorites")
                }
            }
        }
    }

    private fun updateFavoriteButton(city: String) {
        binding.favoriteButton.setImageResource(
            if (databaseHelper.isFavorite(city)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
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
                viewModel.fetchWeather(city)
                viewModel.fetchForecast(city)
                updateFavoriteButton(city)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun observeViewModel() {
        viewModel.weatherState.observe(this) { state ->
            when (state) {
                is WeatherState.Loading -> {
                    binding.loadingIndicator.isVisible = true
                    binding.weatherCard.isVisible = false
                }
                is WeatherState.Success -> {
                    binding.loadingIndicator.isVisible = false
                    binding.weatherCard.isVisible = true
                    with(state.data) {
                        binding.cityName.text = "${name}, ${sys.country}"
                        binding.weatherDetails.text = "${weather.firstOrNull()?.description.orEmpty()} • ${main.temp}°C"
                        binding.weatherIcon.setImageResource(
                            weather.firstOrNull()?.icon?.toWeatherIcon() ?: R.drawable.ic_default_weather
                        )
                    }
                }
                is WeatherState.Error -> {
                    binding.loadingIndicator.isVisible = false
                    showToast(state.message)
                }
            }
        }

        viewModel.forecastState.observe(this) { state ->
            when (state) {
                is ForecastState.Success -> {
                    binding.forecastRecyclerView.adapter = ForecastAdapter(state.data.list)
                }
                is ForecastState.Error -> showToast(state.message)
                else -> Unit
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}