package com.example.domain.model

import androidx.annotation.RawRes
import com.example.R

enum class PrayerType(
  val key: String,
  val arabicName: String,
  val isObligatory: Boolean,
) {
  FAJR("fajr", "الفجر", true),
  SUNRISE("sunrise", "الشروق", false),
  DHUHR("dhuhr", "الظهر", true),
  ASR("asr", "العصر", true),
  MAGHRIB("maghrib", "المغرب", true),
  ISHA("isha", "العشاء", true);

  companion object {
    val obligatoryPrayers: List<PrayerType> = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
  }
}

enum class CalculationMethod(
  val id: String,
  val arabicName: String,
  val fajrAngle: Double,
  val ishaAngle: Double?,
  val ishaIntervalMinutes: Int?,
  val maghribAngle: Double = 0.0,
) {
  EGYPTIAN(
    id = "egyptian",
    arabicName = "الهيئة العامة للمساحة المصرية",
    fajrAngle = 19.5,
    ishaAngle = 17.5,
    ishaIntervalMinutes = null,
  ),
  UMM_AL_QURA(
    id = "umm_al_qura",
    arabicName = "أم القرى - مكة المكرمة",
    fajrAngle = 18.5,
    ishaAngle = null,
    ishaIntervalMinutes = 90,
  ),
  MUSLIM_WORLD_LEAGUE(
    id = "mwl",
    arabicName = "رابطة العالم الإسلامي",
    fajrAngle = 18.0,
    ishaAngle = 17.0,
    ishaIntervalMinutes = null,
  ),
  ISNA(
    id = "isna",
    arabicName = "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
    fajrAngle = 15.0,
    ishaAngle = 15.0,
    ishaIntervalMinutes = null,
  ),
  KARACHI(
    id = "karachi",
    arabicName = "جامعة العلوم الإسلامية - كراتشي",
    fajrAngle = 18.0,
    ishaAngle = 18.0,
    ishaIntervalMinutes = null,
  ),
  DUBAI(
    id = "dubai",
    arabicName = "دائرة الشؤون الإسلامية - دبي",
    fajrAngle = 18.2,
    ishaAngle = 18.2,
    ishaIntervalMinutes = null,
  ),
  QATAR(
    id = "qatar",
    arabicName = "وزارة الأوقاف - قطر",
    fajrAngle = 18.0,
    ishaAngle = null,
    ishaIntervalMinutes = 90,
  ),
  KUWAIT(
    id = "kuwait",
    arabicName = "وزارة الأوقاف - الكويت",
    fajrAngle = 18.0,
    ishaAngle = 17.5,
    ishaIntervalMinutes = null,
  ),
  TURKEY(
    id = "turkey",
    arabicName = "رئاسة الشؤون الدينية التركية",
    fajrAngle = 18.0,
    ishaAngle = 17.0,
    ishaIntervalMinutes = null,
  ),
  SINGAPORE(
    id = "singapore",
    arabicName = "المجلس الإسلامي - سنغافورة",
    fajrAngle = 20.0,
    ishaAngle = 18.0,
    ishaIntervalMinutes = null,
  );

  companion object {
    fun fromId(id: String): CalculationMethod =
      entries.firstOrNull { it.id == id } ?: EGYPTIAN
  }
}

enum class Madhhab(val id: String, val arabicName: String, val shadowFactor: Int) {
  SHAFI("shafi", "الجمهور (الشافعي والمالكي والحنبلي)", 1),
  HANAFI("hanafi", "المذهب الحنفي", 2);

  companion object {
    fun fromId(id: String): Madhhab = entries.firstOrNull { it.id == id } ?: SHAFI
  }
}

enum class HighLatitudeRule(val id: String, val arabicName: String) {
  MIDDLE_OF_NIGHT("middle", "منتصف الليل"),
  SEVENTH_OF_NIGHT("seventh", "سُبع الليل"),
  TWILIGHT_ANGLE("angle", "نسبة الزاوية");

  companion object {
    fun fromId(id: String): HighLatitudeRule = entries.firstOrNull { it.id == id } ?: MIDDLE_OF_NIGHT
  }
}

enum class NotificationMode(val id: String, val arabicName: String) {
  FULL_ADHAN("full_adhan", "أذان كامل"),
  TAKBIR("takbir", "تكبيرة فقط"),
  VIBRATE("vibrate", "اهتزاز فقط"),
  SILENT("silent", "صامت");

