package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import com.example.data.local.OfflineCitiesData
import com.example.domain.model.CalculationMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

data class DetectedLocationResult(
  val cityNameAr: String,
  val countryAr: String,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val suggestedMethod: CalculationMethod,
)

object LocationHelper {

  @SuppressLint("MissingPermission")
  suspend fun detectCurrentLocation(context: Context): DetectedLocationResult? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return null

    val location = withTimeoutOrNull(7_000L) {
      requestFreshOrLastLocation(context, locationManager)
    } ?: getBestLastKnown(locationManager) ?: return null

    val nearest = OfflineCitiesData.findNearestCity(location.latitude, location.longitude).first
    var resolvedCityName = nearest.nameAr
    var resolvedCountry = nearest.countryAr

    // Try platform Geocoder for exact locality if available
    withContext(Dispatchers.IO) {
      runCatching {
        if (Geocoder.isPresent()) {
          val geocoder = Geocoder(context, Locale("ar"))
          @Suppress("DEPRECATION")
          val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
          val addr = addresses?.firstOrNull()
          val locality = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
          if (!locality.isNullOrBlank()) {
            resolvedCityName = locality
          }
          if (!addr?.countryName.isNullOrBlank()) {
            resolvedCountry = addr?.countryName ?: resolvedCountry
          }
        }
      }
    }

    return DetectedLocationResult(
      cityNameAr = resolvedCityName,
      countryAr = resolvedCountry,
      latitude = location.latitude,
      longitude = location.longitude,
      timeZoneId = TimeZone.getDefault().id,
      suggestedMethod = nearest.defaultMethod,
    )
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestFreshOrLastLocation(
    context: Context,
    locationManager: LocationManager,
  ): Location? {
    val provider = when {
      locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
      locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
      else -> return getBestLastKnown(locationManager)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return suspendCancellableCoroutine { cont ->
        val cancelSignal = CancellationSignal()
        cont.invokeOnCancellation { cancelSignal.cancel() }
        runCatching {
          locationManager.getCurrentLocation(
            provider,
            cancelSignal,
            context.mainExecutor,
          ) { loc ->
            if (cont.isActive) cont.resume(loc ?: getBestLastKnown(locationManager))
          }
        }.onFailure {
          if (cont.isActive) cont.resume(getBestLastKnown(locationManager))
        }
      }
    }

    return suspendCancellableCoroutine { cont ->
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          locationManager.removeUpdates(this)
          if (cont.isActive) cont.resume(location)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }
      cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
      runCatching {
        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
      }.onFailure {
        if (cont.isActive) cont.resume(getBestLastKnown(locationManager))
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun getBestLastKnown(locationManager: LocationManager): Location? {
    val providers = listOf(
      LocationManager.GPS_PROVIDER,
      LocationManager.NETWORK_PROVIDER,
      LocationManager.PASSIVE_PROVIDER,
    )
    var best: Location? = null
    for (p in providers) {
      val loc = runCatching { locationManager.getLastKnownLocation(p) }.getOrNull() ?: continue
      if (best == null || loc.time > best.time) {
        best = loc
      }
    }
    return best
  }
}
