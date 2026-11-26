package com.mahavtaar.panchanga.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Computes a plain-language English or Hindi mute status line including exact resume time
 * whenever Instant DND, Timed Mute, or Quiet Hours is active.
 */
fun formatMuteStatusLine(
    audioSettings: AlertAudioSettings,
    timezone: String,
    use24HourFormat: Boolean,
    language: AppLanguage,
    nowMs: Long = System.currentTimeMillis()
): String? {
    val isHi = language == AppLanguage.HINDI
    val zone = TimeFormatters.safeZoneId(timezone)

    if (!audioSettings.masterEnabled) {
        return if (isHi) "सभी अलर्ट बंद हैं" else "All alerts are turned off"
    }
    if (audioSettings.manualDndEnabled) {
        return if (isHi) {
            "DND चालू · आपके बंद करने तक सभी अलर्ट शांत रहेंगे"
        } else {
            "DND Active · Alerts paused until you turn it off"
        }
    }
    val resumeMs = audioSettings.resolveNextResumeTimeMs(nowMs, zone)
    if (resumeMs != null) {
        val resumeFormatted = formatResumeTime(resumeMs, zone, use24HourFormat, language)
        val isTimed = audioSettings.activeTimedDndUntilMs(nowMs) != null
        return if (isTimed) {
            if (isHi) "अलर्ट अस्थायी रूप से शांत · $resumeFormatted पर फिर शुरू होंगे"
            else "Alerts paused · Resumes at $resumeFormatted"
        } else {
            if (isHi) "क्वाइट आवर्स सक्रिय · अलर्ट $resumeFormatted पर फिर शुरू होंगे"
            else "Quiet Hours active · Alerts resume at $resumeFormatted"
        }
    }
    return null
}

private fun formatResumeTime(
    epochMs: Long,
    zoneId: ZoneId,
    use24HourFormat: Boolean,
    language: AppLanguage
): String {
    val zdt = Instant.ofEpochMilli(epochMs).atZone(zoneId)
    val pattern = if (use24HourFormat) "HH:mm" else "hh:mm a"
    val locale = if (language == AppLanguage.HINDI) Locale.forLanguageTag("hi-IN") else Locale.ENGLISH
    return zdt.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/**
 * Subtle inline "?" glossary hint chip that opens the Vedic Glossary bottom sheet directly to [termQuery].
 */
@Composable
fun GlossaryHintChip(
    termQuery: String,
    contentLabel: String,
    onOpenGlossary: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val lang = LocalAppLanguage.current
    val policy = LocalAccessPolicy.current
    if (!policy.hasFeature(com.mahavtaar.panchanga.domain.model.AppFeatureKey.GLOSSARY_DICTIONARY)) return

    val cd = if (lang == AppLanguage.HINDI) "$contentLabel का अर्थ जानें" else "What is $contentLabel?"
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(CircleShape)
            .clickable(onClickLabel = cd) { onOpenGlossary(termQuery) },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Floating pinned "Back to Today" pill displayed whenever the user is viewing a non-today date.
 */
@Composable
fun StickyBackToTodayPill(
    visible: Boolean,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "sticky_back_to_today_pill"
) {
    val lang = LocalAppLanguage.current
    val label = if (lang == AppLanguage.HINDI) "आज पर वापस जाएं" else "Back to Today"
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            onClick = onBackToToday,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EventAvailable,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Reusable confirmation dialog for destructive or high-impact user actions
 * (Reset Defaults, Clear Cache, Sign Out).
 */
@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_dialog_ok")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_dialog_cancel")
            ) {
                Text(cancelLabel)
            }
        }
    )
}

/**
 * 1-sentence friendly permission walkthrough shown before launching Android system permission prompts
 * or system settings (Notifications, Exact Alarms, Location).
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    reason: String,
    withoutPermissionNote: String,
    allowLabel: String,
    notNowLabel: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = withoutPermissionNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                modifier = Modifier.testTag("permission_rationale_allow")
            ) {
                Text(allowLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("permission_rationale_later")
            ) {
                Text(notNowLabel)
            }
        }
    )
}

/**
 * Font-scale-safe choice selector that wraps gracefully on 1.3x–1.5x accessibility text sizes
 * and long Hindi labels without mid-word clipping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ResponsiveChoiceGroup(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    tagFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = labelFor(option),
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(tagFor(option))
            )
        }
    }
}

/**
 * Emphasizes the live countdown prominently while rendering exact Start & End timestamps
 * as clean muted secondary lines for effortless scannability.
 */
