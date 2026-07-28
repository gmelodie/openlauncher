package com.openlauncher.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float = 0f
)

private const val MAX_LAST_KNOWN_AGE_MS = 2 * 60 * 1000L
private const val MIN_BEARING_DISTANCE_M = 3f

class LocationCompassManager(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager   = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _location = MutableStateFlow<LocationData?>(null)
    private val _bearing  = MutableStateFlow(0f)
    private val _gravity  = MutableStateFlow<FloatArray?>(null)
    val location: StateFlow<LocationData?> = _location
    val bearing: StateFlow<Float> = _bearing

    // The altimeter reads the same accelerometer stream instead of registering a
    // second listener on the same sensor.
    val gravity: StateFlow<FloatArray?> = _gravity

    private val gravityBuf   = FloatArray(3)
    private val geomagnetic  = FloatArray(3)
    private var hasGravity     = false
    private var hasGeomagnetic = false
    // Circular low-pass filter for smooth bearing (avoids 0°/360° wrap artifacts)
    private var bearingSin   = 0f
    private var bearingCos   = 1f
    private var lastLocationForBearing: Location? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val alpha = 0.12f
                    for (i in 0..2) gravityBuf[i] = alpha * event.values[i] + (1f - alpha) * gravityBuf[i]
                    hasGravity = true
                    _gravity.value = gravityBuf.copyOf()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    hasGeomagnetic = true
                }
            }
            if (hasGravity && hasGeomagnetic) updateBearingFromSensors()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateBearingFromSensors() {
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotation, inclination, gravityBuf, geomagnetic)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotation, orientation)
        val azimuthRad = orientation[0].toDouble()
        val alpha = 0.10f
        bearingSin = alpha * sin(azimuthRad).toFloat() + (1f - alpha) * bearingSin
        bearingCos = alpha * cos(azimuthRad).toFloat() + (1f - alpha) * bearingCos
        _bearing.value = ((Math.toDegrees(atan2(bearingSin.toDouble(), bearingCos.toDouble())) + 360) % 360).toFloat()
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            _location.value = LocationData(
                latitude  = loc.latitude,
                longitude = loc.longitude,
                altitude  = loc.altitude,
                speedMps  = if (loc.hasSpeed()) loc.speed else 0f
            )
            updateBearingFromLocation(loc)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        // The interface only gained default implementations in API 30, so older
        // head units throw AbstractMethodError when these are absent.
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun updateBearingFromLocation(loc: Location) {
        if (loc.hasBearing()) {
            _bearing.value = loc.bearing
            lastLocationForBearing = loc
            return
        }
        val previous = lastLocationForBearing
        if (previous == null) {
            lastLocationForBearing = loc
            return
        }
        if (previous.distanceTo(loc) <= MIN_BEARING_DISTANCE_M) return
        _bearing.value = (previous.bearingTo(loc) + 360f) % 360f
        lastLocationForBearing = loc
    }

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return
        requestProvider(LocationManager.GPS_PROVIDER, 3000L, 5f)
        requestProvider(LocationManager.NETWORK_PROVIDER, 5000L, 10f)
    }

    @SuppressLint("MissingPermission")
    private fun requestProvider(provider: String, intervalMs: Long, minDistanceM: Float) {
        try {
            if (!locationManager.allProviders.contains(provider)) return
            locationManager.requestLocationUpdates(provider, intervalMs, minDistanceM, locationListener)
            locationManager.getLastKnownLocation(provider)
                ?.takeIf { it.isRecent() }
                ?.let { locationListener.onLocationChanged(it) }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    // A stale fix places the vehicle in the wrong city and drives speed, altitude
    // and the weather lookup from there.
    private fun Location.isRecent(): Boolean {
        val ageMs = SystemClock.elapsedRealtimeNanos() / 1_000_000 - elapsedRealtimeNanos / 1_000_000
        return ageMs in 0..MAX_LAST_KNOWN_AGE_MS
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        locationManager.removeUpdates(locationListener)
        lastLocationForBearing = null
        hasGravity = false
        hasGeomagnetic = false
    }
}
