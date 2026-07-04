package com.skydoves.whisperer.core.model

import com.squareup.moshi.Json

data class Weather(
    val location: String = "",
    val temp: Double,
    val condition: String,
    val humidity: Double = 0.0,
    val uvIndex: Double = 0.0,
    val windSpeed: Double = 0.0,
    val feelsLike: Double = 0.0
)

/** Server shape returned by GET /weather (OpenWeather passthrough). */
data class BackendWeather(
    val name: String? = null,
    val main: Main? = null,
    val weather: List<Condition> = emptyList(),
    val wind: Wind? = null
) {
    data class Main(
        @Json(name = "temp") val temperature: Double? = null,
        val humidity: Int? = null,
        @Json(name = "feels_like") val feelsLike: Double? = null
    )

    data class Condition(@Json(name = "main") val name: String? = null)

    data class Wind(val speed: Double? = null)
}
