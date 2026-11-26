package com.mahavtaar.panchanga.feature.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppThemeMode
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ConfirmActionDialog
import com.mahavtaar.panchanga.ui.components.GlossaryHintChip
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.PermissionRationaleDialog
import com.mahavtaar.panchanga.ui.components.ResponsiveChoiceGroup
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.VedicGlossaryBottomSheet
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNotificationsChanged: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenProfile: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val context = LocalContext.current
    val isHi = lang == AppLanguage.HINDI

    var showNakshatraDialog by remember { mutableStateOf(false) }
    var nakshatraQuery by remember { mutableStateOf("") }
    var glossaryQuery by remember { mutableStateOf<String?>(null) }
    var advancedExpanded by remember { mutableStateOf(false) }

    var showConfirmSignOut by remember { mutableStateOf(false) }
    var showConfirmClearCache by remember { mutableStateOf(false) }
    var showConfirmResetDefaults by remember { mutableStateOf(false) }
    var showNotifyPermissionRationale by remember { mutableStateOf(false) }
    var pendingEnableRunningNow by remember { mutableStateOf(false) }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleDriveAuthResult(result.data, result.resultCode)
    }

    val createDriveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.linkGoogleDriveBackupUri(uri)
        }
    }

    val openDriveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreFromGoogleDriveUri(uri)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnableRunningNow) {
            if (granted) {
                viewModel.setPersistentRunningNotificationEnabled(true)
            }
            pendingEnableRunningNow = false
        }
        onNotificationsChanged()
    }

    if (showNotifyPermissionRationale) {
        PermissionRationaleDialog(
            title = if (isHi) "लाइव सूचना की अनुमति" else "Allow Live Notifications",
            reason = if (isHi) {
                "होरा, तत्व, नाड़ी और तिथि के सटीक लाइव अलर्ट दिखाने के लिए सूचना अनुमति आवश्यक है।"
            } else {
                "Notifications let Mahavtaar show your live Hora, Tattva, Nadi, and Tithi updates in the status panel."
            },
            withoutPermissionNote = if (isHi) {
                "इसके बिना भी आप ऐप के भीतर सभी पंचांग गणनाएँ देख सकते हैं।"
            } else {
                "Without this permission, all Panchanga features still work normally inside the app."
            },
            allowLabel = if (isHi) "अनुमति दें" else "Allow",
            notNowLabel = if (isHi) "अभी नहीं" else "Not Now",
            onAllow = {
                showNotifyPermissionRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    if (pendingEnableRunningNow) {
                        viewModel.setPersistentRunningNotificationEnabled(true)
                        pendingEnableRunningNow = false
                    }
                    onNotificationsChanged()
                }
            },
            onDismiss = {
                showNotifyPermissionRationale = false
                pendingEnableRunningNow = false
            }
        )
    }

    if (showConfirmSignOut) {
        ConfirmActionDialog(
            title = if (isHi) "साइन आउट करें?" else "Sign Out?",
            message = if (isHi) {
                "क्या आप अपने खाते से साइन आउट करना चाहते हैं? आप कभी भी दोबारा साइन इन करके अपना डेटा प्राप्त कर सकते हैं।"
            } else {
                "Are you sure you want to sign out? You can sign back in anytime to restore your saved settings."
            },
            confirmLabel = ext.signOutButton,
            cancelLabel = strings.cancel,
            isDestructive = true,
            onConfirm = {
                showConfirmSignOut = false
                onSignOut?.invoke() ?: viewModel.signOut()
            },
            onDismiss = { showConfirmSignOut = false }
        )
    }

    if (showConfirmClearCache) {
        ConfirmActionDialog(
            title = if (isHi) "ऑफ़लाइन कैश साफ़ करें?" else "Clear Offline Cache?",
            message = if (isHi) {
                "इससे सहेजा गया अस्थायी पंचांग डेटा हट जाएगा और अगली बार खोलने पर पुनः गणना होगी। आपकी निजी सेटिंग्स सुरक्षित रहेंगी।"
            } else {
                "This clears temporary calculated calendar data and refreshes it on next view. Your saved preferences stay intact."
            },
            confirmLabel = if (isHi) "साफ़ करें" else "Clear Cache",
            cancelLabel = strings.cancel,
            isDestructive = true,
            onConfirm = {
                showConfirmClearCache = false
                viewModel.clearOfflineCache()
            },
            onDismiss = { showConfirmClearCache = false }
        )
    }

    if (showConfirmResetDefaults) {
        ConfirmActionDialog(
            title = strings.resetDefaultsTitle,
            message = if (isHi) {
                "यह भाषा, थीम, अयनांश और पंचांग विकल्पों को डिफ़ॉल्ट मानों पर वापस ले आएगा।"
            } else {
                "This resets your language, theme, Ayanamsha, and Panchanga preferences back to default values."
            },
            confirmLabel = strings.resetDefaultsButton,
            cancelLabel = strings.cancel,
            isDestructive = true,
            onConfirm = {
                showConfirmResetDefaults = false
                viewModel.resetDefaults()
            },
            onDismiss = { showConfirmResetDefaults = false }
        )
    }

    if (showNakshatraDialog) {
        val filteredNakshatras = remember(nakshatraQuery) {
            val q = nakshatraQuery.trim().lowercase()
            (1..27).filter { num ->
                if (q.isEmpty()) true
                else {
                    val en = VedicLocalizer.nakshatraNameByNumber(num, AppLanguage.ENGLISH).lowercase()
                    val hi = VedicLocalizer.nakshatraNameByNumber(num, AppLanguage.HINDI).lowercase()
                    num.toString().contains(q) || en.contains(q) || hi.contains(q)
                }
            }
        }
        AlertDialog(
            onDismissRequest = {
                showNakshatraDialog = false
                nakshatraQuery = ""
            },
            title = { Text(strings.birthNakshatraTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nakshatraQuery,
                        onValueChange = { nakshatraQuery = it },
                        label = {
                            Text(if (isHi) "नक्षत्र खोजें (हिंदी या English)" else "Search Nakshatra (English or Hindi)")
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_nakshatra_search")
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filteredNakshatras, key = { it }) { num ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        viewModel.setBirthNakshatra(num)
                                        showNakshatraDialog = false
                                        nakshatraQuery = ""
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("settings_birth_nak_$num"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = prefs.birthNakshatra == num,
                                    onClick = {
                                        viewModel.setBirthNakshatra(num)
                                        showNakshatraDialog = false
                                        nakshatraQuery = ""
                                    }
                                )
                                Column {
                                    Text(
                                        text = "$num. ${VedicLocalizer.nakshatraNameByNumber(num, lang)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    val altLang = if (isHi) AppLanguage.ENGLISH else AppLanguage.HINDI
                                    Text(
                                        text = VedicLocalizer.nakshatraNameByNumber(num, altLang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNakshatraDialog = false
                    nakshatraQuery = ""
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (glossaryQuery != null && policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)) {
        VedicGlossaryBottomSheet(
            initialQuery = glossaryQuery.orEmpty(),
            onDismiss = { glossaryQuery = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    LanguageTogglePill(
                        onToggleLanguage = viewModel::toggleLanguage,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("settings_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (policy.isAdmin) {
                    item(key = "admin_panel_entry") {
                        PanchangaCard {
                            SectionHeader(ext.adminPanelTitle, ext.adminPanelSubtitle)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_open_admin_panel_btn")
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ext.adminPanelTitle)
                            }
                        }
                    }
                }

                // ── GROUP 1: ALERTS, DND & QUIET HOURS (Essentials only; full rules in Studio) ──
                if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                    item(key = "dnd_quiet_hours") {
                        DndAndQuietHoursCard(
                            audioSettings = state.audioSettings,
                            strings = strings,
                            use24HourFormat = prefs.use24HourFormat,
                            testTagPrefix = "settings_",
                            timezone = prefs.selectedLocation.timezone,
                            nextSunriseMs = state.nextSunriseMs,
                            modifier = Modifier.testTag("settings_dnd_quiet_card"),
                            onUpdateAudioSettings = { updated ->
                                viewModel.updateAudioSettings(updated)
                                onNotificationsChanged()
                            }
                        )
                    }

                    item(key = "notifications_essentials") {
                        PanchangaCard {
                            SectionHeader(
                                title = strings.notificationsTitle,
                                subtitle = if (isHi) {
                                    "आवश्यक अलर्ट नियंत्रण — सभी नियम, स्वर और ध्वनियाँ स्टूडियो में बदलें"
                                } else {
                                    "Essential alert controls — customize individual rules and sounds in Studio"
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SettingsSwitchRow(
                                title = strings.notifMasterTitle,
                                subtitle = strings.notifMasterSubtitle,
                                checked = state.audioSettings.masterEnabled,
                                tag = "switch_master_alerts",
                                onCheckedChange = { enabled ->
                                    viewModel.updateAudioSettings(state.audioSettings.copy(masterEnabled = enabled))
                                    onNotificationsChanged()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title = strings.notifRunningNowTitle,
                                subtitle = strings.notifRunningNowSubtitle,
                                checked = state.persistentRunningNotificationEnabled,
                                tag = "switch_persistent_running_now",
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        viewModel.setPersistentRunningNotificationEnabled(false)
                                        onNotificationsChanged()
                                    } else {
                                        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            viewModel.setPersistentRunningNotificationEnabled(true)
                                            onNotificationsChanged()
                                        } else {
                                            pendingEnableRunningNow = true
                                            showNotifyPermissionRationale = true
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenNotificationStudio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_open_notification_studio_btn")
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isHi) "सभी अलर्ट नियम व ध्वनियाँ बदलें" else "Customize All Rules & Sounds"
                                )
                            }
                        }
                    }
                }

                // ── GROUP 2: APPEARANCE & LANGUAGE ──
                item(key = "appearance_and_language") {
                    PanchangaCard {
                        SectionHeader(
                            title = if (isHi) "भाषा और दिखावट" else "Appearance & Language",
                            subtitle = if (isHi) "भाषा, थीम और समय प्रारूप चुनें" else "Choose language, visual theme, and clock format"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.languageSectionTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ResponsiveChoiceGroup(
                            options = AppLanguage.entries,
                            selected = prefs.language,
                            labelFor = { it.nativeLabel },
                            tagFor = { "lang_${it.code}" },
                            onSelect = viewModel::setLanguage
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = strings.appearanceTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ResponsiveChoiceGroup(
                            options = AppThemeMode.entries,
                            selected = prefs.themeMode,
                            labelFor = { mode ->
                                when (mode) {
                                    AppThemeMode.NIGHT_SKY -> strings.themeNightSky
                                    AppThemeMode.PARCHMENT -> strings.themeParchment
                                    AppThemeMode.SYSTEM -> strings.themeSystem
                                }
                            },
                            tagFor = { "theme_${it.name.lowercase()}" },
                            onSelect = viewModel::setThemeMode
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitchRow(
                            title = strings.use24HourTitle,
                            subtitle = strings.use24HourSubtitle,
                            checked = prefs.use24HourFormat,
                            tag = "switch_24h",
                            onCheckedChange = viewModel::setUse24HourFormat
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        WidgetGlassCustomizationSection(
                            glassStyle = prefs.widgetGlassStyle,
                            opacity = prefs.widgetOpacity,
                            isHindi = isHi,
                            onSelectGlassStyle = viewModel::setWidgetGlassStyle,
                            onChangeOpacity = viewModel::setWidgetOpacity
                        )
                    }
                }

                // ── GROUP 3: VEDIC CALCULATIONS ──
                if (policy.hasFeature(AppFeatureKey.CALCULATION_SETTINGS)) {
                    item(key = "vedic_calculations") {
                        PanchangaCard {
                            SectionHeader(
                                title = if (isHi) "वैदिक गणना सेटिंग्स" else "Vedic Calculations",
                                subtitle = if (isHi) "जन्म नक्षत्र, चंद्र मास प्रणाली व अयनांश" else "Birth Nakshatra, lunar calendar tradition, and Ayanamsha"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Birth Nakshatra searchable picker row
                            Text(
                                text = strings.birthNakshatraTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { showNakshatraDialog = true }
                                    .padding(vertical = 8.dp)
                                    .testTag("settings_birth_nakshatra_row"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${prefs.birthNakshatra}. ${VedicLocalizer.nakshatraNameByNumber(prefs.birthNakshatra, lang)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = strings.birthNakshatraSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = strings.select,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Lunar Tradition with inline "?" glossary chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.lunarTraditionTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = strings.lunarTraditionSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                GlossaryHintChip(
                                    termQuery = "Amanta",
                                    contentLabel = strings.lunarTraditionTitle,
                                    onOpenGlossary = { glossaryQuery = it },
                                    testTag = "settings_lunar_tradition_help"
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ResponsiveChoiceGroup(
                                options = MonthSystem.entries,
                                selected = prefs.monthSystem,
                                labelFor = { VedicLocalizer.monthSystemShort(it, lang) },
                                tagFor = { "settings_month_${it.apiKey}" },
                                onSelect = viewModel::setMonthSystem
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Ayanamsha with inline "?" glossary chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.ayanamshaSectionTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = strings.ayanamshaSectionSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                GlossaryHintChip(
                                    termQuery = "Ayanamsha",
                                    contentLabel = strings.ayanamshaSectionTitle,
                                    onOpenGlossary = { glossaryQuery = it },
                                    testTag = "settings_ayanamsha_help"
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            CoordinateSelection.entries.forEach { coord ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clickable { viewModel.setAyanamsha(coord) }
                                        .padding(vertical = 2.dp)
                                        .testTag("ayanamsha_${coord.apiKey}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = prefs.ayanamsha == coord,
                                        onClick = { viewModel.setAyanamsha(coord) }
                                    )
                                    Text(
                                        VedicLocalizer.ayanamshaLabel(coord, lang),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // ── GROUP 4: ACCOUNT, CLOUD BACKUP & COLLAPSIBLE ADVANCED MAINTENANCE ──
                item(key = "cloud_account_and_backup") {
                    PanchangaCard {
                        val user = state.cloudUser
                        val driveStatus = state.driveBackupStatus
                        SectionHeader(
                            title = if (isHi) "खाता और क्लाउड बैकअप" else "Account & Cloud Backup",
                            subtitle = if (isHi) "आपकी सेटिंग्स और रिकॉर्ड सुरक्षित रूप से सिंक रखें" else "Keep your preferences and saved records safely backed up"
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (user != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = ext.cloudAccountSectionTitle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ext.signedInAsLabel}: ${user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: user.uid}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    user.email?.takeIf { it.isNotBlank() && it != user.displayName }?.let { email ->
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val syncText = state.cloudSyncStatus.lastSyncedAtMs?.let { ts ->
                                val formatted = Instant.ofEpochMilli(ts)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
                                "${ext.lastSyncedLabel}: $formatted"
                            } ?: ext.notSyncedYetLabel
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            state.cloudSyncStatus.lastError?.let { err ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = vedic.inauspicious
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = viewModel::syncCloudNow,
                                    enabled = !state.cloudSyncStatus.isSyncing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_sync_now_button")
                                ) {
                                    if (state.cloudSyncStatus.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(ext.syncingNowLabel)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = ext.syncNowButton,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(ext.syncNowButton)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showConfirmSignOut = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = ext.signOutButton,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ext.signOutButton)
                                }
                            }
                            if (onOpenProfile != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onOpenProfile,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_open_profile_button")
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ext.profileTitle)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Plain-language User-Owned Google Drive Auto-Sync controls
                        Text(
                            text = if (isHi) "आपका निजी Google Drive ऑटो-सिंक" else "Your Personal Google Drive Auto-Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHi) {
                                "लोकेशन, नवतारा, कुंडली, अलर्ट व सभी सेटिंग्स सीधे आपके अपने Google Drive (${user?.email ?: "साइन-इन खाता"}) में ऑटो-सिंक होती हैं — डेवलपर सर्वर पर नहीं।"
                            } else {
                                "Locations, Navtara, Kundli, alerts & preferences auto-sync directly to YOUR own Google Drive (${user?.email ?: "signed-in account"}) — never stored on developer servers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val linkedText = if (driveStatus.isDriveOAuthAuthorized || driveStatus.isDriveLinked) {
                            if (isHi) "निजी Google Drive जुड़ा हुआ है · स्वचालित टू-वे सिंक चालू (${driveStatus.totalSyncedRecords} रिकॉर्ड)"
                            else "Connected to your Google Drive · Auto-sync active (${driveStatus.totalSyncedRecords} records)"
                        } else {
                            ext.driveNotConnectedLabel
                        }
                        Text(
                            text = linkedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (driveStatus.isDriveLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        driveStatus.lastDriveSyncAtMs?.let { ts ->
                            val formatted = Instant.ofEpochMilli(ts)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHi) "अंतिम Google Drive बैकअप: $formatted" else "Last Google Drive sync: $formatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        driveStatus.lastError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = vedic.inauspicious
                            )
                        }

                        state.statusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (driveStatus.isDriveLinked) {
                                        viewModel.syncToGoogleDriveNow()
                                    } else {
                                        viewModel.authorizeGoogleDrive(context as? Activity) { pi ->
                                            driveAuthLauncher.launch(IntentSenderRequest.Builder(pi).build())
                                        }
                                    }
                                },
                                enabled = !driveStatus.isBackingUp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_gdrive_primary_button")
                            ) {
                                Icon(
                                    imageVector = if (driveStatus.isDriveLinked) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (driveStatus.isDriveLinked) {
                                        if (isHi) "अभी सिंक करें" else "Sync Now"
                                    } else {
                                        ext.connectGoogleDriveButton
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (driveStatus.isDriveLinked) {
                                        viewModel.restoreFromGoogleDriveUri(null)
                                    } else {
                                        openDriveBackupLauncher.launch(arrayOf("application/json", "text/*"))
                                    }
                                },
                                modifier = Modifier.testTag("settings_gdrive_restore_button")
                            ) {
                                Text(if (isHi) "रीस्टोर करें" else "Restore")
                            }
                        }

                        if (!driveStatus.isDriveLinked || driveStatus.isOAuthBlockedByConsentScreen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { createDriveBackupLauncher.launch(viewModel.suggestedDriveFileName()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_gdrive_link_file_saf_button")
                            ) {
                                Text(
                                    if (isHi) "अपने Google Drive में फ़ाइल लिंक करें (बिना 403 त्रुटि ऑटो-सिंक)"
                                    else "Link File in Your Google Drive (Direct Auto-Sync)"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Collapsible Advanced & Maintenance Drawer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { advancedExpanded = !advancedExpanded }
                                .padding(top = 8.dp)
                                .testTag("settings_advanced_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHi) "उन्नत विकल्प और रखरखाव" else "Advanced & Maintenance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHi) "त्वरित सेटअप, मैन्युअल बैकअप फ़ाइल, कैश व रीसेट" else "Quick Setup replay, manual backup file, cache, and reset",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = advancedExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::replayQuickSetup,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_replay_quick_setup_btn")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isHi) "त्वरित सेटअप फिर से चलाएँ" else "Run Quick Setup Again")
                                }

                                OutlinedButton(
                                    onClick = { createDriveBackupLauncher.launch(viewModel.suggestedDriveFileName()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_gdrive_change_file_button")
                                ) {
                                    Text(if (isHi) "बैकअप फ़ाइल स्वयं चुनें" else "Choose Backup File Manually")
                                }

                                if (policy.hasFeature(AppFeatureKey.CALCULATION_SETTINGS)) {
                                    OutlinedButton(
                                        onClick = { showConfirmClearCache = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("clear_cache_button")
                                    ) {
                                        Text(strings.clearEphemerisCache)
                                    }

                                    OutlinedButton(
                                        onClick = { showConfirmResetDefaults = true },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_defaults_button")
                                    ) {
                                        Text(strings.resetDefaultsButton)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
