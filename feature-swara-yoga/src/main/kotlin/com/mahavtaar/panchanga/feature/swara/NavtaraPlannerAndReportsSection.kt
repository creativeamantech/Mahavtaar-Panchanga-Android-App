package com.mahavtaar.panchanga.feature.swara

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodDay
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NavtaraProfilesCard(
    profiles: List<NavtaraProfile>,
    activeProfileId: Long?,
    onSelectProfile: (NavtaraProfile) -> Unit,
    onAddProfileClick: () -> Unit,
    onEditProfileClick: (NavtaraProfile) -> Unit,
    onPinDefault: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val canJournalSave = LocalAccessPolicy.current.hasFeature(AppFeatureKey.NAVTATA_JOURNAL_SAVE)
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ext.navtaraProfilesTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (canJournalSave) {
                OutlinedButton(
                    onClick = onAddProfileClick,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("navtara_add_profile_button")
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.addProfileLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (profiles.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ext.noSavedProfiles,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("navtara_profiles_row")
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == activeProfileId
                    val nakName = VedicLocalizer.nakshatraNameByNumber(profile.birthNakshatraIndex, lang)
                    InputChip(
                        selected = isSelected,
                        onClick = { onSelectProfile(profile) },
                        label = {
                            Text(
                                text = "${if (profile.isDefault) "★ " else ""}${profile.name} ($nakName·P${profile.birthPada})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("navtara_profile_chip_${profile.id}")
                    )
                }
            }

            if (activeProfile != null && canJournalSave) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onEditProfileClick(activeProfile) },
                        label = { Text(ext.editProfileLabel) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("navtara_edit_profile_button")
                    )
                    if (!activeProfile.isDefault) {
                        FilterChip(
                            selected = false,
                            onClick = { onPinDefault(activeProfile.id) },
                            label = { Text(ext.setDefaultLabel) },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("navtara_pin_default_profile_button")
                        )
                    } else {
                        StatusPill(
                            label = ext.defaultBadgeLabel,
                            textColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { onDeleteProfile(activeProfile.id) },
                        label = { Text(ext.actionDeleteLabel) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("navtara_delete_profile_button")
                    )
                }
            }
        }
    }
}

