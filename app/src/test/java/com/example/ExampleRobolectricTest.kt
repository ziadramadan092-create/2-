package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AthanDatabase
import com.example.data.local.OfflineCitiesData
import com.example.data.repository.AthanRepository
import com.example.domain.model.CalculationMethod
import com.example.domain.model.Madhhab
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerType
import com.example.service.AdhanAudioController
import com.example.service.AdhanPlaybackService
import com.example.service.NotificationHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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
  fun `raw adhan audio resources are valid and readable`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rawIds = listOf(
      R.raw.adhan_makkah,
      R.raw.adhan_madinah,
      R.raw.adhan_egypt,
      R.raw.takbir_short,
    )
    rawIds.forEach { resId ->
      context.resources.openRawResourceFd(resId).use { afd ->
        assertNotNull(afd)
        assertTrue("Raw audio $resId should be non-empty", afd.length > 10_000L)
      }
    }
  }

  @Test
  fun `adhan playback service starts foreground notification and does not self stop`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val makkahUri = android.net.Uri.parse("android.resource://${app.packageName}/${R.raw.adhan_makkah}")
    org.robolectric.shadows.ShadowMediaPlayer.addMediaInfo(
      org.robolectric.shadows.util.DataSource.toDataSource(app, makkahUri),
      org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(17_000, 0),
    )

    val notification = NotificationHelper.buildAdhanForegroundNotification(
      context = app,
      prayerNameAr = "العصر",
      cityNameAr = "القاهرة",
    )
    assertNotNull(notification)

    val startIntent = Intent(app, AdhanPlaybackService::class.java).apply {
      action = AdhanPlaybackService.ACTION_START_ADHAN
      putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME_AR, "العصر")
      putExtra(AdhanPlaybackService.EXTRA_CITY_NAME_AR, "القاهرة")
      putExtra(AdhanPlaybackService.EXTRA_MODE_ID, NotificationMode.FULL_ADHAN.id)
      putExtra(AdhanPlaybackService.EXTRA_VOICE_ID, MuezzinVoice.MAKKAH.id)
    }

    val controller = Robolectric.buildService(AdhanPlaybackService::class.java, startIntent)
    controller.create().startCommand(0, 1)
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val shadowService = Shadows.shadowOf(controller.get())
    assertFalse("AdhanPlaybackService must not stop itself during startCommand", shadowService.isStoppedBySelf)
    assertFalse("stopService must not be queued during startCommand", Shadows.shadowOf(app).nextStoppedService != null)
    assertTrue("AdhanAudioController should report active playback", AdhanAudioController.state.value.isPlaying)

    AdhanAudioController.stop(app)
  }

  @Test
  fun `audio routing selects proper AudioAttributes for preview and alarm across streams`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

    // Ensure alarm and media volumes are audible
    val maxAlarm = am.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM).coerceAtLeast(1)
    val maxMusic = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    am.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxAlarm, 0)
    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxMusic, 0)

    val previewRoute = AdhanAudioController.resolveAudioRouting(am, isPreview = true)
    assertEquals(android.media.AudioAttributes.USAGE_MEDIA, previewRoute.usage)
    assertEquals(android.media.AudioManager.STREAM_MUSIC, previewRoute.legacyStream)
    assertTrue(previewRoute.isAudible)

    val alarmRoute = AdhanAudioController.resolveAudioRouting(am, isPreview = false)
    assertEquals(android.media.AudioAttributes.USAGE_ALARM, alarmRoute.usage)
    assertEquals(android.media.AudioManager.STREAM_ALARM, alarmRoute.legacyStream)
    assertTrue(alarmRoute.isAudible)

    val alarmAttrs = AdhanAudioController.buildAudioAttributes(alarmRoute.usage, alarmRoute.legacyStream)
    assertEquals(android.media.AudioAttributes.USAGE_ALARM, alarmAttrs.usage)
    assertEquals(android.media.AudioAttributes.CONTENT_TYPE_MUSIC, alarmAttrs.contentType)

    // When alarm volume is muted (0) but media is audible, alarm routing falls back to USAGE_MEDIA
    am.setStreamVolume(android.media.AudioManager.STREAM_ALARM, 0, 0)
    val fallbackRoute = AdhanAudioController.resolveAudioRouting(am, isPreview = false)
    assertEquals(android.media.AudioAttributes.USAGE_MEDIA, fallbackRoute.usage)
    assertEquals(android.media.AudioManager.STREAM_MUSIC, fallbackRoute.legacyStream)
    assertTrue(fallbackRoute.isAudible)
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
