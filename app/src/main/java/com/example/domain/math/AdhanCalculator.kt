package com.example.domain.math

import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.HighLatitudeRule
import com.example.domain.model.Madhhab
import com.example.domain.model.PrayerOffsets
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure offline astronomical prayer time calculator based on Jean Meeus solar coordinates
 * and standard Islamic jurisprudence twilight angles.
 */
object AdhanCalculator {

  fun calculate(
    year: Int,
    month: Int, // 1..12
    day: Int,
    latitude: Double,
    longitude: Double,
    timeZone: TimeZone,
    method: CalculationMethod,
    madhhab: Madhhab,
    offsets: PrayerOffsets = PrayerOffsets(),
    highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
  ): DailyPrayerSchedule {
    val calMidnight = Calendar.getInstance(timeZone).apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val tzOffsetHours = timeZone.getOffset(calMidnight.timeInMillis) / 3_600_000.0
    val jd = julianDay(year, month, day)

    // First-pass transit to refine solar coordinates for exact local solar noon
    val approxTransit = approximateTransit(longitude, tzOffsetHours, jd)
    val solarNoon = computeSolarNoonHours(jd, approxTransit, longitude, tzOffsetHours)

    val sunriseHours = computeSunAngleTime(
      jd = jd,
      solarNoonHours = solarNoon,
      latitude = latitude,
      longitude = longitude,
      tzOffsetHours = tzOffsetHours,
      angleBelowHorizon = 0.8333,
      isMorning = true,
    )
    val sunsetHours = computeSunAngleTime(
      jd = jd,
      solarNoonHours = solarNoon,
      latitude = latitude,
      longitude = longitude,
      tzOffsetHours = tzOffsetHours,
      angleBelowHorizon = 0.8333,
      isMorning = false,
    )

    val asrHours = computeAsrTime(
      jd = jd,
      solarNoonHours = solarNoon,
      latitude = latitude,
      shadowFactor = madhhab.shadowFactor,
    )

    var fajrHours = computeSunAngleTime(
      jd = jd,
      solarNoonHours = solarNoon,
      latitude = latitude,
      longitude = longitude,
      tzOffsetHours = tzOffsetHours,
      angleBelowHorizon = method.fajrAngle,
      isMorning = true,
    )

    val maghribHours = if (method.maghribAngle > 0.0) {
      computeSunAngleTime(
        jd = jd,
        solarNoonHours = solarNoon,
        latitude = latitude,
        longitude = longitude,
        tzOffsetHours = tzOffsetHours,
        angleBelowHorizon = method.maghribAngle,
        isMorning = false,
      )
    } else {
      sunsetHours
    }

    var ishaHours = if (method.ishaIntervalMinutes != null) {
      maghribHours + (method.ishaIntervalMinutes / 60.0)
    } else {
      computeSunAngleTime(
        jd = jd,
        solarNoonHours = solarNoon,
        latitude = latitude,
        longitude = longitude,
        tzOffsetHours = tzOffsetHours,
        angleBelowHorizon = method.ishaAngle ?: 17.0,
        isMorning = false,
      )
    }

    // High latitude safety adjustments when twilight doesn't reach requested depression angle
    if (!sunriseHours.isNaN() && !sunsetHours.isNaN()) {
      val nightDuration = (24.0 - sunsetHours) + sunriseHours
      val portionFajr = nightPortion(method.fajrAngle, nightDuration, highLatitudeRule)
      if (fajrHours.isNaN() || (sunriseHours - fajrHours) > portionFajr) {
        fajrHours = sunriseHours - portionFajr
      }

      if (method.ishaIntervalMinutes == null) {
        val ishaAngle = method.ishaAngle ?: 17.0
        val portionIsha = nightPortion(ishaAngle, nightDuration, highLatitudeRule)
        if (ishaHours.isNaN() || (ishaHours - sunsetHours) > portionIsha) {
          ishaHours = sunsetHours + portionIsha
        }
      }
    }

    val baseMillis = calMidnight.timeInMillis
    return DailyPrayerSchedule(
      fajrMillis = hoursToMillis(baseMillis, fajrHours, offsets.fajr),
      sunriseMillis = hoursToMillis(baseMillis, sunriseHours, 0),
      dhuhrMillis = hoursToMillis(baseMillis, solarNoon, offsets.dhuhr),
      asrMillis = hoursToMillis(baseMillis, asrHours, offsets.asr),
      maghribMillis = hoursToMillis(baseMillis, maghribHours, offsets.maghrib),
      ishaMillis = hoursToMillis(baseMillis, ishaHours, offsets.isha),
    )
  }

  private fun nightPortion(
    angle: Double,
    nightHours: Double,
    rule: HighLatitudeRule,
  ): Double = when (rule) {
    HighLatitudeRule.MIDDLE_OF_NIGHT -> nightHours / 2.0
    HighLatitudeRule.SEVENTH_OF_NIGHT -> nightHours / 7.0
    HighLatitudeRule.TWILIGHT_ANGLE -> (angle / 60.0) * nightHours
  }

  private fun hoursToMillis(midnightMillis: Long, fractionalHours: Double, offsetMinutes: Int): Long {
    val safeHours = if (fractionalHours.isNaN()) 12.0 else fractionalHours
    val roundedMinutes = kotlin.math.round(safeHours * 60.0).toLong() + offsetMinutes
    return midnightMillis + roundedMinutes * 60_000L
  }