@Composable
internal fun NavtaraProfileEditorDialog(
    initialProfile: NavtaraProfile?,
    defaultNakshatra: Int,
    defaultPada: Int,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, birthNak: Int, pada: Int, notes: String, makeDefault: Boolean) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    var name by remember(initialProfile) { mutableStateOf(initialProfile?.name ?: defaultName) }
    var birthNak by remember(initialProfile) { mutableIntStateOf(initialProfile?.birthNakshatraIndex ?: defaultNakshatra) }
    var pada by remember(initialProfile) { mutableIntStateOf(initialProfile?.birthPada ?: defaultPada) }
    var notes by remember(initialProfile) { mutableStateOf(initialProfile?.notes ?: "") }
    var makeDefault by remember(initialProfile) { mutableStateOf(initialProfile?.isDefault ?: true) }
    var showNakPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialProfile == null) ext.addProfileLabel else ext.editProfileLabel)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(ext.nativeNameHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("navtara_profile_dialog_name")
                )

                Text(strings.selectBirthNakshatraLabel, style = MaterialTheme.typography.labelMedium)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showNakPicker = true }
                        .testTag("navtara_profile_dialog_nak_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$birthNak. ${VedicLocalizer.nakshatraNameByNumber(birthNak, lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = strings.select)
                    }
                }

                Text(ext.birthPadaLabel, style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    (1..4).forEachIndexed { idx, p ->
                        SegmentedButton(
                            selected = pada == p,
                            onClick = { pada = p },
                            shape = SegmentedButtonDefaults.itemShape(idx, 4),
                            modifier = Modifier.testTag("navtara_profile_dialog_pada_$p")
                        ) { Text("${strings.padaLabel} $p") }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(ext.journalNoteHint) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("navtara_profile_dialog_notes")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ext.pinAsDefaultLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it },
                        modifier = Modifier.testTag("navtara_profile_dialog_default_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim().ifBlank {
                        if (lang == AppLanguage.HINDI) "साधक" else "Native"
                    }
                    onSave(initialProfile?.id, cleanName, birthNak, pada, notes.trim(), makeDefault)
                },
                modifier = Modifier.testTag("navtara_profile_dialog_save")
            ) {
                Text(ext.saveAndApplyLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )

    if (showNakPicker) {
        AlertDialog(
            onDismissRequest = { showNakPicker = false },
            title = { Text(strings.selectBirthNakshatraLabel) },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items((1..27).toList(), key = { it }) { num ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    birthNak = num
                                    showNakPicker = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = birthNak == num,
                                onClick = {
                                    birthNak = num
                                    showNakPicker = false
                                }
                            )
                            Text("$num. ${VedicLocalizer.nakshatraNameByNumber(num, lang)}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNakPicker = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
internal fun LiveSkyNakshatraShiftCard(
    snapshot: PanchangaSnapshot,
    todayResult: NavtaraResult,
    matrix: List<NavtaraResult>,
    onOpenPlannerClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val nowMs = System.currentTimeMillis()

    val currentIdx = snapshot.nakshatra.indexOfFirst { seg ->
        val s = seg.startTimeMs ?: Long.MIN_VALUE
        val e = seg.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    }.let { if (it >= 0) it else 0 }

    val currentSeg = snapshot.nakshatra.getOrNull(currentIdx) ?: return
    val nextSeg = snapshot.nakshatra.getOrNull(currentIdx + 1)
    val nextTara = nextSeg?.let { seg -> matrix.firstOrNull { it.targetNakshatraIndex == seg.number } }

    val startMs = currentSeg.startTimeMs ?: snapshot.sunriseMs
    val endMs = currentSeg.endTimeMs ?: snapshot.nextSunriseMs
    val totalMs = (endMs - startMs).coerceAtLeast(1L)
    val elapsedMs = (nowMs - startMs).coerceIn(0L, totalMs)
    val progress = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (endMs - nowMs).coerceAtLeast(0L)

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(ext.liveNakshatraShiftTitle, modifier = Modifier.weight(1f))
            TextButton(
                onClick = onOpenPlannerClick,
                modifier = Modifier.testTag("navtara_open_planner_quick_button")
            ) {
                Text(ext.navtaraPeriodTabPlanner)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        AuspiciousnessMeterBar(
            progress = progress,
            accentColor = MaterialTheme.colorScheme.primary,
            label = "${ext.currentNakshatraLabel}: ${VedicLocalizer.nakshatraName(currentSeg.number, currentSeg.name, lang)}",
            trailingValue = VedicLocalizer.formatDurationRemaining(remainingMs, lang)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val curAccent = when {
                todayResult.isAuspicious -> vedic.auspicious
                todayResult.isNeutral -> vedic.caution
                else -> vedic.inauspicious
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = curAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, curAccent.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ext.currentNakshatraLabel, style = MaterialTheme.typography.labelSmall, color = curAccent)
                    Text(
                        VedicLocalizer.nakshatraName(currentSeg.number, currentSeg.name, lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (lang == AppLanguage.HINDI) todayResult.taraNameHi else todayResult.taraNameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = curAccent
                    )
                    currentSeg.ends?.let {
                        Text("${strings.endsPrefix}: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (nextSeg != null && nextTara != null) {
                val nextAccent = when {
                    nextTara.isAuspicious -> vedic.auspicious
                    nextTara.isNeutral -> vedic.caution
                    else -> vedic.inauspicious
                }
                val delta = nextTara.score - todayResult.score
                val deltaLabel = if (delta >= 0) "+$delta" else "$delta"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = nextAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, nextAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ext.incomingNakshatraLabel, style = MaterialTheme.typography.labelSmall, color = nextAccent)
                        Text(
                            VedicLocalizer.nakshatraName(nextSeg.number, nextSeg.name, lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${if (lang == AppLanguage.HINDI) nextTara.taraNameHi else nextTara.taraNameEn} ($deltaLabel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = nextAccent
                        )
                        nextSeg.starts?.let {
                            Text("${strings.startsLabel}: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ext.incomingNakshatraLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.continuesPastNextSunrise, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun NavtaraPeriodPlannerSection(
    state: NavtaraUiState,
    report: NavtaraPeriodReport?,
    onSetCustomMode: (Boolean) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onSetCustomDates: (LocalDate, LocalDate) -> Unit,
    onApplyPreset: (Int) -> Unit,
    onGenerateReport: () -> Unit,
    onSaveReportLocally: () -> Unit,
    onSyncJsonToDrive: () -> Unit,
    onExportJson: () -> Unit,
    onOpenDayJournal: (NavtaraPeriodDay) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val canJournalSave = policy.hasFeature(AppFeatureKey.NAVTATA_JOURNAL_SAVE)
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }
    var showAllTransitions by remember { mutableStateOf(false) }

    PanchangaCard {
        SectionHeader(ext.transitPlannerTitle)
        Spacer(modifier = Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.isCustomRange,
                onClick = { onSetCustomMode(false) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("navtara_mode_full_month")
            ) { Text(ext.fullMonthModeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            SegmentedButton(
                selected = state.isCustomRange,
                onClick = { onSetCustomMode(true) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("navtara_mode_custom_range")
            ) { Text(ext.customRangeModeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!state.isCustomRange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onShiftMonth(-1L) },
                    modifier = Modifier.size(48.dp).testTag("navtara_prev_month_button")
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.previousMonthCd)
                }
                Text(
                    text = VedicLocalizer.formatMonthYear(state.selectedYearMonth, lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onShiftMonth(1L) },
                    modifier = Modifier.size(48.dp).testTag("navtara_next_month_button")
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextMonthCd)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { pickingStartDate = true }
                        .testTag("navtara_range_start_button")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(strings.startsLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.rangeStartDate.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { pickingEndDate = true }
                        .testTag("navtara_range_end_button")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(strings.endsLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.rangeEndDate.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { onApplyPreset(0) },
                    label = { Text(ext.preset15Days) },
                    modifier = Modifier.testTag("navtara_preset_15d")
                )
                FilterChip(
                    selected = false,
                    onClick = { onApplyPreset(1) },
                    label = { Text(ext.preset30Days) },
                    modifier = Modifier.testTag("navtara_preset_30d")
                )
                FilterChip(
                    selected = false,
                    onClick = { onApplyPreset(2) },
                    label = { Text(ext.presetThisMonth) },
                    modifier = Modifier.testTag("navtara_preset_this_month")
                )
                FilterChip(
                    selected = false,
                    onClick = { onApplyPreset(3) },
                    label = { Text(ext.presetNextMonth) },
                    modifier = Modifier.testTag("navtara_preset_next_month")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onGenerateReport,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("navtara_generate_report_button")
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(ext.generateReportLabel)
            }
            if (canJournalSave) {
                OutlinedButton(
                    onClick = onSaveReportLocally,
                    enabled = report != null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("navtara_save_report_locally_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.saveReportLocallyLabel)
                }
                OutlinedButton(
                    onClick = onSyncJsonToDrive,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("navtara_planner_sync_drive_json_button")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.driveSyncJsonNowButton)
                }
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("navtara_planner_export_json_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.saveJsonFile)
                }
            }
        }
    }

    if (state.isGeneratingPeriod) {
        Spacer(modifier = Modifier.height(12.dp))
        LoadingStateBox()
    } else if (report != null) {
        Spacer(modifier = Modifier.height(12.dp))

        // Summary Statistics Grid
        PanchangaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatTile(
                    label = ext.auspiciousDaysLabel,
                    value = "${report.stats.auspiciousCount}",
                    valueColor = vedic.auspicious,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = ext.mixedDaysLabel,
                    value = "${report.stats.mixedCount}",
                    valueColor = vedic.caution,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = ext.cautionDaysLabel,
                    value = "${report.stats.cautionCount}",
                    valueColor = vedic.inauspicious,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = ext.neutralDaysLabel,
                    value = "${report.stats.neutralCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ext.reflectionStatsTitle}: ${report.stats.feedbackCount}/${report.stats.totalDays}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${NavtaraFeedbackRating.AUSPICIOUS.emoji} ${report.stats.auspiciousFeedbackCount} · ${NavtaraFeedbackRating.NEUTRAL.emoji} ${report.stats.neutralFeedbackCount} · ${NavtaraFeedbackRating.CAUTION.emoji} ${report.stats.cautionFeedbackCount}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        NavtaraCalendarGridCard(
            report = report,
            onSelectDay = onOpenDayJournal
        )

        if (report.transitions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            PanchangaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(ext.transitionsTitle, "${report.transitions.size}", modifier = Modifier.weight(1f))
                    if (report.transitions.size > 6) {
                        IconButton(onClick = { showAllTransitions = !showAllTransitions }) {
                            Icon(
                                if (showAllTransitions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = strings.details
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val visibleTransitions = if (showAllTransitions) report.transitions else report.transitions.take(6)
                visibleTransitions.forEachIndexed { idx, tr ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 5.dp))
                    val fromStar = VedicLocalizer.nakshatraName(tr.fromNakshatraNumber, tr.fromNakshatraName, lang)
                    val toStar = VedicLocalizer.nakshatraName(tr.toNakshatraNumber, tr.toNakshatraName, lang)
                    val fromTara = if (lang == AppLanguage.HINDI) tr.fromTara.taraNameHi else tr.fromTara.taraNameEn
                    val toTara = if (lang == AppLanguage.HINDI) tr.toTara.taraNameHi else tr.toTara.taraNameEn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${tr.dateIso} · ${tr.transitionTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$fromStar ($fromTara) → $toStar ($toTara)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(
            title = if (lang == AppLanguage.HINDI) "दैनिक गोचर एवं अनुभव डायरी" else "Day-by-Day Transits & Journal",
            subtitle = "${report.days.size} ${if (lang == AppLanguage.HINDI) "दिवस" else "days"}"
        )
        Spacer(modifier = Modifier.height(8.dp))
        report.days.forEach { day ->
            NavtaraPeriodDayCard(day = day, onOpenJournal = { onOpenDayJournal(day) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (pickingStartDate) {
        val initMillis = state.rangeStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSetCustomDates(picked, maxOf(picked, state.rangeEndDate))
                        }
                        pickingStartDate = false
                    }
                ) { Text(strings.select) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) { Text(strings.cancel) }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (pickingEndDate) {
        val initMillis = state.rangeEndDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSetCustomDates(minOf(state.rangeStartDate, picked), picked)
                        }
                        pickingEndDate = false
                    }
                ) { Text(strings.select) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) { Text(strings.cancel) }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun NavtaraCalendarGridCard(
    report: NavtaraPeriodReport,
    onSelectDay: (NavtaraPeriodDay) -> Unit
) {
    val lang = LocalAppLanguage.current
    val vedic = LocalVedicColors.current
    val weekdays = VedicLocalizer.weekdayShortLabels(lang)
    val firstDate = remember(report.startDateIso) { runCatching { LocalDate.parse(report.startDateIso) }.getOrNull() }
    val leadingBlanks = (firstDate?.dayOfWeek?.value ?: 0) % 7

    val paddedCells: List<NavtaraPeriodDay?> = remember(report.days, leadingBlanks) {
        List(leadingBlanks) { null } + report.days
    }
    val rows = remember(paddedCells) { paddedCells.chunked(7) }

    PanchangaCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { wd ->
                Text(
                    text = wd,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until 7) {
                    val day = row.getOrNull(i)
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val accent = when (day.category) {
                            NavtaraDayCategory.AUSPICIOUS -> vedic.auspicious
                            NavtaraDayCategory.MIXED -> vedic.caution
                            NavtaraDayCategory.CAUTION -> vedic.inauspicious
                            NavtaraDayCategory.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val bg = when (day.category) {
                            NavtaraDayCategory.AUSPICIOUS -> vedic.auspiciousContainer.copy(alpha = 0.55f)
                            NavtaraDayCategory.MIXED -> vedic.cautionContainer.copy(alpha = 0.55f)
                            NavtaraDayCategory.CAUTION -> vedic.inauspiciousContainer.copy(alpha = 0.55f)
                            NavtaraDayCategory.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        val canJournalSave = LocalAccessPolicy.current.hasFeature(AppFeatureKey.NAVTATA_JOURNAL_SAVE)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = canJournalSave) { onSelectDay(day) }
                                .testTag("navtara_cal_cell_${day.dateIso}")
                        ) {
                            Column(
                                modifier = Modifier.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${day.dayOfMonth}${day.feedback?.rating?.emoji ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = (if (lang == AppLanguage.HINDI) day.currentTara.taraNameHi else day.currentTara.taraNameEn).take(6),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavtaraPeriodDayCard(
    day: NavtaraPeriodDay,
    onOpenJournal: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val accent = when (day.category) {
        NavtaraDayCategory.AUSPICIOUS -> vedic.auspicious
        NavtaraDayCategory.MIXED -> vedic.caution
        NavtaraDayCategory.CAUTION -> vedic.inauspicious
        NavtaraDayCategory.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${day.dateIso} · ${VedicLocalizer.vaaraName(day.weekday, day.vaara, lang)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${VedicLocalizer.tithiName(day.tithiNumber, day.tithiName, lang)} · ${VedicLocalizer.nakshatraName(day.nakshatraNumber, day.nakshatraName, lang)}${day.nakshatraEnds?.let { " ($it)" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                label = "${if (lang == AppLanguage.HINDI) day.currentTara.taraNameHi else day.currentTara.taraNameEn} (${day.currentTara.score})",
                textColor = accent,
                containerColor = accent.copy(alpha = 0.14f)
            )
        }

        val nextTara = day.nextTara
        val nextNakName = day.nextNakshatraName
        if (nextTara != null && nextNakName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            val nStar = VedicLocalizer.nakshatraName(day.nextNakshatraNumber ?: day.nakshatraNumber, nextNakName, lang)
            val nTara = if (lang == AppLanguage.HINDI) nextTara.taraNameHi else nextTara.taraNameEn
            Text(
                text = "→ $nStar ($nTara · ${nextTara.score})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) day.recommendationHi else day.recommendationEn,
            style = MaterialTheme.typography.bodySmall
        )

        day.feedback?.let { fb ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fb.rating.emoji} ${if (lang == AppLanguage.HINDI) fb.rating.labelHi else fb.rating.labelEn}${if (fb.note.isNotBlank()) ": ${fb.note}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (LocalAccessPolicy.current.hasFeature(AppFeatureKey.NAVTATA_JOURNAL_SAVE)) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onOpenJournal,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("navtara_journal_button_${day.dateIso}")
                ) {
                    Icon(Icons.Rounded.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.dailyJournalLabel)
                }
            }
        }
    }
}

@Composable
internal fun NavtaraJournalEntryDialog(
    dateIso: String,
    subtitle: String,
    existing: NavtaraDailyFeedback?,
    onDismiss: () -> Unit,
    onSave: (NavtaraFeedbackRating, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    var rating by remember(dateIso, existing) { mutableStateOf(existing?.rating ?: NavtaraFeedbackRating.NEUTRAL) }
    var note by remember(dateIso, existing) { mutableStateOf(existing?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${ext.dailyJournalLabel} · $dateIso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NavtaraFeedbackRating.entries.forEach { opt ->
                        FilterChip(
                            selected = rating == opt,
                            onClick = { rating = opt },
                            label = {
                                Text(
                                    text = "${opt.emoji} ${if (lang == AppLanguage.HINDI) opt.labelHi else opt.labelEn}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("navtara_journal_rating_${opt.name}")
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(ext.journalNoteHint) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("navtara_journal_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rating, note.trim()) },
                modifier = Modifier.testTag("navtara_journal_save_button")
            ) {
                Text(ext.saveFeedbackLabel)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (existing != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("navtara_journal_delete_button")
                    ) {
                        Text(ext.actionDeleteLabel, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}
