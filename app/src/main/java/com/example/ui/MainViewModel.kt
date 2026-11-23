package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AthanApplication
import com.example.data.local.AppSettingsEntity
import com.example.data.local.TasbihStateEntity
import com.example.domain.math.AdhanCalculator
import com.example.domain.math.HijriDateFormatter
import com.example.domain.math.QiblaCalculator
import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityInfo
import com.example.domain.model.DhikrCategory
import com.example.domain.model.Madhhab
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerTimeItem
import com.example.domain.model.PrayerType
import com.example.domain.model.ThemeMode
import com.example.service.AdhanAudioController
import com.example.service.AdhanPlaybackState
import com.example.service.LocationHelper
import com.example.service.PrayerAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MainUiState(
  val settings: AppSettingsEntity = AppSettingsEntity(),
  val tasbih: TasbihStateEntity = TasbihStateEntity(),
  val adhkarProgressToday: Map<String, Int> = emptyMap(),
  val adhanPlayback: AdhanPlaybackState = AdhanPlaybackState(),
  val currentTimeFormatted: String = "",
  val currentSecondsFormatted: String = "",
  val hijriDateFormatted: String = "",
  val gregorianDateFormatted: String = "",
  val prayerItems: List<PrayerTimeItem> = emptyList(),
  val nextPrayer: PrayerType = PrayerType.FAJR,
  val nextPrayerTimeFormatted: String = "",
  val countdownFormatted: String = "00:00:00",
  val countdownArabicSummary: String = "",
  val prayerCycleProgress: Float = 0f,
  val qiblaBearingDeg: Double = 0.0,
  val distanceToMakkahKm: Double = 0.0,
  val isDetectingGps: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as AthanApplication
  private val repo = app.repository

  private val _isDetectingGps = MutableStateFlow(false)
  private val _events = MutableSharedFlow<String>()
  val events = _events.asSharedFlow()

  private val tickerFlow = flow {
    while (true) {
      emit(System.currentTimeMillis())
      delay(1_000L)
    }
  }

  val uiState: StateFlow<MainUiState> = combine(
    repo.settingsFlow,
    repo.tasbihFlow,
    repo.observeAdhkarProgressToday(),
    AdhanAudioController.state,
    tickerFlow,
    _isDetectingGps,
  ) { values ->
    val settings = values[0] as AppSettingsEntity
    val tasbih = values[1] as TasbihStateEntity
    @Suppress("UNCHECKED_CAST")
    val adhkarMap = values[2] as Map<String, Int>
    val playback = values[3] as AdhanPlaybackState
    val nowMillis = values[4] as Long
    val detectingGps = values[5] as Boolean

    buildUiState(
      settings = settings,
      tasbih = tasbih,
      adhkarMap = adhkarMap,
      playback = playback,
      nowMillis = nowMillis,
      detectingGps = detectingGps,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = buildUiState(
      settings = AppSettingsEntity(),
      tasbih = TasbihStateEntity(),
      adhkarMap = emptyMap(),
      playback = AdhanPlaybackState(),
      nowMillis = System.currentTimeMillis(),
      detectingGps = false,
    ),
  )

  private fun buildUiState(
    settings: AppSettingsEntity,
    tasbih: TasbihStateEntity,
    adhkarMap: Map<String, Int>,
    playback: AdhanPlaybackState,
    nowMillis: Long,
    detectingGps: Boolean,
  ): MainUiState {
    val tz = TimeZone.getTimeZone(settings.timeZoneId)
    val todayCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }

    val todaySchedule = AdhanCalculator.calculate(
      year = todayCal.get(Calendar.YEAR),
      month = todayCal.get(Calendar.MONTH) + 1,
      day = todayCal.get(Calendar.DAY_OF_MONTH),
      latitude = settings.latitude,
      longitude = settings.longitude,
      timeZone = tz,
      method = settings.calculationMethod(),
      madhhab = settings.madhhab(),
      offsets = settings.offsets(),
      highLatitudeRule = settings.highLatitudeRule(),
    )

    val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    val tomorrowSchedule = AdhanCalculator.calculate(
      year = tomorrowCal.get(Calendar.YEAR),
      month = tomorrowCal.get(Calendar.MONTH) + 1,
      day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
      latitude = settings.latitude,
      longitude = settings.longitude,
      timeZone = tz,
      method = settings.calculationMethod(),
      madhhab = settings.madhhab(),
      offsets = settings.offsets(),
      highLatitudeRule = settings.highLatitudeRule(),
    )

    val obligatory = PrayerType.obligatoryPrayers
    val nextObligatory = obligatory.firstOrNull { todaySchedule.timeFor(it) > nowMillis }
    val nextPrayerType = nextObligatory ?: PrayerType.FAJR
    val nextPrayerMillis = if (nextObligatory != null) {
      todaySchedule.timeFor(nextObligatory)
    } else {
      tomorrowSchedule.fajrMillis
    }

    val prevPrayerMillis = when {
      nextObligatory == null -> todaySchedule.ishaMillis
      nextObligatory == PrayerType.FAJR -> {
        val yestCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val yestSchedule = AdhanCalculator.calculate(
          year = yestCal.get(Calendar.YEAR),
          month = yestCal.get(Calendar.MONTH) + 1,
          day = yestCal.get(Calendar.DAY_OF_MONTH),
          latitude = settings.latitude,
          longitude = settings.longitude,
          timeZone = tz,
          method = settings.calculationMethod(),
          madhhab = settings.madhhab(),
          offsets = settings.offsets(),
          highLatitudeRule = settings.highLatitudeRule(),
        )
        yestSchedule.ishaMillis
      }
      else -> {
        val idx = obligatory.indexOf(nextObligatory)
        todaySchedule.timeFor(obligatory[idx - 1])
      }
    }

    val totalWindow = (nextPrayerMillis - prevPrayerMillis).coerceAtLeast(1L)
    val elapsed = (nowMillis - prevPrayerMillis).coerceIn(0L, totalWindow)
    val progress = (elapsed.toFloat() / totalWindow.toFloat()).coerceIn(0f, 1f)

    val remainingMs = (nextPrayerMillis - nowMillis).coerceAtLeast(0L)
    val totalSec = remainingMs / 1000L
    val remHours = totalSec / 3600L
    val remMinutes = (totalSec % 3600L) / 60L
    val remSeconds = totalSec % 60L
    val countdownStr = String.format(Locale.US, "%02d:%02d:%02d", remHours, remMinutes, remSeconds)

    val summaryAr = buildString {
      append("متبقي ")
      if (remHours > 0) {
        append(remHours).append(" ساعة")
        if (remMinutes > 0) append(" و ")
      }
      if (remMinutes > 0 || remHours == 0L) {
        append(remMinutes).append(" دقيقة")
      }
    }

    val timePattern = if (settings.use24HourFormat) "HH:mm" else "h:mm a"
    val timeFormatter = SimpleDateFormat(timePattern, Locale("ar")).apply { timeZone = tz }
    val clockMainPattern = if (settings.use24HourFormat) "HH:mm" else "h:mm"
    val clockMainFormatter = SimpleDateFormat(clockMainPattern, Locale.US).apply { timeZone = tz }
    val clockSecFormatter = SimpleDateFormat(
      if (settings.use24HourFormat) "ss" else "ss a",
      Locale("ar"),
    ).apply { timeZone = tz }

    val modes = settings.notificationModes()
    val offsets = settings.offsets()

    val items = PrayerType.entries.map { type ->
      val tMillis = todaySchedule.timeFor(type)
      PrayerTimeItem(
        type = type,
        timeMillis = tMillis,
        formattedTime = timeFormatter.format(Date(tMillis)),
        notificationMode = modes.forPrayer(type),
        offsetMinutes = offsets.forPrayer(type),
        isNext = nextObligatory == type,
        isPassed = tMillis <= nowMillis,
      )
    }

    val qiblaBearing = QiblaCalculator.calculateQiblaBearing(settings.latitude, settings.longitude)
    val distMakkah = QiblaCalculator.distanceToMakkahKm(settings.latitude, settings.longitude)

    return MainUiState(
      settings = settings,
      tasbih = tasbih,
      adhkarProgressToday = adhkarMap,
      adhanPlayback = playback,
      currentTimeFormatted = clockMainFormatter.format(Date(nowMillis)),
      currentSecondsFormatted = clockSecFormatter.format(Date(nowMillis)),
      hijriDateFormatted = HijriDateFormatter.formatHijriArabic(
        nowMillis,
        tz,
        settings.hijriAdjustmentDays,
      ),
      gregorianDateFormatted = HijriDateFormatter.formatGregorianArabic(nowMillis, tz),
      prayerItems = items,
      nextPrayer = nextPrayerType,
      nextPrayerTimeFormatted = timeFormatter.format(Date(nextPrayerMillis)),
      countdownFormatted = countdownStr,
      countdownArabicSummary = summaryAr,
      prayerCycleProgress = progress,
      qiblaBearingDeg = qiblaBearing,
      distanceToMakkahKm = distMakkah,
      isDetectingGps = detectingGps,
    )
  }

  fun selectCity(city: CityInfo) {
    viewModelScope.launch {
      repo.updateCity(city, isGps = false)
      rescheduleAlarms()
      _events.emit("تم تحديث المدينة إلى ${city.nameAr}")
    }
  }

  fun detectLocationViaGps(context: Context) {
    if (_isDetectingGps.value) return
    viewModelScope.launch {
      _isDetectingGps.value = true
      val result = LocationHelper.detectCurrentLocation(context)
      _isDetectingGps.value = false
      if (result != null) {
        repo.updateGpsLocation(
          cityNameAr = result.cityNameAr,
          countryAr = result.countryAr,
          latitude = result.latitude,
          longitude = result.longitude,
          timeZoneId = result.timeZoneId,
          suggestedMethod = result.suggestedMethod,
        )
        rescheduleAlarms()
        _events.emit("تم تحديد موقعك: ${result.cityNameAr}")
      } else {
        _events.emit("تعذر الحصول على إشارة GPS. تأكد من تفعيل الموقع أو اختر المدينة يدوياً.")
      }
    }
  }

  fun setCalculationMethod(method: CalculationMethod) {
    viewModelScope.launch {
      repo.updateCalculationMethod(method)
      rescheduleAlarms()
    }
  }

  fun setMadhhab(madhhab: Madhhab) {
    viewModelScope.launch {
      repo.updateMadhhab(madhhab)
      rescheduleAlarms()
    }
  }

  fun setPrayerMode(prayer: PrayerType, mode: NotificationMode) {
    viewModelScope.launch {
      repo.updatePrayerNotificationMode(prayer, mode)
      rescheduleAlarms()
    }
  }

  fun cyclePrayerMode(prayer: PrayerType) {
    if (!prayer.isObligatory) return
    val current = uiState.value.settings.notificationModes().forPrayer(prayer)
    setPrayerMode(prayer, current.next())
  }

  fun adjustPrayerOffset(prayer: PrayerType, deltaMinutes: Int) {
    viewModelScope.launch {
      val current = uiState.value.settings.offsets().forPrayer(prayer)
      repo.updatePrayerOffset(prayer, current + deltaMinutes)
      rescheduleAlarms()
    }
  }

  fun setMuezzinVoice(voice: MuezzinVoice) {
    viewModelScope.launch {
      repo.updateMuezzinVoice(voice)
    }
  }

  fun setCustomAudioUri(context: Context, uri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
    val fileName = resolveDisplayName(context, uri) ?: "ملف صوتي مخصص"
    viewModelScope.launch {
      repo.updateCustomAudio(uri.toString(), fileName)
      _events.emit("تم اختيار الملف الصوتي: $fileName")
    }
  }

  fun previewAdhan(context: Context) {
    val s = uiState.value.settings
    AdhanAudioController.play(
      context = context,
      prayerNameAr = "تجربة الأذان",
      mode = NotificationMode.FULL_ADHAN,
      voice = s.muezzinVoice(),
      customAudioUri = s.customAudioUri,
      isPreview = true,
    )
  }

  fun stopAdhan(context: Context) {
    AdhanAudioController.stop(context)
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { repo.updateThemeMode(mode) }
  }

  fun setUse24HourFormat(use24Hour: Boolean) {
    viewModelScope.launch { repo.updateUse24HourFormat(use24Hour) }
  }

  fun adjustHijriDays(delta: Int) {
    viewModelScope.launch {
      val current = uiState.value.settings.hijriAdjustmentDays
      repo.updateHijriAdjustment(current + delta)
    }
  }

  fun setPreAdhanReminder(enabled: Boolean) {
    viewModelScope.launch {
      repo.updatePreAdhanReminder(enabled)
      rescheduleAlarms()
    }
  }

  fun toggleThemeQuick(currentlyDark: Boolean) {
    val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
    setThemeMode(next)
  }

  // Tasbih & Adhkar
  fun incrementTasbih(onCycleCompleted: () -> Unit) {
    viewModelScope.launch {
      val completed = repo.incrementTasbih()
      if (completed) onCycleCompleted()
    }
  }

  fun resetTasbihCurrent() {
    viewModelScope.launch { repo.resetTasbihCurrent() }
  }

  fun selectTasbihPhrase(phraseId: String) {
    viewModelScope.launch { repo.selectTasbihPhrase(phraseId) }
  }

  fun selectTasbihCycle(cycle: Int) {
    viewModelScope.launch { repo.selectTasbihCycle(cycle) }
  }

  fun decrementDhikr(dhikrId: String, currentRemaining: Int) {
    if (currentRemaining <= 0) return
    viewModelScope.launch { repo.decrementDhikr(dhikrId, currentRemaining) }
  }

  fun resetAdhkarCategory(category: DhikrCategory) {
    viewModelScope.launch {
      repo.resetCategoryAdhkar(category)
      _events.emit("تم إعادة ضبط ${category.arabicTitle}")
    }
  }

  // Backup & Restore
  fun exportBackupToUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val json = repo.exportBackupJson()
      val ok = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        }
      }.isSuccess
      _events.emit(
        if (ok) "تم حفظ النسخة الاحتياطية بنجاح" else "تعذر حفظ النسخة الاحتياطية",
      )
    }
  }

  fun importBackupFromUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val json = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
          input.bufferedReader(Charsets.UTF_8).readText()
        }
      }.getOrNull()

      val restored = if (!json.isNullOrBlank()) repo.importBackupJson(json) else false
      if (restored) {
        rescheduleAlarms()
        _events.emit("تمت استعادة الإعدادات بنجاح")
      } else {
        _events.emit("ملف النسخة الاحتياطية غير صالح")
      }
    }
  }

  private suspend fun rescheduleAlarms() {
    val latest = repo.getSettingsSnapshot()
    PrayerAlarmScheduler.scheduleAll(app, latest)
  }

  private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
      }
    }.getOrNull()
  }
}
