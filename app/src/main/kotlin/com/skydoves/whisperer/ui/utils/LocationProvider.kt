package com.skydoves.whisperer.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume


object LocationProvider {

  const val DEFAULT_LAT = 32.0853
  const val DEFAULT_LON = 34.7818

  data class Place(val lat: Double, val lon: Double, val city: String)

  fun hasPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED


  suspend fun current(context: Context): Place? {
    if (!hasPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val loc = (withTimeoutOrNull(5000L) { requestOneShot(lm) }) ?: bestLastKnown(lm) ?: return null
    return Place(loc.latitude, loc.longitude, cityName(context, loc.latitude, loc.longitude))
  }

  fun lastKnown(context: Context): Place? {
    if (!hasPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val loc = bestLastKnown(lm) ?: return null
    return Place(loc.latitude, loc.longitude, cityName(context, loc.latitude, loc.longitude))
  }

  private fun bestLastKnown(lm: LocationManager): Location? {
    val providers = listOf(
      LocationManager.GPS_PROVIDER,
      LocationManager.NETWORK_PROVIDER,
      LocationManager.PASSIVE_PROVIDER
    )
    var best: Location? = null
    for (p in providers) {
      val loc = try {
        lm.getLastKnownLocation(p)
      } catch (_: SecurityException) {
        null
      } ?: continue
      if (best == null || loc.time > best!!.time) best = loc
    }
    return best
  }

  private suspend fun requestOneShot(lm: LocationManager): Location? =
    suspendCancellableCoroutine { cont ->
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          lm.removeUpdates(this)
          if (cont.isActive) cont.resume(location)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }

      val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

      if (providers.isEmpty()) {
        if (cont.isActive) cont.resume(null)
        return@suspendCancellableCoroutine
      }

      var registered = false
      for (p in providers) {
        try {
          lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
          registered = true
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
      }
      if (!registered && cont.isActive) cont.resume(null)
      cont.invokeOnCancellation { lm.removeUpdates(listener) }
    }

  @Suppress("DEPRECATION")
  private fun cityName(context: Context, lat: Double, lon: Double): String = try {
    val addr = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
    val a = addr?.firstOrNull()
    a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: ""
  } catch (_: Exception) {
    ""
  }
}
