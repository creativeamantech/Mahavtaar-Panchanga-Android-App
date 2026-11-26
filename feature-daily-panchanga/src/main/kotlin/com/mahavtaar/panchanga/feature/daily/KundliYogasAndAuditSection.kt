package com.mahavtaar.panchanga.feature.daily

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.GocharReport
import com.mahavtaar.panchanga.domain.model.KundliJsonExporter
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.domain.model.KundliYogaSummary
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AtmosphericHeroCard
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.export.HtmlReportPreviewDialog
import com.mahavtaar.panchanga.ui.export.KundliHtmlBuilder
import com.mahavtaar.panchanga.ui.export.OfflineReportExportHelper
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.UiReportFormatter
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private enum class YogaFilter { ALL, PRESENT, MITIGATED }

@Composable
internal fun KundliYogasSection(report: KundliReport) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    var filter by remember { mutableStateOf(YogaFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedYoga by remember { mutableStateOf<KundliYogaSummary?>(null) }

    val filtered = remember(report.yogas, filter, searchQuery, lang) {
        val q = searchQuery.trim().lowercase()
        report.yogas.filter { y ->
            val matchTab = when (filter) {
                YogaFilter.ALL -> true
                YogaFilter.PRESENT -> y.isPresent && !y.isMitigated
                YogaFilter.MITIGATED -> y.isMitigated
            }
            val matchQuery = q.isEmpty() ||
                y.nameEn.lowercase().contains(q) ||
                y.nameHi.lowercase().contains(q) ||
                y.descriptionEn.lowercase().contains(q) ||
                y.descriptionHi.lowercase().contains(q)
            matchTab && matchQuery
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanchangaCard {
            SectionHeader(ext.cancellationsLabel.substringAfter("/").trim().ifEmpty { "Yogas" }, VedicLocalizer.rasiNameByZeroIndex(report.lagnaSignIndex, lang))
            Spacer(modifier = Modifier.height(8.dp))
            StatusPill(if (lang == AppLanguage.HINDI) report.doshas.manglikLabelHi else report.doshas.manglikLabelEn, if (report.doshas.isManglik) vedic.inauspicious else vedic.auspicious, if (report.doshas.isManglik) vedic.inauspiciousContainer else vedic.auspiciousContainer)
            Spacer(modifier = Modifier.height(6.dp))
            StatusPill(if (lang == AppLanguage.HINDI) report.doshas.kalsarpaLabelHi else report.doshas.kalsarpaLabelEn, if (report.doshas.isKalsarpa) vedic.caution else vedic.auspicious, if (report.doshas.isKalsarpa) vedic.cautionContainer else vedic.auspiciousContainer)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(ext.searchYogasHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("yogas_search_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == YogaFilter.ALL, onClick = { filter = YogaFilter.ALL }, label = { Text(ext.filterYogasAll) })
            FilterChip(selected = filter == YogaFilter.PRESENT, onClick = { filter = YogaFilter.PRESENT }, label = { Text(ext.filterYogasPresent) })
            FilterChip(selected = filter == YogaFilter.MITIGATED, onClick = { filter = YogaFilter.MITIGATED }, label = { Text(ext.filterYogasMitigated) })
        }

        filtered.forEachIndexed { idx, y ->
            val accent = when {
                y.isMitigated -> vedic.caution
                y.isPresent && y.isAuspicious -> vedic.auspicious
                y.isPresent -> vedic.inauspicious
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            var expanded by remember(y.nameEn) { mutableStateOf(idx == 0) }
            ExpandableInfoCard(
                title = if (lang == AppLanguage.HINDI) y.nameHi else y.nameEn,
                subtitle = if (lang == AppLanguage.HINDI) y.sourceHi else y.sourceEn,
                expanded = expanded,
                onToggleExpand = { expanded = !expanded },
                accentColor = accent,
                trailingBadge = {
                    StatusPill(y.category, accent, accent.copy(alpha = 0.14f))
                }
            ) {
                Text(if (lang == AppLanguage.HINDI) y.descriptionHi else y.descriptionEn, style = MaterialTheme.typography.bodySmall)
                val matched = if (lang == AppLanguage.HINDI) y.matchedConditionsHi else y.matchedConditionsEn
                if (matched.isNotEmpty()) {
                    Text("${ext.matchedConditionsLabel}: ${matched.joinToString("; ")}", style = MaterialTheme.typography.labelSmall, color = vedic.auspicious)
                }
                val cancels = if (lang == AppLanguage.HINDI) y.cancellationsHi else y.cancellationsEn
                if (cancels.isNotEmpty()) {
                    Text("${ext.cancellationsLabel}: ${cancels.joinToString("; ")}", style = MaterialTheme.typography.labelSmall, color = vedic.caution)
                }
                OutlinedButton(onClick = { inspectedYoga = y }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.details)
                }
            }
        }
    }

    inspectedYoga?.let { y ->
        val accent = when {
            y.isMitigated -> vedic.caution
            y.isPresent && y.isAuspicious -> vedic.auspicious
            y.isPresent -> vedic.inauspicious
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val matched = if (lang == AppLanguage.HINDI) y.matchedConditionsHi else y.matchedConditionsEn
        val cancels = if (lang == AppLanguage.HINDI) y.cancellationsHi else y.cancellationsEn
        InspectorBottomSheetFrame(
            title = if (lang == AppLanguage.HINDI) y.nameHi else y.nameEn,
            subtitle = if (lang == AppLanguage.HINDI) y.sourceHi else y.sourceEn,
            icon = Icons.Rounded.AutoAwesome,
            accentColor = accent,
            statusLabel = y.category,
            sheetTestTag = "yoga_inspector_sheet",
            onDismiss = { inspectedYoga = null }
        ) {
            Text(
                text = if (lang == AppLanguage.HINDI) y.descriptionHi else y.descriptionEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (matched.isNotEmpty()) {
                Text(ext.matchedConditionsLabel, style = MaterialTheme.typography.labelLarge, color = vedic.auspicious, fontWeight = FontWeight.Bold)
                matched.forEach { cond ->
                    Text("• $cond", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (cancels.isNotEmpty()) {
                Text(ext.cancellationsLabel, style = MaterialTheme.typography.labelLarge, color = vedic.caution, fontWeight = FontWeight.Bold)
                cancels.forEach { cancel ->
                    Text("• $cancel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KundliGocharSection(
    report: KundliReport,
    selectedDate: LocalDate,
    gocharReport: GocharReport?,
    onSelectDate: (LocalDate) -> Unit,
    onResetToday: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    var showDatePicker by remember { mutableStateOf(false) }

    val transits = gocharReport?.transits ?: report.gocharEffects
    val inSadeSati = gocharReport?.inSadeSati ?: report.doshas.sadeSatiActive
    val sadePhase = if (lang == AppLanguage.HINDI) {
        (gocharReport?.sadeSatiPhaseHi ?: report.sadeSatiPhaseHi).ifBlank { report.doshas.sadeSatiLabelHi }
    } else {
        (gocharReport?.sadeSatiPhaseEn ?: report.sadeSatiPhaseEn).ifBlank { report.doshas.sadeSatiLabelEn }
    }
    val sadeDesc = if (lang == AppLanguage.HINDI) gocharReport?.sadeSatiDescHi ?: report.sadeSatiDescHi else gocharReport?.sadeSatiDescEn ?: report.sadeSatiDescEn

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanchangaCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ext.transitDateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(VedicLocalizer.formatHeaderDate(selectedDate, lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.testTag("gochar_change_date")) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ext.selectTransitDate)
                    }
                    TextButton(onClick = onResetToday, modifier = Modifier.testTag("gochar_reset_now")) {
                        Text(ext.resetTransitNow)
                    }
                }
            }
        }

        AtmosphericHeroCard(
            accentBorder = (if (inSadeSati) vedic.caution else vedic.auspicious).copy(alpha = 0.55f)
        ) {
            SectionHeader(ext.sadeSatiCardTitle, sadePhase)
            if (sadeDesc.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(sadeDesc, style = MaterialTheme.typography.bodySmall)
            }
        }

        transits.forEach { g ->
            val accent = when {
                g.hasVedha -> vedic.caution
                g.isFavorable -> vedic.auspicious
                else -> vedic.inauspicious
            }
            var expanded by remember(g.planetId, selectedDate) { mutableStateOf(g.hasVedha) }
            ExpandableInfoCard(
                title = "${if (lang == AppLanguage.HINDI) g.nameHi else g.nameEn} → ${VedicLocalizer.rasiNameByZeroIndex(g.transitSignIndex, lang)} (%.1f°)".format(g.transitDegreeInSign),
                subtitle = if (lang == AppLanguage.HINDI) g.verdictHi else g.verdictEn,
                expanded = expanded,
                onToggleExpand = { expanded = !expanded },
                accentColor = accent,
                trailingBadge = {
                    StatusPill("H${g.houseFromMoon} / D1:H${g.houseFromLagna}", accent, accent.copy(alpha = 0.14f))
                }
            ) {
                Text(if (lang == AppLanguage.HINDI) g.verdictHi else g.verdictEn, style = MaterialTheme.typography.bodySmall)
                if (g.hasVedha) {
                    val vp = if (lang == AppLanguage.HINDI) g.vedhaPlanetNameHi else g.vedhaPlanetNameEn
                    Text("${ext.vedhaAlertLabel}: ${vp.orEmpty()}", style = MaterialTheme.typography.labelMedium, color = vedic.caution, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDatePicker) {
        val initMs = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        onSelectDate(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(strings.select) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
internal fun KundliAuditExportSection(
    report: KundliReport,
    onImportJson: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val context = LocalContext.current
    val policy = LocalAccessPolicy.current
    val canHtmlExport = policy.hasFeature(AppFeatureKey.HTML_EXPORT_PRINT)
    val canSaveImport = policy.hasFeature(AppFeatureKey.KUNDLI_SAVE_IMPORT)
    val audit = report.audit
    var previewHtml by remember { mutableStateOf<String?>(null) }

    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            val html = KundliHtmlBuilder.build(report, lang)
            OfflineReportExportHelper.writeTextToUri(context, uri, html)
                .onSuccess { onStatusMessage(ext.htmlSavedMsg) }
                .onFailure { err -> onStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = KundliJsonExporter.toJson(report)
            OfflineReportExportHelper.writeTextToUri(context, uri, json)
                .onSuccess { onStatusMessage(ext.jsonSavedMsg) }
                .onFailure { err -> onStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (audit != null) {
            PanchangaCard {
                SectionHeader(ext.auditProvenanceTitle, audit.engineVersion)
                Spacer(modifier = Modifier.height(8.dp))
                AuditRow("Hash", audit.chartHash)
                AuditRow("Julian Day", "%.6f".format(Locale.US, audit.julianDay))
                AuditRow("LST", "%.4f°".format(Locale.US, audit.lstDeg))
                AuditRow("Ayanamsha", "${audit.ayanamshaModel} (%.5f°)".format(Locale.US, audit.ayanamshaDeg))
                AuditRow("Coords", "%.5f°, %.5f°".format(Locale.US, audit.latitude, audit.longitude))
                AuditRow("Timezone", audit.timezone)
            }
        }

        PanchangaCard {
            SectionHeader(ext.calculationConstantsTitle, VedicLocalizer.ayanamshaLabel(report.input.ayanamsha, lang))
            Spacer(modifier = Modifier.height(8.dp))
            AuditRow(ext.ayanamshaFormulaLabel, "Citra @ 180°00'00\" · Lahiri True Citra")
            AuditRow(ext.houseSystemLabel, "Whole Sign (Rasi) · Placidus KP Sub")
            AuditRow(ext.siderealYearDaysLabel, "365.256363 d (Dasha) · 365.242199 d (Solar)")
            AuditRow("Precision", "5 decimals (~1.1 m) · Locale.US")
        }

        if (canHtmlExport) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val fileName = OfflineReportExportHelper.sanitizeFileName("kundli_${report.input.name}", "html")
                        saveHtmlLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_save_html_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.saveHtmlReport)
                }
                OutlinedButton(
                    onClick = { previewHtml = KundliHtmlBuilder.build(report, lang) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_preview_html_button")
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.previewAndPrintHtml)
                }
            }
        }

        if (canSaveImport) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val fileName = OfflineReportExportHelper.sanitizeFileName("kundli_${report.input.name}", "json")
                        saveJsonLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_save_json_file_button")
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.saveJsonFile)
                }
                OutlinedButton(
                    onClick = onImportJson,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_import_json_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.importJsonFile)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    UiReportFormatter.shareText(context, ext.exportTextReport, UiReportFormatter.formatKundliText(report, lang))
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_export_text")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(ext.exportTextReport)
            }
            if (canSaveImport) {
                OutlinedButton(
                    onClick = {
                        UiReportFormatter.shareText(context, ext.exportJsonReport, KundliJsonExporter.toJson(report))
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("kundli_export_json")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.exportJsonReport)
                }
            }
        }
    }

    if (canHtmlExport) {
        previewHtml?.let { html ->
            HtmlReportPreviewDialog(
                title = "${report.input.name} — ${ext.reportPreviewTitle}",
                htmlContent = html,
                suggestedFileName = OfflineReportExportHelper.sanitizeFileName("kundli_${report.input.name}", "html"),
                onDismiss = { previewHtml = null },
                onSavedMessage = onStatusMessage
            )
        }
    }
}

@Composable
private fun AuditRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}
