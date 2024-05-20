package com.example.pogoda

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.pogoda.api.Forecast
import com.example.pogoda.api.Main
import com.example.pogoda.api.RetrofitClient
import com.example.pogoda.api.Weather
import com.example.pogoda.api.WeatherForecastResponse
import com.example.pogoda.api.WeatherManager
import com.example.pogoda.api.WeatherResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ForecastWeatherFragment : Fragment() {
    private lateinit var weatherManager: WeatherManager
    private lateinit var viewRoot: View
    private var cityName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cityName = it.getString("city")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewRoot = inflater.inflate(R.layout.fragment_forecast_weather, container, false)
        cityName = arguments?.getString("city") ?: "Unknown City"

        val retrofitClient = RetrofitClient()
        weatherManager = WeatherManager(api = retrofitClient.create(), context = requireContext())

        viewRoot.findViewById<TextView>(R.id.cityName).text = cityName

        updateForecastData(normalizeCityName(cityName!!), false)

        return viewRoot
    }
    override fun onResume() {
        super.onResume()
        cityName?.let {
            city -> updateForecastData(normalizeCityName(city), false)
        }
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
    fun updateForecastData(city: String, forceRefresh: Boolean) {
        lifecycleScope.launch {
            try {
                val forecastData = weatherManager.getWeatherForecast(city, forceRefresh)
                updateForecastViews(forecastData)
            } catch (e: Exception) {
                Log.e("ForecastWeatherFragment", "Failed to fetch weather data: ", e)
            }
        }
    }

    private fun updateForecastViews(forecastData: WeatherForecastResponse) {
        val cityName = forecastData.city.name
        viewRoot.findViewById<TextView>(R.id.cityName).text = cityName

        val forecasts = forecastData.list
        val unitPreferences = getUnitPreferences()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val today = dateFormat.format(Date())
        val dailyForecasts = forecasts.groupBy {
            dateFormat.format(Date(it.dt * 1000))
        }.filter {
            it.key > today
        }

        dailyForecasts.entries.sortedBy { it.key }.take(4).forEachIndexed { index, entry ->
            val dayForecasts = entry.value

            val noonForecast = dayForecasts.find {
                val time = timeFormat.format(Date(it.dt * 1000))
                time in "11:00".."13:00"
            } ?: dayForecasts.first()

            val iconCode = noonForecast.weather.firstOrNull()?.icon?.replace('n', 'd') ?: ""
            val minTemp = dayForecasts.minOf { it.main.temp_min }
            val maxTemp = dayForecasts.maxOf { it.main.temp_max }
            val formattedMinTemp = formatTemperature(minTemp, unitPreferences["temperature"]!!)
            val formattedMaxTemp = formatTemperature(maxTemp, unitPreferences["temperature"]!!)

            val dayLabelId = resources.getIdentifier("dayLabel${index + 1}", "id", context?.packageName)
            val iconId = resources.getIdentifier("weatherIcon${index + 1}", "id", context?.packageName)
            val tempRangeId = resources.getIdentifier("temperatureRange${index + 1}", "id", context?.packageName)
            val maxTempId = resources.getIdentifier("maxTemperature${index + 1}", "id", context?.packageName)
            val minTempId = resources.getIdentifier("minTemperature${index + 1}", "id", context?.packageName)

            viewRoot.findViewById<ImageView>(iconId)?.let {
                loadWeatherIcon(it, iconCode)
            }
            viewRoot.findViewById<TextView>(dayLabelId)?.text = sdf.format(Date(noonForecast.dt * 1000L))
            viewRoot.findViewById<TextView>(tempRangeId)?.text = "$formattedMaxTemp / $formattedMinTemp"
            viewRoot.findViewById<TextView>(maxTempId)?.text = formattedMaxTemp
            viewRoot.findViewById<TextView>(minTempId)?.text = formattedMinTemp
        }
    }


    private fun getUnitPreferences(): Map<String, String> {
        val sharedPref = activity?.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val temperatureUnit = sharedPref?.getString("TemperatureUnit", "Celsius (°C)") ?: "Celsius (°C)"
        return mapOf("temperature" to temperatureUnit)
    }
    private fun formatTemperature(tempCelsius: Double, unit: String): String {
        val temperature = when (unit) {
            "Fahrenheit (°F)" -> convertCelsiusToFahrenheit(tempCelsius)
            "Kelvin (K)" -> convertCelsiusToKelvin(tempCelsius)
            else -> tempCelsius
        }
        return formatOutput(temperature, unit)
    }

    private fun convertCelsiusToFahrenheit(celsius: Double): Double {
        return celsius * 1.8 + 32
    }

    private fun convertCelsiusToKelvin(celsius: Double): Double {
        return celsius + 273.15
    }

    private fun formatOutput(temperature: Double, unit: String): String {
        return when (unit) {
            "Fahrenheit (°F)" -> "%.0f°F".format(temperature)
            "Kelvin (K)" -> "%.0fK".format(temperature)
            else -> "%.0f°C".format(temperature)
        }
    }
    private fun loadWeatherIcon(imageView: ImageView, iconCode: String) {
        val imageUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }
}


