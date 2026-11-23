package com.example.domain.math

import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

object HijriDateFormatter {

  private val arabicWeekdays = arrayOf(
    "الأحد",
    "الاثنين",
    "الثلاثاء",
    "الأربعاء",
    "الخميس",
    "الجمعة",
    "السبت",
  )

  private val gregorianMonthsAr = arrayOf(
    "يناير",
    "فبراير",
    "مارس",
    "أبريل",
    "مايو",
    "يونيو",
    "يوليو",
    "أغسطس",
    "سبتمبر",
    "أكتوبر",
    "نوفمبر",
    "ديسمبر",
  )

  private val hijriMonthsAr = arrayOf(
    "محرم",
    "صفر",
    "ربيع الأول",
    "ربيع الآخر",
    "جمادى الأولى",
    "جمادى الآخرة",
    "رجب",
    "شعبان",
    "رمضان",
    "شوال",
    "ذو القعدة",
    "ذو الحجة",
  )

  fun formatGregorianArabic(timeMillis: Long, timeZone: TimeZone): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = timeMillis }
    val weekday = arabicWeekdays[cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = gregorianMonthsAr[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return "$weekday، $day $month $year م"
  }

  fun formatHijriArabic(timeMillis: Long, timeZone: TimeZone, adjustmentDays: Int = 0): String {
    val cal = Calendar.getInstance(timeZone).apply {
      timeInMillis = timeMillis
      add(Calendar.DAY_OF_MONTH, adjustmentDays)
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val (hYear, hMonth, hDay) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val gregorian = LocalDate.of(year, month, day)
      val hijrah = HijrahChronology.INSTANCE.date(gregorian)
      Triple(
        hijrah.get(ChronoField.YEAR),
        hijrah.get(ChronoField.MONTH_OF_YEAR),
        hijrah.get(ChronoField.DAY_OF_MONTH),
      )
    } else {
      kuwaitiAlgorithm(year, month, day)
    }

    val monthName = hijriMonthsAr.getOrElse(hMonth - 1) { "" }
    return "$hDay $monthName $hYear هـ"
  }

  /**
   * Tabular Islamic calendar fallback for Android API 24-25.
   */
  private fun kuwaitiAlgorithm(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
    var m = month.toDouble()
    var y = year.toDouble()
    if (m < 3) {
      y -= 1
      m += 12
    }
    var a = floor(y / 100.0)
    var b = 2 - a + floor(a / 4.0)
    if (y < 1583) b = 0.0
    if (y == 1582.0) {
      if (m > 10) b = -10.0
      if (m == 10.0) {
        b = 0.0
        if (day > 4) b = -10.0
      }
    }
    val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524
    val iYear = 10631.0 / 30.0
    val epochAstro = 1948084.0
    val shift1 = 8.01 / 60.0

    var z = jd - epochAstro
    val cyc = floor(z / 10631.0)
    z -= 10631.0 * cyc
    val j = floor((z - shift1) / iYear)
    val iy = 30.0 * cyc + j
    z -= floor(j * iYear + shift1)
    var im = floor((z + 28.5001) / 29.5)
    if (im == 13.0) im = 12.0
    val id = z - floor(29.5001 * im - 29.0)

    return Triple(iy.toInt(), im.toInt(), id.toInt())
  }
}
