package com.example.pogoda.api

data class WeatherForecastResponse(val city: CityInfo,val list: List<Forecast>)
data class Forecast(val main: Main,val weather: List<Weather>, val dt: Long)

data class CityInfo(
    val name: String,
)