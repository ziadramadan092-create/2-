package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdhanPlaybackState(
  val isPlaying: Boolean = false,
  val prayerNameAr: String = "",
  val modeLabel: String = "",
  val isPreview: Boolean = false,
)

data class ResolvedAudioRouting(
  val usage: Int,
  val legacyStream: Int,
  val isAudible: Boolean,
  val warningMessage: String? = null,
)

object AdhanAudioController {
  private val _state = MutableStateFlow(AdhanPlaybackState())
  val state: StateFlow<AdhanPlaybackState> = _state.asStateFlow()

  private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val errors: SharedFlow<String> = _errors.asSharedFlow()

  private val mainHandler = Handler(Looper.getMainLooper())

  private var mediaPlayer: MediaPlayer? = null
  private var playbackGeneration: Long = 0L
  private var playbackStartedAtMs: Long = 0L
  private var lastKnownStreamVolume: Int = -1
  private var volumeReceiver: BroadcastReceiver? = null
  private var registeredContext: Context? = null
  private var audioFocusRequest: AudioFocusRequest? = null
  private var focusAudioManager: AudioManager? = null
  private var legacyFocusListener: AudioManager.OnAudioFocusChangeListener? = null

  internal fun resolveAudioRouting(
    audioManager: AudioManager?,
    isPreview: Boolean,
  ): ResolvedAudioRouting {
    val alarmVol = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1
    val musicVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1

    return if (isPreview) {
      val audible = musicVol > 0
      ResolvedAudioRouting(
        usage = AudioAttributes.USAGE_MEDIA,
        legacyStream = AudioManager.STREAM_MUSIC,
        isAudible = audible,
        warningMessage = if (!audible) {
          "مستوى صوت الوسائط مكتوم في الهاتف، يرجى رفعه لسماع تجربة الأذان"
        } else {
          null
        },
      )
    } else {
      // Scheduled Adhan: use USAGE_ALARM so silent/vibrate ringer modes do not silence Adhan.
      // Fall back to USAGE_MEDIA only if alarm stream is 0 while media stream is audible.
      val useAlarmStream = alarmVol > 0 || musicVol == 0
      val usage = if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
      val stream = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
      val audible = (audioManager?.getStreamVolume(stream) ?: 1) > 0
      ResolvedAudioRouting(
        usage = usage,
        legacyStream = stream,
        isAudible = audible,
        warningMessage = if (!audible) {
          "مستوى صوت المنبه والوسائط مكتوم في الهاتف، يرجى رفع الصوت لسماع الأذان"
        } else {
          null
        },
      )
    }
  }

