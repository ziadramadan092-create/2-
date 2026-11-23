package com.example

import android.app.Application
import com.example.data.local.AthanDatabase
import com.example.data.repository.AthanRepository
import com.example.service.NotificationHelper
import com.example.service.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AthanApplication : Application() {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val database: AthanDatabase by lazy { AthanDatabase.getInstance(this) }
  val repository: AthanRepository by lazy { AthanRepository(database.athanDao()) }

  override fun onCreate() {
    super.onCreate()
    NotificationHelper.ensureChannels(this)
    appScope.launch {
      repository.ensureInitialized()
      val settings = repository.getSettingsSnapshot()
      PrayerAlarmScheduler.scheduleAll(this@AthanApplication, settings)
    }
  }
}
