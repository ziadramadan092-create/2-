package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode

class AdhanPlaybackService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_ADHAN -> {
        AdhanAudioController.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_START_ADHAN -> {
        val prayerNameAr = intent.getStringExtra(EXTRA_PRAYER_NAME_AR) ?: "الصلاة"
        val cityNameAr = intent.getStringExtra(EXTRA_CITY_NAME_AR) ?: ""
        val mode = NotificationMode.fromId(intent.getStringExtra(EXTRA_MODE_ID) ?: "")
        val voice = MuezzinVoice.fromId(intent.getStringExtra(EXTRA_VOICE_ID) ?: "")
        val customUri = intent.getStringExtra(EXTRA_CUSTOM_URI)

        val notification = NotificationHelper.buildAdhanForegroundNotification(
          context = this,
          prayerNameAr = prayerNameAr,
          cityNameAr = cityNameAr,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            NotificationHelper.ADHAN_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
          )
        } else {
          startForeground(NotificationHelper.ADHAN_NOTIFICATION_ID, notification)
        }

        AdhanAudioController.play(
          context = this,
          prayerNameAr = prayerNameAr,
          mode = mode,
          voice = voice,
          customAudioUri = customUri,
          isPreview = false,
          onCompleted = {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
          },
        )
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    AdhanAudioController.stop()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START_ADHAN = "com.aistudio.athan.ACTION_START_ADHAN"
    const val ACTION_STOP_ADHAN = "com.aistudio.athan.ACTION_STOP_ADHAN"
    const val EXTRA_PRAYER_NAME_AR = "extra_prayer_name_ar"
    const val EXTRA_CITY_NAME_AR = "extra_city_name_ar"
    const val EXTRA_MODE_ID = "extra_mode_id"
    const val EXTRA_VOICE_ID = "extra_voice_id"
    const val EXTRA_CUSTOM_URI = "extra_custom_uri"
  }
}