@Composable
fun LiveCountdownWithMutedTimestamps(
    countdownText: String,
    startText: String,
    endText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val startLabel = if (lang == AppLanguage.HINDI) "आरम्भ:" else "Start:"
    val endLabel = if (lang == AppLanguage.HINDI) "समाप्ति:" else "End:"
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = countdownText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
        Text(
            text = "$startLabel $startText  ·  $endLabel $endText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact Quick DND & Timed Mute Bottom Sheet accessible from Home Top Bar (1-tap mute/unmute).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimedDndQuickSheet(
    audioSettings: AlertAudioSettings,
    nextSunriseMs: Long?,
    timezone: String,
    use24HourFormat: Boolean,
    onUpdateAudioSettings: (AlertAudioSettings) -> Unit,
    onOpenNotificationStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nowMs = System.currentTimeMillis()
    val muteStatusLine = formatMuteStatusLine(audioSettings, timezone, use24HourFormat, lang, nowMs)
    val isAnyManualMute = audioSettings.isManualDndActive(nowMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isAnyManualMute) Icons.AutoMirrored.Rounded.VolumeOff else Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = if (isAnyManualMute) vedic.inauspicious else MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.notifDndAndQuietSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = muteStatusLine ?: strings.notifAlertsNormalStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (muteStatusLine != null) vedic.inauspicious else vedic.auspicious,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Instant ON/OFF DND toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(strings.notifInstantDndTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = strings.notifInstantDndSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = audioSettings.manualDndEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateAudioSettings(
                            audioSettings.copy(
                                manualDndEnabled = enabled,
                                manualDndUntilMs = null
                            )
                        )
                    },
                    modifier = Modifier.testTag("quick_dnd_sheet_instant_switch")
                )
            }

            // Quick Timed Mute Chips
            Text(
                text = if (isHi) "अस्थायी मूक अवधि (स्वयं चालू होगा)" else "Timed Mute (Auto-Resumes)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeUntil = audioSettings.activeTimedDndUntilMs(nowMs)
                FilterChip(
                    selected = activeUntil != null && (activeUntil - nowMs) in 1L..(75L * 60_000L),
                    onClick = {
                        onUpdateAudioSettings(
                            audioSettings.copy(
                                manualDndEnabled = false,
                                manualDndUntilMs = nowMs + 3_600_000L
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (isHi) "1 घण्टा मूक" else "Mute 1 hr") },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_dnd_mute_1h")
                )
                FilterChip(
                    selected = activeUntil != null && (activeUntil - nowMs) in (75L * 60_000L + 1L)..(210L * 60_000L),
                    onClick = {
                        onUpdateAudioSettings(
                            audioSettings.copy(
                                manualDndEnabled = false,
                                manualDndUntilMs = nowMs + 3L * 3_600_000L
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (isHi) "3 घण्टे मूक" else "Mute 3 hrs") },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_dnd_mute_3h")
                )
                val targetSunriseMs = nextSunriseMs?.takeIf { it > nowMs + 60_000L }
                if (targetSunriseMs != null) {
                    FilterChip(
                        selected = activeUntil != null && kotlin.math.abs(activeUntil - targetSunriseMs) < 120_000L,
                        onClick = {
                            onUpdateAudioSettings(
                                audioSettings.copy(
                                    manualDndEnabled = false,
                                    manualDndUntilMs = targetSunriseMs
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (isHi) "सूर्योदय तक" else "Until Sunrise") },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_dnd_mute_sunrise")
                    )
                }
                if (isAnyManualMute) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onUpdateAudioSettings(
                                audioSettings.copy(
                                    manualDndEnabled = false,
                                    manualDndUntilMs = null
                                )
                            )
                        },
                        label = { Text(if (isHi) "अभी चालू करें" else "Resume Alerts Now") },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_dnd_resume_now")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenNotificationStudio()
                    },
                    modifier = Modifier.weight(1f).testTag("quick_dnd_open_studio")
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHi) "सभी नियम बदलें" else "All Alert Rules",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("quick_dnd_done")
                ) {
                    Text(if (isHi) "ठीक है" else "Done")
                }
            }
        }
    }
}
