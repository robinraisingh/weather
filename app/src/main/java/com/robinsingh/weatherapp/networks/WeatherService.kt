package com.robinsingh.weatherapp.networks

import com.robinsingh.weatherapp.models.WeatherResponse

import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

//Create a WeatherService interface
// START
/**
 * An Interface which defines the HTTP operations Functions.
 *we construct the full query here
 */
interface WeatherService {

    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid: String?
    ): Call<WeatherResponse>
}
// END