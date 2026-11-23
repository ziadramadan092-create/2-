package com.example.data.repository

import com.example.data.local.AdhkarProgressEntity
import com.example.data.local.AppSettingsEntity
import com.example.data.local.AthanDao
import com.example.data.local.OfflineAdhkarData
import com.example.data.local.TasbihStateEntity
import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityInfo
import com.example.domain.model.DhikrCategory
import com.example.domain.model.Madhhab
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerType
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AthanRepository(private val dao: AthanDao) {

  val settingsFlow: Flow<AppSettingsEntity> = dao.observeSettings().map { it ?: AppSettingsEntity() }

  val tasbihFlow: Flow<TasbihStateEntity> = dao.observeTasbihState().map { it ?: TasbihStateEntity() }

  fun observeAdhkarProgressToday(): Flow<Map<String, Int>> {
    val todayKey = todayDateKey()
    return dao.observeAdhkarProgress(todayKey).map { list ->
      list.associate { it.dhikrId to it.remainingCount }
    }
  }

  suspend fun ensureInitialized() {
    if (dao.getSettingsOnce() == null) {
      dao.upsertSettings(AppSettingsEntity())
    }
    if (dao.getTasbihStateOnce() == null) {
      dao.upsertTasbihState(TasbihStateEntity())
    }
  }

  suspend fun getSettingsSnapshot(): AppSettingsEntity {
    return dao.getSettingsOnce() ?: AppSettingsEntity().also { dao.upsertSettings(it) }
  }

  suspend fun updateCity(city: CityInfo, isGps: Boolean = false) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(
      current.copy(
        cityId = city.id,
        cityNameAr = city.nameAr,
        cityNameEn = city.nameEn,
        countryAr = city.countryAr,
        latitude = city.latitude,
        longitude = city.longitude,
        timeZoneId = city.timeZoneId,
        isGpsLocation = isGps,
        calculationMethodId = city.defaultMethod.id,
      )
    )
  }

  suspend fun updateGpsLocation(
    cityNameAr: String,
    countryAr: String,
    latitude: Double,
    longitude: Double,
    timeZoneId: String,
    suggestedMethod: CalculationMethod,
  ) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(
      current.copy(
        cityId = "gps_custom",
        cityNameAr = cityNameAr,
        cityNameEn = cityNameAr,
        countryAr = countryAr,
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        isGpsLocation = true,
        calculationMethodId = suggestedMethod.id,
      )
    )
  }

  suspend fun updateCalculationMethod(method: CalculationMethod) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(calculationMethodId = method.id))
  }

  suspend fun updateMadhhab(madhhab: Madhhab) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(madhhabId = madhhab.id))
  }

  suspend fun updatePrayerNotificationMode(prayer: PrayerType, mode: NotificationMode) {
    val current = getSettingsSnapshot()
    val updated = when (prayer) {
      PrayerType.FAJR -> current.copy(fajrModeId = mode.id)
      PrayerType.DHUHR -> current.copy(dhuhrModeId = mode.id)
      PrayerType.ASR -> current.copy(asrModeId = mode.id)
      PrayerType.MAGHRIB -> current.copy(maghribModeId = mode.id)
      PrayerType.ISHA -> current.copy(ishaModeId = mode.id)
      PrayerType.SUNRISE -> current
    }
    dao.upsertSettings(updated)
  }

  suspend fun updatePrayerOffset(prayer: PrayerType, offsetMinutes: Int) {
    val clamped = offsetMinutes.coerceIn(-60, 60)
    val current = getSettingsSnapshot()
    val updated = when (prayer) {
      PrayerType.FAJR -> current.copy(fajrOffset = clamped)
      PrayerType.DHUHR -> current.copy(dhuhrOffset = clamped)
      PrayerType.ASR -> current.copy(asrOffset = clamped)
      PrayerType.MAGHRIB -> current.copy(maghribOffset = clamped)
      PrayerType.ISHA -> current.copy(ishaOffset = clamped)
      PrayerType.SUNRISE -> current
    }
    dao.upsertSettings(updated)
  }

  suspend fun updateMuezzinVoice(voice: MuezzinVoice) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(muezzinVoiceId = voice.id))
  }

  suspend fun updateCustomAudio(uri: String, displayName: String) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(
      current.copy(
        muezzinVoiceId = MuezzinVoice.CUSTOM.id,
        customAudioUri = uri,
        customAudioName = displayName,
      )
    )
  }

  suspend fun updateThemeMode(mode: ThemeMode) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(themeModeId = mode.id))
  }

  suspend fun updateUse24HourFormat(use24Hour: Boolean) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(use24HourFormat = use24Hour))
  }

  suspend fun updateHijriAdjustment(days: Int) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(hijriAdjustmentDays = days.coerceIn(-2, 2)))
  }

  suspend fun updatePreAdhanReminder(enabled: Boolean) {
    val current = getSettingsSnapshot()
    dao.upsertSettings(current.copy(preAdhanReminderEnabled = enabled))
  }

  // Tasbih
  suspend fun incrementTasbih(): Boolean {
    val current = dao.getTasbihStateOnce() ?: TasbihStateEntity()
    val nextCountRaw = current.currentCount + 1
    val cycleCompleted = current.targetCycle > 0 && nextCountRaw >= current.targetCycle
    val nextCount = if (cycleCompleted) 0 else nextCountRaw
    dao.upsertTasbihState(
      current.copy(
        currentCount = nextCount,
        totalLifetimeCount = current.totalLifetimeCount + 1L,
      )
    )
    return cycleCompleted
  }

  suspend fun resetTasbihCurrent() {
    val current = dao.getTasbihStateOnce() ?: TasbihStateEntity()
    dao.upsertTasbihState(current.copy(currentCount = 0))
  }

  suspend fun selectTasbihPhrase(phraseId: String) {
    val current = dao.getTasbihStateOnce() ?: TasbihStateEntity()
    dao.upsertTasbihState(current.copy(selectedPhraseId = phraseId, currentCount = 0))
  }

  suspend fun selectTasbihCycle(targetCycle: Int) {
    val current = dao.getTasbihStateOnce() ?: TasbihStateEntity()
    dao.upsertTasbihState(current.copy(targetCycle = targetCycle, currentCount = 0))
  }

  // Adhkar
  suspend fun decrementDhikr(dhikrId: String, currentRemaining: Int) {
    val next = (currentRemaining - 1).coerceAtLeast(0)
    dao.upsertAdhkarProgress(
      AdhkarProgressEntity(
        dhikrId = dhikrId,
        dateKey = todayDateKey(),
        remainingCount = next,
      )
    )
  }

  suspend fun resetCategoryAdhkar(category: DhikrCategory) {
    val ids = OfflineAdhkarData.forCategory(category).map { it.id }
    dao.resetAdhkarProgressForIds(todayDateKey(), ids)
  }

  // JSON Backup & Restore
  suspend fun exportBackupJson(): String {
    val s = getSettingsSnapshot()
    val t = dao.getTasbihStateOnce() ?: TasbihStateEntity()

    val root = JSONObject().apply {
      put("version", 1)
      put("exportedAt", System.currentTimeMillis())
      put("settings", JSONObject().apply {
        put("cityId", s.cityId)
        put("cityNameAr", s.cityNameAr)
        put("cityNameEn", s.cityNameEn)
        put("countryAr", s.countryAr)
        put("latitude", s.latitude)
        put("longitude", s.longitude)
        put("timeZoneId", s.timeZoneId)
        put("isGpsLocation", s.isGpsLocation)
        put("calculationMethodId", s.calculationMethodId)
        put("madhhabId", s.madhhabId)
        put("highLatitudeRuleId", s.highLatitudeRuleId)
        put("fajrModeId", s.fajrModeId)
        put("dhuhrModeId", s.dhuhrModeId)
        put("asrModeId", s.asrModeId)
        put("maghribModeId", s.maghribModeId)
        put("ishaModeId", s.ishaModeId)
        put("fajrOffset", s.fajrOffset)
        put("dhuhrOffset", s.dhuhrOffset)
        put("asrOffset", s.asrOffset)
        put("maghribOffset", s.maghribOffset)
        put("ishaOffset", s.ishaOffset)
        put("muezzinVoiceId", s.muezzinVoiceId)
        put("customAudioUri", s.customAudioUri ?: JSONObject.NULL)
        put("customAudioName", s.customAudioName ?: JSONObject.NULL)
        put("themeModeId", s.themeModeId)
        put("use24HourFormat", s.use24HourFormat)
        put("hijriAdjustmentDays", s.hijriAdjustmentDays)
        put("preAdhanReminderEnabled", s.preAdhanReminderEnabled)
      })
      put("tasbih", JSONObject().apply {
        put("selectedPhraseId", t.selectedPhraseId)
        put("currentCount", t.currentCount)
        put("targetCycle", t.targetCycle)
        put("totalLifetimeCount", t.totalLifetimeCount)
      })
    }
    return root.toString(2)
  }

  suspend fun importBackupJson(jsonString: String): Boolean {
    return runCatching {
      val root = JSONObject(jsonString)
      val s = root.getJSONObject("settings")
      val current = getSettingsSnapshot()
      val customUri = if (s.isNull("customAudioUri")) null else s.optString("customAudioUri", "")
        .takeIf { it.isNotBlank() }
      val customName = if (s.isNull("customAudioName")) null else s.optString("customAudioName", "")
        .takeIf { it.isNotBlank() }

      val restoredSettings = current.copy(
        cityId = s.optString("cityId", current.cityId),
        cityNameAr = s.optString("cityNameAr", current.cityNameAr),
        cityNameEn = s.optString("cityNameEn", current.cityNameEn),
        countryAr = s.optString("countryAr", current.countryAr),
        latitude = s.optDouble("latitude", current.latitude),
        longitude = s.optDouble("longitude", current.longitude),
        timeZoneId = s.optString("timeZoneId", current.timeZoneId),
        isGpsLocation = s.optBoolean("isGpsLocation", current.isGpsLocation),
        calculationMethodId = s.optString("calculationMethodId", current.calculationMethodId),
        madhhabId = s.optString("madhhabId", current.madhhabId),
        highLatitudeRuleId = s.optString("highLatitudeRuleId", current.highLatitudeRuleId),
        fajrModeId = s.optString("fajrModeId", current.fajrModeId),
        dhuhrModeId = s.optString("dhuhrModeId", current.dhuhrModeId),
        asrModeId = s.optString("asrModeId", current.asrModeId),
        maghribModeId = s.optString("maghribModeId", current.maghribModeId),
        ishaModeId = s.optString("ishaModeId", current.ishaModeId),
        fajrOffset = s.optInt("fajrOffset", 0).coerceIn(-60, 60),
        dhuhrOffset = s.optInt("dhuhrOffset", 0).coerceIn(-60, 60),
        asrOffset = s.optInt("asrOffset", 0).coerceIn(-60, 60),
        maghribOffset = s.optInt("maghribOffset", 0).coerceIn(-60, 60),
        ishaOffset = s.optInt("ishaOffset", 0).coerceIn(-60, 60),
        muezzinVoiceId = s.optString("muezzinVoiceId", current.muezzinVoiceId),
        customAudioUri = customUri,
        customAudioName = customName,
        themeModeId = s.optString("themeModeId", current.themeModeId),
        use24HourFormat = s.optBoolean("use24HourFormat", current.use24HourFormat),
        hijriAdjustmentDays = s.optInt("hijriAdjustmentDays", 0).coerceIn(-2, 2),
        preAdhanReminderEnabled = s.optBoolean("preAdhanReminderEnabled", current.preAdhanReminderEnabled),
      )
      dao.upsertSettings(restoredSettings)

      if (root.has("tasbih")) {
        val t = root.getJSONObject("tasbih")
        dao.upsertTasbihState(
          TasbihStateEntity(
            selectedPhraseId = t.optString("selectedPhraseId", "subhan_allah"),
            currentCount = t.optInt("currentCount", 0),
            targetCycle = t.optInt("targetCycle", 33),
            totalLifetimeCount = t.optLong("totalLifetimeCount", 0L),
          )
        )
      }
      true
    }.getOrElse { false }
  }

  private fun todayDateKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
