package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode
import com.example.ui.theme.Cerulean4
import com.example.ui.theme.Cobalt6
import com.example.ui.theme.DeepSapphire8
import com.example.ui.theme.MidnightNavy9
import com.example.ui.theme.NavyBgBottom
import com.example.ui.theme.NavyBgMid
import com.example.ui.theme.NavyBgTop
import com.example.ui.theme.RoyalAzure7
import com.example.ui.theme.SkyGlow3
import com.example.ui.theme.SkyIce0
import com.example.ui.theme.SkyLight2
import com.example.ui.theme.SkyMist1
import com.example.ui.theme.WarmGold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun isAppInDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
  ThemeMode.AUTO -> isSystemInDarkTheme()
  ThemeMode.LIGHT -> false
  ThemeMode.DARK -> true
}

@Composable
fun IslamicGeometricBackdrop(
  themeMode: ThemeMode,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val isDark = isAppInDarkTheme(themeMode)
  val bgColors = if (isDark) {
    listOf(NavyBgTop, NavyBgMid, NavyBgBottom)
  } else {
    listOf(SkyIce0, SkyMist1, Color(0xFFDDF1FC))
  }
  val mandalaColor = if (isDark) {
    SkyGlow3.copy(alpha = 0.14f)
  } else {
    Cobalt6.copy(alpha = 0.13f)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(bgColors)),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Top-right intricate Islamic mandala rosette
      drawIslamicMandalaRosette(
        center = Offset(size.width * 0.90f, size.height * 0.08f),
        radius = size.minDimension * 0.36f,
        color = mandalaColor,
      )
      // Bottom-left intricate Islamic mandala rosette
      drawIslamicMandalaRosette(
        center = Offset(size.width * 0.10f, size.height * 0.89f),
        radius = size.minDimension * 0.32f,
        color = mandalaColor,
      )
    }
    content()
  }
}

fun DrawScope.drawIslamicMandalaRosette(
  center: Offset,
  radius: Float,
  color: Color,
) {
  val stroke = Stroke(width = 2.0f)
  // Concentric rings
  for (rFactor in listOf(1.0f, 0.78f, 0.56f, 0.34f, 0.16f)) {
    drawCircle(color = color, radius = radius * rFactor, center = center, style = stroke)
  }

  // 12-fold interlocking pointed petals & geometric star lines
  val petals = 12
  for (i in 0 until petals) {
    val angleDeg = i * (360.0 / petals)
    val rad = Math.toRadians(angleDeg)
    val nextRad = Math.toRadians(angleDeg + 360.0 / petals)
    val midRad = Math.toRadians(angleDeg + 180.0 / petals)

    val outerPt = Offset(
      center.x + (radius * cos(rad)).toFloat(),
      center.y + (radius * sin(rad)).toFloat(),
    )
    val midInner = Offset(
      center.x + (radius * 0.62f * cos(midRad)).toFloat(),
      center.y + (radius * 0.62f * sin(midRad)).toFloat(),
    )
    val nextOuter = Offset(
      center.x + (radius * cos(nextRad)).toFloat(),
      center.y + (radius * sin(nextRad)).toFloat(),
    )
    val innerBase = Offset(
      center.x + (radius * 0.34f * cos(rad)).toFloat(),
      center.y + (radius * 0.34f * sin(rad)).toFloat(),
    )

    val petalPath = Path().apply {
      moveTo(innerBase.x, innerBase.y)
      quadraticTo(
        center.x + (radius * 0.78f * cos(rad - 0.18)).toFloat(),
        center.y + (radius * 0.78f * sin(rad - 0.18)).toFloat(),
        outerPt.x,
        outerPt.y,
      )
      quadraticTo(
        center.x + (radius * 0.78f * cos(rad + 0.18)).toFloat(),
        center.y + (radius * 0.78f * sin(rad + 0.18)).toFloat(),
        innerBase.x,
        innerBase.y,
      )
    }
    drawPath(petalPath, color = color, style = stroke)

    drawLine(color = color, start = outerPt, end = midInner, strokeWidth = 1.6f)
    drawLine(color = color, start = midInner, end = nextOuter, strokeWidth = 1.6f)
  }
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 22.dp,
  highlighted: Boolean = false,
  isDark: Boolean = isSystemInDarkTheme(),
  content: @Composable ColumnScope.() -> Unit,
) {
  val fillBrush = when {
    highlighted && isDark -> Brush.linearGradient(
      colors = listOf(
        RoyalAzure7.copy(alpha = 0.56f),
        DeepSapphire8.copy(alpha = 0.62f),
      ),
    )
    highlighted && !isDark -> Brush.linearGradient(
      colors = listOf(
        SkyLight2.copy(alpha = 0.78f),
        SkyMist1.copy(alpha = 0.85f),
      ),
    )
    isDark -> Brush.linearGradient(
      colors = listOf(
        Color(0xFF1A3F6A).copy(alpha = 0.64f),
        Color(0xFF112949).copy(alpha = 0.72f),
      ),
    )
    else -> Brush.linearGradient(
      colors = listOf(
        Color.White.copy(alpha = 0.80f),
        SkyIce0.copy(alpha = 0.68f),
      ),
    )
  }

  val borderBrush = when {
    highlighted -> Brush.linearGradient(
      colors = listOf(
        WarmGold.copy(alpha = 0.90f),
        SkyGlow3.copy(alpha = 0.75f),
      ),
    )
    isDark -> Brush.linearGradient(
      colors = listOf(
        SkyGlow3.copy(alpha = 0.34f),
        MidnightNavy9.copy(alpha = 0.55f),
      ),
    )
    else -> Brush.linearGradient(
      colors = listOf(
        Color.White.copy(alpha = 0.95f),
        Cerulean4.copy(alpha = 0.35f),
      ),
    )
  }

  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color.Transparent,
    border = BorderStroke(if (highlighted) 1.6.dp else 1.dp, borderBrush),
    shadowElevation = if (isDark) 0.dp else if (highlighted) 4.dp else 2.dp,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .background(fillBrush)
        .fillMaxWidth(),
      content = content,
    )
  }
}

