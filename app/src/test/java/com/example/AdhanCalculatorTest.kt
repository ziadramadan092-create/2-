package com.example

import com.example.domain.math.AdhanCalculator
import com.example.domain.math.QiblaCalculator
import com.example.domain.model.CalculationMethod
import com.example.domain.model.Madhhab
import com.example.domain.model.PrayerOffsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AdhanCalculatorTest {

  @Test
  fun cairoPrayerTimesAreChronologicallyOrdered() {
    val tz = TimeZone.getTimeZone("Africa/Cairo")
    val schedule = AdhanCalculator.calculate(
      year = 2026,
      month = 11,
      day = 23,
      latitude = 30.0444,
      longitude = 31.2357,
      timeZone = tz,
      method = CalculationMethod.EGYPTIAN,
      madhhab = Madhhab.SHAFI,
    )

    assertTrue(schedule.fajrMillis < schedule.sunriseMillis)
    assertTrue(schedule.sunriseMillis < schedule.dhuhrMillis)
    assertTrue(schedule.dhuhrMillis < schedule.asrMillis)
    assertTrue(schedule.asrMillis < schedule.maghribMillis)
    assertTrue(schedule.maghribMillis < schedule.ishaMillis)

    // Verify Cairo Dhuhr is around 11:30 - 12:15 local time
    val cal = Calendar.getInstance(tz).apply { timeInMillis = schedule.dhuhrMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    assertTrue("Dhuhr hour should be 11 or 12 in Cairo, was $hour", hour in 11..12)
  }

  @Test
  fun hanafiAsrIsLaterThanShafiAsr() {
    val tz = TimeZone.getTimeZone("Asia/Riyadh")
    val shafi = AdhanCalculator.calculate(
      year = 2026,
      month = 6,
      day = 15,
      latitude = 24.7136,
      longitude = 46.6753,
      timeZone = tz,
      method = CalculationMethod.UMM_AL_QURA,
      madhhab = Madhhab.SHAFI,
    )
    val hanafi = AdhanCalculator.calculate(
      year = 2026,
      month = 6,
      day = 15,
      latitude = 24.7136,
      longitude = 46.6753,
      timeZone = tz,
      method = CalculationMethod.UMM_AL_QURA,
      madhhab = Madhhab.HANAFI,
    )

    assertTrue(hanafi.asrMillis > shafi.asrMillis + 30 * 60_000L)
  }

  @Test
  fun ummAlQuraIshaIsExactly90MinutesAfterMaghrib() {
    val tz = TimeZone.getTimeZone("Asia/Riyadh")
    val schedule = AdhanCalculator.calculate(
      year = 2026,
      month = 11,
      day = 23,
      latitude = 21.3891,
      longitude = 39.8579,
      timeZone = tz,
      method = CalculationMethod.UMM_AL_QURA,
      madhhab = Madhhab.SHAFI,
    )
    assertEquals(90 * 60_000L, schedule.ishaMillis - schedule.maghribMillis)
  }

  @Test
  fun manualPrayerOffsetsShiftTimestampsAccurately() {
    val tz = TimeZone.getTimeZone("Africa/Cairo")
    val base = AdhanCalculator.calculate(
      year = 2026,
      month = 11,
      day = 23,
      latitude = 30.0444,
      longitude = 31.2357,
      timeZone = tz,
      method = CalculationMethod.EGYPTIAN,
      madhhab = Madhhab.SHAFI,
    )
    val adjusted = AdhanCalculator.calculate(
      year = 2026,
      month = 11,
      day = 23,
      latitude = 30.0444,
      longitude = 31.2357,
      timeZone = tz,
      method = CalculationMethod.EGYPTIAN,
      madhhab = Madhhab.SHAFI,
      offsets = PrayerOffsets(fajr = 5, dhuhr = -3),
    )

    assertEquals(5 * 60_000L, adjusted.fajrMillis - base.fajrMillis)
    assertEquals(-3 * 60_000L, adjusted.dhuhrMillis - base.dhuhrMillis)
  }

  @Test
  fun qiblaBearingFromCairoPointsSoutheast() {
    val bearing = QiblaCalculator.calculateQiblaBearing(30.0444, 31.2357)
    // Cairo to Makkah is ~136 degrees (Southeast)
    assertTrue("Expected ~136 deg, got $bearing", bearing in 130.0..142.0)
  }
}
