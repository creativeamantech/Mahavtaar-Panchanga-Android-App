package com.mahavtaar.panchanga.feature.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.export.OfflineReportExportHelper
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenNavtara: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isHi = lang == AppLanguage.HINDI

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Pair<Boolean, NavtaraProfile?>?>(null) }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleDriveAuthResult(result.data, result.resultCode)
    }

    val linkDriveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.linkGoogleDriveBackupUri(uri)
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val payload = viewModel.exportFullJson()
                OfflineReportExportHelper.writeTextToUri(context, uri, payload)
                    .onSuccess { viewModel.setStatusMessage(ext.jsonSavedMsg) }
                    .onFailure { err -> viewModel.setStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            OfflineReportExportHelper.readTextFromUri(context, uri)
                .onSuccess { raw -> viewModel.importJsonContent(raw) }
                .onFailure { err -> viewModel.setStatusMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ext.profileTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = ext.profileSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    LanguageTogglePill(
                        onToggleLanguage = viewModel::toggleLanguage,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("profile_screen_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Google Identity, Login/Logout & Account Switch
                item(key = "google_identity_card") {
                    val user = state.cloudUser
                    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
                        ?: user?.email?.substringBefore("@")
                        ?: (if (isHi) "साधक उपयोगकर्ता" else "Signed-In User")
                    val initials = displayName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "ॐ" }

                    PanchangaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("profile_google_display_name")
                                )
                                user?.email?.takeIf { it.isNotBlank() }?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("profile_google_email")
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusPill(
                                    label = if (policy.isAdmin) ext.adminRoleAdmin else ext.adminRoleUser,
                                    textColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = ext.freeStorageNoticeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = vedic.auspicious
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSignOutConfirm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_switch_account_button")
                            ) {
                                Icon(Icons.Rounded.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.switchAccountButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { showSignOutConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.signOutButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Card 2: Personal Birth / Navtara Profiles
                item(key = "birth_profiles_card") {
                    val active = state.activeProfile
                    PanchangaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = ext.navtaraProfilesTitle,
                                subtitle = if (isHi) "Google खाते से जुड़ा जन्म नक्षत्र व नाम प्रोफ़ाइल"
                                else "Birth profile auto-created from Google & synced as JSON",
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { editingProfile = true to null },
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("profile_add_birth_profile_button")
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.addProfileLabel)
                            }
                        }

                        if (state.profiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("profile_birth_chips_row")
                            ) {
                                items(state.profiles, key = { it.id }) { prof ->
                                    val selected = prof.id == active?.id
                                    val nak = VedicLocalizer.nakshatraNameByNumber(prof.birthNakshatraIndex, lang)
                                    InputChip(
                                        selected = selected,
                                        onClick = { viewModel.selectProfile(prof) },
                                        label = {
                                            Text(
                                                text = "${if (prof.isDefault) "★ " else ""}${prof.name} ($nak·P${prof.birthPada})",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.testTag("profile_birth_chip_${prof.id}")
                                    )
                                }
                            }
                        }

                        if (active != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${active.name} · ${VedicLocalizer.nakshatraNameByNumber(active.birthNakshatraIndex, lang)} (${strings.padaLabel} ${active.birthPada})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (active.notes.isNotBlank()) {
                                        Text(
                                            text = active.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${ext.reflectionStatsTitle}: ${state.selectedProfileFeedbackCount} · ${ext.savedReportsLabel}: ${state.savedReportCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = { editingProfile = true to active },
                                    label = { Text(ext.editProfileLabel) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("profile_edit_birth_profile_button")
                                )
                                if (!active.isDefault) {
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.setDefaultProfile(active.id) },
                                        label = { Text(ext.setDefaultLabel) },
                                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.testTag("profile_set_default_button")
                                    )
                                } else {
                                    StatusPill(
                                        label = ext.defaultBadgeLabel,
                                        textColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                                if (state.profiles.size > 1) {
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.deleteProfile(active.id) },
                                        label = { Text(ext.actionDeleteLabel) },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.testTag("profile_delete_birth_profile_button")
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ext.noSavedProfiles,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Card 3: Free User-Owned Google Drive JSON Auto-Sync & Local Vault
                item(key = "drive_json_sync_card") {
                    val drive = state.driveBackupStatus
                    PanchangaCard {
                        SectionHeader(
                            title = if (isHi) "निजी Google Drive ऑटो-सिंक व वॉल्ट" else "Personal Google Drive Auto-Sync & Vault",
                            subtitle = if (drive.isDriveLinked) {
                                "${ext.driveConnectedLabel} · ${drive.linkedFileName ?: viewModel.suggestedDriveFileName()}"
                            } else {
                                ext.driveNotConnectedLabel
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHi) {
                                "लोकेशन, नवतारा, कुंडली, अलर्ट व सभी डेटा सीधे आपके निजी Google Drive (${state.cloudUser?.email ?: "खाता"}) में ऑटो-सिंक होता है।"
                            } else {
                                "Locations, Navtara, Kundli, alerts & preferences auto-sync directly with YOUR own Google Drive (${state.cloudUser?.email ?: "account"})."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!drive.lastReplacedOldFileId.isNullOrBlank()) {
                            Text(
                                text = "✓ ${ext.driveReplacedOldNewBadge}",
                                style = MaterialTheme.typography.labelSmall,
                                color = vedic.auspicious
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        drive.lastDriveSyncAtMs?.let { ts ->
                            val fmt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss"))
                            Text(
                                text = "${ext.lastSyncedLabel}: $fmt (${drive.totalSyncedRecords})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        drive.lastLocalVaultAtMs?.let { ts ->
                            val fmt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss"))
                            Text(
                                text = "${ext.localVaultSavedLabel}: $fmt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        drive.lastError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = vedic.inauspicious)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (drive.isDriveLinked) {
                                    viewModel.syncNow()
                                } else {
                                    viewModel.authorizeGoogleDrive(context as? Activity) { pi ->
                                        driveAuthLauncher.launch(IntentSenderRequest.Builder(pi).build())
                                    }
                                }
                            },
                            enabled = !drive.isBackingUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("profile_sync_drive_button")
                        ) {
                            if (drive.isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ext.syncingNowLabel)
                            } else {
                                Icon(
                                    imageVector = if (drive.isDriveLinked) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (drive.isDriveLinked) ext.driveSyncJsonNowButton else ext.connectGoogleDriveButton)
                            }
                        }

                        if (!drive.isDriveLinked || drive.isOAuthBlockedByConsentScreen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { linkDriveFileLauncher.launch(viewModel.suggestedDriveFileName()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .testTag("profile_link_drive_saf_button")
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
                                onClick = { viewModel.restoreFromDrive(null) },
                                enabled = drive.isDriveLinked && !drive.isBackingUp,
                                modifier = Modifier.heightIn(min = 44.dp).testTag("profile_restore_drive_button")
                            ) {
                                Text(ext.driveRestoreJsonButton)
                            }
                            OutlinedButton(
                                onClick = { exportJsonLauncher.launch(viewModel.suggestedDriveFileName()) },
                                modifier = Modifier.heightIn(min = 44.dp).testTag("profile_export_json_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.saveJsonFile)
                            }
                            OutlinedButton(
                                onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier.heightIn(min = 44.dp).testTag("profile_import_json_button")
                            ) {
                                Text(ext.importJsonFile)
                            }
                        }
                    }
                }

                // Quick shortcuts row
                item(key = "quick_shortcuts") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (policy.canAccessPage(AppPageKey.NAVTATA)) {
                            OutlinedButton(
                                onClick = onOpenNavtara,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_open_navtara_button")
                            ) {
                                Icon(Icons.Rounded.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.moreNavtaraTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_open_settings_button")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.settingsTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(ext.signOutConfirmTitle) },
            text = { Text(ext.signOutConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("profile_confirm_sign_out_button")
                ) {
                    Text(ext.signOutButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    editingProfile?.let { (_, profile) ->
        ProfileBirthEditorDialog(
            initial = profile,
            defaultNak = state.preferences.birthNakshatra,
            defaultName = state.cloudUser?.displayName.orEmpty(),
            onDismiss = { editingProfile = null },
            onSave = { id, name, nak, pada, notes, makeDefault ->
                viewModel.saveProfile(id, name, nak, pada, notes, makeDefault)
                editingProfile = null
            }
        )
    }
}

@Composable
private fun ProfileBirthEditorDialog(
    initial: NavtaraProfile?,
    defaultNak: Int,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, birthNak: Int, pada: Int, notes: String, makeDefault: Boolean) -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    var name by remember(initial) { mutableStateOf(initial?.name ?: defaultName) }
    var birthNak by remember(initial) { mutableIntStateOf(initial?.birthNakshatraIndex ?: defaultNak) }
    var pada by remember(initial) { mutableIntStateOf(initial?.birthPada ?: 1) }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var makeDefault by remember(initial) { mutableStateOf(initial?.isDefault ?: true) }
    var showNakPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) ext.addProfileLabel else ext.editProfileLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(ext.nativeNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_editor_name")
                )
                Text(strings.selectBirthNakshatraLabel, style = MaterialTheme.typography.labelMedium)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showNakPicker = true }
                        .testTag("profile_editor_nak_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$birthNak. ${VedicLocalizer.nakshatraNameByNumber(birthNak, lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = strings.select)
                    }
                }
                Text(ext.birthPadaLabel, style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    (1..4).forEachIndexed { idx, p ->
                        SegmentedButton(
                            selected = pada == p,
                            onClick = { pada = p },
                            shape = SegmentedButtonDefaults.itemShape(idx, 4)
                        ) { Text("${strings.padaLabel} $p") }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(ext.journalNoteHint) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ext.pinAsDefaultLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = makeDefault, onCheckedChange = { makeDefault = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial?.id, name, birthNak, pada, notes, makeDefault) },
                modifier = Modifier.testTag("profile_editor_save_button")
            ) { Text(ext.saveAndApplyLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )

    if (showNakPicker) {
        AlertDialog(
            onDismissRequest = { showNakPicker = false },
            title = { Text(strings.selectBirthNakshatraLabel) },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items((1..27).toList(), key = { it }) { num ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    birthNak = num
                                    showNakPicker = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = birthNak == num,
                                onClick = {
                                    birthNak = num
                                    showNakPicker = false
                                }
                            )
                            Text("$num. ${VedicLocalizer.nakshatraNameByNumber(num, lang)}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNakPicker = false }) { Text(strings.cancel) }
            }
        )
    }
}
