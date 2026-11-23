package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  const val ADHAN_CHANNEL_ID = "athan_playback_channel"
  const val ADHAN_FALLBACK_CHANNEL_ID = "athan_fallback_sound_channel"
  const val REMINDER_CHANNEL_ID = "prayer_reminder_channel"
  const val ADHAN_NOTIFICATION_ID = 1001
  const val REMINDER_BASE_NOTIFICATION_ID = 2000

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val adhanChannel = NotificationChannel(
      ADHAN_CHANNEL_ID,
      context.getString(R.string.adhan_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.adhan_channel_desc)
      setSound(null, null)
      enableVibration(true)
      lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }

    val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_makkah}")
    val alarmAttrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()

    val fallbackChannel = NotificationChannel(
      ADHAN_FALLBACK_CHANNEL_ID,
      context.getString(R.string.adhan_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.adhan_channel_desc)
      setSound(soundUri, alarmAttrs)
      enableVibration(true)
      lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }

    val reminderChannel = NotificationChannel(
      REMINDER_CHANNEL_ID,
      context.getString(R.string.reminder_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.reminder_channel_desc)
      setSound(null, null)
      enableVibration(false)
      lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }

    manager.createNotificationChannel(adhanChannel)
    manager.createNotificationChannel(fallbackChannel)
    manager.createNotificationChannel(reminderChannel)
  }

  fun buildAdhanForegroundNotification(
    context: Context,
    prayerNameAr: String,
    cityNameAr: String,
  ): android.app.Notification {
    ensureChannels(context)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openAppPending = PendingIntent.getActivity(
      context,
      0,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val stopIntent = Intent(context, AdhanPlaybackService::class.java).apply {
      action = AdhanPlaybackService.ACTION_STOP_ADHAN
    }
    val stopPending = PendingIntent.getService(
      context,
      1,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    return NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_adhan)
      .setContentTitle("حان الآن موعد صلاة $prayerNameAr")
      .setContentText("توقيت $cityNameAr • اضغط لإيقاف الأذان")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setContentIntent(openAppPending)
      .addAction(
        android.R.drawable.ic_media_pause,
        context.getString(R.string.stop_adhan_now),
        stopPending,
      )
      .build()
  }

  fun showFallbackAdhanNotification(
    context: Context,
    prayerOrdinal: Int,
    prayerNameAr: String,
    cityNameAr: String,
  ) {
    ensureChannels(context)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openAppPending = PendingIntent.getActivity(
      context,
      prayerOrdinal + 400,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_makkah}")

    val notification = NotificationCompat.Builder(context, ADHAN_FALLBACK_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_adhan)
      .setContentTitle("حان الآن موعد صلاة $prayerNameAr")
      .setContentText("توقيت $cityNameAr • افتح التطبيق لإدارة التنبيهات")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setSound(soundUri)
      .setAutoCancel(true)
      .setContentIntent(openAppPending)
      .build()

    runCatching {
      NotificationManagerCompat.from(context)
        .notify(ADHAN_NOTIFICATION_ID, notification)
    }
  }

  fun showPrayerReminderNotification(
    context: Context,
    prayerOrdinal: Int,
    prayerNameAr: String,
    cityNameAr: String,
    vibrateOnly: Boolean,
  ) {
    ensureChannels(context)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openAppPending = PendingIntent.getActivity(
      context,
      prayerOrdinal + 100,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val subtitle = if (vibrateOnly) {
      "تنبيه بالاهتزاز • $cityNameAr"
    } else {
      "تنبيه صامت • $cityNameAr"
    }

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_adhan)
      .setContentTitle("حان الآن وقت صلاة $prayerNameAr")
      .setContentText(subtitle)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openAppPending)
      .build()

    runCatching {
      NotificationManagerCompat.from(context)
        .notify(REMINDER_BASE_NOTIFICATION_ID + prayerOrdinal, notification)
    }
  }

  fun showPreAdhanReminderNotification(
    context: Context,
    prayerOrdinal: Int,
    prayerNameAr: String,
    cityNameAr: String,
  ) {
    ensureChannels(context)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openAppPending = PendingIntent.getActivity(
      context,
      prayerOrdinal + 300,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_adhan)
      .setContentTitle("اقترب موعد صلاة $prayerNameAr")
      .setContentText("متبقي ١٥ دقيقة على الأذان • $cityNameAr")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openAppPending)
      .build()

    runCatching {
      NotificationManagerCompat.from(context)
        .notify(REMINDER_BASE_NOTIFICATION_ID + 50 + prayerOrdinal, notification)
    }
  }
}