@Composable
fun GlassCircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDark: Boolean = isSystemInDarkTheme(),
  tint: Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    shape = CircleShape,
    color = if (isDark) {
      Color(0xFF1B3F6B).copy(alpha = 0.75f)
    } else {
      Color.White.copy(alpha = 0.82f)
    },
    border = BorderStroke(
      1.dp,
      if (isDark) SkyGlow3.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.95f),
    ),
    shadowElevation = if (isDark) 0.dp else 2.dp,
    modifier = modifier
      .size(44.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
fun AudioPreviewWaveformCard(
  voiceTitle: String,
  voiceSubtitle: String,
  isPlaying: Boolean,
  isDark: Boolean,
  onTogglePlay: () -> Unit,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    isDark = isDark,
    highlighted = isPlaying,
    cornerRadius = 20.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "صوت الأذان: $voiceTitle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = voiceSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        val bars = listOf(0.35f, 0.7f, 0.45f, 0.9f, 0.55f, 0.8f, 0.4f, 0.65f)
        val primaryColor = MaterialTheme.colorScheme.primary
        val mutedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

        Canvas(
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
        ) {
          val barWidth = 6f
          val gap = (size.width - bars.size * barWidth) / (bars.size + 1)
          bars.forEachIndexed { idx, ratio ->
            val h = size.height * ratio
            val x = gap + idx * (barWidth + gap)
            drawRoundRect(
              color = if (isPlaying || idx < 4) primaryColor else mutedColor,
              topLeft = Offset(x, (size.height - h) / 2f),
              size = Size(barWidth, h),
              cornerRadius = CornerRadius(4f, 4f),
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          shadowElevation = 4.dp,
          modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable(onClick = onTogglePlay),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (isPlaying) "إيقاف التجربة" else "تشغيل تجربة الأذان",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Canvas(
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
        ) {
          val barWidth = 6f
          val gap = (size.width - bars.size * barWidth) / (bars.size + 1)
          bars.reversed().forEachIndexed { idx, ratio ->
            val h = size.height * ratio
            val x = gap + idx * (barWidth + gap)
            drawRoundRect(
              color = if (isPlaying) primaryColor else mutedColor,
              topLeft = Offset(x, (size.height - h) / 2f),
              size = Size(barWidth, h),
              cornerRadius = CornerRadius(4f, 4f),
            )
          }
        }
      }
    }
  }
}

@Composable
fun DeveloperCreditCard(
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    isDark = isDark,
    highlighted = true,
    cornerRadius = 20.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Code,
              contentDescription = "المطور",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "تطوير وبرمجة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Eng_Ziad Hanzal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        Text(
          text = "Offline First",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}
