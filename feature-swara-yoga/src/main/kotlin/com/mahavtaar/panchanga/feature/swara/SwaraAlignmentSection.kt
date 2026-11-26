package com.mahavtaar.panchanga.feature.swara

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
internal fun AlignmentEvaluationCard(
    title: String,
    result: NakshatraSwaraAlignmentResult
) {
    val lang = LocalAppLanguage.current
    val vedic = LocalVedicColors.current
    val ext = LocalExtendedStrings.current
    val isAligned = result.rating.equals("ALIGNED", ignoreCase = true)
    val badgeColor = if (isAligned) vedic.auspicious else vedic.caution
    val badgeBg = if (isAligned) vedic.auspiciousContainer else vedic.cautionContainer

    val pakshaDisp = VedicLocalizer.pakshaName(result.paksha, lang)
    val tithiDisp = VedicLocalizer.tithiName(if (result.paksha.contains("K")) result.tithiNumber + 15 else result.tithiNumber, result.tithiName, lang)
    val nakDisp = VedicLocalizer.nakshatraName(result.nakshatraNumber, result.nakshatraName, lang)
    val rashiDisp = VedicLocalizer.rasiNameByZeroIndex(result.rashiIndex, lang)

    PanchangaCard(containerColor = badgeBg.copy(alpha = 0.25f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = if (isAligned) {
                    if (lang == AppLanguage.HINDI) "उत्तम सामञ्जस्य" else "ALIGNED"
                } else {
                    if (lang == AppLanguage.HINDI) "भिन्न ध्रुवता" else "MISMATCH"
                },
                textColor = badgeColor,
                containerColor = badgeBg
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$pakshaDisp $tithiDisp · $nakDisp (${ext.selectPadaLabel} ${result.pada}) · $rashiDisp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ext.selectTithiLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwaraNadiBadge(nadi = result.requiredTithiSwara)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(ext.selectNakshatraLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwaraNadiBadge(nadi = result.supportedNakshatraSwara)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) result.cosmicAdviceHi else result.cosmicAdviceEn,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) result.padaAdviceHi else result.padaAdviceEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val warn = if (lang == AppLanguage.HINDI) result.elementWarningHi else result.elementWarningEn
        if (!warn.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⚠ $warn",
                style = MaterialTheme.typography.bodySmall,
                color = vedic.caution
            )
        }
    }
}

@Composable
internal fun SwaraSimulatorCard(
    simulator: SwaraSimulatorState,
    onUpdate: (isShukla: Boolean, tithiNumber: Int, nakshatraNumber: Int, pada: Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val strings = LocalStrings.current

    PanchangaCard {
        SectionHeader(ext.swaraSimulatorTitle)
        Spacer(modifier = Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = simulator.isShukla,
                onClick = { onUpdate(true, simulator.tithiNumber, simulator.nakshatraNumber, simulator.pada) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("sim_paksha_shukla")
            ) { Text(ext.filterShukla) }
            SegmentedButton(
                selected = !simulator.isShukla,
                onClick = { onUpdate(false, simulator.tithiNumber, simulator.nakshatraNumber, simulator.pada) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("sim_paksha_krishna")
            ) { Text(ext.filterKrishna) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val absTithi = if (simulator.isShukla) simulator.tithiNumber else 15 + simulator.tithiNumber
        val tithiLabel = VedicLocalizer.tithiName(absTithi, "Tithi ${simulator.tithiNumber}", lang)
        SimulatorStepperRow(
            label = "${ext.selectTithiLabel}: $tithiLabel (${simulator.tithiNumber}/15)",
            onPrev = {
                val prev = if (simulator.tithiNumber <= 1) 15 else simulator.tithiNumber - 1
                onUpdate(simulator.isShukla, prev, simulator.nakshatraNumber, simulator.pada)
            },
            onNext = {
                val next = if (simulator.tithiNumber >= 15) 1 else simulator.tithiNumber + 1
                onUpdate(simulator.isShukla, next, simulator.nakshatraNumber, simulator.pada)
            },
            prevTag = "sim_tithi_prev",
            nextTag = "sim_tithi_next",
            descPrev = strings.previousDayCd,
            descNext = strings.nextDayCd
        )

        val nakLabel = "${simulator.nakshatraNumber}. ${VedicLocalizer.nakshatraNameByNumber(simulator.nakshatraNumber, lang)}"
        SimulatorStepperRow(
            label = "${ext.selectNakshatraLabel}: $nakLabel",
            onPrev = {
                val prev = if (simulator.nakshatraNumber <= 1) 27 else simulator.nakshatraNumber - 1
                onUpdate(simulator.isShukla, simulator.tithiNumber, prev, simulator.pada)
            },
            onNext = {
                val next = if (simulator.nakshatraNumber >= 27) 1 else simulator.nakshatraNumber + 1
                onUpdate(simulator.isShukla, simulator.tithiNumber, next, simulator.pada)
            },
            prevTag = "sim_nak_prev",
            nextTag = "sim_nak_next",
            descPrev = strings.previousDayCd,
            descNext = strings.nextDayCd
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(ext.selectPadaLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            (1..4).forEachIndexed { idx, p ->
                SegmentedButton(
                    selected = simulator.pada == p,
                    onClick = { onUpdate(simulator.isShukla, simulator.tithiNumber, simulator.nakshatraNumber, p) },
                    shape = SegmentedButtonDefaults.itemShape(idx, 4),
                    modifier = Modifier.testTag("sim_pada_$p")
                ) { Text("$p") }
            }
        }
    }
}

@Composable
private fun SimulatorStepperRow(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    prevTag: String,
    nextTag: String,
    descPrev: String,
    descNext: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp).testTag(prevTag)) {
                Icon(Icons.Rounded.Remove, contentDescription = descPrev, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp).testTag(nextTag)) {
                Icon(Icons.Rounded.Add, contentDescription = descNext, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun Star28MasterTableCard() {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current

    PanchangaCard {
        SectionHeader(ext.starMasterTableTitle)
        Spacer(modifier = Modifier.height(8.dp))
        VedicSwaraNavtaraCatalog.all28StarNadiRules.forEachIndexed { idx, star ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${star.number}. ${if (lang == AppLanguage.HINDI) star.nameHi else star.nameEn}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = VedicLocalizer.rasiNameByZeroIndex(star.rashiNum - 1, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(VedicLocalizer.nadiShortLabel(star.startNadi, lang), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("→", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(VedicLocalizer.nadiShortLabel(star.endNadi, lang), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
internal fun ZodiacNadiPolarityTableCard() {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current

    PanchangaCard {
        SectionHeader(ext.zodiacNadiTableTitle)
        Spacer(modifier = Modifier.height(8.dp))
        (1..12).forEach { rNum ->
            val (badge, _) = VedicGlossaryCatalog.rasiNadiBadge(rNum, lang)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$rNum. ${VedicLocalizer.rasiNameByZeroIndex(rNum - 1, lang)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusPill(
                    label = badge,
                    textColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            }
            if (rNum < 12) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}
