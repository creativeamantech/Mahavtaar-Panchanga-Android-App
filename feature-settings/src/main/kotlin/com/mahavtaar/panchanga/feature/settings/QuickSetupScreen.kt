package com.mahavtaar.panchanga.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.PermissionRationaleDialog

/**
 * 3-Step First-Launch Quick Setup Wizard (Language -> City/Location -> Alert Preset).
 * Shown automatically on fresh installs and replayable anytime from Settings > Advanced.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSetupScreen(
    onFinished: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val isHi = prefs.language == AppLanguage.HINDI
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(0) }
    var selectedPreset by remember { mutableStateOf(NotificationPreset.ESSENTIAL) }
    var showGpsRationale by remember { mutableStateOf(false) }
    var showNotifyRationale by remember { mutableStateOf(false) }

    BackHandler(enabled = step > 0) {
        step = (step - 1).coerceAtLeast(0)
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectCurrentGpsLocation()
        }
    }

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.completeQuickSetup(selectedPreset)
        onFinished()
    }

    fun finishSetup() {
        val needsNotifyPermission = selectedPreset != NotificationPreset.SILENT &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsNotifyPermission) {
            showNotifyRationale = true
        } else {
            viewModel.completeQuickSetup(selectedPreset)
            onFinished()
        }
    }

    if (showGpsRationale) {
        PermissionRationaleDialog(
            title = if (isHi) "लोकेशन अनुमति" else "Use Your Location",
            reason = if (isHi) {
                "आपके सटीक सूर्योदय, होरा और तत्व समय की गणना के लिए लोकेशन आवश्यक है।"
            } else {
                "Location calculates exact Sunrise, Hora, and Tattva times for your city."
            },
            withoutPermissionNote = if (isHi) {
                "आप बिना जीपीएस के नीचे से शहर चुन सकते हैं।"
            } else {
                "You can also choose any city manually without granting GPS."
            },
            allowLabel = if (isHi) "अनुमति दें" else "Allow GPS",
            notNowLabel = if (isHi) "अभी नहीं" else "Not Now",
            onAllow = {
                showGpsRationale = false
                gpsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showGpsRationale = false }
        )
    }

    if (showNotifyRationale) {
        PermissionRationaleDialog(
            title = if (isHi) "अलर्ट सूचना अनुमति" else "Allow Vedic Alerts",
            reason = if (isHi) {
                "आपके चुने हुए सूर्योदय, एकादशी और साधना अलर्ट समय पर भेजने के लिए सूचना अनुमति चाहिए।"
            } else {
                "Allow notifications so your chosen Sunrise, Vrat, and Vedic alerts arrive right on time."
            },
            withoutPermissionNote = if (isHi) {
                "आप इसे अभी छोड़ सकते हैं और बाद में सेटिंग्स में चालू कर सकते हैं।"
            } else {
                "You can skip this for now and enable alerts anytime from Settings."
            },
            allowLabel = if (isHi) "अनुमति दें" else "Allow",
            notNowLabel = if (isHi) "छोड़ें" else "Skip",
            onAllow = {
                showNotifyRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.completeQuickSetup(selectedPreset)
                    onFinished()
                }
            },
            onDismiss = {
                showNotifyRationale = false
                viewModel.completeQuickSetup(selectedPreset)
                onFinished()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Brand + Step indicator + Skip
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MahavtaarBrandLogo(size = 38.dp)
                        Column {
                            Text(
                                text = if (isHi) "त्वरित सेटअप" else "Quick Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHi) "चरण ${step + 1} / 3" else "Step ${step + 1} of 3",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.completeQuickSetup(null)
                            onFinished()
                        },
                        modifier = Modifier.testTag("quick_setup_skip_btn")
                    ) {
                        Text(if (isHi) "छोड़ें" else "Skip")
                    }
                }
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
            }

            // Middle content (scrollable for large fonts)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        Text(
                            text = if (isHi) "अपनी भाषा चुनें" else "Choose Your Language",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) {
                                "आप ऐप में कभी भी शीर्ष पट्टी से भाषा बदल सकते हैं।"
                            } else {
                                "You can switch between English and Hindi anytime from the top bar."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppLanguage.entries.forEach { option ->
                            val selected = prefs.language == option
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable { viewModel.setLanguage(option) }
                                    .testTag("quick_setup_lang_${option.code}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.nativeLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (option == AppLanguage.HINDI) {
                                                "पूर्ण हिंदी पंचांग, तिथि, नक्षत्र और हिंदी स्वर सूचनाएँ"
                                            } else {
                                                "Full English Panchanga, Vedic calendar, and English voice alerts"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = if (isHi) "अपना शहर चुनें" else "Select Your City",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) {
                                "सटीक सूर्योदय, सूर्यास्त, होरा, तत्व और स्वर समय आपके शहर पर निर्भर करते हैं।"
                            } else {
                                "Exact local Sunrise, Sunset, Hora, Tattva, and Swara timings depend on your city."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PanchangaCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHi) "वर्तमान चयनित स्थान" else "Selected Location",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${prefs.selectedLocation.name} (${prefs.selectedLocation.timezone})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) {
                                    viewModel.detectCurrentGpsLocation()
                                } else {
                                    showGpsRationale = true
                                }
                            },
                            enabled = !state.isDetectingGps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("quick_setup_gps_btn")
                        ) {
                            if (state.isDetectingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHi) "वर्तमान जीपीएस लोकेशन लें" else "Detect Current Location (GPS)")
                        }

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            label = { Text(if (isHi) "शहर खोजें..." else "Search city...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_setup_city_search")
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.searchResults.take(10).forEach { city ->
                                val isCurrent = prefs.selectedLocation.name == city.name
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = { viewModel.selectLocation(city) },
                                    label = {
                                        Text(
                                            text = city.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("quick_setup_city_${city.name}")
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = if (isHi) "अलर्ट प्रीसेट चुनें" else "Choose Your Alert Style",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) {
                                "तुरंत अपनी पसंद का स्तर चुनें — आप बाद में नोटिफिकेशन स्टूडियो में किसी भी नियम को बदल सकते हैं।"
                            } else {
                                "Pick how often you want to be notified — you can fine-tune every rule later in Notification Studio."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NotificationPreset.entries.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable { selectedPreset = preset }
                                    .testTag("quick_setup_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHi) preset.titleHi else preset.titleEn,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isHi) preset.subtitleHi else preset.subtitleEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("quick_setup_back_btn")
                    ) {
                        Text(if (isHi) "पीछे" else "Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 2) {
                            step += 1
                        } else {
                            finishSetup()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag(if (step < 2) "quick_setup_next_btn" else "quick_setup_finish_btn")
                ) {
                    Text(
                        if (step < 2) {
                            if (isHi) "आगे बढ़ें" else "Next"
                        } else {
                            if (isHi) "शुरू करें" else "Get Started"
                        }
                    )
                }
            }
        }
    }
}
