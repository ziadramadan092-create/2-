package com.example.service

import android.content.Context
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.R
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdhanPlaybackState(
  val isPlaying: Boolean = false,
  val prayerNameAr: String = "",
  val modeLabel: String = "",
  val isPreview: Boolean = false,
)

object AdhanAudioController {
  private val _state = MutableStateFlow(AdhanPlaybackState())
  val state: StateFlow<AdhanPlaybackState> = _state.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var volumeObserver: ContentObserver? = null
  private var observedContext: Context? = null
  private var lastObservedVolume: Int = -1

  @Synchronized
  fun play(
    context: Context,
    prayerNameAr: String,
    mode: NotificationMode,
    voice: MuezzinVoice,
    customAudioUri: String?,
    isPreview: Boolean = false,
    onCompleted: (() -> Unit)? = null,
  ) {
    stop(context)
    val appCtx = context.applicationContext

    val usage = if (isPreview) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM
    val attrs = AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()

    val player = MediaPlayer().apply {
      setAudioAttributes(attrs)
    }

    val loaded = try {
      when {
        mode == NotificationMode.TAKBIR -> {
          val afd = appCtx.resources.openRawResourceFd(R.raw.takbir_short)
          player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
          afd.close()
          true
        }
        voice == MuezzinVoice.CUSTOM && !customAudioUri.isNullOrBlank() -> {
          player.setDataSource(appCtx, Uri.parse(customAudioUri))
          true
        }
        else -> {
          val rawId = voice.rawResId ?: R.raw.adhan_makkah
          val afd = appCtx.resources.openRawResourceFd(rawId)
          player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
          afd.close()
          true
        }
      }
    } catch (_: Exception) {
      // Fallback to built-in Makkah sound if custom URI is unavailable
      runCatching {
        val afd = appCtx.resources.openRawResourceFd(R.raw.adhan_makkah)
        player.reset()
        player.setAudioAttributes(attrs)
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
      }.isSuccess
    }

    if (!loaded) {
      player.release()
      onCompleted?.invoke()
      return
    }

    player.setOnPreparedListener { mp ->
      _state.value = AdhanPlaybackState(
        isPlaying = true,
        prayerNameAr = prayerNameAr,
        modeLabel = if (mode == NotificationMode.TAKBIR) "تكبيرة الأذان" else voice.arabicName,
        isPreview = isPreview,
      )
      registerVolumeDownObserver(appCtx, isPreview)
      mp.start()
    }

    player.setOnCompletionListener {
      stop(appCtx)
      onCompleted?.invoke()
    }

    player.setOnErrorListener { _, _, _ ->
      stop(appCtx)
      onCompleted?.invoke()
      true
    }

    mediaPlayer = player
    runCatching { player.prepareAsync() }.onFailure {
      stop(appCtx)
      onCompleted?.invoke()
    }
  }

  @Synchronized
  fun stop(context: Context? = null) {
    unregisterVolumeObserver()
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.release()
      }
    }
    mediaPlayer = null
    _state.value = AdhanPlaybackState()
    context?.let { ctx ->
      // Ensure foreground service stops if it was running
      runCatching {
        val stopIntent = android.content.Intent(ctx, AdhanPlaybackService::class.java).apply {
          action = AdhanPlaybackService.ACTION_STOP_ADHAN
        }
        ctx.stopService(stopIntent)
      }
    }
  }

  fun triggerPrayerVibration(context: Context) {
    val pattern = longArrayOf(0, 350, 200, 350, 200, 500)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
      @Suppress("DEPRECATION")
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(pattern, -1)
      }
    }
  }

  private fun registerVolumeDownObserver(appCtx: Context, isPreview: Boolean) {
    unregisterVolumeObserver()
    val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val stream = if (isPreview) AudioManager.STREAM_MUSIC else AudioManager.STREAM_ALARM
    lastObservedVolume = audioManager.getStreamVolume(stream)

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        val currentVol = audioManager.getStreamVolume(stream)
        if (currentVol < lastObservedVolume && _state.value.isPlaying) {
          stop(appCtx)
        } else {
          lastObservedVolume = currentVol
        }
      }
    }
    runCatching {
      appCtx.contentResolver.registerContentObserver(
        Settings.System.CONTENT_URI,
        true,
        observer,
      )
      volumeObserver = observer
      observedContext = appCtx
    }
  }

  private fun unregisterVolumeObserver() {
    val obs = volumeObserver
    val ctx = observedContext
    if (obs != null && ctx != null) {
      runCatching { ctx.contentResolver.unregisterContentObserver(obs) }
    }
    volumeObserver = null
    observedContext = null
  }
}