  private data class SolarCoordinates(
    val declinationDeg: Double,
    val equationOfTimeMinutes: Double,
  )

  private fun julianDay(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = floor(y / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
  }

  private fun solarCoordinates(jd: Double): SolarCoordinates {
    val t = (jd - 2451545.0) / 36525.0
    val l0 = normalizeDeg(280.4664567 + 36000.76982779 * t + 0.0003032028 * t * t)
    val m = normalizeDeg(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t)
    val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(degToRad(m)) +
      (0.019993 - 0.000101 * t) * sin(degToRad(2 * m)) +
      0.000289 * sin(degToRad(3 * m))
    val trueLon = l0 + c
    val omega = 125.04 - 1934.136 * t
    val apparentLon = trueLon - 0.00569 - 0.00478 * sin(degToRad(omega))
    val meanObliquity = 23.439291 - 0.013004167 * t
    val obliquity = meanObliquity + 0.00256 * cos(degToRad(omega))

    val declination = radToDeg(asin(sin(degToRad(obliquity)) * sin(degToRad(apparentLon))))

    val y = tan(degToRad(obliquity / 2.0)).let { it * it }
    val l0Rad = degToRad(l0)
    val mRad = degToRad(m)
    val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
    val eqTimeRad = y * sin(2 * l0Rad) -
      2 * e * sin(mRad) +
      4 * e * y * sin(mRad) * cos(2 * l0Rad) -
      0.5 * y * y * sin(4 * l0Rad) -
      1.25 * e * e * sin(2 * mRad)
    val eqTimeMinutes = 4.0 * radToDeg(eqTimeRad)

    return SolarCoordinates(declination, eqTimeMinutes)
  }

  private fun approximateTransit(longitude: Double, tzOffsetHours: Double, jd: Double): Double {
    val coords = solarCoordinates(jd + 0.5 - tzOffsetHours / 24.0)
    return 12.0 + tzOffsetHours - (longitude / 15.0) - (coords.equationOfTimeMinutes / 60.0)
  }

  private fun computeSolarNoonHours(
    jd: Double,
    approxNoonHours: Double,
    longitude: Double,
    tzOffsetHours: Double,
  ): Double {
    val fractionDay = (approxNoonHours - tzOffsetHours) / 24.0
    val coords = solarCoordinates(jd + fractionDay)
    return 12.0 + tzOffsetHours - (longitude / 15.0) - (coords.equationOfTimeMinutes / 60.0)
  }

  private fun computeSunAngleTime(
    jd: Double,
    solarNoonHours: Double,
    latitude: Double,
    longitude: Double,
    tzOffsetHours: Double,
    angleBelowHorizon: Double,
    isMorning: Boolean,
  ): Double {
    // First approximation using solar noon declination
    val noonCoords = solarCoordinates(jd + (solarNoonHours - tzOffsetHours) / 24.0)
    val ha0 = hourAngle(latitude, noonCoords.declinationDeg, -angleBelowHorizon)
    if (ha0.isNaN()) return Double.NaN
    val approxHours = if (isMorning) solarNoonHours - ha0 else solarNoonHours + ha0

    // Refine at actual event time
    val eventCoords = solarCoordinates(jd + (approxHours - tzOffsetHours) / 24.0)
    val refinedNoon = 12.0 + tzOffsetHours - (longitude / 15.0) - (eventCoords.equationOfTimeMinutes / 60.0)
    val refinedHa = hourAngle(latitude, eventCoords.declinationDeg, -angleBelowHorizon)
    if (refinedHa.isNaN()) return Double.NaN
    return if (isMorning) refinedNoon - refinedHa else refinedNoon + refinedHa
  }

  private fun computeAsrTime(
    jd: Double,
    solarNoonHours: Double,
    latitude: Double,
    shadowFactor: Int,
  ): Double {
    val coords = solarCoordinates(jd + 0.5)
    val declRad = degToRad(coords.declinationDeg)
    val latRad = degToRad(latitude)
    val angleRad = atan(1.0 / (shadowFactor + tan(abs(latRad - declRad))))
    val altitudeDeg = radToDeg(angleRad)
    val ha = hourAngle(latitude, coords.declinationDeg, altitudeDeg)
    return if (ha.isNaN()) solarNoonHours + 3.5 else solarNoonHours + ha
  }

  private fun hourAngle(latitudeDeg: Double, declinationDeg: Double, altitudeDeg: Double): Double {
    val latRad = degToRad(latitudeDeg)
    val declRad = degToRad(declinationDeg)
    val altRad = degToRad(altitudeDeg)
    val cosHa = (sin(altRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
    if (cosHa < -1.0 || cosHa > 1.0) return Double.NaN
    return radToDeg(acos(cosHa)) / 15.0
  }

  private fun degToRad(deg: Double): Double = deg * PI / 180.0
  private fun radToDeg(rad: Double): Double = rad * 180.0 / PI
  private fun normalizeDeg(deg: Double): Double {
    val mod = deg % 360.0
    return if (mod < 0.0) mod + 360.0 else mod
  }
}
