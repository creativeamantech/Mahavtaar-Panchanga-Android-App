package com.mahavtaar.panchanga.feature.swara

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.KaranaTattvaCycleReport
import com.mahavtaar.panchanga.domain.model.PanchaTattvaReport
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.components.TattvaMandalaCanvas
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TattvaHarmonyAlignmentCard(
    report: PanchaTattvaReport,
    timezone: String,
    onSelectElementFilter: (TattvaElement) -> Unit,
    showNadiPolarity: Boolean = true
) {
    val lang = LocalAppLanguage.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI

    val activeWindow = report.harmonyWindows.firstOrNull { it.isActive }
    val peakWindows = remember(report.harmonyWindows) {
        report.harmonyWindows.filter { it.matchedPillarsCount >= 3 }.take(6)
            .ifEmpty { report.harmonyWindows.take(5) }
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = if (isHi) "४-स्तम्भ तत्व सामंजस्य रिपोर्ट (Peak Harmony)" else "4-Pillar Tattva Alignment Report",
                subtitle = if (isHi) "वार + तिथि + करण + होरा तत्व का संयुक्त मिलान" else "Combined alignment of Vara, Tithi, Karana & Hora Tattva"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active 4-pillar summary strip
        val activeHoraEl = report.activeHoraTattva?.element
        val macroKaranaEl = report.karanaCycle?.activePeriod?.element ?: report.karanaTattva.element
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "वर्तमान ४-स्तम्भ स्थिति" else "Live 4-Pillar Snapshot",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val score = activeWindow?.matchedPillarsCount ?: 1
                    StatusPill(
                        label = if (isHi) "$score/4 स्तम्भ एकसमान" else "$score/4 Pillars Aligned",
                        textColor = if (score >= 3) vedic.auspicious else MaterialTheme.colorScheme.primary,
                        containerColor = if (score >= 3) vedic.auspiciousContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                }
                Text(
                    text = if (isHi) {
                        "• वार: ${VedicLocalizer.tattvaName(report.varaTattva.element, lang)}  |  • तिथि: ${VedicLocalizer.tattvaName(report.tithiTattva.element, lang)}\n" +
                            "• करण (६-मास): ${VedicLocalizer.tattvaName(macroKaranaEl, lang)}  |  • होरा: ${activeHoraEl?.let { VedicLocalizer.tattvaName(it, lang) } ?: "—"}"
                    } else {
                        "• Vara: ${VedicLocalizer.tattvaName(report.varaTattva.element, lang)}  |  • Tithi: ${VedicLocalizer.tattvaName(report.tithiTattva.element, lang)}\n" +
                            "• Karana (6M): ${VedicLocalizer.tattvaName(macroKaranaEl, lang)}  |  • Hora: ${activeHoraEl?.let { VedicLocalizer.tattvaName(it, lang) } ?: "—"}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                activeWindow?.let { win ->
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowTattvaSentence(win.element, lang),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                }
            }
        }

        if (peakWindows.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isHi) "आज के सर्वोच्च तत्व-सामंजस्य काल (कब से कब तक):" else "Today's Peak Harmony Windows (When Pillars Align):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            peakWindows.forEachIndexed { idx, win ->
                if (idx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectElementFilter(win.element) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusPill(
                                label = "${win.matchedPillarsCount}/4",
                                textColor = if (win.matchedPillarsCount >= 3) vedic.auspicious else MaterialTheme.colorScheme.primary,
                                containerColor = if (win.matchedPillarsCount >= 3) vedic.auspiciousContainer else MaterialTheme.colorScheme.primaryContainer
                            )
                            if (win.isActive) {
                                StatusPill(
                                    label = if (isHi) "सक्रिय" else "LIVE",
                                    textColor = vedic.auspicious,
                                    containerColor = vedic.auspiciousContainer
                                )
                            }
                            Text(
                                text = "${VedicLocalizer.horaRulerName(win.horaRuler, lang)} (#${win.horaIndex}) · ${win.durationMins}m",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (showNadiPolarity) {
                            SwaraNadiBadge(nadi = win.nadi)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TattvaBadge(element = win.element)
                    }
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(win.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(win.endTimeMs, timezone, lang),
                        accentColor = if (win.isActive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    Text(
                        text = if (isHi) win.matchedPillarsHi else win.matchedPillarsEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun VaraTattvaDetailedSectionCard(
    report: PanchaTattvaReport,
    showMasterTable: Boolean,
    onToggleMasterTable: () -> Unit,
    showSummary: Boolean = true,
    showMasterTableSection: Boolean = true,
    showNadiPolarity: Boolean = true
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = if (isHi) "१. वार तत्व एवं नाड़ी (Vara Tattva & Nadi)" else "1. Vara Tattva & Nadi (Weekday Element)",
                subtitle = if (isHi) "सूर्योदय से अगले सूर्योदय तक प्रभावी वार तत्व" else "Active from local sunrise to next sunrise"
            )
        }

        if (showSummary) {
            Spacer(modifier = Modifier.height(8.dp))

        // Today's Active Vara Banner
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TattvaMandalaCanvas(element = report.varaTattva.element, sizeDp = 42.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = VedicLocalizer.vaaraName(report.varaTattva.weekday, "", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                label = if (isHi) "आज" else "TODAY",
                                textColor = vedic.auspicious,
                                containerColor = vedic.auspiciousContainer
                            )
                        }
                        TattvaBadge(element = report.varaTattva.element)
                    }
                }
                if (showNadiPolarity) {
                    SwaraNadiBadge(nadi = report.varaTattva.nadi)
                }
            }
        }
        }

        if (showMasterTableSection) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onToggleMasterTable,
                modifier = Modifier.fillMaxWidth().testTag("tattva_toggle_vara_master")
            ) {
                Icon(
                    imageVector = if (showMasterTable) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showMasterTable) {
                        if (isHi) "७-वार तत्व तालिका छिपाएँ" else "Hide 7-Weekday Vara Tattva Table"
                    } else {
                        if (isHi) "संपूर्ण ७-वार तत्व एवं नाड़ी तालिका देखें" else "View Complete 7-Weekday Vara Tattva Table"
                    }
                )
            }

            if (showMasterTable) {
                Spacer(modifier = Modifier.height(10.dp))
                report.varaMasterTable.forEachIndexed { idx, row ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isHi) row.dayNameHi else row.dayNameEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Medium
                                )
                                if (row.isToday) {
                                    StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                                }
                            }
                            Text(
                                text = if (isHi) row.elementLabelHi else row.elementLabelEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = tattvaColorFor(row.element),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (showNadiPolarity) {
                            SwaraNadiBadge(nadi = row.nadi)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TithiTattvaDetailedSectionCard(
    report: PanchaTattvaReport,
    timezone: String,
    showMasterTable: Boolean,
    onToggleMasterTable: () -> Unit,
    pakshaFilter: Int,
    onSelectPakshaFilter: (Int) -> Unit,
    showSummary: Boolean = true,
    showDayTable: Boolean = true,
    showMasterTableSection: Boolean = true,
    showNadiPolarity: Boolean = true
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI
    val tInfo = report.tithiTattva
    val startText = tInfo.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
        ?: tInfo.starts ?: (if (isHi) "पूर्व दिन से" else "Prev Day")
    val endText = tInfo.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
        ?: tInfo.ends ?: (if (isHi) "अगले दिन तक" else "Next Day")

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "२. तिथि तत्व एवं नाड़ी (Tithi Tattva & Nadi)" else "2. Tithi Tattva & Nadi (Start / End / Table)",
            subtitle = if (isHi) {
                "${VedicLocalizer.pakshaName(tInfo.paksha, lang)} · ${VedicLocalizer.tithiName(tInfo.tithiNumber, tInfo.tithiName, lang)} (#${tInfo.tithiInPaksha})"
            } else {
                "${tInfo.paksha} Pakṣa · ${tInfo.tithiName} (Tithi #${tInfo.tithiInPaksha})"
            }
        )

        if (showSummary) {
            Spacer(modifier = Modifier.height(10.dp))

            // Side-by-side Start Panel & End Panel (exact parity with Web App TattvaView.tsx)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "तिथि आरम्भ (START)" else "TITHI START",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = startText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    TattvaBadge(element = tInfo.element)
                    if (showNadiPolarity) {
                        SwaraNadiBadge(nadi = tInfo.nadi)
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "तिथि समाप्ति (END)" else "TITHI END",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = endText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    TattvaBadge(element = tInfo.element)
                    if (showNadiPolarity) {
                        SwaraNadiBadge(nadi = tInfo.endNadi)
                    }
                }
            }
        }

        if (showNadiPolarity) {
            Spacer(modifier = Modifier.height(8.dp))
            // 60-minute alternating Nadi bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHi) "६०-मिनट प्रत्यावर्ती तिथि नाड़ी (Live 60m Alternating Nadi)" else "Live 60-Min Alternating Tithi Nadi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHi) "तिथि आरम्भ से प्रत्येक ६० मिनट पर इड़ा ↔ पिंगला परिवर्तन" else "Alternates Ida ↔ Pingala every 60 mins from Tithi start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SwaraNadiBadge(nadi = tInfo.alternatingNadi)
            }
        }
        }

        // Today's Tithi Transitions Table
        if (showDayTable && report.dayTithiTattvas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isHi) "आज की तिथि समय-सारणी (Today's Tithi Table):" else "Today's Tithi Transitions Table:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            report.dayTithiTattvas.forEachIndexed { idx, item ->
                if (idx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                val itemStart = item.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                    ?: item.starts ?: (if (isHi) "पूर्व दिन से" else "Prev Day")
                val itemEnd = item.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                    ?: item.ends ?: (if (isHi) "अगले दिन तक" else "Next Day")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (item.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${VedicLocalizer.pakshaName(item.paksha, lang)} · ${VedicLocalizer.tithiName(item.number, item.name, lang)} (#${item.tithiInPaksha})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.isActive) {
                                StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                            }
                        }
                        Text(
                            text = if (isHi) item.groupNameHi else item.groupNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    EventStartEndDateTimeView(
                        startText = itemStart,
                        endText = itemEnd,
                        accentColor = if (item.isActive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    TattvaBadge(element = item.element)
                    if (showNadiPolarity) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isHi) "आरम्भ नाड़ी:" else "Start Nadi:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SwaraNadiBadge(nadi = item.nadi)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isHi) "समाप्ति नाड़ी:" else "End Nadi:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SwaraNadiBadge(nadi = item.endNadi)
                            }
                        }
                    }
                    if (item.isActive) {
                        LiveNowClarityBanner(
                            sentence = VedicLocalizer.simpleNowTattvaSentence(item.element, lang),
                            accentColor = vedic.auspicious,
                            containerColor = vedic.auspiciousContainer
                        )
                    }
                }
            }
        }

        if (showMasterTableSection) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onToggleMasterTable,
                modifier = Modifier.fillMaxWidth().testTag("tattva_toggle_tithi_master")
            ) {
                Icon(
                    imageVector = if (showMasterTable) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showMasterTable) {
                        if (isHi) "३०-तिथि तत्व तालिका छिपाएँ" else "Hide 30-Tithi Master Table"
                    } else {
                        if (isHi) "संपूर्ण ३०-तिथि (शुक्ल एवं कृष्ण) तत्व तालिका देखें" else "View Complete 30-Tithi (Shukla & Krishna) Master Table"
                    }
                )
            }

            if (showMasterTable) {
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val labels = if (isHi) listOf("सभी ३०", "शुक्ल (१–१५)", "कृष्ण (१–१५)") else listOf("All 30", "Śukla (1–15)", "Kṛṣṇa (1–15)")
                    labels.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = pakshaFilter == idx,
                            onClick = { onSelectPakshaFilter(idx) },
                            shape = SegmentedButtonDefaults.itemShape(idx, labels.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val filteredRows = remember(report.tithiMasterTable, pakshaFilter) {
                    when (pakshaFilter) {
                        1 -> report.tithiMasterTable.filter { it.rawNumber <= 15 }
                        2 -> report.tithiMasterTable.filter { it.rawNumber > 15 }
                        else -> report.tithiMasterTable
                    }
                }
                filteredRows.forEachIndexed { idx, row ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.isActiveToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isHi) "${row.pakshaHi} #${row.tithiInPaksha} · ${row.tithiNameHi}" else "${row.pakshaEn} #${row.tithiInPaksha} · ${row.tithiNameEn}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (row.isActiveToday) FontWeight.Bold else FontWeight.SemiBold
                                )
                                if (row.isActiveToday) {
                                    StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                                }
                            }
                            Text(
                                text = "${VedicLocalizer.tattvaName(row.element, lang)} · ${if (isHi) row.groupNameHi else row.groupNameEn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tattvaColorFor(row.element),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (showNadiPolarity) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isHi) {
                                        "आरम्भ: ${VedicLocalizer.nadiShortLabel(row.startNadi, lang)} → अन्त: ${VedicLocalizer.nadiShortLabel(row.endNadi, lang)}"
                                    } else {
                                        "Start: ${VedicLocalizer.nadiShortLabel(row.startNadi, lang)} → End: ${VedicLocalizer.nadiShortLabel(row.endNadi, lang)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
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
internal fun DailyKaranaTattvaSectionCard(
    report: PanchaTattvaReport,
    timezone: String,
    showMasterTable: Boolean,
    onToggleMasterTable: () -> Unit,
    showDayTable: Boolean = true,
    showMasterTableSection: Boolean = true,
    showNadiPolarity: Boolean = true
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "३. दैनिक करण तत्व तालिका (Daily Half-Tithi Karana Tattva)" else "3. Daily Half-Tithi Karana Tattva Table",
            subtitle = if (isHi) "आज के प्रत्येक अर्ध-तिथि करण का पक्ष, तिथि, नाड़ी एवं तत्व" else "Today's half-tithi Karana transitions with Paksha, Tithi, Nadi & Tattva"
        )
        if (showDayTable) {
            Spacer(modifier = Modifier.height(8.dp))

            report.dayKaranaTattvas.forEachIndexed { idx, item ->
            if (idx > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            val itemStart = item.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                ?: item.starts ?: (if (isHi) "पूर्व दिन से" else "Prev Day")
            val itemEnd = item.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                ?: item.ends ?: (if (isHi) "अगले दिन तक" else "Next Day")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (item.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = VedicLocalizer.karanaName(item.karanaName, lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.isActive) {
                            StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                        }
                        if (item.isBhadraCaution) {
                            StatusPill(
                                label = if (isHi) "भद्रा (विष्टि) सावधानी" else "Bhadrā (Viṣṭi) Caution",
                                textColor = vedic.inauspicious,
                                containerColor = vedic.inauspiciousContainer
                            )
                        }
                    }
                    if (showNadiPolarity) {
                        SwaraNadiBadge(nadi = item.nadi)
                    }
                }
                EventStartEndDateTimeView(
                    startText = itemStart,
                    endText = itemEnd,
                    accentColor = if (item.isActive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                    compact = true
                )
                TattvaBadge(element = item.element)
                Text(
                    text = if (isHi) {
                        "सम्बन्धित पक्ष/तिथि: ${VedicLocalizer.pakshaName(item.paksha, lang)} · ${VedicLocalizer.tithiName(item.tithiNumber, item.tithiName, lang)} (#${item.tithiNumber})"
                    } else {
                        "Linked Paksha/Tithi: ${item.paksha} · ${item.tithiName} (#${item.tithiNumber})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isActive) {
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowTattvaSentence(item.element, lang),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                }
            }
        }
        }

        if (showMasterTableSection) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onToggleMasterTable,
                modifier = Modifier.fillMaxWidth().testTag("tattva_toggle_karana_master")
            ) {
                Icon(
                    imageVector = if (showMasterTable) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showMasterTable) {
                        if (isHi) "११-करण तत्व संदर्भ तालिका छिपाएँ" else "Hide 11-Karana Master Reference Table"
                    } else {
                        if (isHi) "संपूर्ण ११-करण (७ चर + ४ स्थिर) तत्व तालिका देखें" else "View Complete 11-Karana (7 Chara + 4 Sthira) Master Table"
                    }
                )
            }

            if (showMasterTable) {
                Spacer(modifier = Modifier.height(10.dp))
                report.karanaMasterTable.forEachIndexed { idx, row ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.isActiveToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${row.number}. ${if (isHi) row.nameHi else row.nameEn}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPill(
                                    label = if (isHi) row.categoryHi else row.categoryEn,
                                    textColor = if (row.isBhadraCaution) vedic.inauspicious else MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = if (row.isBhadraCaution) vedic.inauspiciousContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            if (showNadiPolarity) {
                                SwaraNadiBadge(nadi = row.defaultNadi)
                            }
                        }
                        Text(
                            text = if (isHi) {
                                "तत्व: ${VedicLocalizer.tattvaName(row.element, lang)}  |  स्वामी: ${row.rulerHi}"
                            } else {
                                "Element: ${VedicLocalizer.tattvaName(row.element, lang)}  |  Ruler: ${row.rulerEn}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = tattvaColorFor(row.element),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isHi) row.effectHi else row.effectEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpgradedKaranaCycleCard(
    cycle: KaranaTattvaCycleReport,
    timezone: String,
    cycleOffset: Int,
    onSelectCycleOffset: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI
    val zone = remember(timezone) { TimeFormatters.safeZoneId(timezone) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }
    fun fmtDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().format(dateFmt)

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "४. ६-मासिक करण तत्व चक्र (Chaitra ↔ Sharad Cycle)" else "4. 6-Month Macro Karana Tattva Cycle",
            subtitle = "${fmtDate(cycle.startTimeMs)} – ${fmtDate(cycle.endTimeMs)} (${cycle.totalDurationDays}d ${cycle.totalDurationHours}h)"
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Cycle Switcher: Previous (-1) / Current (0) / Next (+1)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val opts = listOf(
                -1 to (if (isHi) "◀ पिछला चक्र" else "◀ Prev 6M"),
                0 to (if (isHi) "वर्तमान चक्र" else "Current Cycle"),
                1 to (if (isHi) "अगला चक्र ▶" else "Next 6M ▶")
            )
            opts.forEachIndexed { idx, (offsetVal, label) ->
                SegmentedButton(
                    selected = cycleOffset == offsetVal,
                    onClick = { onSelectCycleOffset(offsetVal) },
                    shape = SegmentedButtonDefaults.itemShape(idx, opts.size),
                    modifier = Modifier.testTag("karana_cycle_offset_$offsetVal")
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        StatusPill(
            label = if (cycle.cycleType == "CHAITRA_TO_SHARAD") {
                if (isHi) "चैत्र शुक्ल प्रतिपदा → शारदीय (आश्विन) प्रतिपदा" else "Chaitra Śukla Pratipadā → Śāradīya Pratipadā"
            } else {
                if (isHi) "शारदीय (आश्विन) प्रतिपदा → चैत्र शुक्ल प्रतिपदा" else "Śāradīya Pratipadā → Chaitra Śukla Pratipadā"
            },
            textColor = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )

        // Active Macro Element Hero Box (mirrors web app's Current Active Element panel)
        cycle.activePeriod?.let { active ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = tattvaColorFor(active.element).copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, tattvaColorFor(active.element)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "सक्रिय ६-मास करण तत्व" else "ACTIVE 6-MONTH KARANA TATTVA",
                            style = MaterialTheme.typography.labelSmall,
                            color = tattvaColorFor(active.element),
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(
                            label = "${"%.1f".format(Locale.US, active.percentage)}% (${active.weight}/15)",
                            textColor = tattvaColorFor(active.element),
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }
                    TattvaBadge(element = active.element)
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowTattvaSentence(active.element, lang),
                        accentColor = tattvaColorFor(active.element),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(active.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(active.endTimeMs, timezone, lang),
                        accentColor = tattvaColorFor(active.element)
                    )
                    Text(
                        text = if (isHi) {
                            "कुल अवधि: ${active.durationDays} दिन, ${active.durationHours} घंटे"
                        } else {
                            "Duration: ${active.durationDays} Days, ${active.durationHours} Hours"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        // Weighted Progress Strip (1:2:3:5:4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            cycle.periods.forEach { p ->
                Box(
                    modifier = Modifier
                        .weight(p.weight.coerceAtLeast(1).toFloat())
                        .fillMaxHeight()
                        .background(tattvaColorFor(p.element).copy(alpha = if (p.isActive) 1f else 0.38f))
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        cycle.periods.forEachIndexed { idx, p ->
            if (idx > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${idx + 1}.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TattvaBadge(element = p.element)
                        if (p.isActive) {
                            StatusPill(strings.active, vedic.auspicious, vedic.auspiciousContainer)
                        }
                    }
                    Text(
                        text = "${"%.1f".format(Locale.US, p.percentage)}% (${p.durationDays}d ${p.durationHours}h)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatDateTimeWithSeconds(p.startTimeMs, timezone, lang),
                    endText = VedicLocalizer.formatDateTimeWithSeconds(p.endTimeMs, timezone, lang),
                    accentColor = if (p.isActive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                    compact = true
                )
                if (p.maasOverlaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    p.maasOverlaps.forEach { ov ->
                        val maasDisp = VedicLocalizer.masaNameByText(ov.maasName, lang)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (ov.isAdhika) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = vedic.caution,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "• $maasDisp (${ov.overlapPercent}% · ${fmtDate(ov.startTimeMs)} – ${fmtDate(ov.endTimeMs)})" +
                                    if (ov.isAdhika) " — ${ext.adhikaMaasWarning}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ov.isAdhika) vedic.caution else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchableHoraSubTattvaTimelineCard(
    dailyHoras: DailyHoras,
    timezone: String,
    selectedElement: TattvaElement?,
    onSelectElement: (TattvaElement?) -> Unit,
    dayNightFilter: Int,
    onSelectDayNightFilter: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    nowMs: Long,
    showNadiPolarity: Boolean = true
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "५. २४-घंटे होरा उप-तत्व समय-सारणी (कब से कब तक)" else "5. 24-Hour Hora Sub-Tattva Explorer (When to When)",
            subtitle = if (isHi) "सभी २४ होरा × ५ तत्व = १२० दैनिक सूक्ष्म तत्व कालखंड खोजें" else "Search & filter all 24 Horas × 5 Sub-Tattvas (120 daily time windows)"
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tattva_timeline_search_input"),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = strings.back)
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (isHi) "समय (जैसे 09:30), ग्रह (गुरु/सूर्य) या तत्व खोजें..." else "Search time (e.g. 09:30), planet (Jupiter/Sun) or element...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5 Element Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedElement == null,
                onClick = { onSelectElement(null) },
                label = { Text(if (isHi) "सभी ५ तत्व (120)" else "All 5 Elements (120)") },
                modifier = Modifier.testTag("tattva_filter_all")
            )
            TattvaElement.entries.forEach { el ->
                FilterChip(
                    selected = selectedElement == el,
                    onClick = { onSelectElement(if (selectedElement == el) null else el) },
                    label = { Text(VedicLocalizer.tattvaName(el, lang)) },
                    modifier = Modifier.testTag("tattva_filter_${el.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day / Night Segmented Filter
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val tabs = if (isHi) listOf("संपूर्ण दिन-रात (24)", "दिन की होरा (12)", "रात्रि की होरा (12)")
            else listOf("All 24 Horas", "Day Horas (12)", "Night Horas (12)")
            tabs.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = dayNightFilter == idx,
                    onClick = { onSelectDayNightFilter(idx) },
                    shape = SegmentedButtonDefaults.itemShape(idx, tabs.size)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val matchingSlots = remember(dailyHoras, selectedElement, dayNightFilter, q, lang) {
            buildList {
                dailyHoras.horas.forEach { hora ->
                    if (dayNightFilter == 1 && !hora.isDay) return@forEach
                    if (dayNightFilter == 2 && hora.isDay) return@forEach
                    val rulerLoc = VedicLocalizer.horaRulerName(hora.ruler, lang)
                    val nadiLoc = VedicLocalizer.nadiShortLabel(hora.nadi, lang)
                    hora.tattvas.forEach { sub ->
                        if (selectedElement != null && sub.element != selectedElement) return@forEach
                        val elLoc = VedicLocalizer.tattvaName(sub.element, lang)
                        val qualityLoc = VedicLocalizer.tattvaQuality(sub.element, lang)
                        if (q.isNotEmpty()) {
                            val hay = "${hora.ruler} $rulerLoc $elLoc ${sub.element.name} ${sub.startTime} ${sub.endTime} $nadiLoc $qualityLoc #${hora.index}".lowercase(Locale.ROOT)
                            if (!hay.contains(q)) return@forEach
                        }
                        add(hora to sub)
                    }
                }
            }
        }

        Text(
            text = if (isHi) "कुल ${matchingSlots.size} तत्व समय-खंड मिले:" else "Showing ${matchingSlots.size} Tattva time windows:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        matchingSlots.forEachIndexed { idx, (hora, sub) ->
            val isLive = nowMs in sub.startTimeMs until sub.endTimeMs
            if (idx > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TattvaBadge(element = sub.element)
                            Text(
                                text = "(${sub.durationMins}${strings.minShort})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isLive) {
                                StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                            }
                        }
                        EventStartEndDateTimeView(
                            startText = VedicLocalizer.formatDateTimeWithSeconds(sub.startTimeMs, timezone, lang),
                            endText = VedicLocalizer.formatDateTimeWithSeconds(sub.endTimeMs, timezone, lang),
                            accentColor = if (isLive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                            compact = true
                        )
                        Text(
                            text = VedicLocalizer.tattvaQuality(sub.element, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isHi) {
                                "होरा #${hora.index}: ${VedicLocalizer.horaRulerName(hora.ruler, lang)} (${if (hora.isDay) "दिन" else "रात्रि"})"
                            } else {
                                "Hora #${hora.index}: ${VedicLocalizer.horaRulerName(hora.ruler, lang)} (${if (hora.isDay) "Day" else "Night"})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showNadiPolarity) {
                        SwaraNadiBadge(nadi = hora.nadi)
                    }
                }
                if (isLive) {
                    val progress = ((nowMs - sub.startTimeMs).toFloat() / sub.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowTattvaSentence(sub.element, lang),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = vedic.auspicious
                    )
                }
            }
        }
    }
}

internal fun tattvaColorFor(element: TattvaElement): Color = when (element) {
    TattvaElement.PRITHVI -> Color(0xFFD97706)
    TattvaElement.JALA -> Color(0xFF0284C7)
    TattvaElement.TEJAS -> Color(0xFFDC2626)
    TattvaElement.VAYU -> Color(0xFF0D9488)
    TattvaElement.AKASH -> Color(0xFF7C3AED)
}
