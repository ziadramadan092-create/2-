package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.NotificationMode
import com.example.domain.model.PrayerTimeItem
import com.example.domain.model.PrayerType
import com.example.ui.MainUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCircleIconButton
import com.example.ui.components.drawIslamicMandalaRosette
import com.example.ui.components.isAppInDarkTheme
import com.example.ui.theme.Cerulean4
import com.example.ui.theme.Cobalt6
import com.example.ui.theme.RoyalAzure7
import com.example.ui.theme.SkyGlow3
import com.example.ui.theme.WarmGold

@Composable
fun HomeScreen(
  uiState: MainUiState,
  onOpenCityPicker: () -> Unit,
  onCyclePrayerMode: (PrayerType) -> Unit,
  onToggleThemeQuick: (Boolean) -> Unit,
  onOpenSettingsTab: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isAppInDarkTheme(uiState.settings.themeMode())

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 1. Top Header: City, Hijri/Gregorian Dates, Quick Theme & Notification Glass Buttons
    item {
      Column(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onOpenCityPicker() }
              .padding(vertical = 4.dp)
              .testTag("city_picker_button"),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (uiState.settings.isGpsLocation) {
                  Icons.Filled.MyLocation
                } else {
                  Icons.Filled.LocationOn
                },
                contentDescription = "تغيير المدينة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${uiState.settings.cityNameAr}، ${uiState.settings.countryAr}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${uiState.hijriDateFormatted} | ${uiState.gregorianDateFormatted}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            GlassCircleIconButton(
              icon = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
              contentDescription = "تبديل الوضع الليلي والنهاري",
              onClick = { onToggleThemeQuick(isDark) },
              isDark = isDark,
              tint = if (isDark) WarmGold else RoyalAzure7,
              modifier = Modifier.testTag("quick_theme_toggle"),
            )
            GlassCircleIconButton(
              icon = Icons.Filled.Notifications,
              contentDescription = "إعدادات التنبيهات",
              onClick = onOpenSettingsTab,
              isDark = isDark,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("quick_notif_settings"),
            )
          }
        }
      }
    }

    // 2. Frosted Blue Glass Hero Card with Circular Sky-Blue & Gold Countdown Ring
    item {
      Box(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
      ) {
        CountdownHeroGlassCard(
          uiState = uiState,
          isDark = isDark,
        )
      }
    }

    // 3. Prayer Times Header Strip
    item {
      Row(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "مواقيت الصلاة اليوم",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "${uiState.currentTimeFormatted} ${uiState.currentSecondsFormatted}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // 4. Five Obligatory Prayer Glass Cards + Sunrise Pill
    val obligatory = uiState.prayerItems.filter { it.type.isObligatory }
    val sunriseItem = uiState.prayerItems.firstOrNull { it.type == PrayerType.SUNRISE }

    items(obligatory, key = { it.type.key }) { item ->
      Box(modifier = Modifier.widthIn(max = 640.dp)) {
        PrayerTimeGlassCard(
          item = item,
          isDark = isDark,
          onCycleMode = { onCyclePrayerMode(item.type) },
        )
      }
      if (item.type == PrayerType.FAJR && sunriseItem != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.widthIn(max = 640.dp)) {
          SunriseGlassPill(sunriseItem = sunriseItem, isDark = isDark)
        }
      }
    }

    // 5. Developer Credit Footer
    item {
      Text(
        text = "تطوير: Eng_Ziad Hanzal",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, bottom = 14.dp)
          .testTag("developer_credit_footer"),
      )
    }
  }
}

