package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AdhanAudioController
import com.example.ui.MainViewModel
import com.example.ui.components.CityPickerModal
import com.example.ui.components.IslamicGeometricBackdrop
import com.example.ui.components.StopAdhanFloatingBanner
import com.example.ui.components.isAppInDarkTheme
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AthanTheme

enum class MainTab(
  val labelAr: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
) {
  HOME("الرئيسية", Icons.Filled.AccessTimeFilled, Icons.Outlined.AccessTime, "nav_home"),
  QIBLA("القبلة", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_qibla"),
  ADHKAR("الأذكار", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_adhkar"),
  SETTINGS("الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    volumeControlStream = AudioManager.STREAM_MUSIC
    enableEdgeToEdge()
    setContent {
      val viewModel: MainViewModel = viewModel()
      AthanAppRoot(viewModel = viewModel)
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && AdhanAudioController.state.value.isPlaying) {
      AdhanAudioController.stop(this)
      return true
    }
    return super.onKeyDown(keyCode, event)
  }
}

@Composable
fun AthanAppRoot(viewModel: MainViewModel) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
  var showCityPicker by rememberSaveable { mutableStateOf(false) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { grants ->
    val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      viewModel.detectLocationViaGps(context)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { _ -> }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val requestGpsDetection = {
    val fineGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    if (fineGranted || coarseGranted) {
      viewModel.detectLocationViaGps(context)
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
        ),
      )
    }
  }

  if (currentTab != MainTab.HOME) {
    BackHandler { currentTab = MainTab.HOME }
  }

  val isDark = isAppInDarkTheme(uiState.settings.themeMode())
  val navGlassColor = if (isDark) {
    Color(0xEB0B182C)
  } else {
    Color(0xEBEAF7FF)
  }

  AthanTheme(themeMode = uiState.settings.themeMode()) {
    IslamicGeometricBackdrop(themeMode = uiState.settings.themeMode()) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          bottomBar = {
            if (!useNavigationRail) {
              NavigationBar(
                containerColor = navGlassColor,
                tonalElevation = 0.dp,
              ) {
                MainTab.entries.forEach { tab ->
                  val selected = currentTab == tab
                  NavigationBarItem(
                    selected = selected,
                    onClick = { currentTab = tab },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.labelAr,
                      )
                    },
                    label = { Text(tab.labelAr) },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.testTag(tab.tag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useNavigationRail) {
              NavigationRail(
                containerColor = navGlassColor,
                modifier = Modifier.fillMaxHeight(),
              ) {
                MainTab.entries.forEach { tab ->
                  val selected = currentTab == tab
                  NavigationRailItem(
                    selected = selected,
                    onClick = { currentTab = tab },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.labelAr,
                      )
                    },
                    label = { Text(tab.labelAr) },
                    colors = NavigationRailItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.testTag(tab.tag),
                  )
                }
              }
            }

            Column(modifier = Modifier.fillMaxSize()) {
              StopAdhanFloatingBanner(
                playback = uiState.adhanPlayback,
                onStopClick = { viewModel.stopAdhan(context) },
              )

              Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                  MainTab.HOME -> HomeScreen(
                    uiState = uiState,
                    onOpenCityPicker = { showCityPicker = true },
                    onCyclePrayerMode = { viewModel.cyclePrayerMode(it) },
                    onToggleThemeQuick = { viewModel.toggleThemeQuick(it) },
                    onOpenSettingsTab = { currentTab = MainTab.SETTINGS },
                    onRequestNotificationPermission = {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      }
                    },
                  )
                  MainTab.QIBLA -> QiblaScreen(
                    uiState = uiState,
                    onOpenCityPicker = { showCityPicker = true },
                  )
                  MainTab.ADHKAR -> AdhkarScreen(
                    uiState = uiState,
                    onIncrementTasbih = { onDone -> viewModel.incrementTasbih(onDone) },
                    onResetTasbih = { viewModel.resetTasbihCurrent() },
                    onSelectTasbihPhrase = { viewModel.selectTasbihPhrase(it) },
                    onSelectTasbihCycle = { viewModel.selectTasbihCycle(it) },
                    onDecrementDhikr = { id, rem -> viewModel.decrementDhikr(id, rem) },
                    onResetCategory = { viewModel.resetAdhkarCategory(it) },
                  )
                  MainTab.SETTINGS -> SettingsScreen(
                    uiState = uiState,
                    onOpenCityPicker = { showCityPicker = true },
                    onSelectMethod = { viewModel.setCalculationMethod(it) },
                    onSelectMadhhab = { viewModel.setMadhhab(it) },
                    onAdjustHijriDays = { viewModel.adjustHijriDays(it) },
                    onAdjustPrayerOffset = { prayer, delta -> viewModel.adjustPrayerOffset(prayer, delta) },
                    onSetPrayerMode = { prayer, mode -> viewModel.setPrayerMode(prayer, mode) },
                    onSelectMuezzinVoice = { viewModel.setMuezzinVoice(it) },
                    onCustomAudioSelected = { viewModel.setCustomAudioUri(context, it) },
                    onPreviewAdhan = { viewModel.previewAdhan(context) },
                    onTestAdhanService = { viewModel.testAdhanAlarmService(context) },
                    onStopAdhan = { viewModel.stopAdhan(context) },
                    onTogglePreAdhanReminder = { viewModel.setPreAdhanReminder(it) },
                    onSelectThemeMode = { viewModel.setThemeMode(it) },
                    onToggle24Hour = { viewModel.setUse24HourFormat(it) },
                    onExportBackup = { viewModel.exportBackupToUri(context, it) },
                    onImportBackup = { viewModel.importBackupFromUri(context, it) },
                    onRequestNotificationPermission = {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      }
                    },
                  )
                }
              }
            }
          }
        }

        if (showCityPicker) {
          CityPickerModal(
            currentCityId = uiState.settings.cityId,
            isDetectingGps = uiState.isDetectingGps,
            onSelectCity = { viewModel.selectCity(it) },
            onDetectGps = requestGpsDetection,
            onDismiss = { showCityPicker = false },
          )
        }
      }
    }
  }
}
