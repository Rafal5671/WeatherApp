package com.example.pogoda

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pogoda.api.RetrofitClient
import com.example.pogoda.api.WeatherForecastResponse
import com.example.pogoda.api.WeatherManager
import com.example.pogoda.api.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WindWeatherFragment : Fragment() {
    private lateinit var weatherManager: WeatherManager
    private lateinit var viewRoot: View
    private var cityName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cityName = it.getString("city")
        }
    }
    override fun onResume() {
        super.onResume()
        val cityName = arguments?.getString("city") ?: "Unknown City"

        // Jeśli miasto jest znane, załaduj dane o pogodzie
        if (cityName != "Unknown City") {
            updateWindData(normalizeCityName(cityName),false)
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewRoot = inflater.inflate(R.layout.fragment_wind_weather, container, false)
        cityName = arguments?.getString("city")?: "Unknown City"

        val retrofitClient = RetrofitClient()
        weatherManager = WeatherManager(api = retrofitClient.create(), context = requireContext())

        viewRoot.findViewById<TextView>(R.id.cityName).text = cityName
        cityName?.let { city -> updateWindData(normalizeCityName(city), forceRefresh = false) }
        return viewRoot
    }

    fun updateWindData(city: String, forceRefresh: Boolean){
        lifecycleScope.launch {
            try {
                val weatherData = weatherManager.getWeather(city, forceRefresh)
                val forecastData = weatherManager.getWeatherForecast(city,forceRefresh)
                withContext(Dispatchers.Main) {
                    updateWeatherViews(forecastData, weatherData)
                }
            } catch (e: Exception) {
                Log.e("WindWeatherFragment", "Failed to fetch weather data: ", e)
            }
        }
    }
    private fun updateWeatherViews(weatherData: WeatherForecastResponse, currentWeatherData: WeatherResponse) {
        val unitPreferences = getUnitPreferences()
        viewRoot.findViewById<TextView>(R.id.cityName).text = currentWeatherData.name
        val windDirection = formatWindDirection(currentWeatherData.wind.deg.toDouble(),unitPreferences["direction"]!!)
        val windSpeed = formatWindSpeed(currentWeatherData.wind.speed, unitPreferences["wind"]!!)
        val pressure = formatPressure(currentWeatherData.main.pressure, unitPreferences["pressure"]!!)
        val visibility = formatVisibility(currentWeatherData.visibility, unitPreferences["visibility"]!!)

        viewRoot.findViewById<TextView>(R.id.windSpeed).text = windSpeed
        viewRoot.findViewById<TextView>(R.id.windDirection).text = windDirection
        viewRoot.findViewById<TextView>(R.id.humidity).text = "${currentWeatherData.main.humidity}%"
        viewRoot.findViewById<TextView>(R.id.pressure).text = pressure
        viewRoot.findViewById<TextView>(R.id.cloudiness).text = "${currentWeatherData.clouds.all}%"
        viewRoot.findViewById<TextView>(R.id.visibilityValue).text = visibility
    }
    private fun getUnitPreferences(): Map<String, String?> {
        val sharedPref = activity?.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val windUnit = sharedPref?.getString("WindUnit", "Metry na sekundę (m/s)") ?: "Metry na sekundę (m/s)"
        val pressureUnit = sharedPref?.getString("PressureUnit", "Hektopaskal (hPa)") ?: "Hektopaskal (hPa)"
        val directionUnit = sharedPref?.getString("DirectionUnit", "Kierunki geograficzne") ?: "Kierunki geograficzne"
        val visibilityUnit = sharedPref?.getString("VisibilityUnit", "Kilometry (km)") ?: "Kilometry (km)"
        return mapOf("wind" to windUnit, "pressure" to pressureUnit,"direction" to directionUnit,"visibility" to visibilityUnit)
    }
    private fun formatVisibility(visibility: Int, unit: String): String {
        val convertedVisibility = when (unit) {
            "Mile (mil)" -> visibility / 1609.34
            "Kilometry (km)" -> visibility / 1000.0
            else -> visibility.toDouble()
        }
        return formatOutputVisibility(convertedVisibility, unit)
    }

    private fun formatOutputVisibility(visibility: Double, unit: String): String {
        return when (unit) {
            "Mile (mil)" -> "%.1f mil".format(visibility)
            "Kilometry (km)" -> "%.1f km".format(visibility)
            else -> "%d m".format(visibility.toInt())
        }
    }

    private fun formatWindSpeed(speed: Double, unit: String): String {
        val convertedSpeed = when (unit) {
            "Kilometry na godzinę (km/h)" -> speed * 3.6
            "Mile na godzinę (mph)" -> speed * 2.237
            else -> speed
        }
        return formatOutputWind(convertedSpeed,unit)
    }
    private fun formatWindDirection(direction: Double, unit: String): String {
        return when (unit) {
            "Stopnie" -> "%.0f°".format(direction)
            "Kierunki geograficzne" -> convertDegreeToCardinalDirection(direction)
            else -> "N/A"
        }
    }
    private fun formatOutputWind(wind: Double, unit: String): String {
        return when (unit) {
            "Kilometry na godzinę (km/h)" -> "%.0f km/h".format(wind)
            "Mile na godzinę (mph)" -> "%.0f mph".format(wind)
            else -> "%.0f m/s".format(wind)
        }
    }
    private fun formatPressure(pressure: Int, unit: String): String {
        val convertedPressure = when (unit) {
            "Milibar (mbar)" -> pressure
            "Milimetr słupa rtęci (mmHg)" -> pressure * 0.750062
            "Funt na cal kwadratowy (PSI)" -> pressure * 0.0145038
            else -> pressure
        }
        return formatOutputPressure(convertedPressure.toDouble(),unit)
    }
    private fun formatOutputPressure(pressure: Double, unit: String): String {
        return when (unit) {
            "Milibar (mbar)" -> "%.0f mbar".format(pressure)
            "Milimetr słupa rtęci (mmHg)" -> "%.0f mmHg".format(pressure)
            "Funt na cal kwadratowy (PSI)" -> "%.0f PSI".format(pressure)
            else -> "%.0f hPa".format(pressure)
        }
    }
    private fun convertDegreeToCardinalDirection(degrees: Double): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        return directions[((degrees % 360) / 45).toInt() + 1]
    }
}
