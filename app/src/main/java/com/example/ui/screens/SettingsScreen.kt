package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.CalculationMethod
import com.example.domain.model.Madhhab
import com.example.domain.model.MuezzinVoice
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerType
import com.example.domain.model.ThemeMode
import com.example.service.PrayerAlarmScheduler
import com.example.ui.MainUiState
import com.example.ui.components.AudioPreviewWaveformCard
import com.example.ui.components.DeveloperCreditCard
import com.example.ui.components.GlassCard
import com.example.ui.components.isAppInDarkTheme

@Composable
fun SettingsScreen(
  uiState: MainUiState,
  onOpenCityPicker: () -> Unit,
  onSelectMethod: (CalculationMethod) -> Unit,
  onSelectMadhhab: (Madhhab) -> Unit,
  onAdjustHijriDays: (Int) -> Unit,
  onAdjustPrayerOffset: (PrayerType, Int) -> Unit,
  onSetPrayerMode: (PrayerType, NotificationMode) -> Unit,
  onSelectMuezzinVoice: (MuezzinVoice) -> Unit,
  onCustomAudioSelected: (Uri) -> Unit,
  onPreviewAdhan: () -> Unit,
  onStopAdhan: () -> Unit,
  onTogglePreAdhanReminder: (Boolean) -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onToggle24Hour: (Boolean) -> Unit,
  onExportBackup: (Uri) -> Unit,
  onImportBackup: (Uri) -> Unit,
  onRequestNotificationPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val settings = uiState.settings
  val isDark = isAppInDarkTheme(settings.themeMode())
  var showMethodDialog by remember { mutableStateOf(false) }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) onCustomAudioSelected(uri)
  }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) onExportBackup(uri)
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) onImportBackup(uri)
  }

  if (showMethodDialog) {
    AlertDialog(
      onDismissRequest = { showMethodDialog = false },
      title = { Text("طريقة الحساب الفقهية") },
      text = {
        LazyColumn(modifier = Modifier.height(340.dp)) {
          items(CalculationMethod.entries.size) { index ->
            val method = CalculationMethod.entries[index]
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectMethod(method)
                  showMethodDialog = false
                }
                .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = settings.calculationMethod() == method,
                onClick = {
                  onSelectMethod(method)
                  showMethodDialog = false
                },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = method.arabicName,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showMethodDialog = false }) {
          Text("إغلاق")
        }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 1. Sleek Waveform Audio Preview Card + 15-min Pre-Adhan Reminder (matching mockup)
    item {
      val voice = settings.muezzinVoice()
      val sub = if (voice == MuezzinVoice.CUSTOM && !settings.customAudioName.isNullOrBlank()) {
        settings.customAudioName
      } else {
        voice.subtitle
      }
      Column(
        modifier = Modifier.widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AudioPreviewWaveformCard(
          voiceTitle = voice.arabicName,
          voiceSubtitle = sub,
          isPlaying = uiState.adhanPlayback.isPlaying,
          isDark = isDark,
          onTogglePlay = {
            if (uiState.adhanPlayback.isPlaying) onStopAdhan() else onPreviewAdhan()
          },
          modifier = Modifier.testTag("settings_preview_adhan_button"),
        )

        GlassCard(isDark = isDark, cornerRadius = 18.dp) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "تنبيه قبل الأذان بـ ١٥ دقيقة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "إشعار تذكيري قبل دخول وقت كل صلاة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = settings.preAdhanReminderEnabled,
              onCheckedChange = onTogglePreAdhanReminder,
              modifier = Modifier.testTag("pre_adhan_reminder_switch"),
            )
          }
        }
      }
    }

    // 2. Muezzin Voices Selection & Custom Audio Import
    item {
      GlassSettingsSectionCard(title = "اختيار صوت المؤذن", isDark = isDark) {
        MuezzinVoice.entries.forEach { voice ->
          val selected = settings.muezzinVoice() == voice
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (voice == MuezzinVoice.CUSTOM && settings.customAudioUri == null) {
                  audioPickerLauncher.launch(arrayOf("audio/*"))
                } else {
                  onSelectMuezzinVoice(voice)
                }
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selected,
              onClick = {
                if (voice == MuezzinVoice.CUSTOM && settings.customAudioUri == null) {
                  audioPickerLauncher.launch(arrayOf("audio/*"))
                } else {
                  onSelectMuezzinVoice(voice)
                }
              },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(voice.arabicName, style = MaterialTheme.typography.titleMedium)
              val sub = if (voice == MuezzinVoice.CUSTOM && !settings.customAudioName.isNullOrBlank()) {
                settings.customAudioName
              } else {
                voice.subtitle
              }
              Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (voice == MuezzinVoice.CUSTOM) {
              TextButton(onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استيراد")
              }
            }
          }
        }
      }
    }

    // 3. Location & Fiqh Calculation
    item {
      GlassSettingsSectionCard(title = "الموقع وطريقة الحساب", isDark = isDark) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCityPicker() }
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("المدينة الحالية", style = MaterialTheme.typography.titleMedium)
              Text(
                "${settings.cityNameAr}، ${settings.countryAr}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          TextButton(onClick = onOpenCityPicker) { Text("تغيير") }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showMethodDialog = true }
            .padding(vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("طريقة الحساب", style = MaterialTheme.typography.titleMedium)
            Text(
              settings.calculationMethod().arabicName,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          TextButton(onClick = { showMethodDialog = true }) { Text("اختيار") }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Spacer(modifier = Modifier.height(8.dp))
        Text("المذهب الفقهي (صلاة العصر)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Madhhab.entries.forEach { m ->
            FilterChip(
              selected = settings.madhhab() == m,
              onClick = { onSelectMadhhab(m) },
              label = { Text(if (m == Madhhab.SHAFI) "الجمهور" else "الحنفي") },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("تعديل التاريخ الهجري", style = MaterialTheme.typography.titleMedium)
            Text(
              "${settings.hijriAdjustmentDays} يوم",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAdjustHijriDays(-1) }) {
              Icon(Icons.Filled.Remove, contentDescription = "إنقاص يوم هجري")
            }
            Text(
              text = "${settings.hijriAdjustmentDays}",
              style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onAdjustHijriDays(1) }) {
              Icon(Icons.Filled.Add, contentDescription = "زيادة يوم هجري")
            }
          }
        }
      }
    }

    // 4. Per-Prayer Manual Offsets & Alert Modes
    item {
      GlassSettingsSectionCard(title = "التنبيهات والتعديل اليدوي للصلوات", isDark = isDark) {
        val modes = settings.notificationModes()
        val offsets = settings.offsets()

        PrayerType.obligatoryPrayers.forEachIndexed { idx, prayer ->
          var modeMenuExpanded by remember { mutableStateOf(false) }
          val currentMode = modes.forPrayer(prayer)
          val offset = offsets.forPrayer(prayer)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(prayer.arabicName, style = MaterialTheme.typography.titleMedium)
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                  .padding(top = 4.dp)
                  .clickable { modeMenuExpanded = true },
              ) {
                Text(
                  text = currentMode.arabicName,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
              DropdownMenu(
                expanded = modeMenuExpanded,
                onDismissRequest = { modeMenuExpanded = false },
              ) {
                NotificationMode.entries.forEach { m ->
                  DropdownMenuItem(
                    text = { Text(m.arabicName) },
                    leadingIcon = {
                      if (m == currentMode) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                      onSetPrayerMode(prayer, m)
                      modeMenuExpanded = false
                    },
                  )
                }
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { onAdjustPrayerOffset(prayer, -1) }) {
                Icon(Icons.Filled.Remove, contentDescription = "تأخير دقيقة")
              }
              val sign = if (offset > 0) "+" else ""
              Text(
                text = "$sign$offset د",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.widthIn(min = 40.dp),
              )
              IconButton(onClick = { onAdjustPrayerOffset(prayer, 1) }) {
                Icon(Icons.Filled.Add, contentDescription = "تقديم دقيقة")
              }
            }
          }
          if (idx < PrayerType.obligatoryPrayers.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
          }
        }
      }
    }

    // 5. Appearance & Time Format
    item {
      GlassSettingsSectionCard(title = "المظهر وتنسيق الساعة", isDark = isDark) {
        Text("وضع العرض", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = settings.themeMode() == mode,
              onClick = { onSelectThemeMode(mode) },
              label = { Text(mode.arabicName) },
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("استخدام نظام ٢٤ ساعة", style = MaterialTheme.typography.titleMedium)
          Switch(
            checked = settings.use24HourFormat,
            onCheckedChange = onToggle24Hour,
          )
        }
      }
    }

    // 6. Background Permissions Helper
    item {
      val hasPostNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
      val hasExactAlarm = PrayerAlarmScheduler.canScheduleExactAlarms(context)

      if (!hasPostNotif || !hasExactAlarm) {
        GlassSettingsSectionCard(title = "أذونات التنبيه في الخلفية", isDark = isDark) {
          if (!hasPostNotif) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("إذن الإشعارات مطلوب للتنبيه", style = MaterialTheme.typography.bodyMedium)
              TextButton(onClick = onRequestNotificationPermission) {
                Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تفعيل")
              }
            }
          }
          if (!hasExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "إذن المنبهات الدقيقة للأذان بالثانية",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = {
                  val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                  }
                  runCatching { context.startActivity(intent) }
                },
              ) {
                Text("منح الإذن")
              }
            }
          }
        }
      }
    }

    // 7. JSON Backup & Restore
    item {
      GlassSettingsSectionCard(title = "النسخ الاحتياطي والاستعادة (JSON)", isDark = isDark) {
        Text(
          text = "احفظ جميع إعداداتك والمواقيت اليدوية وإحصائيات التسبيح محلياً في ملف JSON واستعدها في أي وقت دون إنترنت.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { exportLauncher.launch("athan_backup.json") },
            modifier = Modifier
              .weight(1f)
              .testTag("export_backup_button"),
          ) {
            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تصدير JSON")
          }
          Button(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier
              .weight(1f)
              .testTag("import_backup_button"),
          ) {
            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("استعادة")
          }
        }
      }
    }

    // 8. Developer Credit Card — Eng_Ziad Hanzal
    item {
      Box(modifier = Modifier.widthIn(max = 640.dp)) {
        DeveloperCreditCard(
          isDark = isDark,
          modifier = Modifier.testTag("developer_credit_card"),
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun GlassSettingsSectionCard(
  title: String,
  isDark: Boolean,
  content: @Composable () -> Unit,
) {
  GlassCard(
    isDark = isDark,
    cornerRadius = 20.dp,
    modifier = Modifier
      .widthIn(max = 640.dp)
      .fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      content()
    }
  }
}
