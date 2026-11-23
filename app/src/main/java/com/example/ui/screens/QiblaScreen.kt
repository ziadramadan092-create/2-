package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.math.QiblaCalculator
import com.example.ui.MainUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.isAppInDarkTheme
import com.example.ui.theme.SkyGlow3
import com.example.ui.theme.SkyIce0
import com.example.ui.theme.SkyMist1
import com.example.ui.theme.SuccessCyanGreen
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
  uiState: MainUiState,
  onOpenCityPicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val isDark = isAppInDarkTheme(uiState.settings.themeMode())
  val sensorManager = remember {
    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }

  var hasSensor by remember { mutableStateOf(true) }
  var rawAzimuth by remember { mutableFloatStateOf(0f) }
  var continuousAzimuth by remember { mutableFloatStateOf(0f) }
  var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

  DisposableEffect(sensorManager) {
    val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    if (rotSensor == null && (accelSensor == null || magSensor == null)) {
      hasSensor = false
      onDispose {}
    } else {
      val accelReading = FloatArray(3)
      val magReading = FloatArray(3)
      var hasAccel = false
      var hasMag = false
      val rotationMatrix = FloatArray(9)
      val orientation = FloatArray(3)

      val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          var azimuthDeg: Float? = null
          if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
          } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelReading, 0, 3)
            hasAccel = true
          } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magReading, 0, 3)
            hasMag = true
          }

          if (azimuthDeg == null && hasAccel && hasMag) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magReading)) {
              SensorManager.getOrientation(rotationMatrix, orientation)
              azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }
          }

          azimuthDeg?.let { newAz ->
            var delta = newAz - rawAzimuth
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            val smoothed = (rawAzimuth + delta * 0.18f + 360f) % 360f
            var contDelta = smoothed - (continuousAzimuth % 360f + 360f) % 360f
            if (contDelta > 180f) contDelta -= 360f
            if (contDelta < -180f) contDelta += 360f
            rawAzimuth = smoothed
            continuousAzimuth += contDelta
          }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
          sensorAccuracy = accuracy
        }
      }

      if (rotSensor != null) {
        sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
      } else {
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
      }

      onDispose {
        sensorManager.unregisterListener(listener)
      }
    }
  }

  val animatedAzimuth by animateFloatAsState(
    targetValue = continuousAzimuth,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "compass_azimuth",
  )

  val qiblaBearing = uiState.qiblaBearingDeg.toFloat()
  val diff = remember(rawAzimuth, qiblaBearing) {
    var d = qiblaBearing - rawAzimuth
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    d
  }
  val isAligned = hasSensor && abs(diff) <= 3.5f

  var wasAligned by remember { mutableStateOf(false) }
  LaunchedEffect(isAligned) {
    if (isAligned && !wasAligned) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    wasAligned = isAligned
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 600.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "اتجاه القبلة",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      // Location Glass Header
      GlassCard(
        isDark = isDark,
        cornerRadius = 18.dp,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenCityPicker() },
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = uiState.settings.cityNameAr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "المسافة إلى مكة: ${uiState.distanceToMakkahKm.roundToInt()} كم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "${qiblaBearing.roundToInt()}° ${QiblaCalculator.cardinalDirectionArabic(uiState.qiblaBearingDeg)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }

      AnimatedVisibility(visible = isAligned) {
        Surface(
          color = SuccessCyanGreen.copy(alpha = 0.18f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("qibla_aligned_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = SuccessCyanGreen,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "تم تحديد الاتجاه بنجاح • أنت نحو القبلة",
              style = MaterialTheme.typography.titleMedium,
              color = SuccessCyanGreen,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (!hasSensor) {
        GlassCard(
          isDark = isDark,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "جهازك لا يحتوي على مستشعر بوصلة مغناطيسية. اتجاه القبلة من الشمال الحقيقي هو ${qiblaBearing.roundToInt()}° (${QiblaCalculator.cardinalDirectionArabic(uiState.qiblaBearingDeg)}).",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      // Royal-Blue & Sky-Blue Glassmorphic Compass Dial
      val textMeasurer = rememberTextMeasurer()
      val primaryColor = MaterialTheme.colorScheme.primary
      val goldColor = MaterialTheme.colorScheme.tertiary
      val dialBaseColor = if (isDark) Color(0xFF102847) else SkyIce0.copy(alpha = 0.94f)
      val ringTint = if (isDark) Color(0xFF1E4878) else SkyMist1
      val onSurfaceColor = MaterialTheme.colorScheme.onSurface

      Box(
        modifier = Modifier
          .fillMaxWidth(0.84f)
          .aspectRatio(1f)
          .testTag("qibla_compass_dial"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val radius = size.minDimension / 2f * 0.88f

          if (isAligned) {
            drawCircle(
              color = SuccessCyanGreen.copy(alpha = 0.22f),
              radius = radius * 1.12f,
              center = center,
            )
          }

          // Outer Blue Glass Ring
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(dialBaseColor, ringTint),
              center = center,
              radius = radius,
            ),
            radius = radius,
            center = center,
          )
          drawCircle(
            color = if (isAligned) SuccessCyanGreen else SkyGlow3.copy(alpha = 0.65f),
            radius = radius,
            center = center,
            style = Stroke(width = if (isAligned) 7f else 4f),
          )
          drawCircle(
            color = dialBaseColor,
            radius = radius * 0.78f,
            center = center,
          )

          // Top Phone Heading Pointer
          val topMarker = Path().apply {
            moveTo(center.x, center.y - radius - 18f)
            lineTo(center.x - 14f, center.y - radius + 6f)
            lineTo(center.x + 14f, center.y - radius + 6f)
            close()
          }
          drawPath(
            path = topMarker,
            color = if (isAligned) SuccessCyanGreen else primaryColor,
          )

          rotate(degrees = -animatedAzimuth, pivot = center) {
            for (deg in 0 until 360 step 15) {
              val rad = Math.toRadians(deg.toDouble() - 90.0)
              val isMajor = deg % 90 == 0
              val innerR = radius * if (isMajor) 0.80f else 0.87f
              val outerR = radius * 0.94f
              drawLine(
                color = if (deg == 0) primaryColor else onSurfaceColor.copy(alpha = 0.4f),
                start = Offset(
                  center.x + (innerR * cos(rad)).toFloat(),
                  center.y + (innerR * sin(rad)).toFloat(),
                ),
                end = Offset(
                  center.x + (outerR * cos(rad)).toFloat(),
                  center.y + (outerR * sin(rad)).toFloat(),
                ),
                strokeWidth = if (isMajor) 4f else 2f,
                cap = StrokeCap.Round,
              )
            }

            drawCardinalLabel(textMeasurer, "ش", 0.0, center, radius * 0.65f, primaryColor)
            drawCardinalLabel(textMeasurer, "ق", 90.0, center, radius * 0.65f, onSurfaceColor)
            drawCardinalLabel(textMeasurer, "ج", 180.0, center, radius * 0.65f, onSurfaceColor)
            drawCardinalLabel(textMeasurer, "غ", 270.0, center, radius * 0.65f, onSurfaceColor)

            // Qibla Needle + Golden Kaaba Emblem
            rotate(degrees = qiblaBearing, pivot = center) {
              val needleColor = if (isAligned) SuccessCyanGreen else goldColor
              val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.76f)
                lineTo(center.x - 18f, center.y + 18f)
                lineTo(center.x, center.y)
                lineTo(center.x + 18f, center.y + 18f)
                close()
              }
              drawPath(path = arrowPath, color = needleColor)

              val kaabaSize = 36f
              val kaabaTopLeft = Offset(
                center.x - kaabaSize / 2f,
                center.y - radius * 0.90f - kaabaSize / 2f,
              )
              drawRect(
                color = Color(0xFF1F231E),
                topLeft = kaabaTopLeft,
                size = Size(kaabaSize, kaabaSize),
              )
              drawRect(
                color = Color(0xFFE8C164),
                topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + 8f),
                size = Size(kaabaSize, 6f),
              )
            }
          }

          drawCircle(
            color = primaryColor,
            radius = 12f,
            center = center,
          )
        }
      }

      // Large Crisp Qibla Angle Display
      Text(
        text = "${qiblaBearing.roundToInt()}° قِبْلَة",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      // Guidance Glass Card
      GlassCard(
        isDark = isDark,
        cornerRadius = 18.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            val guidance = when {
              !hasSensor -> "وجّه هاتفك نحو ${QiblaCalculator.cardinalDirectionArabic(uiState.qiblaBearingDeg)} (${qiblaBearing.roundToInt()}°)"
              isAligned -> "تم تحديد الاتجاه بنجاح • الهاتف يواجه الكعبة المشرفة"
              diff > 0 -> "أدر الهاتف لليمين بمقدار ${abs(diff).roundToInt()}°"
              else -> "أدر الهاتف لليسار بمقدار ${abs(diff).roundToInt()}°"
            }
            Text(
              text = guidance,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            val calHint = if (sensorAccuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
              "دقة البوصلة منخفضة: حرّك الهاتف على شكل رقم 8 بالهواء للمعايرة"
            } else {
              "ضع الهاتف بشكل أفقي مسطح بعيداً عن المعادن"
            }
            Text(
              text = calHint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardinalLabel(
  textMeasurer: TextMeasurer,
  label: String,
  angleDeg: Double,
  center: Offset,
  radius: Float,
  color: Color,
) {
  val rad = Math.toRadians(angleDeg - 90.0)
  val layoutResult = textMeasurer.measure(
    text = label,
    style = TextStyle(
      color = color,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    ),
  )
  val x = center.x + (radius * cos(rad)).toFloat() - layoutResult.size.width / 2f
  val y = center.y + (radius * sin(rad)).toFloat() - layoutResult.size.height / 2f
  drawText(layoutResult, topLeft = Offset(x, y))
}
