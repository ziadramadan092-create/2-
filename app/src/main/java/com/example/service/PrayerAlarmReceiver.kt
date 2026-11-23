package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.local.AthanDatabase
import com.example.data.repository.AthanRepository
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val wakeLock = runCatching {
      pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Athan:AlarmReceiverLock")?.apply {
        setReferenceCounted(false)
        acquire(25_000L)
      }
    }.getOrNull()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = AthanDatabase.getInstance(context)
        val repo = AthanRepository(db.athanDao())
        val settings = repo.getSettingsSnapshot()

        if (intent.action == ACTION_PRAYER_ALARM) {
          val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)
          val prayer = PrayerType.obligatoryPrayers.firstOrNull { it.key == prayerKey }
          if (prayer != null) {
            val mode = settings.notificationModes().forPrayer(prayer)
            when (mode) {
              NotificationMode.FULL_ADHAN,
              NotificationMode.TAKBIR -> {
                val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                  action = AdhanPlaybackService.ACTION_START_ADHAN
                  putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME_AR, prayer.arabicName)
                  putExtra(AdhanPlaybackService.EXTRA_CITY_NAME_AR, settings.cityNameAr)
                  putExtra(AdhanPlaybackService.EXTRA_MODE_ID, mode.id)
                  putExtra(AdhanPlaybackService.EXTRA_VOICE_ID, settings.muezzinVoiceId)
                  putExtra(AdhanPlaybackService.EXTRA_CUSTOM_URI, settings.customAudioUri)
                }
                val startedService = runCatching {
                  ContextCompat.startForegroundService(context, serviceIntent)
                }.isSuccess

                if (!startedService) {
                  // Fallback when OS blocks background FGS start (e.g. exact alarm permission not granted yet)
                  NotificationHelper.showFallbackAdhanNotification(
                    context = context,
                    prayerOrdinal = prayer.ordinal,
                    prayerNameAr = prayer.arabicName,
                    cityNameAr = settings.cityNameAr,
                  )
                  AdhanAudioController.play(
                    context = context,
                    prayerNameAr = prayer.arabicName,
                    mode = mode,
                    voice = MuezzinVoice.fromId(settings.muezzinVoiceId),
                    customAudioUri = settings.customAudioUri,
                    isPreview = false,
                    fromService = true,
                  )
                }
              }
              NotificationMode.VIBRATE -> {
                AdhanAudioController.triggerPrayerVibration(context)
                NotificationHelper.showPrayerReminderNotification(
                  context = context,
                  prayerOrdinal = prayer.ordinal,
                  prayerNameAr = prayer.arabicName,
                  cityNameAr = settings.cityNameAr,
                  vibrateOnly = true,
                )
              }
              NotificationMode.SILENT -> {
                NotificationHelper.showPrayerReminderNotification(
                  context = context,
                  prayerOrdinal = prayer.ordinal,
                  prayerNameAr = prayer.arabicName,
                  cityNameAr = settings.cityNameAr,
                  vibrateOnly = false,
                )
              }
            }
          }
        } else if (intent.action == ACTION_PRE_PRAYER_REMINDER) {
          val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)
          val prayer = PrayerType.obligatoryPrayers.firstOrNull { it.key == prayerKey }
          if (prayer != null && settings.preAdhanReminderEnabled) {
            NotificationHelper.showPreAdhanReminderNotification(
              context = context,
              prayerOrdinal = prayer.ordinal,
              prayerNameAr = prayer.arabicName,
              cityNameAr = settings.cityNameAr,
            )
          }
        }

        // Always reschedule upcoming prayers
        PrayerAlarmScheduler.scheduleAll(context, settings)
      } finally {
        runCatching { if (wakeLock?.isHeld == true) wakeLock.release() }
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_PRAYER_ALARM = "com.aistudio.athan.ACTION_PRAYER_ALARM"
    const val ACTION_PRE_PRAYER_REMINDER = "com.aistudio.athan.ACTION_PRE_PRAYER_REMINDER"
    const val ACTION_MIDNIGHT_RESCHEDULE = "com.aistudio.athan.ACTION_MIDNIGHT_RESCHEDULE"
    const val EXTRA_PRAYER_KEY = "extra_prayer_key"
  }
}
