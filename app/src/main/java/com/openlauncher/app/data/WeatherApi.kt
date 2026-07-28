package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature")  val temperature: Double,
    @SerializedName("windspeed")    val windspeed: Double,
    @SerializedName("weathercode")  val weathercode: Int,
    @SerializedName("is_day")       val isDay: Int
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude")         latitude: Double,
        @Query("longitude")        longitude: Double,
        @Query("current_weather")  currentWeather: Boolean = true,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("windspeed_unit")   windspeedUnit: String = "kmh"
    ): OpenMeteoResponse
}

object WeatherApi {
    private val client = OkHttpClient.Builder().build()

    val service: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)
}

// A head unit parked in a garage, or one whose GPS never locks, still has to
// show the weather. Both services below map the public IP address of the
// connection to a city, which is close enough for a temperature and a sky
// condition, and they need no key and no phone application.
// Gson builds this without the constructor, so every field stays nullable and a
// missing "success" means the service simply does not report one.
data class GeoIpPosition(
    @SerializedName("latitude")  val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("city")      val city: String?,
    @SerializedName("success")   val success: Boolean?,
    @SerializedName("error")     val error: Any?
) {
    val isUsable: Boolean
        get() = success != false && error == null && latitude != null && longitude != null
}

interface GeoIpService {
    @GET
    suspend fun locate(@Url url: String): GeoIpPosition
}

object GeoIpApi {
    private val client = OkHttpClient.Builder().build()

    // ipwho.is answers first; ipapi.co covers the day ipwho.is rate-limits.
    val endpoints = listOf("https://ipwho.is/", "https://ipapi.co/json/")

    val service: GeoIpService = Retrofit.Builder()
        .baseUrl(endpoints.first())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeoIpService::class.java)
}
