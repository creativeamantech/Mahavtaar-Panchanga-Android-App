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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.GoogleDriveBackupStatus
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.SavedNavtaraReport
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.export.TimezoneCatalog
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NavtaraDriveJsonSyncCard(
    cloudUser: CloudUser?,
    driveStatus: GoogleDriveBackupStatus,
    onConnectDrive: () -> Unit,
    onSyncJsonNow: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onExportJsonFile: () -> Unit,
    onImportJsonFile: () -> Unit,
    onLinkDriveFile: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val isHi = lang == AppLanguage.HINDI
    val accountLabel = cloudUser?.displayName?.takeIf { it.isNotBlank() }
        ?: cloudUser?.email
        ?: (if (isHi) "लोकल डिवाइस" else "Local Device")

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "आपका निजी Google Drive ऑटो-सिंक" else "Your Personal Google Drive Auto-Sync",
            subtitle = "${ext.freeStorageNoticeLabel} ($accountLabel)"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (driveStatus.isDriveLinked) {
                "${ext.driveConnectedLabel}: ${driveStatus.linkedFileName ?: "mahavtaar_panchanga_backup.json"}"
            } else {
                ext.driveNotConnectedLabel
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (driveStatus.isDriveLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!driveStatus.lastReplacedOldFileId.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ ${ext.driveReplacedOldNewBadge}",
                style = MaterialTheme.typography.labelSmall,
                color = vedic.auspicious
            )
        }

        driveStatus.lastLocalVaultAtMs?.let { ts ->
            val fmt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss"))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${ext.localVaultSavedLabel}: $fmt (${driveStatus.navtaraProfileCount}P · ${driveStatus.navtaraFeedbackCount}F · ${driveStatus.savedReportCount}R)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        driveStatus.lastDriveSyncAtMs?.let { ts ->
            val fmt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss"))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${ext.lastSyncedLabel}: $fmt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        driveStatus.lastError?.let { err ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = err, style = MaterialTheme.typography.bodySmall, color = vedic.inauspicious)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { if (driveStatus.isDriveLinked) onSyncJsonNow() else onConnectDrive() },
            enabled = !driveStatus.isBackingUp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("navtara_drive_sync_button")
        ) {
            if (driveStatus.isBackingUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(ext.syncingNowLabel)
            } else {
                Icon(
                    imageVector = if (driveStatus.isDriveLinked) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (driveStatus.isDriveLinked) ext.driveSyncJsonNowButton else ext.connectGoogleDriveButton)
            }
        }

        if (!driveStatus.isDriveLinked || driveStatus.isOAuthBlockedByConsentScreen) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLinkDriveFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("navtara_drive_link_saf_button")
            ) {
                Text(
                    if (isHi) "Google Drive फ़ाइल लिंक करें (बिना 403 त्रुटि ऑटो-सिंक)"
                    else "Link File in Your Google Drive (Direct Auto-Sync)"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onRestoreFromDrive,
                enabled = driveStatus.isDriveLinked && !driveStatus.isBackingUp,
                modifier = Modifier.heightIn(min = 44.dp).testTag("navtara_drive_restore_button")
            ) {
                Text(ext.driveRestoreJsonButton)
            }
            OutlinedButton(
                onClick = onExportJsonFile,
                modifier = Modifier.heightIn(min = 44.dp).testTag("navtara_export_json_button")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(ext.saveJsonFile)
            }
            OutlinedButton(
                onClick = onImportJsonFile,
                modifier = Modifier.heightIn(min = 44.dp).testTag("navtara_import_json_button")
            ) {
                Text(ext.importJsonFile)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NavtaraJournalHistoryCard(
    feedbacksByDate: Map<String, NavtaraDailyFeedback>,
    selectedFilter: NavtaraFeedbackRating?,
    onSelectFilter: (NavtaraFeedbackRating?) -> Unit,
    onEditFeedback: (NavtaraDailyFeedback) -> Unit,
    onDeleteFeedback: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val allEntries = feedbacksByDate.values.sortedByDescending { it.dateIso }
    val filtered = if (selectedFilter == null) allEntries else allEntries.filter { it.rating == selectedFilter }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            SectionHeader(ext.navtaraJournalHistoryTitle, "${allEntries.size}", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onSelectFilter(null) },
                label = { Text(ext.filterAllDays) },
                modifier = Modifier.testTag("navtara_history_filter_all")
            )
            NavtaraFeedbackRating.entries.forEach { rating ->
                FilterChip(
                    selected = selectedFilter == rating,
                    onClick = { onSelectFilter(rating) },
                    label = {
                        Text(
                            text = "${rating.emoji} ${if (lang == AppLanguage.HINDI) rating.labelHi else rating.labelEn}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("navtara_history_filter_${rating.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text(
                text = ext.navtaraNoJournalHistoryMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filtered.forEachIndexed { idx, fb ->
                if (idx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                val accent = when (fb.rating) {
                    NavtaraFeedbackRating.AUSPICIOUS -> vedic.auspicious
                    NavtaraFeedbackRating.NEUTRAL -> vedic.caution
                    NavtaraFeedbackRating.CAUTION -> vedic.inauspicious
                }
                val bg = when (fb.rating) {
                    NavtaraFeedbackRating.AUSPICIOUS -> vedic.auspiciousContainer
                    NavtaraFeedbackRating.NEUTRAL -> vedic.cautionContainer
                    NavtaraFeedbackRating.CAUTION -> vedic.inauspiciousContainer
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(fb.dateIso, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                StatusPill(
                                    label = "${fb.rating.emoji} ${if (lang == AppLanguage.HINDI) fb.rating.labelHi else fb.rating.labelEn}",
                                    textColor = accent,
                                    containerColor = bg
                                )
                            }
                            if (fb.note.isNotBlank()) {
                                Text(fb.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row {
                            IconButton(
                                onClick = { onEditFeedback(fb) },
                                modifier = Modifier.size(48.dp).testTag("navtara_history_edit_${fb.dateIso}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = ext.editProfileLabel, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { onDeleteFeedback(fb.dateIso) },
                                modifier = Modifier.size(48.dp).testTag("navtara_history_delete_${fb.dateIso}")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = ext.actionDeleteLabel, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NavtaraSavedReportsSection(
    savedReports: List<SavedNavtaraReport>,
    onOpenReport: (SavedNavtaraReport) -> Unit,
    onExportJson: () -> Unit,
    onDeleteReport: (Long) -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    PanchangaCard {
        SectionHeader(ext.savedReportsLabel, "${savedReports.size}")
        Spacer(modifier = Modifier.height(8.dp))

        if (savedReports.isEmpty()) {
            Text(
                text = ext.noSavedReportsMsg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            savedReports.forEachIndexed { idx, rep ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))
                val birthStar = VedicLocalizer.nakshatraNameByNumber(rep.birthNakshatraIndex, lang)
                val coords = TimezoneCatalog.formatCoordinates(rep.latitude, rep.longitude, 5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rep.profileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$birthStar (P${rep.birthPada}) · ${rep.startDateIso} → ${rep.endDateIso}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${rep.locationName} · $coords · ${rep.timezone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteReport(rep.id) },
                            modifier = Modifier.size(48.dp).testTag("navtara_delete_saved_report_${rep.id}")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = ext.actionDeleteLabel, tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill("${ext.auspiciousDaysLabel}: ${rep.auspiciousCount}", vedic.auspicious, vedic.auspiciousContainer)
                        StatusPill("${ext.mixedDaysLabel}: ${rep.mixedCount}", vedic.caution, vedic.cautionContainer)
                        StatusPill("${ext.cautionDaysLabel}: ${rep.cautionCount}", vedic.inauspicious, vedic.inauspiciousContainer)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenReport(rep) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("navtara_open_saved_report_${rep.id}")
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(ext.actionOpenLabel)
                        }
                        Button(
                            onClick = onExportJson,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("navtara_export_saved_json_${rep.id}")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(ext.saveJsonFile)
                        }
                    }
                }
            }
        }
    }
}
