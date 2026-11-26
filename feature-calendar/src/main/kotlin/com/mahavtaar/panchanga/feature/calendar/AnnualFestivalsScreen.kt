package com.mahavtaar.panchanga.feature.calendar

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.domain.model.AnnualFestivalItem
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class AnnualFestivalsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val year: Int = LocalDate.now().year,
    val selectedCategory: String = "all",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<AnnualFestivalItem> = emptyList()
)

@HiltViewModel
class AnnualFestivalsViewModel @Inject constructor(
    private val astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnualFestivalsUiState())
    val uiState: StateFlow<AnnualFestivalsUiState> = _uiState.asStateFlow()
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                triggerLoadIfNeeded(prefs, _uiState.value.year)
            }
        }
    }

    private fun triggerLoadIfNeeded(prefs: UserPreferences, year: Int) {
        val loc = prefs.selectedLocation
        val key = "${loc.name}|${"%.5f".format(Locale.US, loc.latitude)}|${"%.5f".format(Locale.US, loc.longitude)}|${loc.timezone}|$year|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}"
        if (key == lastComputeKey) return
        lastComputeKey = key
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                astrologyRepository.getAnnualFestivals(
                    location = prefs.selectedLocation,
                    year = year,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha
                )
            }.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, items = list) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Failed to load festivals") }
            }
        }
    }

    fun previousYear() {
        val nextYear = (_uiState.value.year - 1).coerceAtLeast(1950)
        _uiState.update { it.copy(year = nextYear) }
        triggerLoadIfNeeded(_uiState.value.preferences, nextYear)
    }

    fun nextYear() {
        val nextYear = (_uiState.value.year + 1).coerceAtMost(2099)
        _uiState.update { it.copy(year = nextYear) }
        triggerLoadIfNeeded(_uiState.value.preferences, nextYear)
    }

    fun selectCategory(cat: String) = _uiState.update { it.copy(selectedCategory = cat) }

    fun onSearchQueryChanged(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun retry() {
        lastComputeKey = null
        triggerLoadIfNeeded(_uiState.value.preferences, _uiState.value.year)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualFestivalsScreen(
    onNavigateBack: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    viewModel: AnnualFestivalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val showEventDetails = policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    var expandedId by remember { mutableStateOf<String?>(null) }
    var inspectedFestival by remember { mutableStateOf<AnnualFestivalItem?>(null) }

    val categories = remember(lang) {
        if (lang == AppLanguage.HINDI) {
            listOf(
                "all" to "सभी",
                "festival" to "प्रमुख पर्व",
                "ekadashi" to "एकादशी",
                "purnima" to "पूर्णिमा",
                "amavasya" to "अमावस्या",
                "pradosham" to "प्रदोष",
                "chaturthi" to "चतुर्थी",
                "shivaratri" to "शिवरात्रि"
            )
        } else {
            listOf(
                "all" to "All",
                "festival" to "Major Festivals",
                "ekadashi" to "Ekadashi",
                "purnima" to "Purnima",
                "amavasya" to "Amavasya",
                "pradosham" to "Pradosham",
                "chaturthi" to "Chaturthi",
                "shivaratri" to "Shivaratri"
            )
        }
    }

    val filtered = remember(state.items, state.selectedCategory, state.searchQuery, lang) {
        val q = state.searchQuery.trim().lowercase()
        state.items.filter { item ->
            val catOk = state.selectedCategory == "all" || item.observance.category.equals(state.selectedCategory, true)
            val queryOk = q.isEmpty() ||
                item.observance.title.lowercase().contains(q) ||
                VedicLocalizer.observanceTitle(item.observance, lang).lowercase().contains(q) ||
                item.masaName.lowercase().contains(q) ||
                item.tithiName.lowercase().contains(q)
            catOk && queryOk
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.festivalsTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("festivals_back_button")) {
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
                    .testTag("annual_festivals_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "year_bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::previousYear, modifier = Modifier.testTag("festivals_prev_year")) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.previousMonthCd)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.year}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            val loc = state.preferences.selectedLocation
                            Text(
                                text = "${loc.name} (${"%.4f°, %.4f°".format(Locale.US, loc.latitude, loc.longitude)}) · ${VedicLocalizer.monthSystemShort(state.preferences.monthSystem, lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = viewModel::nextYear, modifier = Modifier.testTag("festivals_next_year")) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextMonthCd)
                        }
                    }
                }

                item(key = "search") {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text(strings.searchFestivalsHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("festivals_search_input")
                    )
                }

                item(key = "filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.first }) { (key, label) ->
                            FilterChip(
                                selected = state.selectedCategory == key,
                                onClick = { viewModel.selectCategory(key) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("festival_filter_$key")
                            )
                        }
                    }
                }

                when {
                    state.isLoading -> item(key = "loading") {
                        LoadingStateBox(message = strings.loadingAnnualFestivals)
                    }
                    state.errorMessage != null -> item(key = "error") {
                        ErrorStateCard(state.errorMessage!!, onRetry = viewModel::retry)
                    }
                    filtered.isEmpty() -> item(key = "empty") {
                        PanchangaCard {
                            Text(strings.noFestivalsMatched, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    else -> {
                        items(filtered, key = { it.id }) { item ->
                            val isExpanded = expandedId == item.id
                            val monthLabel = VedicLocalizer.gregorianMonthName(item.monthValue, lang)
                            val masaDisplay = VedicLocalizer.masaName(0, false, false, item.masaName, lang)
                            val pakshaDisplay = VedicLocalizer.pakshaName(item.paksha, lang)
                            val tithiDisplay = VedicLocalizer.tithiName(item.tithiNumber, item.tithiName, lang)
                            val significance = VedicGlossaryCatalog.observanceSignificance(item.observance, lang)
                            val practices = VedicGlossaryCatalog.observancePractices(item.observance, lang)

                            ExpandableInfoCard(
                                title = VedicLocalizer.observanceTitle(item.observance, lang),
                                subtitle = "${VedicLocalizer.observanceSubtitle(item.observance, lang)} · ${VedicLocalizer.observanceDeity(item.observance, lang)}",
                                expanded = isExpanded,
                                onToggleExpand = { expandedId = if (isExpanded) null else item.id },
                                accentColor = MaterialTheme.colorScheme.secondary,
                                trailingBadge = {
                                    StatusPill(
                                        label = "${item.dayOfMonth} ${monthLabel.take(3)}",
                                        textColor = MaterialTheme.colorScheme.secondary,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                },
                                summaryContent = {
                                    Text(
                                        text = "$masaDisplay · $pakshaDisplay · $tithiDisplay",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (showEventDetails) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${ext.significanceLabel}: $significance",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            ) {
                                if (showEventDetails && practices.isNotEmpty()) {
                                    Text(
                                        text = ext.recommendedPracticesLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    practices.forEach { step ->
                                        Text(
                                            text = "✓ $step",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { inspectedFestival = item },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.details)
                                    }
                                    if (canNotifs) {
                                        OutlinedButton(
                                            onClick = onOpenNotificationStudio,
                                            modifier = Modifier.weight(1f).testTag("festival_alert_btn_${item.id}")
                                        ) {
                                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(ext.addVratReminder)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            inspectedFestival?.let { item ->
                val monthLabel = VedicLocalizer.gregorianMonthName(item.monthValue, lang)
                val masaDisplay = VedicLocalizer.masaName(0, false, false, item.masaName, lang)
                val pakshaDisplay = VedicLocalizer.pakshaName(item.paksha, lang)
                val tithiDisplay = VedicLocalizer.tithiName(item.tithiNumber, item.tithiName, lang)
                val significance = VedicGlossaryCatalog.observanceSignificance(item.observance, lang)
                val practices = VedicGlossaryCatalog.observancePractices(item.observance, lang)

                InspectorBottomSheetFrame(
                    title = VedicLocalizer.observanceTitle(item.observance, lang),
                    subtitle = "${VedicLocalizer.observanceSubtitle(item.observance, lang)} · ${VedicLocalizer.observanceDeity(item.observance, lang)}",
                    icon = Icons.Rounded.AutoAwesome,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    statusLabel = "${item.dayOfMonth} ${monthLabel.take(3)}",
                    sheetTestTag = "annual_festival_inspector_sheet",
                    onDismiss = { inspectedFestival = null },
                    footerActions = if (canNotifs) {
                        {
                            OutlinedButton(
                                onClick = {
                                    inspectedFestival = null
                                    onOpenNotificationStudio()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.addVratReminder)
                            }
                        }
                    } else null
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryStatTile(strings.tithiLabel, "$pakshaDisplay · $tithiDisplay", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
                        TelemetryStatTile(
                            label = if (lang == AppLanguage.HINDI) "मास" else "Lunar Masa",
                            value = masaDisplay,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showEventDetails) {
                        Text(
                            text = "${ext.significanceLabel}: $significance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (practices.isNotEmpty()) {
                            Text(ext.recommendedPracticesLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            practices.forEach { step ->
                                Text("✓ $step", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
