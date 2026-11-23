package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.local.AppSettingsEntity
import com.example.domain.math.AdhanCalculator
import com.example.domain.model.PrayerType
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {

  private const val MIDNIGHT_RESCHEDULE_REQUEST_CODE = 999

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
  }

  fun scheduleAll(context: Context, settings: AppSettingsEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val tz = TimeZone.getTimeZone(settings.timeZoneId)
    val now = System.currentTimeMillis()

    val todayCal = Calendar.getInstance(tz).apply { timeInMillis = now }
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

    PrayerType.obligatoryPrayers.forEach { prayer ->
      val todayTrigger = todaySchedule.timeFor(prayer)
      val triggerAt = if (todayTrigger > now + 2_000L) {
        todayTrigger
      } else {
        tomorrowSchedule.timeFor(prayer)
      }

      val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
        action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayer.key)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        prayer.ordinal + 10,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      scheduleExactOrFallback(context, alarmManager, triggerAt, pendingIntent)

      // Optional 15-minute pre-Adhan reminder
      val preIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
        action = PrayerAlarmReceiver.ACTION_PRE_PRAYER_REMINDER
        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayer.key)
      }
      val prePending = PendingIntent.getBroadcast(
        context,
        prayer.ordinal + 200,
        preIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val preTrigger = triggerAt - 15 * 60_000L
      if (settings.preAdhanReminderEnabled && preTrigger > now + 2_000L) {
        if (canScheduleExactAlarms(context)) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTrigger, prePending)
        } else {
          alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTrigger, prePending)
        }
      } else {
        alarmManager.cancel(prePending)
      }
    }

    // Schedule daily refresh shortly after midnight
    val midnightCal = (tomorrowCal.clone() as Calendar).apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 5)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val midnightIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = PrayerAlarmReceiver.ACTION_MIDNIGHT_RESCHEDULE
    }
    val midnightPending = PendingIntent.getBroadcast(
      context,
      MIDNIGHT_RESCHEDULE_REQUEST_CODE,
      midnightIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    if (canScheduleExactAlarms(context)) {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        midnightCal.timeInMillis,
        midnightPending,
      )
    } else {
      alarmManager.set(AlarmManager.RTC_WAKEUP, midnightCal.timeInMillis, midnightPending)
    }
  }

  private fun scheduleExactOrFallback(
    context: Context,
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    pendingIntent: PendingIntent,
  ) {
    if (canScheduleExactAlarms(context)) {
      val showIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      alarmManager.setAlarmClock(
        AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
        pendingIntent,
      )
    } else {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent,
      )
    }
  }
}
