package com.example.domain.math

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {
  private const val KAABA_LAT = 21.422487
  private const val KAABA_LNG = 39.826206
  private const val EARTH_RADIUS_KM = 6371.0

  /**
   * Returns Qibla direction in degrees [0, 360) clockwise from True North.
   */
  fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
    val lat1 = Math.toRadians(latitude)
    val lon1 = Math.toRadians(longitude)
    val lat2 = Math.toRadians(KAABA_LAT)
    val lon2 = Math.toRadians(KAABA_LNG)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val bearingRad = atan2(y, x)
    val bearingDeg = (bearingRad * 180.0 / PI + 360.0) % 360.0
    return bearingDeg
  }

  /**
   * Returns great-circle distance to Makkah in kilometers.
   */
  fun distanceToMakkahKm(latitude: Double, longitude: Double): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(KAABA_LAT)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(KAABA_LNG - longitude)

    val a = sin(dLat / 2) * sin(dLat / 2) +
      cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
  }

  fun cardinalDirectionArabic(bearingDeg: Double): String {
    val normalized = ((bearingDeg % 360.0) + 360.0) % 360.0
    return when {
      normalized < 22.5 || normalized >= 337.5 -> "الشمال"
      normalized < 67.5 -> "الشمال الشرقي"
      normalized < 112.5 -> "الشرق"
      normalized < 157.5 -> "الجنوب الشرقي"
      normalized < 202.5 -> "الجنوب"
      normalized < 247.5 -> "الجنوب الغربي"
      normalized < 292.5 -> "الغرب"
      else -> "الشمال الغربي"
    }
  }
}
