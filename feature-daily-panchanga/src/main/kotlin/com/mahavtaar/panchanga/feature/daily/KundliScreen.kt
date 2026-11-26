package com.mahavtaar.panchanga.feature.daily

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.BirthGender
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.SavedKundliChart
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.export.OfflineReportExportHelper
import com.mahavtaar.panchanga.ui.export.TimezoneCatalog
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundliScreen(
    onNavigateBack: () -> Unit,
    initialTab: KundliTab = KundliTab.CHARTS,
    viewModel: KundliViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val context = LocalContext.current
    val policy = LocalAccessPolicy.current
    val canKundliPage = policy.canAccessPage(AppPageKey.KUNDLI)
    val canGunaMilan = policy.canAccessPage(AppPageKey.GUNA_MILAN)
    val canAdvancedTabs = policy.hasFeature(AppFeatureKey.KUNDLI_ADVANCED_TABS)
    val canSaveImport = policy.hasFeature(AppFeatureKey.KUNDLI_SAVE_IMPORT)
    val canCustomCoords = policy.hasFeature(AppFeatureKey.CUSTOM_COORDINATES)
    val snackbarHostState = remember { SnackbarHostState() }
    var editorExpanded by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) { viewModel.setInitialTab(initialTab) }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            OfflineReportExportHelper.readTextFromUri(context, uri)
                .onSuccess { raw -> viewModel.importKundliJson(raw) }
                .onFailure { err -> viewModel.setStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    val tabs = remember(canKundliPage, canGunaMilan, canAdvancedTabs, strings, ext) {
        if (!canKundliPage && canGunaMilan) {
            listOf(KundliTab.GUNA_MILAN to strings.tabGunaMilan)
        } else {
            buildList {
                add(KundliTab.CHARTS to strings.tabChart)
                add(KundliTab.PLANETS to strings.tabPlanets)
                add(KundliTab.DASHA to strings.tabDasha)
                add(KundliTab.VARGAS to strings.tabVargas)
                if (canAdvancedTabs) {
                    add(KundliTab.SHADBALA to strings.tabShadbala)
                    add(KundliTab.ASHTAKAVARGA to strings.tabAshtakavarga)
                    add(KundliTab.KP to ext.tabKpAstrology)
                    add(KundliTab.JAIMINI to ext.tabJaimini)
                }
                add(KundliTab.YOGAS to strings.doshaAndYogaTitle)
                add(KundliTab.GOCHAR to strings.tabGochar)
                add(KundliTab.AUDIT_EXPORT to ext.tabAuditExport)
                if (canGunaMilan) {
                    add(KundliTab.GUNA_MILAN to strings.tabGunaMilan)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.kundliTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("kundli_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (canSaveImport && canKundliPage) {
                        IconButton(
                            onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                            modifier = Modifier.testTag("kundli_topbar_import_json")
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = ext.importJsonFile, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    LanguageTogglePill(onToggleLanguage = viewModel::toggleLanguage, modifier = Modifier.padding(end = 12.dp))
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
                modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("kundli_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.selectedTab != KundliTab.GUNA_MILAN) {
                    item(key = "birth_profile") {
                        BirthProfileCard(
                            state = state,
                            expanded = editorExpanded,
                            canSaveImport = canSaveImport,
                            onToggleExpand = { editorExpanded = !editorExpanded },
                            onUpdateInput = viewModel::updateBirthInput,
                            onOpenCityPicker = { showCityPicker = true },
                            onSelectCity = viewModel::selectBirthCity,
                            onRectify = viewModel::rectifyMinutes,
                            onSetNow = viewModel::setBirthNow,
                            onSaveProfile = viewModel::saveCurrentProfile,
                            onLoadChart = viewModel::loadSavedChart,
                            onDeleteChart = viewModel::deleteSavedChart,
                            onImportJson = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                            onCompute = {
                                editorExpanded = false
                                viewModel.computeKundli()
                            }
                        )
                    }
                }

                item(key = "tabs") {
                    LazyRow(
                        modifier = Modifier.testTag("kundli_tabs_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs, key = { it.first.name }) { (tab, label) ->
                            FilterChip(
                                selected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("kundli_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                if (state.selectedTab == KundliTab.GUNA_MILAN) {
                    item(key = "guna_milan_section") {
                        GunaMilanSection(
                            state = state,
                            onUpdate = viewModel::updateGunaMilan
                        )
                    }
                } else {
                    when {
                        state.isLoading -> item(key = "loading") { LoadingStateBox() }
                        state.errorMessage != null -> item(key = "error") {
                            ErrorStateCard(state.errorMessage!!, onRetry = viewModel::computeKundli)
                        }
                        state.report != null -> {
                            val rep = state.report!!
                            when (state.selectedTab) {
                                KundliTab.CHARTS -> item(key = "charts") {
                                    KundliChartsSection(rep, state.chartStyle, viewModel::setChartStyle)
                                }
                                KundliTab.PLANETS -> item(key = "planets") { KundliPlanetsSection(rep) }
                                KundliTab.DASHA -> item(key = "dasha") { KundliDashaSection(rep) }
                                KundliTab.VARGAS -> item(key = "vargas") {
                                    KundliVargasSection(rep, state.selectedVargaCode, state.chartStyle, viewModel::selectVarga)
                                }
                                KundliTab.SHADBALA -> item(key = "shadbala") { KundliShadbalaSection(rep) }
                                KundliTab.ASHTAKAVARGA -> item(key = "av") { KundliAshtakavargaSection(rep) }
                                KundliTab.KP -> item(key = "kp") { KundliKpSection(rep) }
                                KundliTab.JAIMINI -> item(key = "jaimini") { KundliJaiminiSection(rep) }
                                KundliTab.YOGAS -> item(key = "yogas") { KundliYogasSection(rep) }
                                KundliTab.GOCHAR -> item(key = "gochar") {
                                    KundliGocharSection(
                                        report = rep,
                                        selectedDate = state.selectedTransitDate,
                                        gocharReport = state.customGocharReport,
                                        onSelectDate = viewModel::updateGocharDate,
                                        onResetToday = viewModel::resetGocharToday
                                    )
                                }
                                KundliTab.AUDIT_EXPORT -> item(key = "audit") {
                                    KundliAuditExportSection(
                                        report = rep,
                                        onImportJson = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                                        onStatusMessage = viewModel::setStatusMessage
                                    )
                                }
                                KundliTab.GUNA_MILAN -> Unit
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            initialLocation = state.input.location,
            query = state.citySearchQuery,
            savedLocations = state.savedLocations,
            cities = state.cityResults,
            allowCustomCoordinates = canCustomCoords,
            onQueryChange = viewModel::searchCities,
            onSelectCity = { city ->
                viewModel.selectBirthCity(city)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BirthProfileCard(
    state: KundliUiState,
    expanded: Boolean,
    canSaveImport: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateInput: ((KundliBirthInput) -> KundliBirthInput) -> Unit,
    onOpenCityPicker: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onRectify: (Int) -> Unit,
    onSetNow: () -> Unit,
    onSaveProfile: () -> Unit,
    onLoadChart: (SavedKundliChart) -> Unit,
    onDeleteChart: (Long) -> Unit,
    onImportJson: () -> Unit,
    onCompute: () -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val lang = LocalAppLanguage.current
    val isHi = lang == com.mahavtaar.panchanga.domain.model.AppLanguage.HINDI
    val input = state.input
    var yearStr by remember(input.year) { mutableStateOf(input.year.toString()) }
    var monthStr by remember(input.month) { mutableStateOf(input.month.toString()) }
    var dayStr by remember(input.day) { mutableStateOf(input.day.toString()) }
    var hourStr by remember(input.hour) { mutableStateOf(input.hour.toString()) }
    var minStr by remember(input.minute) { mutableStateOf(input.minute.toString()) }

    val quickCities = remember(state.savedLocations, state.cityResults, state.preferences.selectedLocation) {
        val seen = HashSet<String>()
        buildList {
            val all = buildList {
                add(state.preferences.selectedLocation)
                addAll(state.savedLocations)
                addAll(state.cityResults.take(12))
            }
            for (c in all) {
                val key = c.name.trim().lowercase(Locale.US)
                if (seen.add(key)) add(c)
            }
        }
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.birthDetailsTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${input.name} · %02d/%02d/%04d · %02d:%02d".format(Locale.US, input.day, input.month, input.year, input.hour, input.minute),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${input.location.name} (${"%.5f°, %.5f°".format(Locale.US, input.location.latitude, input.location.longitude)} · ${input.location.timezone})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onToggleExpand, modifier = Modifier.testTag("kundli_edit_birth_button")) {
                Icon(Icons.Rounded.Edit, contentDescription = ext.cityPickerTitle, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isHi) "तिथि/समय" else "Date/Time")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Always-visible Birth Location Selector Button + Quick City Chips (from saved & popular app cities)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOpenCityPicker,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_city_picker_button")
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = ext.cityPickerTitle, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${ext.kundliPlaceHint}: ${input.location.name}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canSaveImport) {
                OutlinedButton(
                    onClick = onSaveProfile,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("kundli_quick_save_profile_button")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = ext.saveProfileButton, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHi) "सेव करें" else "Save", maxLines = 1)
                }
            }
        }

        if (quickCities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isHi) "जन्म स्थान चुनें (सहेजे गए व प्रमुख शहर):" else "Select Birth Place (Saved & Major Cities):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("kundli_quick_cities_row")
            ) {
                items(quickCities, key = { "quick_${it.name}_${it.latitude}" }) { city ->
                    val isCurrent = city.name.equals(input.location.name, ignoreCase = true)
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onSelectCity(city) },
                        label = { Text(city.name, maxLines = 1) },
                        leadingIcon = if (isCurrent) {
                            {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        if (canSaveImport && state.savedCharts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(ext.savedProfilesTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.savedCharts, key = { it.id }) { chart ->
                    val p = chart.input
                    val isSelected = p.name == input.name && p.year == input.year && p.month == input.month &&
                        p.day == input.day && p.hour == input.hour && p.minute == input.minute
                    val lagnaShort = VedicLocalizer.rasiShortByZeroIndex(chart.lagnaSignIndex, lang)
                    val nakShort = VedicLocalizer.nakshatraNameByNumber(chart.moonNakshatraNumber, lang).take(8)
                    InputChip(
                        selected = isSelected,
                        onClick = { onLoadChart(chart) },
                        label = {
                            Text("${p.name} · ${p.location.name} ($lagnaShort · $nakShort)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingIcon = {
                            IconButton(onClick = { onDeleteChart(chart.id) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = ext.closeLabel, modifier = Modifier.size(14.dp))
                            }
                        }
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = input.name,
                onValueChange = { v -> onUpdateInput { it.copy(name = v) } },
                label = { Text(ext.kundliNameHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("kundli_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(ext.birthGenderLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = input.gender == BirthGender.MALE, onClick = { onUpdateInput { it.copy(gender = BirthGender.MALE) } }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(ext.genderMale) }
                SegmentedButton(selected = input.gender == BirthGender.FEMALE, onClick = { onUpdateInput { it.copy(gender = BirthGender.FEMALE) } }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(ext.genderFemale) }
                SegmentedButton(selected = input.gender == BirthGender.UNSPECIFIED, onClick = { onUpdateInput { it.copy(gender = BirthGender.UNSPECIFIED) } }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(ext.genderOther) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dayStr, onValueChange = { v -> dayStr = v.filter { it.isDigit() }.take(2); dayStr.toIntOrNull()?.let { d -> onUpdateInput { it.copy(day = d.coerceIn(1, 31)) } } }, label = { Text("DD") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthStr, onValueChange = { v -> monthStr = v.filter { it.isDigit() }.take(2); monthStr.toIntOrNull()?.let { m -> onUpdateInput { it.copy(month = m.coerceIn(1, 12)) } } }, label = { Text("MM") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = yearStr, onValueChange = { v -> yearStr = v.filter { it.isDigit() }.take(4); yearStr.toIntOrNull()?.let { y -> if (y in 1900..2100) onUpdateInput { it.copy(year = y) } } }, label = { Text("YYYY") }, singleLine = true, modifier = Modifier.weight(1.3f))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = hourStr, onValueChange = { v -> hourStr = v.filter { it.isDigit() }.take(2); hourStr.toIntOrNull()?.let { h -> onUpdateInput { it.copy(hour = h.coerceIn(0, 23)) } } }, label = { Text(ext.kundliHourLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = minStr, onValueChange = { v -> minStr = v.filter { it.isDigit() }.take(2); minStr.toIntOrNull()?.let { m -> onUpdateInput { it.copy(minute = m.coerceIn(0, 59)) } } }, label = { Text(ext.kundliMinuteLabel) }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(ext.rectifyTimeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(-10, -1, 1, 10).forEach { delta ->
                    TextButton(onClick = { onRectify(delta) }, modifier = Modifier.testTag("kundli_rectify_$delta")) {
                        Text(if (delta > 0) "+${delta}m" else "${delta}m")
                    }
                }
                TextButton(onClick = onSetNow, modifier = Modifier.testTag("kundli_rectify_now")) {
                    Text(strings.now)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canSaveImport) {
                    OutlinedButton(
                        onClick = onSaveProfile,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_save_profile_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(ext.saveProfileButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(
                    onClick = onCompute,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_compute_button")
                ) {
                    Text(strings.computeChartButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (canSaveImport) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onImportJson,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("kundli_profile_import_json")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.importJsonFile)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPickerDialog(
    initialLocation: CityLocation,
    query: String,
    savedLocations: List<CityLocation>,
    cities: List<CityLocation>,
    allowCustomCoordinates: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalExtendedStrings.current
    val strings = LocalStrings.current
    val isHi = LocalAppLanguage.current == com.mahavtaar.panchanga.domain.model.AppLanguage.HINDI
    var pickerTab by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf(initialLocation.name) }
    var customLat by remember { mutableStateOf("%.5f".format(Locale.US, initialLocation.latitude)) }
    var customLon by remember { mutableStateOf("%.5f".format(Locale.US, initialLocation.longitude)) }
    var customTz by remember { mutableStateOf(initialLocation.timezone) }
    var tzQuery by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val tzOptions = remember(tzQuery) { TimezoneCatalog.filterOptions(tzQuery, limit = 30) }
    val savedNamesLower = remember(savedLocations) {
        savedLocations.map { it.name.trim().lowercase(Locale.US) }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ext.cityPickerTitle) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                if (allowCustomCoordinates) {
                    PrimaryTabRow(selectedTabIndex = pickerTab) {
                        Tab(
                            selected = pickerTab == 0,
                            onClick = { pickerTab = 0 },
                            modifier = Modifier.testTag("kundli_picker_tab_search"),
                            text = { Text(ext.searchCityHint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                        Tab(
                            selected = pickerTab == 1,
                            onClick = { pickerTab = 1 },
                            modifier = Modifier.testTag("kundli_picker_tab_custom"),
                            text = { Text(ext.customCoordinatesTab, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!allowCustomCoordinates || pickerTab == 0) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(ext.searchCityHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("city_picker_search")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cities, key = { "${it.name}_${it.latitude}_${it.longitude}" }) { city ->
                            val isSavedCity = city.name.trim().lowercase(Locale.US) in savedNamesLower || city.isCustom
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSavedCity) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSavedCity) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth().clickable { onSelectCity(city) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${city.timezone} · ${"%.5f°, %.5f°".format(Locale.US, city.latitude, city.longitude)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSavedCity) {
                                        Text(
                                            text = if (isHi) "सहेजा गया" else "Saved",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it; errorMsg = null },
                            label = { Text(ext.locationLabelHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("kundli_custom_name_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customLat,
                                onValueChange = { customLat = it; errorMsg = null },
                                label = { Text(ext.latitudeHint) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("kundli_custom_lat_input")
                            )
                            OutlinedTextField(
                                value = customLon,
                                onValueChange = { customLon = it; errorMsg = null },
                                label = { Text(ext.longitudeHint) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("kundli_custom_lon_input")
                            )
                        }
                        OutlinedTextField(
                            value = tzQuery,
                            onValueChange = { tzQuery = it },
                            label = { Text("${ext.timezonePickerTitle}: $customTz") },
                            placeholder = { Text(ext.searchTimezoneHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("kundli_custom_tz_search")
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tzOptions.forEach { opt ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (opt.id == customTz) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth().clickable { customTz = opt.id }
                                ) {
                                    Text(opt.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                }
                            }
                        }
                        errorMsg?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                val cleanName = customName.trim()
                                val lat = TimezoneCatalog.parseLatitude(customLat)
                                val lon = TimezoneCatalog.parseLongitude(customLon)
                                when {
                                    cleanName.isBlank() -> errorMsg = ext.invalidLocationNameMsg
                                    lat == null -> errorMsg = ext.invalidLatitudeMsg
                                    lon == null -> errorMsg = ext.invalidLongitudeMsg
                                    else -> onSelectCity(
                                        CityLocation(
                                            name = cleanName,
                                            country = "Custom",
                                            latitude = lat,
                                            longitude = lon,
                                            timezone = customTz,
                                            isCustom = true
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("kundli_custom_apply_button")
                        ) {
                            Text(ext.saveAndApplyLabel)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
