package com.mahavtaar.panchanga.feature.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KundliKpSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (report.kpRulingPlanets.isNotEmpty()) {
            PanchangaCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                SectionHeader(ext.kpRulingPlanetsTitle)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.kpRulingPlanets.forEach { rp ->
                        StatusPill(
                            label = "${if (lang == AppLanguage.HINDI) rp.factorHi else rp.factorEn}: ${rp.planet}",
                            textColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }

        if (report.kpCusps.isNotEmpty()) {
            PanchangaCard {
                SectionHeader(ext.kpCuspsTitle)
                Spacer(modifier = Modifier.height(8.dp))
                report.kpCusps.forEachIndexed { idx, c ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 5.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Cusp ${c.houseNumber} · ${VedicLocalizer.rasiShortByZeroIndex(c.signIndex, lang)}", style = MaterialTheme.typography.titleSmall)
                            Text(c.dmsLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${c.signLord} / ${c.starLord} / ${c.subLord} / ${c.subSubLord}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        PanchangaCard {
            SectionHeader(ext.kpSignificatorsTitle)
            Spacer(modifier = Modifier.height(8.dp))
            report.kpTable.forEachIndexed { idx, row ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lang == AppLanguage.HINDI) row.nameHi else row.nameEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${row.signLord} / ${row.starLord} / ${row.subLord} / ${row.subSubLord}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "${VedicLocalizer.rasiShortByZeroIndex(row.signIndex, lang)} ${row.dmsLabel} · H${row.houseOccupied} | L1: ${row.level1Houses.joinToString(",")} · L2: ${row.level2Houses.joinToString(",")} · L3: ${row.level3Houses.joinToString(",")} · L4: ${row.level4Houses.joinToString(",")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun KundliJaiminiSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    var expandedDashaIdx by remember { mutableStateOf(report.charaDashas.indexOfFirst { it.isCurrent }.takeIf { it >= 0 }) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanchangaCard {
            SectionHeader(
                title = ext.charaKarakasTitle,
                subtitle = "${strings.arudhaLagnaLabel}: ${VedicLocalizer.rasiNameByZeroIndex(report.arudhaLagnaSignIndex, lang)} · ${ext.upapadaLagnaLabel}: ${VedicLocalizer.rasiNameByZeroIndex(report.upapadaLagnaSignIndex, lang)}"
            )
            Spacer(modifier = Modifier.height(8.dp))
            report.jaiminiKarakas.forEachIndexed { idx, k ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${k.code} · ${if (lang == AppLanguage.HINDI) k.titleHi else k.titleEn}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("${if (lang == AppLanguage.HINDI) k.planetNameHi else k.planetNameEn} (%.1f°)".format(k.degreeInSign), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        report.karakamsha?.let { km ->
            PanchangaCard {
                SectionHeader(ext.karakamshaTitle)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kārakāṃśa: ${VedicLocalizer.rasiNameByZeroIndex(km.karakamshaSignIndex, lang)} (H${km.karakamshaHouseInD1}) · Svāṃśa: ${VedicLocalizer.rasiNameByZeroIndex(km.swamshaSignIndex, lang)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (km.planetsInKarakamshaD9.isNotEmpty() || km.planetsAspectingKarakamshaD9.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "D9: ${km.planetsInKarakamshaD9.joinToString(", ").ifEmpty { "—" }} | Drishti: ${km.planetsAspectingKarakamshaD9.joinToString(", ").ifEmpty { "—" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (report.arudhaPadas.isNotEmpty()) {
            PanchangaCard {
                SectionHeader(ext.arudhaPadasTitle)
                Spacer(modifier = Modifier.height(8.dp))
                report.arudhaPadas.forEachIndexed { idx, a ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 5.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${a.code} (${if (lang == AppLanguage.HINDI) a.nameHi else a.nameEn})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (a.isExceptionApplied) {
                                Text(if (lang == AppLanguage.HINDI) a.exceptionReasonHi else a.exceptionReasonEn, style = MaterialTheme.typography.labelSmall, color = vedic.caution)
                            }
                        }
                        StatusPill("${VedicLocalizer.rasiShortByZeroIndex(a.signIndex, lang)} (H${a.finalHouseFromLagna})", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    }
                }
            }
        }

        if (report.jaiminiRashiDrishti.isNotEmpty()) {
            PanchangaCard {
                SectionHeader(ext.rashiDrishtiTitle)
                Spacer(modifier = Modifier.height(8.dp))
                report.jaiminiRashiDrishti.forEachIndexed { idx, rd ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${VedicLocalizer.rasiShortByZeroIndex(rd.signIndex, lang)} (${if (lang == AppLanguage.HINDI) rd.mobilityHi else rd.mobilityEn})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("→ ${rd.aspectedSignIndices.joinToString(", ") { VedicLocalizer.rasiShortByZeroIndex(it, lang) }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (report.charaDashas.isNotEmpty()) {
            PanchangaCard {
                SectionHeader(ext.charaDashaTitle)
                Spacer(modifier = Modifier.height(8.dp))
                report.charaDashas.forEachIndexed { idx, cd ->
                    val open = expandedDashaIdx == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (cd.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, if (cd.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).clickable { expandedDashaIdx = if (open) null else idx }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${VedicLocalizer.rasiNameByZeroIndex(cd.signIndex, lang)} (${cd.durationYears}y)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${cd.startDate} – ${cd.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (cd.isCurrent) StatusPill(strings.active, vedic.auspicious, vedic.auspiciousContainer)
                            }
                            if (open && cd.subPeriods.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                cd.subPeriods.forEach { sp ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("↳ ${VedicLocalizer.rasiShortByZeroIndex(sp.signIndex, lang)} (${sp.durationMonths}m)", style = MaterialTheme.typography.labelSmall)
                                        Text("${sp.startDate} – ${sp.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
