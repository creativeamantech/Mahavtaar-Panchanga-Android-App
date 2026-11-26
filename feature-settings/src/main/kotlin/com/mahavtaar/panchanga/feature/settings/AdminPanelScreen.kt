package com.mahavtaar.panchanga.feature.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.EmailInviteRule
import com.mahavtaar.panchanga.domain.model.ManagedUserAccess
import com.mahavtaar.panchanga.domain.model.PermissionCategory
import com.mahavtaar.panchanga.domain.model.ROOT_ADMIN_EMAIL
import com.mahavtaar.panchanga.domain.model.SwaraTier
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.ui.access.AccessPermissionCatalog
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.localization.AccessControlStrings
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ac = AccessControlStrings.forLanguage(lang)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    state.confirmForceLogoutUser?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissForceLogoutConfirmation,
            title = { Text(ac.forceSignOutConfirmTitle) },
            text = {
                Text("${ac.forceSignOutConfirmBody}\n\n${target.policy.email} (${target.session?.deviceName.orEmpty()})")
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmForceLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("admin_confirm_force_logout_button")
                ) {
                    Text(ac.forceSignOutButton)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissForceLogoutConfirmation) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = ac.adminPanelTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ac.adminPanelSubtitle,
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
                        modifier = Modifier.testTag("admin_back_button")
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
                    .widthIn(max = 760.dp)
                    .testTag("admin_panel_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.isSignedIn) {
                    item(key = "admin_signin_required_banner") {
                        PanchangaCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_signin_required_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "क्लाउड एडमिन एक्सेस के लिए Google साइन-इन आवश्यक है"
                                    } else {
                                        "Google Sign-In Required for Cloud Admin Access"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "अभी ऐप लोकल कैश मोड में चल रहा है। उपयोगकर्ता, सेशन और ईमेल इनवाइट देखने या बदलने के लिए सेटिंग्स → क्लाउड अकाउंट में एडमिन Google अकाउंट से साइन-इन करें।"
                                    } else {
                                        "The app is currently running in local cache mode. Sign in with your Admin Google account under Settings → Cloud Account to view and manage live users, sessions, and email invites."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item(key = "admin_tabs") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = state.selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {}
                        ) {
                            Tab(
                                selected = state.selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                modifier = Modifier.testTag("admin_tab_pending"),
                                text = {
                                    Text(
                                        text = "${ac.tabPendingQueue} (${state.pendingUsers.size})",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            Tab(
                                selected = state.selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                modifier = Modifier.testTag("admin_tab_users"),
                                text = {
                                    Text(
                                        text = "${ac.tabUsersAndSessions} (${state.managedUsers.size})",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            Tab(
                                selected = state.selectedTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                modifier = Modifier.testTag("admin_tab_invites"),
                                text = {
                                    Text(
                                        text = ac.tabPreAuthorizeEmail,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                when (state.selectedTab) {
                    0 -> {
                        val pending = state.pendingUsers
                        if (pending.isEmpty()) {
                            item(key = "no_pending") {
                                PanchangaCard {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                            Text(ac.noPendingUsersTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(ac.noPendingUsersSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(items = pending, key = { "pending_${it.policy.userId}" }) { user ->
                                ManagedUserAccessCard(
                                    item = user,
                                    onConfigure = { viewModel.openUserEditor(user, preselectActive = true) },
                                    onQuickEventsOnly = { viewModel.quickAuthorizeEventsOnly(user) },
                                    onToggleSuspend = { viewModel.quickSetUserStatus(user, AccessStatus.SUSPENDED) },
                                    onForceLogout = { viewModel.requestForceLogoutConfirmation(user) }
                                )
                            }
                        }
                    }

                    1 -> {
                        item(key = "user_search") {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                label = { Text(ac.searchUsersHint) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_search_users_input")
                            )
                        }

                        items(
                            items = state.filteredUsers,
                            key = { "user_${it.policy.userId}" }
                        ) { user ->
                            val isSuspended = user.policy.status == AccessStatus.SUSPENDED
                            ManagedUserAccessCard(
                                item = user,
                                onConfigure = { viewModel.openUserEditor(user, preselectActive = false) },
                                onQuickEventsOnly = if (user.policy.status == AccessStatus.PENDING) {
                                    { viewModel.quickAuthorizeEventsOnly(user) }
                                } else null,
                                onToggleSuspend = if (!user.policy.email.equals(ROOT_ADMIN_EMAIL, ignoreCase = true)) {
                                    {
                                        val nextStatus = if (isSuspended) AccessStatus.ACTIVE else AccessStatus.SUSPENDED
                                        viewModel.quickSetUserStatus(user, nextStatus)
                                    }
                                } else null,
                                onForceLogout = { viewModel.requestForceLogoutConfirmation(user) }
                            )
                        }
                    }

                    2 -> {
                        item(key = "pre_auth_form") {
                            PreAuthorizeEmailFormCard(
                                state = state,
                                onEmailChange = viewModel::updateInviteEmail,
                                onRoleChange = viewModel::updateInviteRole,
                                onStatusChange = viewModel::updateInviteStatus,
                                onTogglePage = viewModel::toggleInvitePage,
                                onToggleFeature = viewModel::toggleInviteFeature,
                                onPresetEventsOnly = viewModel::applyInviteEventsOnly,
                                onPresetTattvaTimeOnly = viewModel::applyInviteTattvaTimeOnly,
                                onSetAllTattvaFeatures = viewModel::setInviteAllTattvaFeatures,
                                onSetSwaraTier = viewModel::setInviteSwaraTier,
                                onSetAllSwaraFeatures = viewModel::setInviteAllSwaraFeatures,
                                onPresetSelectAll = viewModel::applyInviteSelectAll,
                                onPresetClearAll = viewModel::applyInviteClearAll,
                                onSaveInvite = viewModel::saveEmailInvite
                            )
                        }

                        item(key = "existing_invites_header") {
                            SectionHeader(
                                title = ac.existingInvitesTitle,
                                subtitle = "${state.emailInvites.size}"
                            )
                        }

                        if (state.emailInvites.isEmpty()) {
                            item(key = "empty_invites") {
                                PanchangaCard {
                                    Text(
                                        text = ac.noEmailInvitesYet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = state.emailInvites,
                                key = { "inv_${it.emailHash}" }
                            ) { invite ->
                                EmailInviteRuleCard(
                                    invite = invite,
                                    onEdit = { viewModel.loadInviteIntoForm(invite) },
                                    onDelete = { viewModel.deleteEmailInvite(invite.emailHash) }
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    state.editingUserDraft?.let { draft ->
        UserPermissionEditorSheet(
            draft = draft,
            isSaving = state.isSaving,
            onDismiss = viewModel::dismissUserEditor,
            onRoleChange = viewModel::updateDraftRole,
            onStatusChange = viewModel::updateDraftStatus,
            onTogglePage = viewModel::toggleDraftPage,
            onToggleFeature = viewModel::toggleDraftFeature,
            onPresetEventsOnly = viewModel::applyDraftEventsOnlyBaseline,
            onPresetTattvaTimeOnly = viewModel::applyDraftTattvaTimeOnly,
            onSetAllTattvaFeatures = viewModel::setDraftAllTattvaFeatures,
            onSetSwaraTier = viewModel::setDraftSwaraTier,
            onSetAllSwaraFeatures = viewModel::setDraftAllSwaraFeatures,
            onPresetSelectAll = viewModel::applyDraftSelectAll,
            onPresetClearAll = viewModel::applyDraftClearAll,
            onSave = viewModel::saveEditingUserDraft
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagedUserAccessCard(
    item: ManagedUserAccess,
    onConfigure: () -> Unit,
    onQuickEventsOnly: (() -> Unit)?,
    onToggleSuspend: (() -> Unit)?,
    onForceLogout: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val vedic = LocalVedicColors.current
    val policy = item.policy
    val session = item.session

    val statusColor = when (policy.status) {
        AccessStatus.ACTIVE -> vedic.auspicious
        AccessStatus.PENDING -> vedic.caution
        AccessStatus.SUSPENDED -> vedic.inauspicious
    }
    val statusContainer = when (policy.status) {
        AccessStatus.ACTIVE -> vedic.auspiciousContainer
        AccessStatus.PENDING -> vedic.cautionContainer
        AccessStatus.SUSPENDED -> vedic.inauspiciousContainer
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = policy.email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!policy.displayName.isNullOrBlank()) {
                    Text(
                        text = policy.displayName.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onConfigure,
                modifier = Modifier.testTag("admin_edit_user_${policy.userId}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = ac.editPermissionsTitle,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusPill(
                label = AccessPermissionCatalog.statusLabel(policy.status, lang),
                textColor = statusColor,
                containerColor = statusContainer
            )
            StatusPill(
                label = AccessPermissionCatalog.roleLabel(policy.role, lang),
                textColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            StatusPill(
                label = "${policy.allowedPages.size} ${ac.pagesAssignedLabel} · ${policy.allowedFeatures.size} ${ac.featuresAssignedLabel}",
                textColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Single-Device Session Lock row
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Devices,
                        contentDescription = null,
                        tint = if (session != null && !session.forcedLogout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = if (session != null && !session.forcedLogout) {
                                "${ac.activeDeviceLabel}: ${session.deviceName}"
                            } else {
                                ac.noActiveSessionLabel
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (session != null && session.updatedAtMs > 0L) {
                            val formatted = Instant.ofEpochMilli(session.updatedAtMs)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (session != null && !session.forcedLogout) {
                    TextButton(
                        onClick = onForceLogout,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("admin_force_logout_${policy.userId}")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ac.forceSignOutButton)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onConfigure,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("admin_configure_btn_${policy.userId}")
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(ac.authorizeAndConfigureButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (onQuickEventsOnly != null) {
                OutlinedButton(
                    onClick = onQuickEventsOnly,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("admin_quick_events_btn_${policy.userId}")
                ) {
                    Text(ac.quickEventsOnlyButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else if (onToggleSuspend != null) {
                val isSuspended = policy.status == AccessStatus.SUSPENDED
                OutlinedButton(
                    onClick = onToggleSuspend,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .heightIn(min = 46.dp)
                        .testTag("admin_suspend_btn_${policy.userId}")
                ) {
                    Text(if (isSuspended) ac.reactivateButton else ac.suspendAccessButton)
                }
            }
        }
    }
}

@Composable
private fun UserPermissionEditorSheet(
    draft: AdminPermissionEditorDraft,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onStatusChange: (AccessStatus) -> Unit,
    onTogglePage: (AppPageKey) -> Unit,
    onToggleFeature: (AppFeatureKey) -> Unit,
    onPresetEventsOnly: () -> Unit,
    onPresetTattvaTimeOnly: () -> Unit,
    onSetAllTattvaFeatures: (Boolean) -> Unit,
    onSetSwaraTier: (SwaraTier) -> Unit,
    onSetAllSwaraFeatures: (Boolean) -> Unit,
    onPresetSelectAll: () -> Unit,
    onPresetClearAll: () -> Unit,
    onSave: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)

    InspectorBottomSheetFrame(
        title = ac.editPermissionsTitle,
        subtitle = draft.email,
        icon = Icons.Rounded.VerifiedUser,
        accentColor = MaterialTheme.colorScheme.primary,
        statusLabel = "${draft.allowedPages.size} ${ac.pagesAssignedLabel} · ${draft.allowedFeatures.size} ${ac.featuresAssignedLabel}",
        sheetTestTag = "admin_permission_editor_sheet",
        onDismiss = onDismiss,
        footerActions = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("admin_save_permissions_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ac.savingAccessLabel)
                } else {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ac.saveAccessButton)
                }
            }
        }
    ) {
        PermissionMatrixFormContent(
            role = draft.role,
            status = draft.status,
            allowedPages = draft.allowedPages,
            allowedFeatures = draft.allowedFeatures,
            onRoleChange = onRoleChange,
            onStatusChange = onStatusChange,
            onTogglePage = onTogglePage,
            onToggleFeature = onToggleFeature,
            onPresetEventsOnly = onPresetEventsOnly,
            onPresetTattvaTimeOnly = onPresetTattvaTimeOnly,
            onSetAllTattvaFeatures = onSetAllTattvaFeatures,
            onSetSwaraTier = onSetSwaraTier,
            onSetAllSwaraFeatures = onSetAllSwaraFeatures,
            onPresetSelectAll = onPresetSelectAll,
            onPresetClearAll = onPresetClearAll
        )
    }
}

@Composable
private fun PreAuthorizeEmailFormCard(
    state: AdminPanelUiState,
    onEmailChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onStatusChange: (AccessStatus) -> Unit,
    onTogglePage: (AppPageKey) -> Unit,
    onToggleFeature: (AppFeatureKey) -> Unit,
    onPresetEventsOnly: () -> Unit,
    onPresetTattvaTimeOnly: () -> Unit,
    onSetAllTattvaFeatures: (Boolean) -> Unit,
    onSetSwaraTier: (SwaraTier) -> Unit,
    onSetAllSwaraFeatures: (Boolean) -> Unit,
    onPresetSelectAll: () -> Unit,
    onPresetClearAll: () -> Unit,
    onSaveInvite: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)

    PanchangaCard {
        SectionHeader(
            title = ac.preAuthFormTitle,
            subtitle = ac.preAuthFormSubtitle
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.inviteEmailInput,
            onValueChange = onEmailChange,
            label = { Text(ac.googleEmailInputLabel) },
            leadingIcon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_invite_email_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionMatrixFormContent(
            role = state.inviteRole,
            status = state.inviteStatus,
            allowedPages = state.invitePages,
            allowedFeatures = state.inviteFeatures,
            onRoleChange = onRoleChange,
            onStatusChange = onStatusChange,
            onTogglePage = onTogglePage,
            onToggleFeature = onToggleFeature,
            onPresetEventsOnly = onPresetEventsOnly,
            onPresetTattvaTimeOnly = onPresetTattvaTimeOnly,
            onSetAllTattvaFeatures = onSetAllTattvaFeatures,
            onSetSwaraTier = onSetSwaraTier,
            onSetAllSwaraFeatures = onSetAllSwaraFeatures,
            onPresetSelectAll = onPresetSelectAll,
            onPresetClearAll = onPresetClearAll
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onSaveInvite,
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("admin_save_invite_button")
        ) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(ac.saveEmailInviteButton)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionMatrixFormContent(
    role: UserRole,
    status: AccessStatus,
    allowedPages: Set<AppPageKey>,
    allowedFeatures: Set<AppFeatureKey>,
    onRoleChange: (UserRole) -> Unit,
    onStatusChange: (AccessStatus) -> Unit,
    onTogglePage: (AppPageKey) -> Unit,
    onToggleFeature: (AppFeatureKey) -> Unit,
    onPresetEventsOnly: () -> Unit,
    onPresetTattvaTimeOnly: () -> Unit,
    onSetAllTattvaFeatures: (Boolean) -> Unit,
    onSetSwaraTier: (SwaraTier) -> Unit,
    onSetAllSwaraFeatures: (Boolean) -> Unit,
    onPresetSelectAll: () -> Unit,
    onPresetClearAll: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val expandedCategories = remember {
        mutableStateMapOf(
            PermissionCategory.CORE_EVENTS to true,
            PermissionCategory.TIMINGS_TRANSITS to true,
            PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI to false,
            PermissionCategory.SYSTEM_EXPORTS to false
        )
    }

    Text(
        text = ac.roleSectionTitle,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        UserRole.entries.forEachIndexed { idx, r ->
            SegmentedButton(
                selected = role == r,
                onClick = { onRoleChange(r) },
                shape = SegmentedButtonDefaults.itemShape(idx, UserRole.entries.size),
                modifier = Modifier.testTag("admin_role_${r.name.lowercase()}")
            ) {
                Text(AccessPermissionCatalog.roleLabel(r, lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = ac.statusSectionTitle,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AccessStatus.entries.forEachIndexed { idx, st ->
            SegmentedButton(
                selected = status == st,
                onClick = { onStatusChange(st) },
                shape = SegmentedButtonDefaults.itemShape(idx, AccessStatus.entries.size),
                modifier = Modifier.testTag("admin_status_${st.name.lowercase()}")
            ) {
                Text(AccessPermissionCatalog.statusLabel(st, lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = allowedPages == setOf(AppPageKey.EVENTS_TIMELINE) && allowedFeatures.isEmpty(),
            onClick = onPresetEventsOnly,
            label = { Text(ac.presetEventsOnly) },
            modifier = Modifier.testTag("admin_preset_events_only")
        )
        FilterChip(
            selected = allowedPages == setOf(AppPageKey.TATTVA) && allowedFeatures.isEmpty(),
            onClick = onPresetTattvaTimeOnly,
            label = { Text(ac.presetTattvaTimeOnly) },
            modifier = Modifier.testTag("admin_preset_tattva_time_only")
        )
        FilterChip(
            selected = allowedPages.size == AppPageKey.entries.size && allowedFeatures.size == AppFeatureKey.entries.size,
            onClick = onPresetSelectAll,
            label = { Text(ac.presetSelectAll) },
            modifier = Modifier.testTag("admin_preset_select_all")
        )
        FilterChip(
            selected = allowedPages.isEmpty() && allowedFeatures.isEmpty(),
            onClick = onPresetClearAll,
            label = { Text(ac.presetClearAll) },
            modifier = Modifier.testTag("admin_preset_clear_all")
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    PermissionCategory.entries.forEach { category ->
        val catPages = AppPageKey.entries.filter { it.category == category }
        val catFeatures = AppFeatureKey.entries.filter { it.category == category }
        val selectedPagesCount = catPages.count { it in allowedPages }
        val selectedFeaturesCount = catFeatures.count { it in allowedFeatures }
        val isExpanded = expandedCategories[category] ?: false

        ExpandableInfoCard(
            title = AccessPermissionCatalog.categoryTitle(category, lang),
            subtitle = AccessPermissionCatalog.categorySubtitle(category, lang),
            expanded = isExpanded,
            onToggleExpand = { expandedCategories[category] = !isExpanded },
            trailingBadge = {
                StatusPill(
                    label = "$selectedPagesCount/${catPages.size} P · $selectedFeaturesCount/${catFeatures.size} F",
                    textColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        ) {
            Text(
                text = ac.pagesAssignedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            catPages.forEach { page ->
                val checked = role == UserRole.ADMIN || page in allowedPages
                PermissionCheckboxRow(
                    title = AccessPermissionCatalog.pageTitle(page, lang),
                    subtitle = AccessPermissionCatalog.pageSubtitle(page, lang),
                    checked = checked,
                    testTag = "admin_chk_page_${page.name.lowercase()}",
                    onToggle = { onTogglePage(page) }
                )
            }

            if (catFeatures.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Text(
                    text = ac.featuresAssignedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                if (category == PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI) {
                    val tattvaFeatures = AppFeatureKey.tattvaFeatures
                    val allTattvaSelected = tattvaFeatures.all { it in allowedFeatures }
                    val noTattvaSelected = tattvaFeatures.none { it in allowedFeatures }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = allTattvaSelected,
                            onClick = { onSetAllTattvaFeatures(true) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "तत्त्व: सभी ऑन" else "Tattva: All On",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_tattva_all_on")
                        )
                        FilterChip(
                            selected = noTattvaSelected && AppPageKey.TATTVA in allowedPages,
                            onClick = { onSetAllTattvaFeatures(false) },
                            label = {
                                Text(
                                    text = ac.presetTattvaTimeOnly,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_tattva_time_only_inline")
                        )
                    }
                    val swaraFeatures = AppFeatureKey.swaraFeatures
                    val allSwaraSelected = swaraFeatures.all { it in allowedFeatures }
                    val noSwaraSelected = swaraFeatures.none { it in allowedFeatures }
                    val hasTapasvi = role == UserRole.ADMIN || AppFeatureKey.SWARA_TIER_TAPASVI in allowedFeatures
                    val hasYogi = !hasTapasvi && AppFeatureKey.SWARA_TIER_YOGI in allowedFeatures
                    val hasSadhak = !hasTapasvi && !hasYogi && AppFeatureKey.SWARA_TIER_SADHAK in allowedFeatures
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = hasSadhak,
                            onClick = { onSetSwaraTier(SwaraTier.SADHAKA) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "स्वर: साधक" else "Swara: Sādhaka",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_swara_tier_sadhak")
                        )
                        FilterChip(
                            selected = hasYogi,
                            onClick = { onSetSwaraTier(SwaraTier.YOGI) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "स्वर: योगी" else "Swara: Yogī",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_swara_tier_yogi")
                        )
                        FilterChip(
                            selected = hasTapasvi,
                            onClick = { onSetSwaraTier(SwaraTier.TAPASVI) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "स्वर: तपस्वी" else "Swara: Tapasvī",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_swara_tier_tapasvi")
                        )
                        FilterChip(
                            selected = allSwaraSelected,
                            onClick = { onSetAllSwaraFeatures(true) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "स्वर: सभी ऑन" else "Swara: All On",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_swara_all_on")
                        )
                        FilterChip(
                            selected = noSwaraSelected && AppPageKey.SWARA_YOGA in allowedPages,
                            onClick = { onSetAllSwaraFeatures(false) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "स्वर: सब हटाएँ" else "Swara: Clear",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("admin_swara_clear_inline")
                        )
                    }
                }
                catFeatures.forEach { feature ->
                    val checked = role == UserRole.ADMIN || feature in allowedFeatures
                    PermissionCheckboxRow(
                        title = AccessPermissionCatalog.featureTitle(feature, lang),
                        subtitle = AccessPermissionCatalog.featureSubtitle(feature, lang),
                        checked = checked,
                        testTag = "admin_chk_feature_${feature.name.lowercase()}",
                        onToggle = { onToggleFeature(feature) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmailInviteRuleCard(
    invite: EmailInviteRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ac = AccessControlStrings.forLanguage(lang)
    val strings = LocalStrings.current

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.email,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusPill(
                        label = AccessPermissionCatalog.roleLabel(invite.role, lang),
                        textColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatusPill(
                        label = "${invite.allowedPages.size} ${ac.pagesAssignedLabel} · ${invite.allowedFeatures.size} ${ac.featuresAssignedLabel}",
                        textColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("admin_edit_invite_${invite.emailHash}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = ac.editPermissionsTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("admin_delete_invite_${invite.emailHash}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.cancel,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
