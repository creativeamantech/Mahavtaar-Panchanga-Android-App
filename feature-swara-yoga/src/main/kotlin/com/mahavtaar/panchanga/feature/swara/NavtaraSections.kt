package com.mahavtaar.panchanga.feature.swara

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.TaraVedhaAnalysis
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BirthNakshatraMetaChipsCard(birthNakshatra: Int, birthPada: Int) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val meta = VedicSwaraNavtaraCatalog.nakshatraMeta(birthNakshatra)
    val purushartha = VedicSwaraNavtaraCatalog.padaPurushartha(birthPada)

    PanchangaCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetaChip("${ext.nakshatraLordLabel}: ${if (lang == AppLanguage.HINDI) meta.lordHi else meta.lordEn}")
            MetaChip("${ext.nakshatraDeityLabel}: ${if (lang == AppLanguage.HINDI) meta.deityHi else meta.deityEn}")
            MetaChip("${ext.nakshatraGanaLabel}: ${if (lang == AppLanguage.HINDI) meta.ganaHi else meta.ganaEn}")
            MetaChip("${ext.nakshatraNadiLabel}: ${if (lang == AppLanguage.HINDI) meta.nadiHi else meta.nadiEn}")
            MetaChip(VedicLocalizer.tattvaName(meta.tattva, lang))
            MetaChip("${ext.purusharthaLabel}: ${if (lang == AppLanguage.HINDI) purushartha.nameHi else purushartha.nameEn}")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) purushartha.guidanceHi else purushartha.guidanceEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun GandantaStatusCard(birthNakshatra: Int, birthPada: Int) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val alert = VedicSwaraNavtaraCatalog.checkGandanta(birthNakshatra, birthPada)
    val accent = when {
        alert.isGandanta -> vedic.inauspicious
        alert.isMoola -> vedic.caution
        else -> vedic.auspicious
    }
    val bg = when {
        alert.isGandanta -> vedic.inauspiciousContainer
        alert.isMoola -> vedic.cautionContainer
        else -> vedic.auspiciousContainer
    }

    PanchangaCard(containerColor = bg.copy(alpha = 0.3f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ext.gandantaWarningTitle, style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.weight(1f))
            StatusPill(
                label = when {
                    alert.isGandanta -> ext.gandantaStatusSandhi
                    alert.isMoola -> ext.gandantaStatusMoola
                    else -> ext.gandantaStatusClear
                },
                textColor = accent,
                containerColor = bg
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) alert.titleHi else alert.titleEn,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) alert.guidanceHi else alert.guidanceEn,
            style = MaterialTheme.typography.bodySmall
        )
        val ritual = if (lang == AppLanguage.HINDI) alert.ritualHi else alert.ritualEn
        if (ritual.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• $ritual",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun VedhaOverrideCard(vedha: TaraVedhaAnalysis, todayResult: NavtaraResult) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val accent = if (vedha.hasOverride) vedic.caution else vedic.auspicious

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ext.vedhaOverrideTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            StatusPill(
                label = "${todayResult.score} → ${vedha.adjustedScore}%",
                textColor = accent,
                containerColor = accent.copy(alpha = 0.14f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (lang == AppLanguage.HINDI) vedha.summaryHi else vedha.summaryEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun LiveStarTransitsCard(snapshot: PanchangaSnapshot, birthNakshatra: Int) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val strings = LocalStrings.current
    val nakTransits = snapshot.upcomingTransits.filter { it.type.equals("nakshatra", true) }.take(6)

    PanchangaCard {
        SectionHeader(ext.liveStarTransitTitle)
        Spacer(modifier = Modifier.height(8.dp))
        snapshot.planets.take(7).forEachIndexed { idx, p ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))
            val dist = ((p.nakshatraNumber - birthNakshatra + 27) % 27) + 1
            val taraIdx = if (dist % 9 == 0) 9 else dist % 9
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${p.symbol} ${VedicLocalizer.planetName(p.id, p.name, lang)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${VedicLocalizer.nakshatraName(p.nakshatraNumber, p.nakshatra, lang)} · ${strings.padaLabel} ${p.pada} (T$taraIdx)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (nakTransits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            nakTransits.forEach { ev ->
                val pName = VedicLocalizer.planetName(ev.planetId, ev.planetName, lang)
                val dest = VedicLocalizer.transitValue(ev.type, ev.toValue, ev.toSanskrit, lang)
                val rel = VedicLocalizer.transitRelativeText(ev.relativeText, lang)
                Text(
                    text = "• $pName → $dest (${ev.dateStr} · $rel)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun TaraDasaTableCard(birthNakshatra: Int) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val rows = VedicSwaraNavtaraCatalog.computeTaraDasa(birthNakshatra)

    PanchangaCard {
        SectionHeader(ext.taraDasaTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEachIndexed { idx, row ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${row.taraNumber}. ${if (lang == AppLanguage.HINDI) row.taraNameHi else row.taraNameEn}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (lang == AppLanguage.HINDI) row.planetHi else row.planetEn} · ${row.years}y",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