  internal fun buildAudioAttributes(usage: Int, legacyStream: Int): AudioAttributes {
    val builder = AudioAttributes.Builder()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      @Suppress("DEPRECATION")
      builder.setLegacyStreamType(legacyStream)
    }
    return builder
      .setUsage(usage)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()
  }

  @Synchronized
  fun play(
    context: Context,
    prayerNameAr: String,
    mode: NotificationMode,
    voice: MuezzinVoice,
    customAudioUri: String?,
    isPreview: Boolean = false,
    fromService: Boolean = false,
    onCompleted: (() -> Unit)? = null,
  ) {
    if (fromService) {
      stopPlaybackOnly()
    } else {
      stop(context)
    }

    val appCtx = context.applicationContext
    val gen = ++playbackGeneration

    val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (!isPreview && audioManager != null && (
        audioManager.mode == AudioManager.MODE_IN_CALL ||
          audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
      )
    ) {
      triggerPrayerVibration(appCtx)
      invokeOnMain(onCompleted)
      return
    }

    val routing = resolveAudioRouting(audioManager, isPreview)
    routing.warningMessage?.let { _errors.tryEmit(it) }

    val attrs = buildAudioAttributes(routing.usage, routing.legacyStream)
    val player = createConfiguredPlayer(appCtx, attrs, isPreview)

    var usedCustomFallback = false
    val loaded = try {
      when {
        mode == NotificationMode.TAKBIR -> {
          loadRawResource(appCtx, player, attrs, isPreview, R.raw.takbir_short)
        }
        voice == MuezzinVoice.CUSTOM && !customAudioUri.isNullOrBlank() -> {
          loadCustomUri(appCtx, player, attrs, isPreview, Uri.parse(customAudioUri))
        }
        else -> {
          val rawId = voice.rawResId ?: R.raw.adhan_makkah
          loadRawResource(appCtx, player, attrs, isPreview, rawId)
        }
      }
    } catch (_: Exception) {
      usedCustomFallback = voice == MuezzinVoice.CUSTOM && !customAudioUri.isNullOrBlank()
      runCatching {
        resetPlayerConfig(appCtx, player, attrs, isPreview)
        loadRawResource(appCtx, player, attrs, isPreview, R.raw.adhan_makkah)
      }.getOrDefault(false)
    }

    if (!loaded) {
      runCatching { player.release() }
      _errors.tryEmit("تعذر تحميل ملف صوت الأذان")
      invokeOnMain(onCompleted)
      return
    }

    if (usedCustomFallback) {
      _errors.tryEmit("تعذر قراءة الملف المخصص، تم التشغيل بصوت الحرم المكي")
    }

    player.setOnPreparedListener { mp ->
      synchronized(this@AdhanAudioController) {
        if (gen != playbackGeneration) {
          runCatching { mp.release() }
          return@setOnPreparedListener
        }
        requestAudioFocus(audioManager, attrs, routing.legacyStream, gen, fromService, appCtx, onCompleted)
        runCatching { mp.setVolume(1.0f, 1.0f) }
        playbackStartedAtMs = System.currentTimeMillis()
        lastKnownStreamVolume = audioManager?.getStreamVolume(routing.legacyStream) ?: -1
        _state.value = AdhanPlaybackState(
          isPlaying = true,
          prayerNameAr = prayerNameAr,
          modeLabel = if (mode == NotificationMode.TAKBIR) "تكبيرة الأذان" else voice.arabicName,
          isPreview = isPreview,
        )
        registerVolumeDownReceiver(appCtx, routing.legacyStream, gen)
        runCatching { mp.start() }.onFailure {
          _errors.tryEmit("تعذر بدء تشغيل صوت الأذان")
          stop(if (fromService) null else appCtx)
          invokeOnMain(onCompleted)
        }
      }
    }

    player.setOnCompletionListener {
      synchronized(this@AdhanAudioController) {
        if (gen != playbackGeneration) return@setOnCompletionListener
        stop(if (fromService) null else appCtx)
      }
      invokeOnMain(onCompleted)
    }

    player.setOnErrorListener { _, what, extra ->
      synchronized(this@AdhanAudioController) {
        if (gen != playbackGeneration) return@setOnErrorListener true
        _errors.tryEmit("حدث خطأ أثناء تشغيل الصوت (رمز $what/$extra)")
        stop(if (fromService) null else appCtx)
      }
      invokeOnMain(onCompleted)
      true
    }

    mediaPlayer = player
    runCatching {
      player.prepareAsync()
    }.onFailure {
      _errors.tryEmit("تعذر تهيئة مشغل الصوت")
      stop(if (fromService) null else appCtx)
      invokeOnMain(onCompleted)
    }
  }

  @Synchronized
  fun stop(context: Context? = null) {
    stopPlaybackOnly()
    context?.let { ctx ->
      runCatching {
        ctx.stopService(Intent(ctx, AdhanPlaybackService::class.java))
      }
    }
  }

  @Synchronized
  private fun stopPlaybackOnly() {
    playbackGeneration++
    lastKnownStreamVolume = -1
    unregisterVolumeReceiver()
    abandonAudioFocus()
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.release()
      }
    }
    mediaPlayer = null
    _state.value = AdhanPlaybackState()
  }

  private fun createConfiguredPlayer(
    appCtx: Context,
    attrs: AudioAttributes,
    isPreview: Boolean,
  ): MediaPlayer = MediaPlayer().apply {
    setAudioAttributes(attrs)
    if (!isPreview) {
      runCatching { setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK) }
    }
  }

  private fun resetPlayerConfig(
    appCtx: Context,
    player: MediaPlayer,
    attrs: AudioAttributes,
    isPreview: Boolean,
  ) {
    player.reset()
    player.setAudioAttributes(attrs)
    if (!isPreview) {
      runCatching { player.setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK) }
    }
  }

  private fun loadCustomUri(
    appCtx: Context,
    player: MediaPlayer,
    attrs: AudioAttributes,
    isPreview: Boolean,
    uri: Uri,
  ): Boolean {
    val directLoaded = runCatching {
      player.setDataSource(appCtx, uri)
      true
    }.getOrDefault(false)
    if (directLoaded) return true

    resetPlayerConfig(appCtx, player, attrs, isPreview)
    appCtx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
      player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
      return true
    }
    return false
  }

  private fun loadRawResource(
    appCtx: Context,
    player: MediaPlayer,
    attrs: AudioAttributes,
    isPreview: Boolean,
    rawResId: Int,
  ): Boolean {
    val afdLoaded = runCatching {
      appCtx.resources.openRawResourceFd(rawResId)?.use { afd ->
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        true
      } ?: false
    }.getOrDefault(false)

    if (afdLoaded) return true

    runCatching { resetPlayerConfig(appCtx, player, attrs, isPreview) }
    val uri = Uri.parse("android.resource://${appCtx.packageName}/$rawResId")
    player.setDataSource(appCtx, uri)
    return true
  }

  private fun requestAudioFocus(
    audioManager: AudioManager?,
    attrs: AudioAttributes,
    legacyStream: Int,
    gen: Long,
    fromService: Boolean,
    appCtx: Context,
    onCompleted: (() -> Unit)?,
  ) {
    if (audioManager == null) return
    val listener = AudioManager.OnAudioFocusChangeListener { change ->
      var shouldComplete = false
      synchronized(this@AdhanAudioController) {
        if (gen != playbackGeneration) return@OnAudioFocusChangeListener
        when (change) {
          AudioManager.AUDIOFOCUS_LOSS,
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
            stop(if (fromService) null else appCtx)
            shouldComplete = true
          }
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
            runCatching { mediaPlayer?.setVolume(0.25f, 0.25f) }
          }
          AudioManager.AUDIOFOCUS_GAIN -> {
            runCatching { mediaPlayer?.setVolume(1.0f, 1.0f) }
          }
        }
      }
      if (shouldComplete) {
        invokeOnMain(onCompleted)
      }
    }

    focusAudioManager = audioManager
    legacyFocusListener = listener

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attrs)
        .setAcceptsDelayedFocusGain(false)
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener(listener, mainHandler)
        .build()
      audioFocusRequest = req
      runCatching { audioManager.requestAudioFocus(req) }
    } else {
      @Suppress("DEPRECATION")
      runCatching {
        audioManager.requestAudioFocus(
          listener,
          legacyStream,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        )
      }
    }
  }

  private fun abandonAudioFocus() {
    val am = focusAudioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { req ->
        runCatching { am.abandonAudioFocusRequest(req) }
      }
      audioFocusRequest = null
    } else {
      legacyFocusListener?.let { listener ->
        @Suppress("DEPRECATION")
        runCatching { am.abandonAudioFocus(listener) }
      }
    }
    legacyFocusListener = null
    focusAudioManager = null
  }

  private fun registerVolumeDownReceiver(appCtx: Context, activeStream: Int, gen: Long) {
    unregisterVolumeReceiver()
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != VOLUME_CHANGED_ACTION) return
        val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
        if (streamType != activeStream) return

        val prevExtra = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, -1)
        val currVol = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
        val prevVol = if (prevExtra >= 0) prevExtra else lastKnownStreamVolume
        if (currVol >= 0) lastKnownStreamVolume = currVol

        val elapsedSinceStart = System.currentTimeMillis() - playbackStartedAtMs
        if (
          elapsedSinceStart > 300L &&
          currVol >= 0 &&
          prevVol > currVol &&
          _state.value.isPlaying &&
          gen == playbackGeneration
        ) {
          stop(appCtx)
        }
      }
    }

    runCatching {
      val filter = IntentFilter(VOLUME_CHANGED_ACTION)
      ContextCompat.registerReceiver(appCtx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
      volumeReceiver = receiver
      registeredContext = appCtx
    }
  }

  private fun unregisterVolumeReceiver() {
    val rec = volumeReceiver
    val ctx = registeredContext
    if (rec != null && ctx != null) {
      runCatching { ctx.unregisterReceiver(rec) }
    }
    volumeReceiver = null
    registeredContext = null
  }

  private fun invokeOnMain(block: (() -> Unit)?) {
    if (block == null) return
    if (Looper.myLooper() == Looper.getMainLooper()) {
      block()
    } else {
      mainHandler.post(block)
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

  private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
  private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
  private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
  private const val EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
}
