package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineAdhkarData
import com.example.domain.model.DhikrCategory
import com.example.domain.model.DhikrItem
import com.example.ui.MainUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.isAppInDarkTheme
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SuccessCyanGreen

@Composable
fun AdhkarScreen(
  uiState: MainUiState,
  onIncrementTasbih: (() -> Unit) -> Unit,
  onResetTasbih: () -> Unit,
  onSelectTasbihPhrase: (String) -> Unit,
  onSelectTasbihCycle: (Int) -> Unit,
  onDecrementDhikr: (String, Int) -> Unit,
  onResetCategory: (DhikrCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isAppInDarkTheme(uiState.settings.themeMode())
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf(
    "السبحة",
    DhikrCategory.MORNING.arabicTitle,
    DhikrCategory.EVENING.arabicTitle,
    DhikrCategory.AFTER_PRAYER.arabicTitle,
  )

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier
        .widthIn(max = 640.dp)
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      tabs.forEachIndexed { index, title ->
        FilterChip(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          label = {
            Text(
              text = title,
              style = MaterialTheme.typography.labelLarge,
            )
          },
          modifier = Modifier.testTag("adhkar_tab_$index"),
        )
      }
    }

    when (selectedTab) {
      0 -> TasbihView(
        uiState = uiState,
        isDark = isDark,
        onIncrement = onIncrementTasbih,
        onReset = onResetTasbih,
        onSelectPhrase = onSelectTasbihPhrase,
        onSelectCycle = onSelectTasbihCycle,
      )
      1 -> AdhkarListView(
        category = DhikrCategory.MORNING,
        isDark = isDark,
        progressMap = uiState.adhkarProgressToday,
        onDecrement = onDecrementDhikr,
        onResetCategory = onResetCategory,
      )
      2 -> AdhkarListView(
        category = DhikrCategory.EVENING,
        isDark = isDark,
        progressMap = uiState.adhkarProgressToday,
        onDecrement = onDecrementDhikr,
        onResetCategory = onResetCategory,
      )
      3 -> AdhkarListView(
        category = DhikrCategory.AFTER_PRAYER,
        isDark = isDark,
        progressMap = uiState.adhkarProgressToday,
        onDecrement = onDecrementDhikr,
        onResetCategory = onResetCategory,
      )
    }
  }
}

@Composable
private fun TasbihView(
  uiState: MainUiState,
  isDark: Boolean,
  onIncrement: (() -> Unit) -> Unit,
  onReset: () -> Unit,
  onSelectPhrase: (String) -> Unit,
  onSelectCycle: (Int) -> Unit,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val tasbih = uiState.tasbih
  val currentPhrase = OfflineAdhkarData.tasbihPhrases.firstOrNull {
    it.id == tasbih.selectedPhraseId
  } ?: OfflineAdhkarData.tasbihPhrases.first()

  val cycleOptions = listOf(33 to "٣٣", 99 to "٩٩", 100 to "١٠٠", 0 to "مفتوح")
  val progress = if (tasbih.targetCycle > 0) {
    (tasbih.currentCount.toFloat() / tasbih.targetCycle.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  Column(
    modifier = Modifier
      .widthIn(max = 640.dp)
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OfflineAdhkarData.tasbihPhrases.forEach { phrase ->
        FilterChip(
          selected = phrase.id == tasbih.selectedPhraseId,
          onClick = { onSelectPhrase(phrase.id) },
          label = { Text(phrase.text, maxLines = 1) },
        )
      }
    }

    GlassCard(
      isDark = isDark,
      cornerRadius = 22.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = currentPhrase.text,
          fontFamily = AmiriFontFamily,
          fontSize = 24.sp,
          lineHeight = 36.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = currentPhrase.virtue,
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "الدورة:",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      cycleOptions.forEach { (value, label) ->
        FilterChip(
          selected = tasbih.targetCycle == value,
          onClick = { onSelectCycle(value) },
          label = { Text(label) },
        )
      }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val goldColor = MaterialTheme.colorScheme.tertiary

    Box(
      modifier = Modifier
        .size(226.dp)
        .clip(CircleShape)
        .clickable {
          triggerLightTasbihHaptic(context, haptic, strong = false)
          onIncrement {
            triggerLightTasbihHaptic(context, haptic, strong = true)
          }
        }
        .testTag("tasbih_tap_button"),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeW = 18f
        drawCircle(
          color = trackColor,
          style = Stroke(width = strokeW),
        )
        if (tasbih.targetCycle > 0) {
          drawArc(
            brush = Brush.sweepGradient(listOf(primaryColor, goldColor, primaryColor)),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        } else {
          drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            style = Stroke(width = strokeW),
          )
        }
      }

      GlassCard(
        isDark = isDark,
        highlighted = true,
        cornerRadius = 100.dp,
        modifier = Modifier.size(184.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "${tasbih.currentCount}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
          )
          if (tasbih.targetCycle > 0) {
            Text(
              text = "من ${tasbih.targetCycle}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.TouchApp,
              contentDescription = "اضغط للتسبيح",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
      ) {
        Text(
          text = "إجمالي التسبيحات: ${tasbih.totalLifetimeCount}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }

      IconButton(
        onClick = onReset,
        modifier = Modifier.testTag("tasbih_reset_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "تصفير العداد الحالي",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun AdhkarListView(
  category: DhikrCategory,
  isDark: Boolean,
  progressMap: Map<String, Int>,
  onDecrement: (String, Int) -> Unit,
  onResetCategory: (DhikrCategory) -> Unit,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val items = remember(category) { OfflineAdhkarData.forCategory(category) }
  val completedCount = items.count { item ->
    val remaining = progressMap[item.id] ?: item.targetCount
    remaining == 0
  }

  Column(
    modifier = Modifier
      .widthIn(max = 640.dp)
      .fillMaxSize(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "أُنجز $completedCount من ${items.size}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      TextButton(onClick = { onResetCategory(category) }) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("إعادة ضبط")
      }
    }

    LinearProgressIndicator(
      progress = { if (items.isEmpty()) 0f else completedCount.toFloat() / items.size.toFloat() },
      strokeCap = StrokeCap.Round,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(6.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(items, key = { it.id }) { item ->
        val remaining = progressMap[item.id] ?: item.targetCount
        DhikrGlassCard(
          item = item,
          isDark = isDark,
          remaining = remaining,
          onTap = {
            if (remaining > 0) {
              triggerLightTasbihHaptic(context, haptic, strong = remaining == 1)
              onDecrement(item.id, remaining)
            }
          },
        )
      }
    }
  }
}

@Composable
private fun DhikrGlassCard(
  item: DhikrItem,
  isDark: Boolean,
  remaining: Int,
  onTap: () -> Unit,
) {
  val isDone = remaining == 0

  GlassCard(
    isDark = isDark,
    highlighted = isDone,
    cornerRadius = 20.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(enabled = !isDone, onClick = onTap)
      .testTag("dhikr_card_${item.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Text(
        text = item.text,
        fontFamily = AmiriFontFamily,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = item.reference,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
          shape = CircleShape,
          color = if (isDone) SuccessCyanGreen else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (isDone) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "مكتمل",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Text(
                text = "$remaining",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

private fun triggerLightTasbihHaptic(
  context: Context,
  haptic: HapticFeedback,
  strong: Boolean,
) {
  val durationMs = if (strong) 45L else 18L
  val vibrated = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator?.vibrate(
        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE),
      )
      vm != null
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      @Suppress("DEPRECATION")
      val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      vib?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      vib != null
    } else {
      false
    }
  }.getOrDefault(false)

  if (!vibrated) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
  }
}
