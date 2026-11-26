package com.mahavtaar.panchanga.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.BuiltinVedicTone
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VedicAlertCategory
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.PermissionRationaleDialog
import com.mahavtaar.panchanga.ui.components.ResponsiveChoiceGroup
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.formatMuteStatusLine
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.PanchangaStrings
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStudioScreen(
    onNavigateBack: () -> Unit,
    focusEventTypeName: String? = null,
    viewModel: NotificationStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val lang = LocalAppLanguage.current
    val isHindi = lang == AppLanguage.HINDI
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingEnableRunningNow by remember { mutableStateOf(false) }
    var showNotifyPermissionRationale by remember { mutableStateOf(false) }
    var showExactAlarmRationale by remember { mutableStateOf(false) }
    var didApplyInitialFocus by remember(focusEventTypeName) { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnableRunningNow) {
            if (granted) {
                viewModel.togglePersistentRunningNotification(true)
            }
            pendingEnableRunningNow = false
        }
        viewModel.refreshPermissionsAndSchedule(silent = false)
    }

    LaunchedEffect(focusEventTypeName, state.rules) {
        if (!didApplyInitialFocus && !focusEventTypeName.isNullOrBlank() && state.rules.isNotEmpty()) {
            if (viewModel.openRuleByEventTypeName(focusEventTypeName)) {
                didApplyInitialFocus = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndSchedule(silent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (showNotifyPermissionRationale) {
        PermissionRationaleDialog(
            title = strings.notifPermissionNeededTitle,
            reason = if (isHindi) {
                "सटीक तत्व, नाड़ी, होरा और व्रत रिमाइंडर समय पर भेजने के लिए सूचना अनुमति आवश्यक है।"
            } else {
                "Allow notifications so Mahavtaar can deliver timely Tattva, Nadi, Hora, and Vrat alerts."
            },
            withoutPermissionNote = if (isHindi) {
                "यदि आप अभी अनुमति नहीं देते हैं, तो भी आप ऐप के भीतर सभी गणनाएँ देख सकते हैं।"
            } else {
                "If you skip this now, all calendar and Panchanga tools still work inside the app."
            },
            allowLabel = strings.notifGrantPermissionButton,
            notNowLabel = if (isHindi) "अभी नहीं" else "Not Now",
            onAllow = {
                showNotifyPermissionRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotifyPermissionRationale = false
                pendingEnableRunningNow = false
            }
        )
    }

    if (showExactAlarmRationale) {
        PermissionRationaleDialog(
            title = strings.notifExactAlarmBannerTitle,
            reason = if (isHindi) {
                "सूर्योदय, होरा और तत्व परिवर्तन सेकंड के स्तर पर सटीक होते हैं — सटीक अलार्म अनुमति उन्हें समय पर बजाने में मदद करती है।"
            } else {
                "Sunrise, Hora, and Tattva shifts happen at exact timestamps — allowing Exact Alarms ensures alerts fire right on time."
            },
            withoutPermissionNote = if (isHindi) {
                "इसके बिना Android कुछ अलर्ट कुछ मिनट की देरी से भेज सकता है।"
            } else {
                "Without this, Android may delay alerts by a few minutes to save battery."
            },
            allowLabel = strings.notifAllowExactAlarmButton,
            notNowLabel = if (isHindi) "अभी नहीं" else "Not Now",
            onAllow = {
                showExactAlarmRationale = false
                runCatching {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            },
            onDismiss = { showExactAlarmRationale = false }
        )
    }

    val editingRule = state.editingRule
    if (editingRule != null) {
        NotificationRuleEditorSheet(
            initialRule = editingRule,
            voiceRecordings = state.voiceRecordings,
            currentlyPlayingKey = state.currentlyPlayingKey,
            onDismiss = viewModel::closeRuleEditor,
            onSave = viewModel::saveRule,
            onTestNow = viewModel::testRule,
            onPlayTunePreview = viewModel::playTunePreview,
            onPlayRecording = viewModel::playRecording,
            onStopPreview = viewModel::stopAudioPreview
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.notificationStudioTopBarTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notif_studio_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    LanguageTogglePill(
                        onToggleLanguage = viewModel::toggleLanguage,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("notif_studio_language_toggle")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Permission Banners
                if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item(key = "perm_post_notif") {
                        PermissionWarningBanner(
                            title = strings.notifPermissionNeededTitle,
                            body = strings.notifPermissionNeededBody,
                            actionLabel = strings.notifGrantPermissionButton,
                            testTag = "notif_grant_permission_btn",
                            onAction = { showNotifyPermissionRationale = true }
                        )
                    }
                }

                if (!state.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item(key = "perm_exact_alarm") {
                        PermissionWarningBanner(
                            title = strings.notifExactAlarmBannerTitle,
                            body = strings.notifExactAlarmBannerBody,
                            actionLabel = strings.notifAllowExactAlarmButton,
                            testTag = "notif_allow_exact_alarm_btn",
                            onAction = { showExactAlarmRationale = true }
                        )
                    }
                }

                // Master Control + Next Alert Hero Card
                item(key = "master_hero") {
                    MasterNotificationHeroCard(
                        audioSettings = state.audioSettings,
                        nextAlert = state.upcomingAlerts.firstOrNull(),
                        timezone = state.preferences.selectedLocation.timezone,
                        use24HourFormat = state.preferences.use24HourFormat,
                        isHindi = isHindi,
                        strings = strings,
                        onToggleMaster = viewModel::toggleMasterEnabled,
                        onTogglePersistentRunning = { enabled ->
                            if (!enabled) {
                                viewModel.togglePersistentRunningNotification(false)
                            } else if (state.hasNotificationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                viewModel.togglePersistentRunningNotification(true)
                            } else {
                                pendingEnableRunningNow = true
                                showNotifyPermissionRationale = true
                            }
                        }
                    )
                }

                // 3-Tab Large-Font-Safe Selector
                item(key = "studio_tabs") {
                    val tabIndices = listOf(0, 1, 2)
                    val tabLabels = listOf(
                        strings.notifTabEvents,
                        strings.notifTabVoiceQuiet,
                        strings.notifTabQueue
                    )
                    ResponsiveChoiceGroup(
                        options = tabIndices,
                        selected = state.selectedTab,
                        labelFor = { tabLabels[it] },
                        tagFor = { "notif_tab_$it" },
                        onSelect = viewModel::selectTab,
                        modifier = Modifier.testTag("notif_studio_tabs")
                    )
                }

                when (state.selectedTab) {
                    0 -> {
                        // 1-Tap Notification Presets
                        item(key = "notification_presets") {
                            NotificationPresetsCard(
                                rules = state.rules,
                                masterEnabled = state.audioSettings.masterEnabled,
                                isHindi = isHindi,
                                onApplyPreset = viewModel::applyPreset
                            )
                        }

                        // Category Filter Bar
                        item(key = "category_chips") {
                            CategoryFilterBar(
                                selectedCategory = state.selectedCategory,
                                strings = strings,
                                onSelect = viewModel::selectCategory
                            )
                        }

                        val filteredRules = if (state.selectedCategory == null) {
                            state.rules
                        } else {
                            state.rules.filter { it.eventType.category == state.selectedCategory }
                        }

                        items(filteredRules, key = { it.eventType.name }) { rule ->
                            EventRuleCard(
                                rule = rule,
                                voiceRecordings = state.voiceRecordings,
                                isHindi = isHindi,
                                strings = strings,
                                onToggleEnabled = { enabled ->
                                    viewModel.toggleEventEnabled(rule.eventType, enabled)
                                },
                                onCustomizeClick = { viewModel.openRuleEditor(rule) }
                            )
                        }
                    }

                    1 -> {
                        item(key = "voice_studio_section") {
                            VoiceStudioSection(
                                audioSettings = state.audioSettings,
                                voiceRecordings = state.voiceRecordings,
                                isRecording = state.isRecording,
                                recordingElapsedSec = state.recordingElapsedSec,
                                currentlyPlayingKey = state.currentlyPlayingKey,
                                isHindi = isHindi,
                                strings = strings,
                                onUpdateAudioSettings = { updated ->
                                    viewModel.updateAudioSettings(updated, reschedule = true)
                                },
                                onTestTattvaVoice = { viewModel.speakSampleVoice(isTattvaSample = true) },
                                onTestNadiVoice = { viewModel.speakSampleVoice(isTattvaSample = false) },
                                onStartRecording = viewModel::startRecording,
                                onStopRecording = viewModel::stopRecordingAndSave,
                                onCancelRecording = viewModel::cancelRecording,
                                onPlayRecording = viewModel::playRecording,
                                onDeleteRecording = viewModel::deleteRecording,
                                timezone = state.preferences.selectedLocation.timezone,
                                use24HourFormat = state.preferences.use24HourFormat
                            )
                        }
                    }

                    else -> {
                        // Queue & History Tab
                        item(key = "queue_actions") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.refreshPermissionsAndSchedule(silent = false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("notif_refresh_schedule_btn")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.notifRescheduleNowButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = viewModel::resetNotificationDefaults,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("notif_reset_defaults_btn")
                                ) {
                                    Text(strings.notifResetRulesButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        item(key = "upcoming_header") {
                            SectionHeader(
                                title = "${strings.notifUpcomingQueueTitle} (${state.upcomingAlerts.size})",
                                subtitle = state.preferences.selectedLocation.name
                            )
                        }

                        if (state.upcomingAlerts.isEmpty()) {
                            item(key = "empty_queue") {
                                PanchangaCard {
                                    Text(
                                        text = strings.notifNoUpcomingAlerts,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(state.upcomingAlerts, key = { it.id }) { alert ->
                                ScheduledAlertRowCard(
                                    item = alert,
                                    timezone = state.preferences.selectedLocation.timezone,
                                    isHindi = isHindi,
                                    strings = strings
                                )
                            }
                        }

                        if (state.recentHistory.isNotEmpty()) {
                            item(key = "recent_header") {
                                SectionHeader(title = strings.notifRecentHistoryTitle)
                            }
                            items(state.recentHistory, key = { "hist_${it.id}_${it.updatedAtMs}" }) { item ->
                                ScheduledAlertRowCard(
                                    item = item,
                                    timezone = state.preferences.selectedLocation.timezone,
                                    isHindi = isHindi,
                                    strings = strings
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    title: String,
    body: String,
    actionLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessAlarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )
            }
            Button(
                onClick = onAction,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationPresetsCard(
    rules: List<NotificationRule>,
    masterEnabled: Boolean,
    isHindi: Boolean,
    onApplyPreset: (NotificationPreset) -> Unit
) {
    val enabledTypes = remember(rules) { rules.filter { it.isEnabled }.map { it.eventType }.toSet() }
    val selectablePresets = remember {
        listOf(
            NotificationPreset.ESSENTIAL,
            NotificationPreset.PRACTICE,
            NotificationPreset.FULL
        )
    }

    PanchangaCard {
        Text(
            text = if (isHindi) "1-टैप अलर्ट प्रीसेट" else "1-Tap Alert Presets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isHindi) {
                "तुरंत चुने हुए नियम लागू करें — आपके कस्टम स्वर व ध्वनियाँ सुरक्षित रहेंगी"
            } else {
                "Apply a curated alert set in one tap — your custom tones and voices stay untouched"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectablePresets.forEach { preset ->
                val isActive = masterEnabled && enabledTypes == preset.enabledEventTypes()
                FilterChip(
                    selected = isActive,
                    onClick = { onApplyPreset(preset) },
                    label = {
                        Text(
                            text = if (isHindi) preset.titleHi else preset.titleEn,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("notif_preset_${preset.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun MasterNotificationHeroCard(
    audioSettings: AlertAudioSettings,
    nextAlert: ScheduledAlertItem?,
    timezone: String,
    use24HourFormat: Boolean,
    isHindi: Boolean,
    strings: PanchangaStrings,
    onToggleMaster: (Boolean) -> Unit,
    onTogglePersistentRunning: (Boolean) -> Unit
) {
    val masterEnabled = audioSettings.masterEnabled
    val persistentRunningEnabled = audioSettings.persistentRunningNotificationEnabled
    val lang = if (isHindi) AppLanguage.HINDI else AppLanguage.ENGLISH
    val vedic = LocalVedicColors.current
    val muteStatusLine = formatMuteStatusLine(
        audioSettings = audioSettings,
        timezone = timezone,
        use24HourFormat = use24HourFormat,
        language = lang
    )

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = strings.notifMasterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.notifMasterSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = masterEnabled,
                onCheckedChange = onToggleMaster,
                modifier = Modifier.testTag("notif_master_switch")
            )
        }

        if (muteStatusLine != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_mute_status_banner")
            ) {
                Text(
                    text = muteStatusLine,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = vedic.inauspicious,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = strings.notifRunningNowTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.notifRunningNowSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = persistentRunningEnabled,
                onCheckedChange = onTogglePersistentRunning,
                modifier = Modifier.testTag("notif_running_now_switch")
            )
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.notifNextAlertLabel.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (nextAlert != null && masterEnabled) {
                    val title = if (isHindi) nextAlert.titleHi else nextAlert.titleEn
                    val body = if (isHindi) nextAlert.bodyHi else nextAlert.bodyEn
                    val formattedTime = formatEpochLocal(nextAlert.triggerTimeMs, timezone, isHindi)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (body.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        FormattedAlertBodyText(body = body)
                    }
                } else {
                    Text(
                        text = strings.notifNoUpcomingAlerts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selectedCategory: VedicAlertCategory?,
    strings: PanchangaStrings,
    onSelect: (VedicAlertCategory?) -> Unit
) {
    val chips: List<Pair<VedicAlertCategory?, String>> = listOf(
        null to strings.notifCategoryAll,
        VedicAlertCategory.TATTVA to strings.notifCategoryTattva,
        VedicAlertCategory.SWARA_NADI to strings.notifCategorySwaraNadi,
        VedicAlertCategory.HORA to strings.notifCategoryHora,
        VedicAlertCategory.MUHURTA to strings.notifCategoryMuhurta,
        VedicAlertCategory.ANGAS_CELESTIAL to strings.notifCategoryAngas,
        VedicAlertCategory.VRAT_FESTIVAL to strings.notifCategoryVrats
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(chips, key = { it.first?.name ?: "ALL" }) { (category, label) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelect(category) },
                label = { Text(label) },
                modifier = Modifier.testTag("notif_cat_${category?.name ?: "ALL"}")
            )
        }
    }
}

@Composable
private fun EventRuleCard(
    rule: NotificationRule,
    voiceRecordings: List<com.mahavtaar.panchanga.domain.model.VoiceRecording>,
    isHindi: Boolean,
    strings: PanchangaStrings,
    onToggleEnabled: (Boolean) -> Unit,
    onCustomizeClick: () -> Unit
) {
    val accent = categoryAccentColor(rule.eventType.category)
    val icon = categoryIcon(rule.eventType.category)
    val title = if (isHindi) rule.eventType.titleHi else rule.eventType.titleEn
    val subtitle = if (isHindi) rule.eventType.descriptionHi else rule.eventType.descriptionEn

    val tuneLabel = if (rule.tuneSource == TuneSourceType.BUILTIN_VEDIC) {
        val tone = BuiltinVedicTone.fromId(rule.tuneUriOrId)
        if (isHindi) tone.titleHi else tone.titleEn
    } else {
        rule.tuneDisplayName.ifBlank { strings.notifTuneSourceAudioFile }
    }

    val voiceLabel = when (rule.voiceSource) {
        VoiceSourceType.BUILTIN_TTS -> strings.notifVoiceSourceTts
        VoiceSourceType.CUSTOM_RECORDING -> {
            voiceRecordings.firstOrNull { it.id == rule.voiceRecordingId }?.displayName
                ?: strings.notifVoiceSourceRecording
        }
    }

    val leadLabel = if (rule.leadTimeMinutes > 0) {
        "${rule.leadTimeMinutes} ${strings.notifMinutesBeforeSuffix}"
    } else {
        if (isHindi) "ठीक समय पर" else "Exact Time"
    }

    PanchangaCard(
        modifier = Modifier
            .clickable(onClick = onCustomizeClick)
            .testTag("notif_rule_card_${rule.eventType.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("notif_rule_switch_${rule.eventType.name}")
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniInfoChip(text = leadLabel, tint = accent)
                if (rule.deliveryMode != AlertDeliveryMode.VOICE_ONLY && rule.deliveryMode != AlertDeliveryMode.SILENT_BANNER) {
                    MiniInfoChip(text = tuneLabel, tint = MaterialTheme.colorScheme.secondary)
                }
                if (rule.deliveryMode == AlertDeliveryMode.TUNE_AND_VOICE || rule.deliveryMode == AlertDeliveryMode.VOICE_ONLY) {
                    MiniInfoChip(text = voiceLabel, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniInfoChip(text: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ScheduledAlertRowCard(
    item: ScheduledAlertItem,
    timezone: String,
    isHindi: Boolean,
    strings: PanchangaStrings
) {
    val title = if (isHindi) item.titleHi else item.titleEn
    val body = if (isHindi) item.bodyHi else item.bodyEn
    val timeText = formatEpochLocal(item.triggerTimeMs, timezone, isHindi)
    val accent = categoryAccentColor(item.eventType.category)

    val statusText = when (item.status) {
        ScheduledAlertStatus.SCHEDULED -> strings.notifStatusScheduled
        ScheduledAlertStatus.DELIVERED -> strings.notifStatusDelivered
        ScheduledAlertStatus.SUPPRESSED_QUIET -> strings.notifStatusSuppressedQuiet
        ScheduledAlertStatus.SUPPRESSED_DND -> strings.notifStatusSuppressedDnd
    }

    val deliveryIcon = when (item.deliveryMode) {
        AlertDeliveryMode.TUNE_AND_VOICE -> Icons.Rounded.RecordVoiceOver
        AlertDeliveryMode.VOICE_ONLY -> Icons.Rounded.GraphicEq
        AlertDeliveryMode.TUNE_ONLY -> Icons.Rounded.MusicNote
        AlertDeliveryMode.SILENT_BANNER -> Icons.AutoMirrored.Rounded.VolumeOff
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(deliveryIcon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FormattedAlertBodyText(body = body)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                MiniInfoChip(text = statusText, tint = accent)
            }
        }
    }
}

@Composable
private fun FormattedAlertBodyText(body: String) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lines = body.lines().filter { it.isNotBlank() }
    val annotated = buildAnnotatedString {
        lines.forEachIndexed { idx, line ->
            if (idx > 0) append("\n")
            val isNadiOrTattva = line.contains("Nadi", ignoreCase = true) ||
                line.contains("Tattva", ignoreCase = true) ||
                line.contains("नाड़ी") ||
                line.contains("तत्व")
            withStyle(
                SpanStyle(
                    fontWeight = if (isNadiOrTattva) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNadiOrTattva) primaryColor else mutedColor
                )
            ) {
                append(line)
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 6,
        overflow = TextOverflow.Ellipsis
    )
}

internal fun categoryAccentColor(category: VedicAlertCategory): Color = when (category) {
    VedicAlertCategory.TATTVA -> Color(0xFFD95B26)
    VedicAlertCategory.SWARA_NADI -> Color(0xFF0D7C66)
    VedicAlertCategory.HORA -> Color(0xFF6B4EC7)
    VedicAlertCategory.MUHURTA -> Color(0xFFB57A16)
    VedicAlertCategory.ANGAS_CELESTIAL -> Color(0xFF2B6CB0)
    VedicAlertCategory.VRAT_FESTIVAL -> Color(0xFFC5305C)
}

internal fun categoryIcon(category: VedicAlertCategory): ImageVector = when (category) {
    VedicAlertCategory.TATTVA -> Icons.Rounded.LocalFireDepartment
    VedicAlertCategory.SWARA_NADI -> Icons.Rounded.Air
    VedicAlertCategory.HORA -> Icons.Rounded.Schedule
    VedicAlertCategory.MUHURTA -> Icons.Rounded.AutoAwesome
    VedicAlertCategory.ANGAS_CELESTIAL -> Icons.Rounded.WbSunny
    VedicAlertCategory.VRAT_FESTIVAL -> Icons.AutoMirrored.Rounded.VolumeUp
}

internal fun formatEpochLocal(epochMs: Long, timezone: String, isHindi: Boolean = false): String =
    runCatching {
        TimeFormatters.formatLocalizedDateTimeWithSeconds(epochMs, timezone, isHindi)
    }.getOrDefault("--:--:--")
