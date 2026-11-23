package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OfflineCitiesData
import com.example.domain.model.CityInfo
import com.example.service.AdhanPlaybackState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer

@Composable
fun StopAdhanFloatingBanner(
  playback: AdhanPlaybackState,
  onStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = playback.isPlaying,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
    modifier = modifier,
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = AlertRedContainer,
      tonalElevation = 6.dp,
      shadowElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = AlertRed,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = if (playback.isPreview) "تجربة صوت الأذان" else "حان موعد صلاة ${playback.prayerNameAr}",
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF5A1111),
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${playback.modeLabel} • اضغط زر خفض الصوت للكتم",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF7B2626),
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onStopClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = AlertRed,
            contentColor = Color.White,
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier.testTag("stop_adhan_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.StopCircle,
            contentDescription = "إيقاف الأذان",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "إيقاف",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerModal(
  currentCityId: String,
  isDetectingGps: Boolean,
  onSelectCity: (CityInfo) -> Unit,
  onDetectGps: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }
  val filteredCities = remember(query) { OfflineCitiesData.search(query) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
    ) {
      Text(
        text = "اختيار المدينة والموقع",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(12.dp))

      FilledTonalButton(
        onClick = {
          onDetectGps()
        },
        enabled = !isDetectingGps,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("gps_detect_button"),
      ) {
        if (isDetectingGps) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("جاري تحديد الموقع عبر GPS...")
        } else {
          Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = "تحديد الموقع تلقائياً",
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("تحديد موقعي تلقائياً عبر GPS")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("ابحث عن المدينة أو الدولة (مثال: القاهرة، مكة، Cairo)...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("city_search_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp),
      ) {
        items(filteredCities, key = { it.id }) { city ->
          val selected = city.id == currentCityId
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelectCity(city)
                onDismiss()
              }
              .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (selected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = city.nameAr,
                  style = MaterialTheme.typography.titleMedium,
                  color = if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  },
                )
                Text(
                  text = "${city.countryAr} • ${city.nameEn}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (selected) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "المحددة",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
      }
    }
  }
}
