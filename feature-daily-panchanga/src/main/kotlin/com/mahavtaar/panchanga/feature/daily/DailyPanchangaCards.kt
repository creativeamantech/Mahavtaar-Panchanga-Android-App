package com.mahavtaar.panchanga.feature.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.GlossaryHintChip
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DishaShoolaPanchakaCard(
    snapshot: PanchangaSnapshot,
    onOpenGlossary: (String) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val disha = if (lang == AppLanguage.HINDI) snapshot.dishaShoolaHi else snapshot.dishaShoolaEn
    val panchaka = if (lang == AppLanguage.HINDI) snapshot.panchakaStatusHi else snapshot.panchakaStatusEn

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Explore, contentDescription = ext.dishaShoolaLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(ext.dishaShoolaLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(disha.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                StatusPill(
                    label = if (snapshot.hasBhadra) ext.bhadraWarningLabel else ext.bhadraClearLabel,
                    textColor = if (snapshot.hasBhadra) vedic.inauspicious else vedic.auspicious,
                    containerColor = if (snapshot.hasBhadra) vedic.inauspiciousContainer else vedic.auspiciousContainer
                )
                GlossaryHintChip(
                    termQuery = "Bhadra",
                    contentLabel = if (lang == AppLanguage.HINDI) "भद्रा / विष्टि" else "Bhadra / Vishti",
                    onOpenGlossary = onOpenGlossary,
                    testTag = "daily_bhadra_help_chip"
                )
            }
        }
        if (panchaka.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = ext.panchakaStatusLabel, tint = vedic.caution, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${ext.panchakaStatusLabel}: $panchaka",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VaraDetailCard(snapshot: PanchangaSnapshot) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val attrs = VedicGlossaryCatalog.vaaraAttributes(snapshot.weekday, lang)

    PanchangaCard {
        val srText = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunriseMs, snapshot.timezone, lang)
        val nsrText = VedicLocalizer.formatDateTimeWithSeconds(snapshot.nextSunriseMs, snapshot.timezone, lang)
        SectionHeader(
            title = strings.anga5VaraTitle,
            subtitle = strings.varaRunsSubtitle(srText, nsrText),
            trailing = {
                SwaraNadiBadge(nadi = snapshot.swaraYoga.sunriseSwara)
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventStartEndDateTimeView(
                startText = srText,
                endText = nsrText,
                accentColor = MaterialTheme.colorScheme.primary,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
                label = VedicLocalizer.vaaraName(snapshot.weekday, snapshot.vaara, lang),
                textColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.dayLabel,
                value = snapshot.dayDuration,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.nightLabel,
                value = snapshot.nightDuration,
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusPill("${ext.vaaraDeityLabel}: ${attrs.deity}", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
            StatusPill("${ext.vaaraRulerLabel}: ${attrs.ruler}", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
            StatusPill("${ext.vaaraColorLabel}: ${attrs.color}", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
            StatusPill("${ext.vaaraMetalLabel}: ${attrs.metal}", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${strings.sunRasiPrefix}: ${VedicLocalizer.rasiByName(snapshot.sunRasi, lang)} · ${strings.kaliDayLabel}: ${snapshot.kaliDay} · JD: ${snapshot.jd}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ViewModeToggleRow(
    isGridMode: Boolean,
    onToggleGridMode: (Boolean) -> Unit
) {
    val ext = LocalExtendedStrings.current
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !isGridMode,
            onClick = { onToggleGridMode(false) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            icon = { Icon(Icons.Rounded.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.testTag("muhurta_view_list")
        ) { Text(ext.listViewLabel) }
        SegmentedButton(
            selected = isGridMode,
            onClick = { onToggleGridMode(true) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            icon = { Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.testTag("muhurta_view_grid")
        ) { Text(ext.gridViewLabel) }
    }
}

@Composable
internal fun SandhyaAndSpecialMuhurtasCard(
    snapshot: PanchangaSnapshot,
    nowMs: Long,
    onInspectTiming: (String, TimingInterval) -> Unit = { _, _ -> }
) {
    val vedic = LocalVedicColors.current
    val ext = LocalExtendedStrings.current
    val entries = listOfNotNull(
        snapshot.pratahSandhya?.let { Triple(ext.pratahSandhyaLabel, it, true) },
        snapshot.vijayaMuhurta?.let { Triple(ext.vijayaMuhurtaLabel, it, true) },
        snapshot.godhuliMuhurta?.let { Triple(ext.godhuliMuhurtaLabel, it, true) },
        snapshot.sayahnaSandhya?.let { Triple(ext.sayahnaSandhyaLabel, it, true) },
        snapshot.nishitaMuhurta?.let { Triple(ext.nishitaMuhurtaLabel, it, false) }
    )
    if (entries.isEmpty()) return

    PanchangaCard {
        SectionHeader(title = ext.sandhyaAndSpecialMuhurtas)
        Spacer(modifier = Modifier.height(6.dp))
        entries.forEachIndexed { idx, (label, interval, isAusp) ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val accent = if (isAusp) vedic.auspicious else MaterialTheme.colorScheme.tertiary
            val bg = if (isAusp) vedic.auspiciousContainer else MaterialTheme.colorScheme.tertiaryContainer
            TimingIntervalRow(
                displayTitle = label,
                interval = interval,
                nowMs = nowMs,
                accent = accent,
                badgeBg = bg,
                timezone = snapshot.timezone,
                isCaution = !isAusp,
                onClick = { onInspectTiming(label, interval) }
            )
        }
    }
}

@Composable
internal fun MuhurtasSectionCard(
    snapshot: PanchangaSnapshot,
    nowMs: Long,
    isGridMode: Boolean,
    onInspectTiming: (String, TimingInterval) -> Unit = { _, _ -> },
    onOpenGlossary: (String) -> Unit = {}
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current

    val auspiciousList = buildList {
        add(snapshot.brahmaMuhurta)
        add(snapshot.abhijitMuhurta)
        addAll(snapshot.amritaKala)
    }
    val inauspiciousList = buildList {
        add(snapshot.rahuKala)
        add(snapshot.yamaganda)
        add(snapshot.gulikaKala)
        addAll(snapshot.durmuhurta)
        addAll(snapshot.varjyam)
    }

    PanchangaCard {
        SectionHeader(title = strings.muhurtasTitle, subtitle = strings.muhurtasSubtitle)
        Spacer(modifier = Modifier.height(10.dp))
        Text(strings.auspiciousMuhurtas, style = MaterialTheme.typography.labelLarge, color = vedic.auspicious)
        Spacer(modifier = Modifier.height(6.dp))
        if (isGridMode) {
            IntervalsGrid(auspiciousList, lang, nowMs, snapshot.timezone, vedic.auspicious, vedic.auspiciousContainer, onInspectTiming)
        } else {
            auspiciousList.forEachIndexed { idx, it ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val label = VedicLocalizer.muhurtaOrPeriodName(it.name, lang)
                TimingIntervalRow(label, it, nowMs, vedic.auspicious, vedic.auspiciousContainer, snapshot.timezone, isCaution = false) {
                    onInspectTiming(label, it)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.inauspiciousKalas, style = MaterialTheme.typography.labelLarge, color = vedic.inauspicious)
            GlossaryHintChip(
                termQuery = "Rahu",
                contentLabel = if (lang == AppLanguage.HINDI) "राहु काल" else "Rahu Kalam",
                onOpenGlossary = onOpenGlossary,
                testTag = "daily_rahu_help_chip"
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (isGridMode) {
            IntervalsGrid(inauspiciousList, lang, nowMs, snapshot.timezone, vedic.inauspicious, vedic.inauspiciousContainer, onInspectTiming)
        } else {
            inauspiciousList.forEachIndexed { idx, it ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val label = VedicLocalizer.muhurtaOrPeriodName(it.name, lang)
                TimingIntervalRow(label, it, nowMs, vedic.inauspicious, vedic.inauspiciousContainer, snapshot.timezone, isCaution = true) {
                    onInspectTiming(label, it)
                }
            }
        }
    }
}

@Composable
internal fun ChoghadiyaCard(
    title: String,
    subtitle: String,
    intervals: List<TimingInterval>,
    nowMs: Long,
    isGridMode: Boolean,
    timezone: String = "Asia/Kolkata",
    onInspectTiming: (String, TimingInterval) -> Unit = { _, _ -> }
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    PanchangaCard {
        SectionHeader(title = title, subtitle = subtitle)
        Spacer(modifier = Modifier.height(10.dp))
        if (isGridMode) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                intervals.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { item ->
                            val accent = if (item.isAuspicious) vedic.auspicious else vedic.inauspicious
                            val bg = if (item.isAuspicious) vedic.auspiciousContainer else vedic.inauspiciousContainer
                            val label = VedicLocalizer.muhurtaOrPeriodName(item.name, lang)
                            TimingGridTile(
                                title = label,
                                interval = item,
                                nowMs = nowMs,
                                timezone = timezone,
                                accent = accent,
                                badgeBg = bg,
                                onClick = { onInspectTiming(label, item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            intervals.forEachIndexed { idx, item ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val accent = if (item.isAuspicious) vedic.auspicious else vedic.inauspicious
                val bg = if (item.isAuspicious) vedic.auspiciousContainer else vedic.inauspiciousContainer
                val label = VedicLocalizer.muhurtaOrPeriodName(item.name, lang)
                TimingIntervalRow(label, item, nowMs, accent, bg, timezone, isCaution = !item.isAuspicious) {
                    onInspectTiming(label, item)
                }
            }
        }
    }
}

@Composable
private fun IntervalsGrid(
    items: List<TimingInterval>,
    lang: AppLanguage,
    nowMs: Long,
    timezone: String,
    accent: Color,
    badgeBg: Color,
    onInspectTiming: (String, TimingInterval) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { item ->
                    val label = VedicLocalizer.muhurtaOrPeriodName(item.name, lang)
                    TimingGridTile(
                        title = label,
                        interval = item,
                        nowMs = nowMs,
                        timezone = timezone,
                        accent = accent,
                        badgeBg = badgeBg,
                        onClick = { onInspectTiming(label, item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimingGridTile(
    title: String,
    interval: TimingInterval,
    nowMs: Long,
    timezone: String,
    accent: Color,
    badgeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val isActive = interval.startTimeMs != null && interval.endTimeMs != null &&
        nowMs in interval.startTimeMs!! until interval.endTimeMs!!
    val startStr = interval.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.start
    val endStr = interval.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.end
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) badgeBg.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) accent else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (isActive) StatusPill(strings.now, accent, badgeBg, fontWeight = FontWeight.Bold)
            }
            EventStartEndDateTimeView(
                startText = startStr,
                endText = endStr,
                accentColor = accent,
                compact = true
            )
        }
    }
}

@Composable
internal fun TimingIntervalRow(
    displayTitle: String,
    interval: TimingInterval,
    nowMs: Long,
    accent: Color,
    badgeBg: Color,
    timezone: String = "Asia/Kolkata",
    isCaution: Boolean = !interval.isAuspicious,
    onClick: (() -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val isActive = interval.startTimeMs != null && interval.endTimeMs != null &&
        nowMs in interval.startTimeMs!! until interval.endTimeMs!!
    val startStr = interval.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.start
    val endStr = interval.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.end

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayTitle, style = MaterialTheme.typography.bodyMedium, color = accent, fontWeight = FontWeight.Bold)
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(strings.now, accent, badgeBg, fontWeight = FontWeight.Bold)
                    }
                }
                EventStartEndDateTimeView(
                    startText = startStr,
                    endText = endStr,
                    accentColor = accent,
                    compact = true
                )
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = displayTitle,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (isActive) {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowIntervalSentence(
                    title = displayTitle,
                    isCaution = isCaution,
                    endMs = interval.endTimeMs,
                    nowMs = nowMs,
                    lang = lang
                ),
                accentColor = accent,
                containerColor = badgeBg
            )
        }
    }
}
