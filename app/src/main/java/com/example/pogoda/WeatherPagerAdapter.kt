package com.example.pogoda

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WeatherPagerAdapter(activity: AppCompatActivity, private val city: String) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            0 -> CurrentWeatherFragment()
            1 -> WindWeatherFragment()
            2 -> ForecastWeatherFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }

        fragment.arguments = Bundle().apply {
            putString("city", city)
        }
        return fragment
    }
}
