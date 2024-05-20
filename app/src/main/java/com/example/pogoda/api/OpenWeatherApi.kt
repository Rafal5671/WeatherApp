package com.example.pogoda.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {
    @GET("weather")
    suspend fun getCurrentWeatherData(
        @Query("q") city: String?,
        @Query("appid") apiKey: String?,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl"
    ): WeatherResponse
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("q") city: String?,
        @Query("appid") apiKey: String?,
        @Query("exclude") exclude: String = "current,minutely,hourly,alerts",
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl"
    ): WeatherForecastResponse
}