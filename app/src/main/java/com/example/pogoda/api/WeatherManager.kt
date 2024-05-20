package com.example.pogoda.api

import android.content.Context
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class WeatherManager(private val api: OpenWeatherApi,private val context: Context) {
    private val apiKey = "Your Api Key"

    suspend fun getWeather(city: String, forceRefresh: Boolean = false): WeatherResponse {
        val fileName = "$city-weather.json"
        val file = File(context.filesDir, fileName)

        if (forceRefresh || shouldFetchNewData(file)) {
            try {
                val response = api.getCurrentWeatherData(city, apiKey)
                saveWeatherDataAsJson(response, city)
                return response
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    throw NoSuchElementException("City not found: $city")
                } else {
                    if (file.exists()) {
                        return readWeatherDataFromFile(file)
                    } else {
                        throw IllegalStateException("Network error and no local data available: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                if (file.exists()) {
                    return readWeatherDataFromFile(file)
                } else {
                    throw IllegalStateException("Network error and no local data available: ${e.message}")
                }
            }
        }

        if (file.exists()) {
            return readWeatherDataFromFile(file)
        } else {
            throw IllegalStateException("No available weather data and unable to fetch new data.")
        }
    }
    suspend fun getWeatherForecast(city: String,forceRefresh: Boolean): WeatherForecastResponse {
        val fileName = "$city-forecast.json"
        val file = File(context.filesDir, fileName)

        if (forceRefresh || shouldFetchNewData(file)) {
            try {
                val response = api.getWeatherForecast(city, apiKey)
                saveForecastDataAsJson(response, city)
                return response
            }
            catch (e: HttpException) {
                if (e.code() == 404) {
                    throw NoSuchElementException("City not found: $city")
                } else {
                    if (file.exists()) {
                        return readForecastDataFromFile(file)
                    } else {
                        throw IllegalStateException("Network error and no local data available: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                if (file.exists()) {
                    return readForecastDataFromFile(file)
                } else {
                    throw IllegalStateException("Network error and no local data available: ${e.message}")
                }
            }
        }

        if (file.exists()) {
            return readForecastDataFromFile(file)
        } else {
            throw IllegalStateException("No available weather data and unable to fetch new data.")
        }
    }
    private fun saveWeatherDataAsJson(weatherData: WeatherResponse, city: String) {
        val gson = Gson()
        val weatherJson = gson.toJson(weatherData)
        File(context.filesDir, "$city-weather.json").writeText(weatherJson)
    }
    private fun saveForecastDataAsJson(weatherData: WeatherForecastResponse, city: String) {
        val gson = Gson()
        val weatherJson = gson.toJson(weatherData)
        File(context.filesDir, "$city-forecast.json").writeText(weatherJson)
    }
    private fun readWeatherDataFromFile(file: File): WeatherResponse {
        val gson = Gson()
        val weatherJson = file.readText()
        return gson.fromJson(weatherJson, WeatherResponse::class.java)
    }
    private fun readForecastDataFromFile(file: File): WeatherForecastResponse {
        val gson = Gson()
        val weatherJson = file.readText()
        return gson.fromJson(weatherJson, WeatherForecastResponse::class.java)
    }
    private fun shouldFetchNewData(file: File): Boolean {
        return !file.exists() || isDataStale(file)
    }

    private fun isDataStale(file: File): Boolean {
        val lastModified = file.lastModified()
        val currentTime = System.currentTimeMillis()
        val oneHourMillis = 3600000
        return currentTime - lastModified > oneHourMillis
    }
}