package com.example.pogoda

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.pogoda.api.RetrofitClient
import com.example.pogoda.api.WeatherForecastResponse
import com.example.pogoda.api.WeatherManager
import com.example.pogoda.api.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrentWeatherFragment : Fragment() {
    private var cityName: String? = null
    private lateinit var weatherManager: WeatherManager
    private lateinit var viewRoot: View

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
        viewRoot = inflater.inflate(R.layout.fragment_current_weather, container, false)
        cityName = arguments?.getString("city") ?: "Unknown City"

        val retrofitClient = RetrofitClient()
        weatherManager = WeatherManager(api = retrofitClient.create(), context = requireContext())

        cityName?.let { city -> updateWeatherData(normalizeCityName(city), forceRefresh = false) }

        return viewRoot
    }
    override fun onResume() {
        super.onResume()
        cityName?.let { city -> updateWeatherData(normalizeCityName(city), forceRefresh = false) }
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

    private fun getUnitPreferences(): Map<String, String> {
        val sharedPref = activity?.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val temperatureUnit = sharedPref?.getString("TemperatureUnit", "Celsius (°C)") ?: "Celsius (°C)"
        return mapOf("temperature" to temperatureUnit)
    }

    fun updateWeatherData(city: String, forceRefresh: Boolean) {
        lifecycleScope.launch {
            try {
                val weatherData = weatherManager.getWeather(city, forceRefresh)
                val forecastData = weatherManager.getWeatherForecast(city, forceRefresh)
                withContext(Dispatchers.Main) {
                    updateWeatherViews(forecastData, weatherData)
                }
            } catch (e: NoSuchElementException) {
                withContext(Dispatchers.Main) {
                    showCityNotFoundDialog()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    private fun showErrorDialog() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Error")
                .setMessage("Failed to fetch weather data due to network issues. Please check your internet connection.")
                .setPositiveButton("OK") { _, _ ->
                    it.finish()
                }
                .show()
        }
    }
    private fun showCityNotFoundDialog() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("City Not Found")
                .setMessage("The specified city could not be found. Please check the city name and try again.")
                .setPositiveButton("OK") { _, _ ->
                    it.finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun updateWeatherViews(weatherData: WeatherForecastResponse, currentWeatherData: WeatherResponse) {
        val unitPreferences = getUnitPreferences()

        val temperature = formatTemperature(currentWeatherData.main.temp, unitPreferences["temperature"]!!)
        val realFeel = formatTemperature(currentWeatherData.main.feels_like,unitPreferences["temperature"]!!,)
        viewRoot.findViewById<TextView>(R.id.cityName).text = currentWeatherData.name
        viewRoot.findViewById<TextView>(R.id.weatherDescription).text = currentWeatherData.weather[0].description
        viewRoot.findViewById<TextView>(R.id.temperature).text = temperature
        if(calculateMaxTemp(weatherData) > currentWeatherData.main.temp_max)
            viewRoot.findViewById<TextView>(R.id.tempMax).text = formatTemperature(calculateMaxTemp(weatherData),unitPreferences["temperature"]!!)
        else
            viewRoot.findViewById<TextView>(R.id.tempMax).text = formatTemperature(currentWeatherData.main.temp_max,unitPreferences["temperature"]!!)
        viewRoot.findViewById<TextView>(R.id.tempMin).text = formatTemperature(calculateMinTemp(weatherData), unitPreferences["temperature"]!!)
        viewRoot.findViewById<TextView>(R.id.dateTime).text = convertUnixToDateTime(currentWeatherData.dt)
        viewRoot.findViewById<TextView>(R.id.realFeel).text = "RealFeel $realFeel"
        val iconUrl = "https://openweathermap.org/img/wn/${currentWeatherData.weather[0].icon}@4x.png"
        Glide.with(this@CurrentWeatherFragment)
            .load(iconUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(viewRoot.findViewById(R.id.weatherIconImageView))
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
    private fun convertUnixToDateTime(unixSeconds: Long): String {
        val date = Date(unixSeconds * 1000L)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
    private fun calculateMaxTemp(weatherData: WeatherForecastResponse): Double {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todayForecasts = weatherData.list.filter {
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date(it.dt * 1000L)) == todayDate
        }

        return todayForecasts.maxByOrNull { it.main.temp_max }?.main?.temp_max ?: 0.0
    }

    private fun calculateMinTemp(weatherData: WeatherForecastResponse): Double {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todayForecasts = weatherData.list.filter {
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date(it.dt * 1000L)) == todayDate
        }

        return todayForecasts.minByOrNull { it.main.temp_min }?.main?.temp_min ?: 0.0
    }

}



