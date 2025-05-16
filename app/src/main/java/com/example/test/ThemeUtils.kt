package com.example.test

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

object ThemeUtils {
    fun applyTheme(context: Context, rootView: View?) {
        if (rootView == null) {
            Log.e("ThemeUtils", "Root view is null, skipping theme application")
            return
        }
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val textColor = ContextCompat.getColor(context, if (isDarkMode) R.color.text_primary_dark else R.color.text_primary)
        val iconTint = ContextCompat.getColor(context, if (isDarkMode) R.color.icon_tint_dark else R.color.icon_tint)
        val navItemTint = ContextCompat.getColor(context, if (isDarkMode) R.color.nav_item_tint_dark else R.color.nav_item_tint)
        val cardBackgroundColor = try {
            ContextCompat.getColor(context, if (isDarkMode) R.color.surface_dark else R.color.surface)
        } catch (e: Exception) {
            Log.e("ThemeUtils", "Error loading card background color: ${e.message}")
            0xFF1E3A8A.toInt() // Hardcoded surface_dark (#1E3A8A)
        }

        try {
            // Update main layout background
            rootView.setBackgroundResource(if (isDarkMode) R.drawable.gradient_background_dark else R.drawable.gradient_background)
            Log.d("ThemeUtils", "Applied background to rootView (${rootView.javaClass.simpleName})")

            // Update toolbar
            rootView.findViewById<Toolbar>(R.id.toolbar)?.let { toolbar ->
                toolbar.setBackgroundResource(if (isDarkMode) R.drawable.bar_dark else R.drawable.bar)
                toolbar.children.forEach { child ->
                    if (child is TextView) child.setTextColor(textColor)
                }
                Log.d("ThemeUtils", "Applied toolbar background and text color")
            } ?: Log.w("ThemeUtils", "Toolbar not found, skipping")

            // Update weather card
            rootView.findViewById<MaterialCardView>(R.id.weatherCard)?.let { weatherCard ->
                weatherCard.setCardBackgroundColor(cardBackgroundColor)
                Log.d("ThemeUtils", "Set weatherCard background to ${if (isDarkMode) "surface_dark (#1E3A8A, $cardBackgroundColor)" else "surface ($cardBackgroundColor)"}")
                val weatherCardContent = weatherCard.findViewById<ConstraintLayout>(R.id.weatherCardContent)
                weatherCardContent?.setBackgroundResource(
                    if (isDarkMode) R.drawable.hero_gradient_dark else R.drawable.hero_gradient
                )?.also { Log.d("ThemeUtils", "Applied weatherCardContent background") }
                // Update text views if they exist
                listOf(R.id.currentCityText, R.id.currentWeatherText, R.id.weatherDescription, R.id.cityName, R.id.weatherDetails)
                    .forEach { id ->
                        weatherCard.findViewById<TextView>(id)?.let { textView ->
                            textView.setTextColor(ContextCompat.getColor(context, if (isDarkMode) R.color.text_primary_dark else R.color.text_primary))
                            Log.d("ThemeUtils", "Set text color for ID $id to ${if (isDarkMode) "text_primary_dark" else "text_primary"}")
                        }
                    }
                weatherCard.findViewById<ImageView>(R.id.weatherIcon)?.let { icon ->
                    icon.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
                    Log.d("ThemeUtils", "Set weatherIcon tint")
                }
            } ?: Log.e("ThemeUtils", "weatherCard not found, skipping styling")

            // Update details card (MainActivity only)
            rootView.findViewById<MaterialCardView>(R.id.detailsCard)?.let { detailsCard ->
                detailsCard.setCardBackgroundColor(cardBackgroundColor)
                detailsCard.findViewById<LinearLayout>(R.id.detailsCardContent)?.children?.forEach { view ->
                    if (view is LinearLayout) {
                        view.children.forEach { child ->
                            if (child is TextView) child.setTextColor(textColor)
                            if (child is ImageView) child.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
                        }
                    }
                }
                Log.d("ThemeUtils", "Applied detailsCard styling")
            } ?: Log.w("ThemeUtils", "detailsCard not found, skipping")

            // Update bottom navigation
            rootView.findViewById<BottomNavigationView>(R.id.bottomNav)?.let { bottomNav ->
                bottomNav.setBackgroundResource(
                    if (isDarkMode) R.drawable.bottom_nav_background_dark else R.drawable.bottom_nav_background
                )
                bottomNav.itemIconTintList = ColorStateList.valueOf(navItemTint)
                bottomNav.itemTextColor = ColorStateList.valueOf(navItemTint)
                Log.d("ThemeUtils", "Applied bottomNav styling")
            } ?: Log.w("ThemeUtils", "bottomNav not found, skipping")

            // Update buttons
            listOf(R.id.refreshButton, R.id.themeToggleButton, R.id.favoriteButton).forEach { id ->
                rootView.findViewById<ImageButton>(id)?.let { button ->
                    button.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
                    Log.d("ThemeUtils", "Set tint for button ID $id")
                }
            }
        } catch (e: Exception) {
            Log.e("ThemeUtils", "Error applying theme: ${e.message}", e)
        }
    }
}