package com.mahavtaar.panchanga.feature.daily

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavagrahaRemedy
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavagrahaMantrasViewModel @Inject constructor(
    astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _remedies = MutableStateFlow(astrologyRepository.getNavagrahaRemedies())
    val remedies: StateFlow<List<NavagrahaRemedy>> = _remedies.asStateFlow()

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = preferencesRepository.preferences.first().language
            preferencesRepository.setLanguage(current.toggled())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavagrahaMantrasScreen(
    onNavigateBack: () -> Unit,
    viewModel: NavagrahaMantrasViewModel = hiltViewModel()
) {
    val remedies by viewModel.remedies.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    var selectedId by remember { mutableStateOf(remedies.firstOrNull()?.id ?: "sun") }
    val selected = remember(remedies, selectedId) {
        remedies.firstOrNull { it.id == selectedId } ?: remedies.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navagrahaMantrasTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("mantras_back_button")) {
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
                    .testTag("navagraha_mantras_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "selector") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(remedies, key = { it.id }) { graha ->
                            val isSelected = graha.id == selected.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedId = graha.id }
                                    .testTag("mantra_graha_${graha.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(graha.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = if (lang == AppLanguage.HINDI) graha.nameHi else graha.nameEn.substringBefore(" —"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "header_card") {
                    PanchangaCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) selected.nameHi else selected.nameEn,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (lang == AppLanguage.HINDI) selected.nameEn else selected.nameHi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(selected.symbol, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        AttributePairRow(
                            strings.deitiesPrefix,
                            if (lang == AppLanguage.HINDI) selected.deityHi else selected.deityEn,
                            strings.gemstoneLabel,
                            if (lang == AppLanguage.HINDI) selected.gemstoneHi else selected.gemstoneEn
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AttributePairRow(
                            strings.directionLabel,
                            if (lang == AppLanguage.HINDI) selected.directionHi else selected.directionEn,
                            strings.samidhaLabel,
                            if (lang == AppLanguage.HINDI) selected.samidhaHi else selected.samidhaEn
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AttributePairRow(
                            strings.fastDayLabel,
                            if (lang == AppLanguage.HINDI) selected.fastDayHi else selected.fastDayEn,
                            strings.japCountLabel,
                            if (lang == AppLanguage.HINDI) selected.japCountHi else selected.japCountEn
                        )
                    }
                }

                item(key = "beej") {
                    MantraBlockCard(title = strings.beejMantraLabel, mantra = selected.beejMantra, highlight = true)
                }

                item(key = "dhyan") {
                    MantraBlockCard(title = strings.dhyanMantraLabel, mantra = selected.dhyanMantra, highlight = false)
                }

                item(key = "gayatri") {
                    MantraBlockCard(title = strings.gayatriMantraLabel, mantra = selected.gayatriMantra, highlight = false)
                }

                item(key = "vedokta") {
                    MantraBlockCard(title = strings.vedoktaMantraLabel, mantra = selected.vedoktaMantra, highlight = false)
                }

                item(key = "daan") {
                    PanchangaCard {
                        Text(strings.charityDaanLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) selected.daanHi else selected.daanEn,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AttributePairRow(l1: String, v1: String, l2: String, v2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(l1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(v1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(l2, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(v2, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MantraBlockCard(title: String, mantra: String, highlight: Boolean) {
    PanchangaCard(
        containerColor = if (highlight) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mantra,
            style = MaterialTheme.typography.titleMedium,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight
        )
    }
}
