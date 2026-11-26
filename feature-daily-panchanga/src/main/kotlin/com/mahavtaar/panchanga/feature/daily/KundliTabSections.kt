package com.mahavtaar.panchanga.feature.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
internal fun KundliChartsSection(
    report: KundliReport,
    chartStyle: KundliChartStyle,
    onStyleChange: (KundliChartStyle) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = chartStyle == KundliChartStyle.NORTH_INDIAN,
                onClick = { onStyleChange(KundliChartStyle.NORTH_INDIAN) },
                shape = SegmentedButtonDefaults.itemShape(0, 3),
                modifier = Modifier.testTag("kundli_style_north")
            ) { Text(strings.northIndianStyle) }
            SegmentedButton(
                selected = chartStyle == KundliChartStyle.SOUTH_INDIAN,
                onClick = { onStyleChange(KundliChartStyle.SOUTH_INDIAN) },
                shape = SegmentedButtonDefaults.itemShape(1, 3),
                modifier = Modifier.testTag("kundli_style_south")
            ) { Text(strings.southIndianStyle) }
            SegmentedButton(
                selected = chartStyle == KundliChartStyle.EAST_INDIAN,
                onClick = { onStyleChange(KundliChartStyle.EAST_INDIAN) },
                shape = SegmentedButtonDefaults.itemShape(2, 3),
                modifier = Modifier.testTag("kundli_style_east")
            ) { Text(ext.chartStyleEast) }
        }

        PanchangaCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(if (lang == AppLanguage.HINDI) "लग्न राशि" else "Lagna", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${VedicLocalizer.rasiShortByZeroIndex(report.lagnaSignIndex, lang)} ${report.lagnaDms}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(strings.moonRasiPrefix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(VedicLocalizer.rasiShortByZeroIndex(report.moonSignIndex, lang), style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text(strings.nakshatraLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${VedicLocalizer.nakshatraNameByNumber(report.moonNakshatraNumber, lang)} (${strings.padaLabel} ${report.moonPada})", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        PanchangaCard {
            SectionHeader(strings.lagnaChartTitle, "${strings.ayanamshaPrefix}: %.2f°".format(report.ayanamshaDeg))
            Spacer(modifier = Modifier.height(10.dp))
            KundliChartCanvas(lagnaSignIndex = report.lagnaSignIndex, planets = report.planets, chartStyle = chartStyle, centerTitle = "D1")
        }

        PanchangaCard {
            SectionHeader(strings.navamshaChartTitle)
            Spacer(modifier = Modifier.height(10.dp))
            KundliChartCanvas(
                lagnaSignIndex = report.navamshaLagnaSignIndex,
                planets = report.planets,
                planetSignsOverride = report.planets.associate { it.id to it.signD9 },
                chartStyle = chartStyle,
                centerTitle = "D9"
            )
        }

        if (report.housesD1.isNotEmpty()) {
            PanchangaCard {
                SectionHeader(ext.bhavaPlacementsTitle)
                Spacer(modifier = Modifier.height(8.dp))
                report.housesD1.forEachIndexed { idx, h ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 5.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("H${h.houseNumber} · ${VedicLocalizer.rasiNameByZeroIndex(h.signIndex, lang)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(if (lang == AppLanguage.HINDI) h.significanceHi else h.significanceEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (lang == AppLanguage.HINDI) h.lordHi else h.lordEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun KundliPlanetsSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        report.planets.forEach { p ->
            PanchangaCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${p.symbol} ${if (lang == AppLanguage.HINDI) p.nameHi else p.nameEn}" +
                                if (p.isRetrograde) " (${strings.retrogradeBadge})" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${VedicLocalizer.rasiNameByZeroIndex(p.signIndex, lang)} · ${p.dmsLabel} · H${p.houseD1} (D9: H${p.houseD9})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${VedicLocalizer.nakshatraNameByNumber(p.nakshatraNumber, lang)} (${strings.padaLabel} ${p.pada})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusPill(
                        label = if (lang == AppLanguage.HINDI) p.dignityHi else p.dignityEn,
                        textColor = if (p.isExaltedOrOwn) vedic.auspicious else MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = if (p.isExaltedOrOwn) vedic.auspiciousContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun KundliDashaSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    var expandedMaha by remember { mutableStateOf(report.vimshottariDashas.firstOrNull { it.isCurrent }?.planetId) }
    var expandedAntar by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanchangaCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
            Text(strings.currentDashaLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(if (lang == AppLanguage.HINDI) report.currentMahadashaLabelHi else report.currentMahadashaLabelEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val balance = if (lang == AppLanguage.HINDI) report.birthBalanceHi else report.birthBalanceEn
            if (balance.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("${ext.birthDashaBalanceLabel}: $balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        report.vimshottariDashas.forEach { maha ->
            val isExpanded = expandedMaha == maha.planetId
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (maha.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (maha.isCurrent) 1.5.dp else 1.dp, if (maha.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { expandedMaha = if (isExpanded) null else maha.planetId }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${if (lang == AppLanguage.HINDI) maha.planetNameHi else maha.planetNameEn} (${maha.durationYears}y)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${maha.startDate} – ${maha.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (maha.isCurrent) StatusPill(strings.active, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        maha.antardashas.forEach { antar ->
                            val antarKey = "${maha.planetId}_${antar.planetId}"
                            val antarOpen = expandedAntar == antarKey || (expandedAntar == null && antar.isCurrent)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedAntar = if (antarOpen) null else antarKey }
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (lang == AppLanguage.HINDI) "${maha.planetNameHi} / ${antar.planetNameHi}" else "${maha.planetNameEn} / ${antar.planetNameEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (antar.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (antar.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text("${antar.startDate} – ${antar.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (antarOpen && antar.pratyantardashas.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(start = 14.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        antar.pratyantardashas.forEach { prat ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    text = "↳ ${if (lang == AppLanguage.HINDI) prat.planetNameHi else prat.planetNameEn} (${prat.durationDays}d)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (prat.isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (prat.isCurrent) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text("${prat.startDate} – ${prat.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun KundliVargasSection(
    report: KundliReport,
    selectedVargaCode: String,
    chartStyle: KundliChartStyle,
    onSelectVarga: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val selected = report.vargas.firstOrNull { it.code == selectedVargaCode } ?: report.vargas.first()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(report.vargas, key = { it.code }) { varga ->
                FilterChip(selected = varga.code == selected.code, onClick = { onSelectVarga(varga.code) }, label = { Text(varga.code) })
            }
        }
        PanchangaCard {
            SectionHeader(if (lang == AppLanguage.HINDI) selected.titleHi else selected.titleEn, if (lang == AppLanguage.HINDI) selected.purposeHi else selected.purposeEn)
            Spacer(modifier = Modifier.height(10.dp))
            KundliChartCanvas(lagnaSignIndex = selected.lagnaSignIndex, planets = report.planets, planetSignsOverride = selected.planetSigns, chartStyle = chartStyle, centerTitle = selected.code)
        }
    }
}

@Composable
internal fun GunaMilanSection(
    state: KundliUiState,
    onUpdate: (String, Int, Int, Int, Boolean, String, Int, Int, Int, Boolean) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val report = state.gunaMilanReport

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanchangaCard {
            Text(strings.groomSectionTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = state.boyName,
                onValueChange = { onUpdate(it, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) },
                label = { Text(ext.kundliNameHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            StepperSelectorRow(
                label = strings.nakshatraLabel,
                valueText = "${state.boyNakshatra}. ${VedicLocalizer.nakshatraNameByNumber(state.boyNakshatra, lang)} (${strings.padaLabel} ${state.boyPada})",
                onPrev = { onUpdate(state.boyName, ((state.boyNakshatra - 2 + 27) % 27) + 1, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) },
                onNext = { onUpdate(state.boyName, (state.boyNakshatra % 27) + 1, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) }
            )
            StepperSelectorRow(
                label = strings.padaLabel,
                valueText = "${strings.padaLabel} ${state.boyPada}",
                onPrev = { onUpdate(state.boyName, state.boyNakshatra, ((state.boyPada + 2) % 4) + 1, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) },
                onNext = { onUpdate(state.boyName, state.boyNakshatra, (state.boyPada % 4) + 1, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) }
            )
            StepperSelectorRow(
                label = strings.moonRasiPrefix,
                valueText = VedicLocalizer.rasiNameByZeroIndex(state.boyRasi, lang),
                onPrev = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, (state.boyRasi + 11) % 12, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) },
                onNext = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, (state.boyRasi + 1) % 12, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.manglikToggleLabel, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.boyManglik, onCheckedChange = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, it, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) })
            }
        }

        PanchangaCard {
            Text(strings.brideSectionTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = state.girlName,
                onValueChange = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, it, state.girlNakshatra, state.girlPada, state.girlRasi, state.girlManglik) },
                label = { Text(ext.kundliNameHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            StepperSelectorRow(
                label = strings.nakshatraLabel,
                valueText = "${state.girlNakshatra}. ${VedicLocalizer.nakshatraNameByNumber(state.girlNakshatra, lang)} (${strings.padaLabel} ${state.girlPada})",
                onPrev = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, ((state.girlNakshatra - 2 + 27) % 27) + 1, state.girlPada, state.girlRasi, state.girlManglik) },
                onNext = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, (state.girlNakshatra % 27) + 1, state.girlPada, state.girlRasi, state.girlManglik) }
            )
            StepperSelectorRow(
                label = strings.padaLabel,
                valueText = "${strings.padaLabel} ${state.girlPada}",
                onPrev = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, ((state.girlPada + 2) % 4) + 1, state.girlRasi, state.girlManglik) },
                onNext = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, (state.girlPada % 4) + 1, state.girlRasi, state.girlManglik) }
            )
            StepperSelectorRow(
                label = strings.moonRasiPrefix,
                valueText = VedicLocalizer.rasiNameByZeroIndex(state.girlRasi, lang),
                onPrev = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, (state.girlRasi + 11) % 12, state.girlManglik) },
                onNext = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, (state.girlRasi + 1) % 12, state.girlManglik) }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.manglikToggleLabel, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.girlManglik, onCheckedChange = { onUpdate(state.boyName, state.boyNakshatra, state.boyPada, state.boyRasi, state.boyManglik, state.girlName, state.girlNakshatra, state.girlPada, state.girlRasi, it) })
            }
        }

        if (report != null) {
            val accent = if (report.isRecommended) vedic.auspicious else vedic.inauspicious
            PanchangaCard(containerColor = accent.copy(alpha = 0.12f)) {
                Text(strings.totalScoreLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${report.totalScore} / 36 (${report.percentage}%)", style = MaterialTheme.typography.headlineMedium, color = accent, modifier = Modifier.testTag("guna_milan_score"))
                Text(if (lang == AppLanguage.HINDI) report.verdictHi else report.verdictEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(if (lang == AppLanguage.HINDI) report.manglikNoteHi else report.manglikNoteEn, style = MaterialTheme.typography.bodySmall, color = if (report.manglikCompatible) vedic.auspicious else vedic.inauspicious)
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (lang == AppLanguage.HINDI) report.remedyHi else report.remedyEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            report.kootas.forEach { k ->
                val kAccent = if (k.isDosha) vedic.inauspicious else vedic.auspicious
                PanchangaCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (lang == AppLanguage.HINDI) k.nameHi else k.nameEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(if (lang == AppLanguage.HINDI) k.descriptionHi else k.descriptionEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill("${k.obtainedScore}/${k.maxScore}", kAccent, kAccent.copy(alpha = 0.14f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperSelectorRow(label: String, valueText: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Row {
            TextButton(onClick = onPrev) { Text("◀") }
            TextButton(onClick = onNext) { Text("▶") }
        }
    }
}
