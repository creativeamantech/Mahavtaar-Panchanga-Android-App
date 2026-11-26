package com.mahavtaar.panchanga.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
internal fun KundliShadbalaSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    var subTab by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = subTab == 0, onClick = { subTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 4), modifier = Modifier.testTag("shadbala_sub_rank")) { Text(ext.shadbalaSubRank) }
            SegmentedButton(selected = subTab == 1, onClick = { subTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 4), modifier = Modifier.testTag("shadbala_sub_matrix")) { Text(ext.shadbalaSubMatrix) }
            SegmentedButton(selected = subTab == 2, onClick = { subTab = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 4), modifier = Modifier.testTag("shadbala_sub_ishta")) { Text(ext.shadbalaSubIshtaKashta) }
            SegmentedButton(selected = subTab == 3, onClick = { subTab = 3 }, shape = SegmentedButtonDefaults.itemShape(3, 4), modifier = Modifier.testTag("shadbala_sub_bhava")) { Text(ext.shadbalaSubBhava) }
        }

        when (subTab) {
            0 -> {
                report.shadbala.sortedBy { it.rank }.forEach { row ->
                    val ratio = (row.totalRupas / 9.0).toFloat().coerceIn(0f, 1f)
                    val accent = if (row.isStrong) vedic.auspicious else vedic.caution
                    var expanded by remember(row.planetId) { mutableStateOf(row.rank == 1) }
                    ExpandableInfoCard(
                        title = "#${row.rank} ${if (lang == AppLanguage.HINDI) row.nameHi else row.nameEn}",
                        subtitle = "${"%.1f".format(row.totalVirupas)} ${ext.virupasUnit}",
                        expanded = expanded,
                        onToggleExpand = { expanded = !expanded },
                        accentColor = accent,
                        trailingBadge = {
                            StatusPill("${row.totalRupas} / ${row.requiredRupas} ${ext.rupasUnit}", accent, accent.copy(alpha = 0.14f))
                        },
                        summaryContent = {
                            AuspiciousnessMeterBar(
                                progress = ratio,
                                accentColor = accent,
                                label = if (lang == AppLanguage.HINDI) "षड्बल शक्ति अनुपात" else "Shadbala Strength Ratio",
                                trailingValue = "${(ratio * 100).toInt()}%"
                            )
                        }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "स्थान" else "Sthana", "%.0f".format(row.sthanaBala), Modifier.weight(1f))
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "दिग्" else "Dig", "%.0f".format(row.digBala), Modifier.weight(1f))
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "काल" else "Kala", "%.0f".format(row.kalaBala), Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "चेष्टा" else "Cheshta", "%.0f".format(row.cheshtaBala), Modifier.weight(1f))
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "नैसर्गिक" else "Naisargika", "%.0f".format(row.naisargikaBala), Modifier.weight(1f))
                            TelemetryStatTile(if (lang == AppLanguage.HINDI) "दृक्" else "Drik", "%.0f".format(row.drikBala), Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TelemetryStatTile(ext.ishtaPhalaLabel, "%.1f".format(row.ishtaPhala), Modifier.weight(1f), valueColor = vedic.auspicious)
                            TelemetryStatTile(ext.kashtaPhalaLabel, "%.1f".format(row.kashtaPhala), Modifier.weight(1f), valueColor = vedic.inauspicious)
                        }
                    }
                }
            }
            1 -> {
                report.shadbala.forEach { row ->
                    PanchangaCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (lang == AppLanguage.HINDI) row.nameHi else row.nameEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${"%.1f".format(row.totalVirupas)} ${ext.virupasUnit}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "स्थान %.0f · दिग् %.0f · काल %.0f · चेष्टा %.0f · नैसर्गिक %.0f · दृक् %.0f".format(row.sthanaBala, row.digBala, row.kalaBala, row.cheshtaBala, row.naisargikaBala, row.drikBala)
                            } else {
                                "Sthana %.0f · Dig %.0f · Kala %.0f · Cheshta %.0f · Naisargika %.0f · Drik %.0f".format(row.sthanaBala, row.digBala, row.kalaBala, row.cheshtaBala, row.naisargikaBala, row.drikBala)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            2 -> {
                report.shadbala.forEach { row ->
                    val net = row.ishtaPhala - row.kashtaPhala
                    val accent = if (net >= 0) vedic.auspicious else vedic.inauspicious
                    PanchangaCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (lang == AppLanguage.HINDI) row.nameHi else row.nameEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatusPill("${ext.ishtaPhalaLabel}: ${"%.1f".format(row.ishtaPhala)} / ${ext.kashtaPhalaLabel}: ${"%.1f".format(row.kashtaPhala)}", accent, accent.copy(alpha = 0.14f))
                        }
                    }
                }
            }
            3 -> {
                report.bhavaBala.sortedBy { it.rank }.forEach { bb ->
                    PanchangaCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("H${bb.houseNumber} · ${VedicLocalizer.rasiNameByZeroIndex(bb.signIndex, lang)} (#${bb.rank})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "अधिपति %.0f · दिग् %.0f · दृष्टि %.0f".format(bb.bhavadhipatiBala, bb.bhavaDigBala, bb.bhavaDrishtiBala)
                                    } else {
                                        "Lord %.0f · Dig %.0f · Drishti %.0f".format(bb.bhavadhipatiBala, bb.bhavaDigBala, bb.bhavaDrishtiBala)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill("${"%.2f".format(bb.totalRupas)} ${ext.rupasUnit}", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KundliAshtakavargaSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val av = report.ashtakavarga
    var subTab by remember { mutableIntStateOf(0) }
    val planetKeys = remember(av.bhinnashtakavarga) { av.bhinnashtakavarga.keys.toList() }
    var selectedBavPlanet by remember(planetKeys) { mutableStateOf(planetKeys.firstOrNull() ?: "sun") }

    val kakshyaLords = if (lang == AppLanguage.HINDI) {
        listOf("शनि (0–3.75°)", "गुरु (3.75–7.5°)", "मङ्गल (7.5–11.25°)", "सूर्य (11.25–15°)", "शुक्र (15–18.75°)", "बुध (18.75–22.5°)", "चन्द्र (22.5–26.25°)", "लग्न (26.25–30°)")
    } else {
        listOf("Saturn (0–3.75°)", "Jupiter (3.75–7.5°)", "Mars (7.5–11.25°)", "Sun (11.25–15°)", "Venus (15–18.75°)", "Mercury (18.75–22.5°)", "Moon (22.5–26.25°)", "Ascendant (26.25–30°)")
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = subTab == 0, onClick = { subTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 4), modifier = Modifier.testTag("av_sub_sav")) { Text(ext.avSubSav) }
            SegmentedButton(selected = subTab == 1, onClick = { subTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 4), modifier = Modifier.testTag("av_sub_bav")) { Text(ext.avSubBav) }
            SegmentedButton(selected = subTab == 2, onClick = { subTab = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 4), modifier = Modifier.testTag("av_sub_shodhana")) { Text(ext.avSubShodhana) }
            SegmentedButton(selected = subTab == 3, onClick = { subTab = 3 }, shape = SegmentedButtonDefaults.itemShape(3, 4), modifier = Modifier.testTag("av_sub_kakshya")) { Text(ext.avSubKakshya) }
        }

        when (subTab) {
            0 -> {
                PanchangaCard {
                    SectionHeader(
                        title = "${ext.avSubSav} (${ext.totalSavLabel}: ${av.totalSav} ${ext.bindusUnit})",
                        subtitle = VedicLocalizer.rasiNameByZeroIndex(av.strongestSignIndex, lang)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    av.sarvashtakavarga.forEachIndexed { signIdx, bindus ->
                        val accent = when {
                            bindus >= 30 -> vedic.auspicious
                            bindus >= 25 -> MaterialTheme.colorScheme.primary
                            else -> vedic.inauspicious
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${signIdx + 1}. ${VedicLocalizer.rasiNameByZeroIndex(signIdx, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                StatusPill("$bindus ${ext.bindusUnit}", accent, accent.copy(alpha = 0.15f))
                            }
                            LinearProgressIndicator(
                                progress = { (bindus / 42f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
            1 -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(planetKeys, key = { it }) { pId ->
                        FilterChip(
                            selected = pId == selectedBavPlanet,
                            onClick = { selectedBavPlanet = pId },
                            label = { Text(VedicLocalizer.planetName(pId, pId.replaceFirstChar { it.uppercase() }, lang)) }
                        )
                    }
                }
                val row = av.bhinnashtakavarga[selectedBavPlanet].orEmpty()
                PanchangaCard {
                    SectionHeader("${VedicLocalizer.planetName(selectedBavPlanet, selectedBavPlanet, lang)} · ${row.sum()} ${ext.bindusUnit}")
                    Spacer(modifier = Modifier.height(8.dp))
                    row.forEachIndexed { signIdx, b ->
                        val accent = when {
                            b >= 5 -> vedic.auspicious
                            b <= 2 -> vedic.inauspicious
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${signIdx + 1}. ${VedicLocalizer.rasiNameByZeroIndex(signIdx, lang)}", style = MaterialTheme.typography.bodyMedium)
                                StatusPill("$b / 8 ${ext.bindusUnit}", accent, accent.copy(alpha = 0.14f))
                            }
                            LinearProgressIndicator(
                                progress = { (b / 8f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
            2 -> {
                PanchangaCard {
                    SectionHeader("${ext.trikonaShodhitaLabel} & ${ext.ekadhipatyaShodhitaLabel}")
                    Spacer(modifier = Modifier.height(8.dp))
                    for (signIdx in 0 until 12) {
                        val raw = av.sarvashtakavarga.getOrElse(signIdx) { 0 }
                        val tri = av.trikonaShodhitaSav.getOrElse(signIdx) { raw }
                        val eka = av.ekadhipatyaShodhitaSav.getOrElse(signIdx) { tri }
                        if (signIdx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${signIdx + 1}. ${VedicLocalizer.rasiShortByZeroIndex(signIdx, lang)}", style = MaterialTheme.typography.bodySmall)
                            Text("SAV $raw → Δ $tri → Ek $eka", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            3 -> {
                PanchangaCard {
                    SectionHeader(ext.avSubKakshya)
                    Spacer(modifier = Modifier.height(8.dp))
                    report.planets.take(7).forEachIndexed { idx, p ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                        val kIdx = ((p.degreeInSign / 3.75).toInt()).coerceIn(0, 7)
                        val bavScore = av.bhinnashtakavarga[p.id]?.getOrElse(p.signIndex) { 4 } ?: 4
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${p.symbol} ${if (lang == AppLanguage.HINDI) p.nameHi else p.nameEn}", style = MaterialTheme.typography.titleSmall)
                                Text(kakshyaLords[kIdx], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            StatusPill("$bavScore ${ext.bindusUnit}", vedic.auspicious, vedic.auspiciousContainer)
                        }
                    }
                }
            }
        }
    }
}