  fun next(): NotificationMode {
    val all = entries
    return all[(ordinal + 1) % all.size]
  }

  companion object {
    fun fromId(id: String): NotificationMode =
      entries.firstOrNull { it.id == id } ?: FULL_ADHAN
  }
}

enum class MuezzinVoice(
  val id: String,
  val arabicName: String,
  val subtitle: String,
  @param:RawRes val rawResId: Int?,
) {
  MAKKAH("makkah", "أذان الحرم المكي", "مقام الحجاز الهادئ", R.raw.adhan_makkah),
  MADINAH("madinah", "أذان المسجد النبوي", "مقام البياتي", R.raw.adhan_madinah),
  EGYPT("egypt", "أذان القاهرة المصري", "الأذان المصري الكلاسيكي", R.raw.adhan_egypt),
  EGYPT_NAQSHBANDI("egypt_naqshbandi", "أذان الشيخ سيد النقشبندي", "الأذان المصري الخاشع", R.raw.adhan_naqshbandi),
  EGYPT_HARAK("egypt_harak", "أذان الشيخ عبد الناصر حرك", "الأذان المصري الأصيل", R.raw.adhan_harak),
  CUSTOM("custom", "ملف صوتي مخصص", "من ذاكرة الهاتف", null);

  companion object {
    fun fromId(id: String): MuezzinVoice = entries.firstOrNull { it.id == id } ?: MAKKAH
  }
}

enum class ThemeMode(val id: String, val arabicName: String) {
  AUTO("auto", "تلقائي"),
  LIGHT("light", "نهاري"),
  DARK("dark", "ليلي");

  companion object {
    fun fromId(id: String): ThemeMode = entries.firstOrNull { it.id == id } ?: AUTO
  }
}

data class CityInfo(
  val id: String,
  val nameAr: String,
  val nameEn: String,
  val countryAr: String,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val defaultMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
)

data class PrayerOffsets(
  val fajr: Int = 0,
  val dhuhr: Int = 0,
  val asr: Int = 0,
  val maghrib: Int = 0,
  val isha: Int = 0,
) {
  fun forPrayer(type: PrayerType): Int = when (type) {
    PrayerType.FAJR -> fajr
    PrayerType.SUNRISE -> 0
    PrayerType.DHUHR -> dhuhr
    PrayerType.ASR -> asr
    PrayerType.MAGHRIB -> maghrib
    PrayerType.ISHA -> isha
  }
}

data class PrayerNotificationModes(
  val fajr: NotificationMode = NotificationMode.FULL_ADHAN,
  val dhuhr: NotificationMode = NotificationMode.FULL_ADHAN,
  val asr: NotificationMode = NotificationMode.FULL_ADHAN,
  val maghrib: NotificationMode = NotificationMode.FULL_ADHAN,
  val isha: NotificationMode = NotificationMode.FULL_ADHAN,
) {
  fun forPrayer(type: PrayerType): NotificationMode = when (type) {
    PrayerType.FAJR -> fajr
    PrayerType.SUNRISE -> NotificationMode.SILENT
    PrayerType.DHUHR -> dhuhr
    PrayerType.ASR -> asr
    PrayerType.MAGHRIB -> maghrib
    PrayerType.ISHA -> isha
  }
}

data class PrayerTimeItem(
  val type: PrayerType,
  val timeMillis: Long,
  val formattedTime: String,
  val notificationMode: NotificationMode,
  val offsetMinutes: Int,
  val isNext: Boolean,
  val isPassed: Boolean,
)

data class DailyPrayerSchedule(
  val fajrMillis: Long,
  val sunriseMillis: Long,
  val dhuhrMillis: Long,
  val asrMillis: Long,
  val maghribMillis: Long,
  val ishaMillis: Long,
) {
  fun timeFor(type: PrayerType): Long = when (type) {
    PrayerType.FAJR -> fajrMillis
    PrayerType.SUNRISE -> sunriseMillis
    PrayerType.DHUHR -> dhuhrMillis
    PrayerType.ASR -> asrMillis
    PrayerType.MAGHRIB -> maghribMillis
    PrayerType.ISHA -> ishaMillis
  }
}
