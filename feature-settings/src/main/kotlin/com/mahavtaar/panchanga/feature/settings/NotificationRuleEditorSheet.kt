package com.mahavtaar.panchanga.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.BuiltinVedicTone
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationRuleEditorSheet(
    initialRule: NotificationRule,
    voiceRecordings: List<VoiceRecording>,
    currentlyPlayingKey: String?,
    onDismiss: () -> Unit,
    onSave: (NotificationRule) -> Unit,
    onTestNow: (NotificationRule) -> Unit,
    onPlayTunePreview: (key: String, source: TuneSourceType, uriOrId: String) -> Unit,
    onPlayRecording: (VoiceRecording) -> Unit,
    onStopPreview: () -> Unit
) {
    val strings = LocalStrings.current
    val lang = LocalAppLanguage.current
    val isHindi = lang == AppLanguage.HINDI
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember(initialRule.eventType) { mutableStateOf(initialRule) }
    var customLeadInput by remember(initialRule.eventType) {
        mutableStateOf(if (initialRule.leadTimeMinutes > 0) initialRule.leadTimeMinutes.toString() else "")
    }

    // System Ringtone Picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val title = runCatching {
                    RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                }.getOrNull().orEmpty().ifBlank { "System Ringtone" }
                draft = draft.copy(
                    tuneSource = TuneSourceType.SYSTEM_RINGTONE,
                    tuneUriOrId = uri.toString(),
                    tuneDisplayName = title
                )
            }
        }
    }

    // Zero-permission SAF Audio File Picker (OpenDocument)
    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = resolveDocumentDisplayName(context, uri)
            draft = draft.copy(
                tuneSource = TuneSourceType.CUSTOM_AUDIO_URI,
                tuneUriOrId = uri.toString(),
                tuneDisplayName = displayName
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) draft.eventType.titleHi else draft.eventType.titleEn,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHindi) draft.eventType.descriptionHi else draft.eventType.descriptionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = draft.isEnabled,
                    onCheckedChange = { draft = draft.copy(isEnabled = it) },
                    modifier = Modifier.testTag("editor_enabled_switch")
                )
            }

            HorizontalDivider()

            // Exact Transition Start Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.notifAtStartLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.notifyAtStart,
                    onCheckedChange = { draft = draft.copy(notifyAtStart = it) },
                    modifier = Modifier.testTag("editor_at_start_switch")
                )
            }

            // Advance Lead Time Selector
            Text(
                text = strings.notifLeadTimeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val presets = listOf(0, 5, 10, 15, 30, 60)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { mins ->
                    val label = if (mins == 0) strings.notifLeadTimeOff else "${mins}m"
                    FilterChip(
                        selected = draft.leadTimeMinutes == mins,
                        onClick = {
                            draft = draft.copy(leadTimeMinutes = mins)
                            customLeadInput = if (mins > 0) mins.toString() else ""
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("editor_lead_$mins")
                    )
                }
            }
            OutlinedTextField(
                value = customLeadInput,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(3)
                    customLeadInput = digits
                    val parsed = digits.toIntOrNull()?.coerceIn(0, 180) ?: 0
                    draft = draft.copy(leadTimeMinutes = parsed)
                },
                label = { Text(strings.notifCustomMinutesLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_custom_lead_input")
            )

            // Delivery Mode Selector
            Text(
                text = strings.notifDeliveryModeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    AlertDeliveryMode.TUNE_AND_VOICE to strings.notifModeTuneAndVoice,
                    AlertDeliveryMode.VOICE_ONLY to strings.notifModeVoiceOnly,
                    AlertDeliveryMode.TUNE_ONLY to strings.notifModeTuneOnly,
                    AlertDeliveryMode.SILENT_BANNER to strings.notifModeSilentBanner
                )
                modes.forEach { (mode, label) ->
                    FilterChip(
                        selected = draft.deliveryMode == mode,
                        onClick = { draft = draft.copy(deliveryMode = mode) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("editor_mode_${mode.name}")
                    )
                }
            }

            // Custom Tune Picker (when Tune is part of delivery mode)
            if (draft.deliveryMode == AlertDeliveryMode.TUNE_AND_VOICE || draft.deliveryMode == AlertDeliveryMode.TUNE_ONLY) {
                HorizontalDivider()
                Text(
                    text = strings.notifTuneSectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = draft.tuneSource == TuneSourceType.BUILTIN_VEDIC,
                        onClick = {
                            val defaultTone = BuiltinVedicTone.fromId(draft.tuneUriOrId)
                            draft = draft.copy(
                                tuneSource = TuneSourceType.BUILTIN_VEDIC,
                                tuneUriOrId = defaultTone.id,
                                tuneDisplayName = defaultTone.titleEn
                            )
                        },
                        label = { Text(strings.notifTuneSourceBuiltin) },
                        modifier = Modifier.testTag("editor_tune_src_builtin")
                    )
                    FilterChip(
                        selected = draft.tuneSource == TuneSourceType.SYSTEM_RINGTONE,
                        onClick = {
                            val existingUri = draft.tuneUriOrId.takeIf { draft.tuneSource == TuneSourceType.SYSTEM_RINGTONE }
                                ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                if (existingUri != null) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                }
                            }
                            runCatching { ringtonePickerLauncher.launch(intent) }
                        },
                        label = { Text(strings.notifTuneSourceRingtone) },
                        modifier = Modifier.testTag("editor_tune_src_ringtone")
                    )
                    FilterChip(
                        selected = draft.tuneSource == TuneSourceType.CUSTOM_AUDIO_URI,
                        onClick = { runCatching { customAudioLauncher.launch(arrayOf("audio/*")) } },
                        label = { Text(strings.notifTuneSourceAudioFile) },
                        modifier = Modifier.testTag("editor_tune_src_file")
                    )
                }

                when (draft.tuneSource) {
                    TuneSourceType.BUILTIN_VEDIC -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BuiltinVedicTone.entries.forEach { tone ->
                                val selected = draft.tuneUriOrId == tone.id
                                val playKey = "tone_${tone.id}"
                                val isPlaying = currentlyPlayingKey == playKey
                                Surface(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            draft = draft.copy(
                                                tuneSource = TuneSourceType.BUILTIN_VEDIC,
                                                tuneUriOrId = tone.id,
                                                tuneDisplayName = tone.titleEn
                                            )
                                        }
                                        .testTag("editor_tone_${tone.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                draft = draft.copy(
                                                    tuneSource = TuneSourceType.BUILTIN_VEDIC,
                                                    tuneUriOrId = tone.id,
                                                    tuneDisplayName = tone.titleEn
                                                )
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isHindi) tone.titleHi else tone.titleEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isHindi) tone.subtitleHi else tone.subtitleEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onPlayTunePreview(playKey, TuneSourceType.BUILTIN_VEDIC, tone.id)
                                            },
                                            modifier = Modifier.testTag("editor_play_tone_${tone.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                                contentDescription = "Preview tune",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TuneSourceType.SYSTEM_RINGTONE,
                    TuneSourceType.CUSTOM_AUDIO_URI -> {
                        val playKey = "custom_tune_preview"
                        val isPlaying = currentlyPlayingKey == playKey
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (draft.tuneSource == TuneSourceType.CUSTOM_AUDIO_URI) {
                                        Icons.Rounded.AudioFile
                                    } else {
                                        Icons.Rounded.MusicNote
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = draft.tuneDisplayName.ifBlank { strings.notifTuneSourceAudioFile },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        onPlayTunePreview(playKey, draft.tuneSource, draft.tuneUriOrId)
                                    },
                                    modifier = Modifier.testTag("editor_play_custom_tune")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = "Preview custom tune"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Spoken Voice Source Section (when Voice is part of delivery mode)
            if (draft.deliveryMode == AlertDeliveryMode.TUNE_AND_VOICE || draft.deliveryMode == AlertDeliveryMode.VOICE_ONLY) {
                HorizontalDivider()
                Text(
                    text = strings.notifVoiceSectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.voiceSource == VoiceSourceType.BUILTIN_TTS,
                        onClick = { draft = draft.copy(voiceSource = VoiceSourceType.BUILTIN_TTS) },
                        label = { Text(strings.notifVoiceSourceTts) },
                        modifier = Modifier.testTag("editor_voice_tts")
                    )
                    FilterChip(
                        selected = draft.voiceSource == VoiceSourceType.CUSTOM_RECORDING,
                        onClick = {
                            val firstRec = draft.voiceRecordingId ?: voiceRecordings.firstOrNull()?.id
                            draft = draft.copy(
                                voiceSource = VoiceSourceType.CUSTOM_RECORDING,
                                voiceRecordingId = firstRec
                            )
                        },
                        label = { Text(strings.notifVoiceSourceRecording) },
                        modifier = Modifier.testTag("editor_voice_recording")
                    )
                }

                if (draft.voiceSource == VoiceSourceType.BUILTIN_TTS) {
                    OutlinedTextField(
                        value = draft.customHindiTemplate,
                        onValueChange = { draft = draft.copy(customHindiTemplate = it) },
                        label = { Text(strings.notifCustomScriptHintHi) },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_custom_hi_script")
                    )
                    OutlinedTextField(
                        value = draft.customEnglishTemplate,
                        onValueChange = { draft = draft.copy(customEnglishTemplate = it) },
                        label = { Text(strings.notifCustomScriptHintEn) },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_custom_en_script")
                    )
                } else {
                    if (voiceRecordings.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = strings.notifNoRecordingsYet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            voiceRecordings.forEach { rec ->
                                val selected = draft.voiceRecordingId == rec.id
                                val isPlaying = currentlyPlayingKey == "rec_${rec.id}"
                                Surface(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draft = draft.copy(voiceRecordingId = rec.id) }
                                        .testTag("editor_select_rec_${rec.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { draft = draft.copy(voiceRecordingId = rec.id) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rec.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${(rec.durationMs / 1000L).coerceAtLeast(1)}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onPlayRecording(rec) }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                                contentDescription = "Play voice clip"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action Buttons: Test Alert Now + Save Rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onTestNow(draft) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_test_alert_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.notifTestAlertNowButton)
                }
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_save_rule_btn")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.notifSaveRuleButton)
                }
            }
        }
    }
}

private fun resolveDocumentDisplayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        }
    }.getOrNull().orEmpty().ifBlank { "Custom Audio" }
}
