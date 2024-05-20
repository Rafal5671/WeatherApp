package com.example.pogoda.api

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val clouds: Clouds,
    val visibility: Int,
    val sys: Sys,
    val name: String,
    val dt: Long,
    val code: Int
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)
data class Clouds(
    val all: Int
)
data class Wind(
    val speed: Double,
    val deg: Int
)

data class Sys(
    val country: String
)
