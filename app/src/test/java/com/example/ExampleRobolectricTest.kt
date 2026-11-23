package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AthanDatabase
import com.example.data.local.OfflineCitiesData
import com.example.data.repository.AthanRepository
import com.example.domain.model.CalculationMethod
import com.example.domain.model.Madhhab
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("الأذان", appName)
  }

  @Test
  fun `room repository persists settings and roundtrips json backup`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AthanDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val repo = AthanRepository(db.athanDao())
      repo.ensureInitialized()

      val makkah = OfflineCitiesData.cities.first { it.id == "makkah" }
      repo.updateCity(makkah)
      repo.updateCalculationMethod(CalculationMethod.UMM_AL_QURA)
      repo.updateMadhhab(Madhhab.HANAFI)
      repo.updatePrayerOffset(PrayerType.FAJR, 4)
      repo.updatePrayerNotificationMode(PrayerType.ASR, NotificationMode.VIBRATE)
      repo.incrementTasbih()

      val json = repo.exportBackupJson()
      assertTrue(json.contains("makkah"))

      // Change settings and then restore from JSON
      val cairo = OfflineCitiesData.cities.first { it.id == "cairo" }
      repo.updateCity(cairo)
      repo.updateMadhhab(Madhhab.SHAFI)

      val ok = repo.importBackupJson(json)
      assertTrue(ok)

      val restored = repo.getSettingsSnapshot()
      assertEquals("makkah", restored.cityId)
      assertEquals(CalculationMethod.UMM_AL_QURA, restored.calculationMethod())
      assertEquals(Madhhab.HANAFI, restored.madhhab())
      assertEquals(4, restored.fajrOffset)
      assertEquals(NotificationMode.VIBRATE, restored.notificationModes().forPrayer(PrayerType.ASR))
    } finally {
      db.close()
    }
  }
}
