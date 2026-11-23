package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AthanDatabase
import com.example.data.repository.AthanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (
      action == Intent.ACTION_BOOT_COMPLETED ||
      action == Intent.ACTION_TIME_CHANGED ||
      action == Intent.ACTION_TIMEZONE_CHANGED
    ) {
      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val db = AthanDatabase.getInstance(context)
          val repo = AthanRepository(db.athanDao())
          val settings = repo.getSettingsSnapshot()
          PrayerAlarmScheduler.scheduleAll(context, settings)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }
}
