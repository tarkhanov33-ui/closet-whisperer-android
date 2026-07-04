package com.skydoves.whisperer.core.network.service

import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoCurrent(
    val temperature_2m: Double,
    val relative_humidity_2m: Double,
    val apparent_temperature: Double,
    val weather_code: Int,
    val wind_speed_10m: Double
)

data class OpenMeteoResponse(
    val current: OpenMeteoCurrent
)

interface WeatherService {
    @GET("v1/forecast")
    suspend fun fetchCurrentWeather(
        @Query("latitude") latitude: Double = 32.0853,
        @Query("longitude") longitude: Double = 34.7818,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
    ): ApiResponse<OpenMeteoResponse>
}
