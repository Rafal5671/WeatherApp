package com.example.pogoda

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class DetailActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var refreshButton: Button
    private lateinit var handler: Handler
    private lateinit var refreshRunnable: Runnable
    private var refreshTime = 300000 //milisekundy
    private var isTablet: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        isTablet = resources.getBoolean(R.bool.isTablet)

        if (isTablet) {
            replaceFragments()
        } else {
            viewPager = findViewById(R.id.viewPager)
            setupViewPager()
        }

        refreshButton = findViewById(R.id.refreshButton)
        setupRefreshButton()

        val favoriteButton: ImageView = findViewById(R.id.favoriteButton)
        favoriteButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        setupTimedRefresh()

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Dane mogą być nieaktualne, brak dostępu do Internetu.", Toast.LENGTH_LONG).show()
        }

    }
    private fun replaceFragments() {
        val selectedCity = intent.getStringExtra("selectedCity") ?: return
        replaceFragment(R.id.fragmentContainer1, CurrentWeatherFragment().apply {
            arguments = Bundle().apply {
                putString("city", selectedCity)
            }
        }, "f0")

        replaceFragment(R.id.fragmentContainer2, WindWeatherFragment().apply {
            arguments = Bundle().apply {
                putString("city", selectedCity)
            }
        }, "f1")

        replaceFragment(R.id.fragmentContainer3, ForecastWeatherFragment().apply {
            arguments = Bundle().apply {
                putString("city", selectedCity)
            }
        }, "f2")
    }
    private fun replaceFragment(containerId: Int, fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .commit()
    }

    private fun setupTimedRefresh()
    {
        handler = Handler(mainLooper)
        refreshRunnable = object : Runnable {
            override fun run() {
                refreshFragments()
                handler.postDelayed(this, refreshTime.toLong())
            }
        }
        handler.post(refreshRunnable)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    private fun refreshFragments() {
        val fragment1 = supportFragmentManager.findFragmentByTag("f0")
        if (fragment1 is CurrentWeatherFragment) {
            val city1 = fragment1.arguments?.getString("city")
            city1?.let {
                fragment1.updateWeatherData(normalizeCityName(it), forceRefresh = true)
            }
        }

        val fragment2 = supportFragmentManager.findFragmentByTag("f1")
        if (fragment2 is WindWeatherFragment) {
            val city2 = fragment2.arguments?.getString("city")
            city2?.let {
                fragment2.updateWindData(normalizeCityName(it), forceRefresh = true)
            }
        }

        val fragment3 = supportFragmentManager.findFragmentByTag("f2")
        if (fragment3 is ForecastWeatherFragment) {
            val city3 = fragment3.arguments?.getString("city")
            city3?.let {
                fragment3.updateForecastData(normalizeCityName(it), forceRefresh = true)
            }
        }
        ///Toast.makeText(this, "Odświeżam dane...", Toast.LENGTH_SHORT).show()
    }
    private fun normalizeCityName(city: String): String {
        val polishToEnglishMap = mapOf(
            'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
            'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
            'Ą' to 'A', 'Ć' to 'C', 'Ę' to 'E', 'Ł' to 'L', 'Ń' to 'N',
            'Ó' to 'O', 'Ś' to 'S', 'Ź' to 'Z', 'Ż' to 'Z'
        )

        return city.map { polishToEnglishMap[it] ?: it }.joinToString("")
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun setupViewPager() {
        val selectedCity = intent.getStringExtra("selectedCity") ?: return
        val adapter = WeatherPagerAdapter(this, selectedCity)
        viewPager.adapter = adapter
    }

    private fun setupRefreshButton() {
        refreshButton.setOnClickListener {
            refreshFragments()
        }
    }
}
