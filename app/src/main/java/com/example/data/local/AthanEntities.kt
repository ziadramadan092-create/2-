package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.CalculationMethod
import com.example.domain.model.HighLatitudeRule
import com.example.domain.model.Madhhab
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerNotificationModes
import com.example.domain.model.PrayerOffsets
import com.example.domain.model.ThemeMode

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val cityId: String = "cairo",
  val cityNameAr: String = "القاهرة",
  val cityNameEn: String = "Cairo",
  val countryAr: String = "مصر",
  val latitude: Double = 30.0444,
  val longitude: Double = 31.2357,
  val timeZoneId: String = "Africa/Cairo",
  val isGpsLocation: Boolean = false,
  val calculationMethodId: String = CalculationMethod.EGYPTIAN.id,
  val madhhabId: String = Madhhab.SHAFI.id,
  val highLatitudeRuleId: String = HighLatitudeRule.MIDDLE_OF_NIGHT.id,
  val fajrModeId: String = NotificationMode.FULL_ADHAN.id,
  val dhuhrModeId: String = NotificationMode.FULL_ADHAN.id,
  val asrModeId: String = NotificationMode.FULL_ADHAN.id,
  val maghribModeId: String = NotificationMode.FULL_ADHAN.id,
  val ishaModeId: String = NotificationMode.FULL_ADHAN.id,
  val fajrOffset: Int = 0,
  val dhuhrOffset: Int = 0,
  val asrOffset: Int = 0,
  val maghribOffset: Int = 0,
  val ishaOffset: Int = 0,
  val muezzinVoiceId: String = MuezzinVoice.MAKKAH.id,
  val customAudioUri: String? = null,
  val customAudioName: String? = null,
  val themeModeId: String = ThemeMode.DARK.id,
  val use24HourFormat: Boolean = false,
  val hijriAdjustmentDays: Int = 0,
  val preAdhanReminderEnabled: Boolean = false,
) {
  fun calculationMethod(): CalculationMethod = CalculationMethod.fromId(calculationMethodId)
  fun madhhab(): Madhhab = Madhhab.fromId(madhhabId)
  fun highLatitudeRule(): HighLatitudeRule = HighLatitudeRule.fromId(highLatitudeRuleId)
  fun muezzinVoice(): MuezzinVoice = MuezzinVoice.fromId(muezzinVoiceId)
  fun themeMode(): ThemeMode = ThemeMode.fromId(themeModeId)

  fun notificationModes(): PrayerNotificationModes = PrayerNotificationModes(
    fajr = NotificationMode.fromId(fajrModeId),
    dhuhr = NotificationMode.fromId(dhuhrModeId),
    asr = NotificationMode.fromId(asrModeId),
    maghrib = NotificationMode.fromId(maghribModeId),
    isha = NotificationMode.fromId(ishaModeId),
  )

  fun offsets(): PrayerOffsets = PrayerOffsets(
    fajr = fajrOffset,
    dhuhr = dhuhrOffset,
    asr = asrOffset,
    maghrib = maghribOffset,
    isha = ishaOffset,
  )
}

@Entity(tableName = "tasbih_state")
data class TasbihStateEntity(
  @PrimaryKey val id: Int = 1,
  val selectedPhraseId: String = "subhan_allah",
  val currentCount: Int = 0,
  val targetCycle: Int = 33, // 33, 99, 100, or 0 for open
  val totalLifetimeCount: Long = 0L,
)

@Entity(tableName = "adhkar_progress", primaryKeys = ["dhikrId", "dateKey"])
data class AdhkarProgressEntity(
  val dhikrId: String,
  val dateKey: String,
  val remainingCount: Int,
)
