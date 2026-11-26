package com.mahavtaar.panchanga.feature.swara

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AtmosphericHeroCard
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.GlossaryHintChip
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.NavtaraRadialWheelCanvas
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.components.VedicGlossaryBottomSheet
import com.mahavtaar.panchanga.ui.export.OfflineReportExportHelper
import com.mahavtaar.panchanga.ui.export.TimezoneCatalog
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.UiReportFormatter
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

private data class NavtaraJournalEditTarget(
    val dateIso: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NavtaraScreen(
    onNavigateBack: () -> Unit,
    viewModel: NavtaraViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val policy = LocalAccessPolicy.current
    val canJournalSave = policy.hasFeature(AppFeatureKey.NAVTATA_JOURNAL_SAVE)

    var showPicker by remember { mutableStateOf(false) }
    var showInspectDatePicker by remember { mutableStateOf(false) }
    var inspectedTara by remember { mutableStateOf<NavtaraResult?>(null) }
    var editingProfileDialog by remember { mutableStateOf<Pair<Boolean, NavtaraProfile?>?>(null) }
    var journalEditTarget by remember { mutableStateOf<NavtaraJournalEditTarget?>(null) }
    var glossaryQuery by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = state.plannerTab != 0) {
        viewModel.selectPlannerTab(0)
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleDriveAuthResult(result.data, result.resultCode)
    }

    val linkDriveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.linkGoogleDriveBackupUri(uri)
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val payload = viewModel.exportNavtaraJsonString()
                OfflineReportExportHelper.writeTextToUri(context, uri, payload)
                    .onSuccess { viewModel.setStatusMessage(ext.jsonSavedMsg) }
                    .onFailure { err -> viewModel.setStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            OfflineReportExportHelper.readTextFromUri(context, uri)
                .onSuccess { raw -> viewModel.importJsonString(raw) }
                .onFailure { err -> viewModel.setStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    val triggerDriveSyncOrConnect: () -> Unit = {
        if (state.driveBackupStatus.isDriveLinked) {
            viewModel.syncDriveJsonNow()
        } else {
            viewModel.authorizeGoogleDrive(context as? Activity) { pi ->
                driveAuthLauncher.launch(IntentSenderRequest.Builder(pi).build())
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.navtaraTopBarTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val loc = state.preferences.selectedLocation
                        Text(
                            text = "${loc.name} · ${TimezoneCatalog.formatCoordinates(loc.latitude, loc.longitude, 5)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("navtara_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    GlossaryHintChip(
                        termQuery = "Tara",
                        contentLabel = if (lang == AppLanguage.HINDI) "तारा बल / नवतारा" else "Tara Bala / Navtara",
                        onOpenGlossary = { glossaryQuery = it },
                        testTag = "navtara_tara_help_chip"
                    )
                    IconButton(
                        onClick = triggerDriveSyncOrConnect,
                        modifier = Modifier.testTag("navtara_topbar_drive_sync_button")
                    ) {
                        Icon(
                            imageVector = if (state.driveBackupStatus.isDriveOAuthAuthorized) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                            contentDescription = ext.driveSyncJsonNowButton,
                            tint = if (state.driveBackupStatus.isDriveOAuthAuthorized) vedic.auspicious else MaterialTheme.colorScheme.primary
                        )
                    }
                    state.todayResult?.let { todayRes ->
                        IconButton(
                            onClick = {
                                val birthNakName = VedicLocalizer.nakshatraNameByNumber(state.preferences.birthNakshatra, lang)
                                val transitNakName = VedicLocalizer.nakshatraNameByNumber(todayRes.targetNakshatraIndex, lang)
                                val gandanta = VedicSwaraNavtaraCatalog.checkGandanta(state.preferences.birthNakshatra, state.birthPada)
                                val gTitle = if (lang == AppLanguage.HINDI) gandanta.titleHi else gandanta.titleEn
                                val body = UiReportFormatter.formatNavtaraSummary(
                                    nativeName = state.nativeName,
                                    birthNakshatraName = birthNakName,
                                    birthPada = state.birthPada,
                                    todayNakshatraName = transitNakName,
                                    todayResult = todayRes,
                                    gandantaTitle = gTitle,
                                    lang = lang
                                )
                                UiReportFormatter.shareText(context, ext.shareSummary, body)
                            },
                            modifier = Modifier.testTag("navtara_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = ext.shareSummary, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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
            when {
                state.isLoading -> LoadingStateBox()
                state.errorMessage != null -> Box(Modifier.padding(16.dp)) {
                    ErrorStateCard(state.errorMessage!!, onRetry = viewModel::retry)
                }
                else -> {
                    val todayRes = state.todayResult
                    val birthNak = state.preferences.birthNakshatra
                    val tabs = buildList {
                        add(ext.navtaraPeriodTabToday)
                        add(ext.navtaraPeriodTabPlanner)
                        if (canJournalSave) add(ext.navtaraPeriodTabSaved)
                    }
                    val activePlannerTab = state.plannerTab.coerceIn(0, tabs.lastIndex)

                    Column(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)) {
                        PrimaryTabRow(
                            selectedTabIndex = activePlannerTab,
                            containerColor = MaterialTheme.colorScheme.background
                        ) {
                            tabs.forEachIndexed { idx, label ->
                                Tab(
                                    selected = activePlannerTab == idx,
                                    onClick = { viewModel.selectPlannerTab(idx) },
                                    text = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("navtara_tab_$idx")
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("navtara_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "saved_profiles_bar") {
                                NavtaraProfilesCard(
                                    profiles = state.profiles,
                                    activeProfileId = state.activeProfileId,
                                    onSelectProfile = viewModel::selectProfile,
                                    onAddProfileClick = { editingProfileDialog = true to null },
                                    onEditProfileClick = { p -> editingProfileDialog = true to p },
                                    onPinDefault = viewModel::setDefaultProfile,
                                    onDeleteProfile = viewModel::deleteProfile
                                )
                            }

                            when (activePlannerTab) {
                                0 -> {
                                    item(key = "birth_selector") {
                                        NavtaraBirthSelectorCard(
                                            nativeName = state.nativeName,
                                            birthNak = birthNak,
                                            birthPada = state.birthPada,
                                            onNameChange = viewModel::setNativeName,
                                            onPickNakshatra = { showPicker = true },
                                            onSelectPada = viewModel::setBirthPada
                                        )
                                    }

                                    item(key = "inspect_date_bar") {
                                        PanchangaCard {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = ext.navtaraInspectDateLabel,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                TextButton(
                                                    onClick = viewModel::resetInspectDateToToday,
                                                    modifier = Modifier.testTag("navtara_inspect_today_reset")
                                                ) {
                                                    Text(strings.today)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.shiftInspectDate(-1L) },
                                                    modifier = Modifier.size(48.dp).testTag("navtara_inspect_prev_day")
                                                ) {
                                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.previousMonthCd)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { showInspectDatePicker = true }
                                                        .testTag("navtara_inspect_date_button")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Text(
                                                            text = state.inspectDate.toString(),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.shiftInspectDate(1L) },
                                                    modifier = Modifier.size(48.dp).testTag("navtara_inspect_next_day")
                                                ) {
                                                    Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextMonthCd)
                                                }
                                            }
                                        }
                                    }

                                    item(key = "birth_meta_chips") {
                                        BirthNakshatraMetaChipsCard(birthNakshatra = birthNak, birthPada = state.birthPada)
                                    }

                                    item(key = "gandanta_card") {
                                        GandantaStatusCard(birthNakshatra = birthNak, birthPada = state.birthPada)
                                    }

                                    if (todayRes != null) {
                                        state.snapshot?.let { snap ->
                                            item(key = "live_sky_shift") {
                                                LiveSkyNakshatraShiftCard(
                                                    snapshot = snap,
                                                    todayResult = todayRes,
                                                    matrix = state.matrix,
                                                    onOpenPlannerClick = { viewModel.selectPlannerTab(1) }
                                                )
                                            }
                                        }

                                        item(key = "navtara_wheel") {
                                            val activeTaraNumber = ((todayRes.distance - 1) % 9) + 1
                                            PanchangaCard {
                                                SectionHeader(
                                                    title = if (lang == AppLanguage.HINDI) "नवतारा चक्र (९ तारा मंडल)" else "9-Sector Navtara Radial Wheel",
                                                    subtitle = if (lang == AppLanguage.HINDI) "किसी भी तारा खंड पर टैप करके फल व परिहार देखें" else "Tap any sector (1–9) to inspect Tara verdict & remedies"
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    NavtaraRadialWheelCanvas(
                                                        taraResults = state.matrix,
                                                        activeTaraNumber = activeTaraNumber,
                                                        onSelectTaraNumber = { taraNum ->
                                                            inspectedTara = state.matrix.firstOrNull { ((it.distance - 1) % 9) + 1 == taraNum } ?: todayRes
                                                        },
                                                        modifier = Modifier.size(236.dp).testTag("navtara_radial_wheel_canvas")
                                                    )
                                                }
                                            }
                                        }

                                        item(key = "today_verdict") {
                                            NavtaraVerdictHeroCard(todayRes = todayRes)
                                        }

                                        state.snapshot?.let { snap ->
                                            val vedha = VedicSwaraNavtaraCatalog.evaluateTaraGocharaVedha(todayRes, snap.planets)
                                            item(key = "vedha_override") { VedhaOverrideCard(vedha = vedha, todayResult = todayRes) }
                                            item(key = "live_star_transits") { LiveStarTransitsCard(snapshot = snap, birthNakshatra = birthNak) }
                                        }

                                        todayRes.parihara?.let { p ->
                                            item(key = "parihara") {
                                                PanchangaCard {
                                                    SectionHeader(strings.pariharaRemediesTitle)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("${strings.deitiesPrefix}: ${if (lang == AppLanguage.HINDI) p.deityHi else p.deityEn}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                    Text("Mantra: ${if (lang == AppLanguage.HINDI) p.mantraHi else p.mantraEn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("${strings.charityDaanLabel}: ${if (lang == AppLanguage.HINDI) p.donationHi else p.donationEn}", style = MaterialTheme.typography.bodySmall)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    val avoids = if (lang == AppLanguage.HINDI) p.avoidHi else p.avoidEn
                                                    avoids.forEach { item ->
                                                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = vedic.inauspicious)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item(key = "tara_dasa") {
                                        TaraDasaTableCard(birthNakshatra = birthNak)
                                    }

                                    item(key = "matrix_header_and_filters") {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SectionHeader(strings.full27MatrixTitle, strings.full27MatrixSubtitle)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilterChip(
                                                    selected = state.matrixFilter == null,
                                                    onClick = { viewModel.setMatrixFilter(null) },
                                                    label = { Text(ext.navtaraMatrixFilterAll) },
                                                    modifier = Modifier.testTag("navtara_matrix_filter_all")
                                                )
                                                FilterChip(
                                                    selected = state.matrixFilter == NavtaraDayCategory.AUSPICIOUS,
                                                    onClick = { viewModel.setMatrixFilter(NavtaraDayCategory.AUSPICIOUS) },
                                                    label = { Text(ext.auspiciousDaysLabel) },
                                                    modifier = Modifier.testTag("navtara_matrix_filter_auspicious")
                                                )
                                                FilterChip(
                                                    selected = state.matrixFilter == NavtaraDayCategory.MIXED,
                                                    onClick = { viewModel.setMatrixFilter(NavtaraDayCategory.MIXED) },
                                                    label = { Text(ext.mixedDaysLabel) },
                                                    modifier = Modifier.testTag("navtara_matrix_filter_mixed")
                                                )
                                                FilterChip(
                                                    selected = state.matrixFilter == NavtaraDayCategory.CAUTION,
                                                    onClick = { viewModel.setMatrixFilter(NavtaraDayCategory.CAUTION) },
                                                    label = { Text(ext.cautionDaysLabel) },
                                                    modifier = Modifier.testTag("navtara_matrix_filter_caution")
                                                )
                                            }
                                        }
                                    }

                                    val filteredMatrix = when (state.matrixFilter) {
                                        NavtaraDayCategory.AUSPICIOUS -> state.matrix.filter { it.isAuspicious }
                                        NavtaraDayCategory.MIXED -> state.matrix.filter { it.isNeutral }
                                        NavtaraDayCategory.CAUTION -> state.matrix.filter { !it.isAuspicious && !it.isNeutral }
                                        else -> state.matrix
                                    }
                                    items(filteredMatrix, key = { it.targetNakshatraIndex }) { item ->
                                        val isToday = item.targetNakshatraIndex == state.transitNakshatraNumber
                                        val accent = when {
                                            item.isAuspicious -> vedic.auspicious
                                            item.isNeutral -> vedic.caution
                                            else -> vedic.inauspicious
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                if (isToday) 1.5.dp else 1.dp,
                                                if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { inspectedTara = item }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("${item.targetNakshatraIndex}. ${VedicLocalizer.nakshatraNameByNumber(item.targetNakshatraIndex, lang)}", style = MaterialTheme.typography.titleSmall)
                                                        if (isToday) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            StatusPill(strings.today, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                                        }
                                                    }
                                                    Text(
                                                        text = "${if (lang == AppLanguage.HINDI) item.taraNameHi else item.taraNameEn} · ${strings.paryayaCycleLabel} ${item.paryaya}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                StatusPill(label = "${item.score}%", textColor = accent, containerColor = accent.copy(alpha = 0.14f))
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    item(key = "period_planner") {
                                        NavtaraPeriodPlannerSection(
                                            state = state,
                                            report = state.periodReport,
                                            onSetCustomMode = viewModel::setPeriodModeCustom,
                                            onShiftMonth = viewModel::shiftMonth,
                                            onSetCustomDates = viewModel::setCustomRangeDates,
                                            onApplyPreset = viewModel::applyRangePreset,
                                            onGenerateReport = viewModel::generatePeriodReport,
                                            onSaveReportLocally = { viewModel.saveCurrentPeriodReport(ext.reportSavedSuccessMsg) },
                                            onSyncJsonToDrive = triggerDriveSyncOrConnect,
                                            onExportJson = { saveJsonLauncher.launch(viewModel.suggestedJsonFileName()) },
                                            onOpenDayJournal = { day ->
                                                val sub = "${VedicLocalizer.nakshatraName(day.nakshatraNumber, day.nakshatraName, lang)} · ${if (lang == AppLanguage.HINDI) day.currentTara.taraNameHi else day.currentTara.taraNameEn}"
                                                journalEditTarget = NavtaraJournalEditTarget(day.dateIso, sub)
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    item(key = "drive_json_sync_card") {
                                        NavtaraDriveJsonSyncCard(
                                            cloudUser = state.cloudUser,
                                            driveStatus = state.driveBackupStatus,
                                            onConnectDrive = triggerDriveSyncOrConnect,
                                            onSyncJsonNow = viewModel::syncDriveJsonNow,
                                            onRestoreFromDrive = { viewModel.restoreFromDrive(null) },
                                            onExportJsonFile = { saveJsonLauncher.launch(viewModel.suggestedJsonFileName()) },
                                            onImportJsonFile = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                                            onLinkDriveFile = { linkDriveFileLauncher.launch(viewModel.suggestedJsonFileName()) }
                                        )
                                    }
                                    item(key = "journal_history_card") {
                                        NavtaraJournalHistoryCard(
                                            feedbacksByDate = state.feedbacksByDate,
                                            selectedFilter = state.journalFilter,
                                            onSelectFilter = viewModel::setJournalFilter,
                                            onEditFeedback = { fb ->
                                                journalEditTarget = NavtaraJournalEditTarget(fb.dateIso, state.nativeName)
                                            },
                                            onDeleteFeedback = viewModel::deleteDailyFeedback
                                        )
                                    }
                                    item(key = "saved_reports") {
                                        NavtaraSavedReportsSection(
                                            savedReports = state.savedReports,
                                            onOpenReport = viewModel::openSavedReport,
                                            onExportJson = { saveJsonLauncher.launch(viewModel.suggestedJsonFileName()) },
                                            onDeleteReport = viewModel::deleteSavedReport
                                        )
                                    }
                                    item(key = "saved_tab_tara_dasa") {
                                        TaraDasaTableCard(birthNakshatra = birthNak)
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            inspectedTara?.let { res ->
                val accent = when {
                    res.isAuspicious -> vedic.auspicious
                    res.isNeutral -> vedic.caution
                    else -> vedic.inauspicious
                }
                InspectorBottomSheetFrame(
                    title = if (lang == AppLanguage.HINDI) res.taraNameHi else res.taraNameEn,
                    subtitle = "${res.targetNakshatraIndex}. ${VedicLocalizer.nakshatraNameByNumber(res.targetNakshatraIndex, lang)}",
                    icon = Icons.Rounded.Stars,
                    accentColor = accent,
                    statusLabel = if (lang == AppLanguage.HINDI) res.natureHi else res.natureEn,
                    sheetTestTag = "navtara_sector_inspector_sheet",
                    onDismiss = { inspectedTara = null }
                ) {
                    AuspiciousnessMeterBar(
                        progress = res.score / 100f,
                        accentColor = accent,
                        label = strings.taraScoreLabel,
                        trailingValue = "${res.score}/100"
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryStatTile(strings.paryayaCycleLabel, "${res.paryaya}", Modifier.weight(1f))
                        TelemetryStatTile(
                            label = if (lang == AppLanguage.HINDI) "नक्षत्र दूरी" else "Distance",
                            value = "#${res.distance} / 27",
                            valueColor = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (lang == AppLanguage.HINDI) res.descriptionHi else res.descriptionEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    res.parihara?.let { p ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(strings.pariharaRemediesTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${strings.deitiesPrefix}: ${if (lang == AppLanguage.HINDI) p.deityHi else p.deityEn}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Mantra: ${if (lang == AppLanguage.HINDI) p.mantraHi else p.mantraEn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("${strings.charityDaanLabel}: ${if (lang == AppLanguage.HINDI) p.donationHi else p.donationEn}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (canJournalSave) {
        editingProfileDialog?.let { (_, profile) ->
            NavtaraProfileEditorDialog(
                initialProfile = profile,
                defaultNakshatra = state.preferences.birthNakshatra,
                defaultPada = state.birthPada,
                defaultName = state.nativeName,
                onDismiss = { editingProfileDialog = null },
                onSave = { id, name, nak, pada, notes, makeDefault ->
                    viewModel.saveOrUpdateProfile(id, name, nak, pada, notes, makeDefault)
                    editingProfileDialog = null
                }
            )
        }

        journalEditTarget?.let { target ->
            val existing = state.feedbacksByDate[target.dateIso]
            NavtaraJournalEntryDialog(
                dateIso = target.dateIso,
                subtitle = target.subtitle,
                existing = existing,
                onDismiss = { journalEditTarget = null },
                onSave = { rating, note ->
                    viewModel.saveDailyFeedback(target.dateIso, rating, note)
                    journalEditTarget = null
                },
                onDelete = if (existing != null) {
                    {
                        viewModel.deleteDailyFeedback(target.dateIso)
                        journalEditTarget = null
                    }
                } else null
            )
        }
    }

    if (showInspectDatePicker) {
        val initMillis = state.inspectDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showInspectDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.setInspectDate(picked)
                        }
                        showInspectDatePicker = false
                    }
                ) { Text(strings.select) }
            },
            dismissButton = {
                TextButton(onClick = { showInspectDatePicker = false }) { Text(strings.cancel) }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(strings.selectBirthNakshatraLabel) },
            text = {
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    items((1..27).toList(), key = { it }) { num ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBirthNakshatra(num)
                                    showPicker = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.preferences.birthNakshatra == num,
                                onClick = {
                                    viewModel.setBirthNakshatra(num)
                                    showPicker = false
                                }
                            )
                            Text("$num. ${VedicLocalizer.nakshatraNameByNumber(num, lang)}")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text(strings.cancel) }
            }
        )
    }

    if (glossaryQuery != null && policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)) {
        VedicGlossaryBottomSheet(
            initialQuery = glossaryQuery.orEmpty(),
            onDismiss = { glossaryQuery = null }
        )
    }
}

@Composable
private fun NavtaraBirthSelectorCard(
    nativeName: String,
    birthNak: Int,
    birthPada: Int,
    onNameChange: (String) -> Unit,
    onPickNakshatra: () -> Unit,
    onSelectPada: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    PanchangaCard {
        OutlinedTextField(
            value = nativeName,
            onValueChange = onNameChange,
            label = { Text(ext.nativeNameHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("navtara_native_name")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(strings.selectBirthNakshatraLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onPickNakshatra)
                .testTag("navtara_birth_picker"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$birthNak. ${VedicLocalizer.nakshatraNameByNumber(birthNak, lang)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = strings.select)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(ext.birthPadaLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            (1..4).forEachIndexed { idx, p ->
                SegmentedButton(
                    selected = birthPada == p,
                    onClick = { onSelectPada(p) },
                    shape = SegmentedButtonDefaults.itemShape(idx, 4),
                    modifier = Modifier.testTag("navtara_pada_$p")
                ) { Text("${strings.padaLabel} $p") }
            }
        }
    }
}

@Composable
private fun NavtaraVerdictHeroCard(todayRes: NavtaraResult) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val accent = when {
        todayRes.isAuspicious -> vedic.auspicious
        todayRes.isNeutral -> vedic.caution
        else -> vedic.inauspicious
    }
    val container = when {
        todayRes.isAuspicious -> vedic.auspiciousContainer
        todayRes.isNeutral -> vedic.cautionContainer
        else -> vedic.inauspiciousContainer
    }

    AtmosphericHeroCard(accentBorder = accent.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${strings.todayTransitNakshatraLabel}: ${VedicLocalizer.nakshatraNameByNumber(todayRes.targetNakshatraIndex, lang)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == AppLanguage.HINDI) todayRes.taraNameHi else todayRes.taraNameEn,
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent
                )
            }
            StatusPill(
                label = if (lang == AppLanguage.HINDI) todayRes.natureHi else todayRes.natureEn,
                textColor = accent,
                containerColor = container
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        AuspiciousnessMeterBar(
            progress = todayRes.score / 100f,
            accentColor = accent,
            label = "${strings.paryayaCycleLabel}: ${todayRes.paryaya} · #${todayRes.distance}/27",
            trailingValue = "${todayRes.score}/100"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(if (lang == AppLanguage.HINDI) todayRes.descriptionHi else todayRes.descriptionEn, style = MaterialTheme.typography.bodyMedium)
    }
}
