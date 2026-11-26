package com.mahavtaar.panchanga.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.localization.PanchangaStrings
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.minimumInteractiveComponentSize
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.ui.components.formatMuteStatusLine
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.util.Locale

@Composable
fun VoiceStudioSection(
    audioSettings: AlertAudioSettings,
    voiceRecordings: List<VoiceRecording>,
    isRecording: Boolean,
    recordingElapsedSec: Int,
    currentlyPlayingKey: String?,
    isHindi: Boolean,
    strings: PanchangaStrings,
    onUpdateAudioSettings: (AlertAudioSettings) -> Unit,
    onTestTattvaVoice: () -> Unit,
    onTestNadiVoice: () -> Unit,
    onStartRecording: (String) -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onPlayRecording: (VoiceRecording) -> Unit,
    onDeleteRecording: (VoiceRecording) -> Unit,
    timezone: String = "Asia/Kolkata",
    use24HourFormat: Boolean = false,
    nextSunriseMs: Long? = null
) {
    val context = LocalContext.current
    var clipTitle by remember(isHindi) {
        mutableStateOf(if (isHindi) "मेरा तत्व व नाड़ी मंत्र" else "My Vedic Alert Voice")
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording(clipTitle)
        }
    }

    fun requestMicAndRecord() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onStartRecording(clipTitle)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Built-in Offline Bilingual TTS Voice Controls
        PanchangaCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.notifBuiltInVoiceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.notifBuiltInVoiceSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.notifLanguageSyncNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.notifSpeechRateLabel, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = String.format(Locale.US, "%.2fx", audioSettings.ttsSpeechRate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = audioSettings.ttsSpeechRate,
                onValueChange = { onUpdateAudioSettings(audioSettings.copy(ttsSpeechRate = it)) },
                valueRange = 0.7f..1.4f,
                modifier = Modifier.testTag("voice_speed_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.notifSpeechPitchLabel, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = String.format(Locale.US, "%.2f", audioSettings.ttsPitch),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = audioSettings.ttsPitch,
                onValueChange = { onUpdateAudioSettings(audioSettings.copy(ttsPitch = it)) },
                valueRange = 0.8f..1.3f,
                modifier = Modifier.testTag("voice_pitch_slider")
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tattvaPlaying = currentlyPlayingKey == "tts_tattva"
                val nadiPlaying = currentlyPlayingKey == "tts_nadi"
                OutlinedButton(
                    onClick = onTestTattvaVoice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_test_tattva_btn")
                ) {
                    Icon(
                        imageVector = if (tattvaPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.notifTestTattvaVoiceButton)
                }
                OutlinedButton(
                    onClick = onTestNadiVoice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_test_nadi_btn")
                ) {
                    Icon(
                        imageVector = if (nadiPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.notifTestNadiVoiceButton)
                }
            }
        }

        // 2. Personal Voice Recorder Studio
        PanchangaCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.notifVoiceRecorderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.notifVoiceRecorderSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = clipTitle,
                onValueChange = { clipTitle = it },
                enabled = !isRecording,
                label = { Text(strings.notifClipNameLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_clip_name_input")
            )

            Spacer(Modifier.height(10.dp))

            if (isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_stop_save_btn")
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.notifStopRecordingButton} (${recordingElapsedSec}s / 25s)")
                    }
                    OutlinedButton(
                        onClick = onCancelRecording,
                        modifier = Modifier.testTag("voice_cancel_rec_btn")
                    ) {
                        Text(strings.notifCancelRecordingButton)
                    }
                }
            } else {
                Button(
                    onClick = { requestMicAndRecord() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_start_rec_btn")
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.notifStartRecordingButton)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Text(
                text = "${strings.notifSavedRecordingsTitle} (${voiceRecordings.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            if (voiceRecordings.isEmpty()) {
                Text(
                    text = strings.notifNoRecordingsYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    voiceRecordings.forEach { rec ->
                        val isPlaying = currentlyPlayingKey == "rec_${rec.id}"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onPlayRecording(rec) },
                                    modifier = Modifier.testTag("voice_play_clip_${rec.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play clip",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${(rec.durationMs / 1000L).coerceAtLeast(1)}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteRecording(rec) },
                                    modifier = Modifier.testTag("voice_delete_clip_${rec.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete clip",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. DND Mode & Quiet Hours Card
        DndAndQuietHoursCard(
            audioSettings = audioSettings,
            strings = strings,
            onUpdateAudioSettings = onUpdateAudioSettings,
            use24HourFormat = use24HourFormat,
            timezone = timezone,
            nextSunriseMs = nextSunriseMs
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DndAndQuietHoursCard(
    audioSettings: AlertAudioSettings,
    strings: PanchangaStrings,
    onUpdateAudioSettings: (AlertAudioSettings) -> Unit,
    modifier: Modifier = Modifier,
    use24HourFormat: Boolean = false,
    testTagPrefix: String = "",
    timezone: String = "Asia/Kolkata",
    nextSunriseMs: Long? = null
) {
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val vedic = LocalVedicColors.current
    val nowMs = System.currentTimeMillis()
    val isDndOn = audioSettings.isManualDndActive(nowMs)
    val activeTimedUntil = audioSettings.activeTimedDndUntilMs(nowMs)
    val isQuietOn = audioSettings.quietHoursEnabled
    val muteStatusLine = formatMuteStatusLine(
        audioSettings = audioSettings,
        timezone = timezone,
        use24HourFormat = use24HourFormat,
        language = lang,
        nowMs = nowMs
    )
    val badgeBg = when {
        isDndOn -> MaterialTheme.colorScheme.errorContainer
        isQuietOn -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeFg = when {
        isDndOn -> MaterialTheme.colorScheme.onErrorContainer
        isQuietOn -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val badgeText = when {
        isDndOn -> strings.notifDndStatusActive
        isQuietOn -> strings.notifQuietHoursStatusActive
        else -> strings.notifAlertsNormalStatus
    }

    PanchangaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDndOn) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDndOn) Icons.AutoMirrored.Rounded.VolumeOff else Icons.Rounded.Bedtime,
                        contentDescription = strings.notifDndAndQuietSectionTitle,
                        tint = if (isDndOn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = strings.notifDndAndQuietSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.notifDndAndQuietSectionSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.testTag("${testTagPrefix}dnd_status_badge")
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (muteStatusLine != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (isDndOn) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}mute_resume_banner")
            ) {
                Text(
                    text = muteStatusLine,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDndOn) vedic.inauspicious else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))

        // Instant App DND Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = strings.notifInstantDndTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDndOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
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
                modifier = Modifier.testTag("${testTagPrefix}instant_dnd_switch")
            )
        }

        Spacer(Modifier.height(8.dp))

        // 1-Tap Timed Mute Presets
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeTimedUntil != null && (activeTimedUntil - nowMs) in 1L..(75L * 60_000L),
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
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}timed_mute_1h")
            )
            FilterChip(
                selected = activeTimedUntil != null && (activeTimedUntil - nowMs) in (75L * 60_000L + 1L)..(210L * 60_000L),
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
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}timed_mute_3h")
            )
            val targetSunriseMs = nextSunriseMs?.takeIf { it > nowMs + 60_000L }
            if (targetSunriseMs != null) {
                FilterChip(
                    selected = activeTimedUntil != null && kotlin.math.abs(activeTimedUntil - targetSunriseMs) < 120_000L,
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
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${testTagPrefix}timed_mute_sunrise")
                )
            }
            if (isDndOn) {
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
                    label = { Text(if (isHi) "अभी चालू करें" else "Resume Now") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${testTagPrefix}timed_mute_resume")
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))

        // Scheduled Quiet Hours Toggle + Start/End Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = strings.notifQuietHoursTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.notifQuietHoursSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = audioSettings.quietHoursEnabled,
                onCheckedChange = { enabled ->
                    onUpdateAudioSettings(audioSettings.copy(quietHoursEnabled = enabled))
                },
                modifier = Modifier.testTag("${testTagPrefix}quiet_hours_switch")
            )
        }

        AnimatedVisibility(visible = audioSettings.quietHoursEnabled) {
            Column {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuietTimeStepper(
                        label = strings.notifQuietStartLabel,
                        minuteOfDay = audioSettings.quietStartMinuteOfDay,
                        use24HourFormat = use24HourFormat,
                        testTagPrefix = "${testTagPrefix}quiet_start",
                        modifier = Modifier.weight(1f),
                        onChange = { next ->
                            onUpdateAudioSettings(audioSettings.copy(quietStartMinuteOfDay = next))
                        }
                    )
                    QuietTimeStepper(
                        label = strings.notifQuietEndLabel,
                        minuteOfDay = audioSettings.quietEndMinuteOfDay,
                        use24HourFormat = use24HourFormat,
                        testTagPrefix = "${testTagPrefix}quiet_end",
                        modifier = Modifier.weight(1f),
                        onChange = { next ->
                            onUpdateAudioSettings(audioSettings.copy(quietEndMinuteOfDay = next))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))

        // Respect Phone Silent & OS DND Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = strings.notifRespectSilentDndTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.notifRespectSilentDndSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = audioSettings.respectSilentAndDnd,
                onCheckedChange = { enabled ->
                    onUpdateAudioSettings(audioSettings.copy(respectSilentAndDnd = enabled))
                },
                modifier = Modifier.testTag("${testTagPrefix}respect_silent_dnd_switch")
            )
        }
    }
}

@Composable
private fun QuietTimeStepper(
    label: String,
    minuteOfDay: Int,
    use24HourFormat: Boolean,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { onChange((minuteOfDay - 30 + 1440) % 1440) },
                    modifier = Modifier.testTag("${testTagPrefix}_minus")
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Subtract 30 minutes")
                }
                Text(
                    text = formatMinuteOfDay(minuteOfDay, use24HourFormat),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { onChange((minuteOfDay + 30) % 1440) },
                    modifier = Modifier.testTag("${testTagPrefix}_plus")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add 30 minutes")
                }
            }
        }
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int, use24HourFormat: Boolean = false): String {
    val total = ((minuteOfDay % 1440) + 1440) % 1440
    val h24 = total / 60
    val m = total % 60
    if (use24HourFormat) {
        return String.format(Locale.US, "%02d:%02d", h24, m)
    }
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
}