@Composable
private fun CountdownHeroGlassCard(
  uiState: MainUiState,
  isDark: Boolean,
) {
  val primaryArc = if (isDark) SkyGlow3 else RoyalAzure7
  val midArc = if (isDark) Cerulean4 else Cobalt6
  val goldColor = MaterialTheme.colorScheme.tertiary
  val trackColor = if (isDark) {
    SkyGlow3.copy(alpha = 0.16f)
  } else {
    RoyalAzure7.copy(alpha = 0.14f)
  }
  val innerMandalaColor = if (isDark) {
    SkyGlow3.copy(alpha = 0.10f)
  } else {
    Cobalt6.copy(alpha = 0.08f)
  }

  GlassCard(
    isDark = isDark,
    cornerRadius = 28.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 18.dp, horizontal = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      // Subtle corner mandala inside hero card matching mockup
      Canvas(modifier = Modifier.matchParentSize()) {
        drawIslamicMandalaRosette(
          center = Offset(size.width * 0.14f, size.height * 0.78f),
          radius = size.minDimension * 0.42f,
          color = innerMandalaColor,
        )
        drawIslamicMandalaRosette(
          center = Offset(size.width * 0.86f, size.height * 0.22f),
          radius = size.minDimension * 0.36f,
          color = innerMandalaColor,
        )
      }

      Box(
        modifier = Modifier.size(236.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 22f
          // Inner soft ring disc
          drawCircle(
            color = if (isDark) {
              Color(0xFF0E2542).copy(alpha = 0.55f)
            } else {
              Color.White.copy(alpha = 0.55f)
            },
            radius = size.minDimension / 2f - strokeWidth / 2f,
          )
          drawCircle(
            color = trackColor,
            style = Stroke(width = strokeWidth),
          )
          drawArc(
            brush = Brush.sweepGradient(
              colors = listOf(
                goldColor,
                primaryArc,
                midArc,
                goldColor,
              ),
            ),
            startAngle = -90f,
            sweepAngle = (360f * uiState.prayerCycleProgress).coerceAtLeast(22f),
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(20.dp),
        ) {
          Text(
            text = "الأذان القادم: ${uiState.nextPrayer.arabicName}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = uiState.countdownFormatted,
            style = MaterialTheme.typography.displaySmall,
            fontSize = 33.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp,
            modifier = Modifier.testTag("live_countdown_text"),
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = uiState.countdownArabicSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(50),
            color = if (isDark) {
              Color(0xFF1F4773).copy(alpha = 0.85f)
            } else {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            },
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              goldColor.copy(alpha = 0.55f),
            ),
          ) {
            Text(
              text = uiState.nextPrayerTimeFormatted,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = if (isDark) WarmGold else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SunriseGlassPill(
  sunriseItem: PrayerTimeItem,
  isDark: Boolean,
) {
  GlassCard(
    isDark = isDark,
    cornerRadius = 16.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.WbTwilight,
          contentDescription = "الشروق",
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "وقت الشروق",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
        )
      }
      Text(
        text = sunriseItem.formattedTime,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun PrayerTimeGlassCard(
  item: PrayerTimeItem,
  isDark: Boolean,
  onCycleMode: () -> Unit,
) {
  val iconTint = when (item.type) {
    PrayerType.FAJR, PrayerType.ISHA -> if (isDark) SkyGlow3 else RoyalAzure7
    else -> MaterialTheme.colorScheme.tertiary
  }

  GlassCard(
    isDark = isDark,
    highlighted = item.isNext,
    cornerRadius = 18.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("prayer_card_${item.type.key}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Surface(
          shape = CircleShape,
          color = if (item.isNext) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
          } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
          },
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = prayerIcon(item.type),
              contentDescription = item.type.arabicName,
              tint = iconTint,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.type.arabicName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = if (item.isNext) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else if (item.isPassed) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            )
            if (item.isNext) {
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = "الصلاة القادمة",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onTertiary,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
              }
            }
            if (item.offsetMinutes != 0) {
              Spacer(modifier = Modifier.width(6.dp))
              val sign = if (item.offsetMinutes > 0) "+" else ""
              Text(
                text = "($sign${item.offsetMinutes} د)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Text(
            text = item.notificationMode.arabicName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = item.formattedTime,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = if (item.isNext) {
            if (isDark) Color.White else MaterialTheme.colorScheme.primary
          } else if (item.isPassed) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
          } else {
            MaterialTheme.colorScheme.onSurface
          },
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
          onClick = onCycleMode,
          modifier = Modifier.testTag("cycle_mode_${item.type.key}"),
        ) {
          Icon(
            imageVector = modeIcon(item.notificationMode),
            contentDescription = "تغيير تنبيه ${item.type.arabicName}: ${item.notificationMode.arabicName}",
            tint = if (item.notificationMode == NotificationMode.SILENT) {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            } else {
              MaterialTheme.colorScheme.primary
            },
          )
        }
      }
    }
  }
}

private fun prayerIcon(type: PrayerType): ImageVector = when (type) {
  PrayerType.FAJR -> Icons.Filled.NightsStay
  PrayerType.SUNRISE -> Icons.Filled.WbTwilight
  PrayerType.DHUHR -> Icons.Filled.WbSunny
  PrayerType.ASR -> Icons.Filled.WbSunny
  PrayerType.MAGHRIB -> Icons.Filled.WbTwilight
  PrayerType.ISHA -> Icons.Filled.NightsStay
}

private fun modeIcon(mode: NotificationMode): ImageVector = when (mode) {
  NotificationMode.FULL_ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
  NotificationMode.TAKBIR -> Icons.Filled.NotificationsActive
  NotificationMode.VIBRATE -> Icons.Filled.Vibration
  NotificationMode.SILENT -> Icons.Filled.NotificationsOff
}
