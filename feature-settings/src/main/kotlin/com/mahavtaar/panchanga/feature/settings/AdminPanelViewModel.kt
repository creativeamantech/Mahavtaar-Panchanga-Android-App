package com.mahavtaar.panchanga.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.EmailInviteRule
import com.mahavtaar.panchanga.domain.model.ManagedUserAccess
import com.mahavtaar.panchanga.domain.model.SwaraTier
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.domain.model.UserSessionLock
import com.mahavtaar.panchanga.domain.repository.AccessControlRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPermissionEditorDraft(
    val userId: String,
    val email: String,
    val displayName: String?,
    val role: UserRole,
    val status: AccessStatus,
    val allowedPages: Set<AppPageKey>,
    val allowedFeatures: Set<AppFeatureKey>
)

data class AdminPanelUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val managedUsers: List<ManagedUserAccess> = emptyList(),
    val emailInvites: List<EmailInviteRule> = emptyList(),
    val editingUserDraft: AdminPermissionEditorDraft? = null,
    val inviteEmailInput: String = "",
    val inviteRole: UserRole = UserRole.USER,
    val inviteStatus: AccessStatus = AccessStatus.ACTIVE,
    val invitePages: Set<AppPageKey> = setOf(AppPageKey.EVENTS_TIMELINE),
    val inviteFeatures: Set<AppFeatureKey> = emptySet(),
    val confirmForceLogoutUser: ManagedUserAccess? = null,
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
) {
    val pendingUsers: List<ManagedUserAccess>
        get() = managedUsers.filter { it.policy.status == AccessStatus.PENDING }

    val filteredUsers: List<ManagedUserAccess>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) return managedUsers
            return managedUsers.filter { item ->
                item.policy.email.lowercase().contains(q) ||
                    (item.policy.displayName?.lowercase()?.contains(q) == true) ||
                    (item.session?.deviceName?.lowercase()?.contains(q) == true)
            }
        }
}

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val accessControlRepository: AccessControlRepository,
    private val cloudAccountRepository: CloudAccountRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPanelUiState())
    val uiState: StateFlow<AdminPanelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }

        viewModelScope.launch {
            cloudAccountRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isSignedIn = user != null,
                        signedInEmail = user?.email
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                accessControlRepository.observeAllUserAccess(),
                accessControlRepository.observeAllUserSessions(),
                accessControlRepository.observeEmailInvites()
            ) { accessResult, sessionsResult, invitesResult ->
                Triple(accessResult, sessionsResult, invitesResult)
            }.collect { (accessResult, sessionsResult, invitesResult) ->
                val policies = accessResult.getOrDefault(emptyList())
                val sessionsByUid: Map<String, UserSessionLock> =
                    sessionsResult.getOrDefault(emptyList()).associateBy { it.userId }
                val invites = invitesResult.getOrDefault(emptyList())
                val combined = policies.map { policy ->
                    ManagedUserAccess(
                        policy = policy,
                        session = sessionsByUid[policy.userId]
                    )
                }
                val err = accessResult.exceptionOrNull()?.localizedMessage
                    ?: sessionsResult.exceptionOrNull()?.localizedMessage
                    ?: invitesResult.exceptionOrNull()?.localizedMessage

                _uiState.update { cur ->
                    cur.copy(
                        managedUsers = combined,
                        emailInvites = invites,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index.coerceIn(0, 2)) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun openUserEditor(user: ManagedUserAccess, preselectActive: Boolean = false) {
        val p = user.policy
        val initialStatus = if (preselectActive && p.status == AccessStatus.PENDING) {
            AccessStatus.ACTIVE
        } else {
            p.status
        }
        val initialPages = if (preselectActive && p.allowedPages.isEmpty()) {
            setOf(AppPageKey.EVENTS_TIMELINE)
        } else {
            p.allowedPages
        }
        _uiState.update {
            it.copy(
                editingUserDraft = AdminPermissionEditorDraft(
                    userId = p.userId,
                    email = p.email,
                    displayName = p.displayName,
                    role = p.role,
                    status = initialStatus,
                    allowedPages = initialPages,
                    allowedFeatures = p.allowedFeatures
                )
            )
        }
    }

    fun dismissUserEditor() {
        _uiState.update { it.copy(editingUserDraft = null) }
    }

    fun updateDraftRole(role: UserRole) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val nextPages = if (role == UserRole.ADMIN) AppPageKey.entries.toSet() else draft.allowedPages
            val nextFeatures = if (role == UserRole.ADMIN) AppFeatureKey.entries.toSet() else draft.allowedFeatures
            cur.copy(
                editingUserDraft = draft.copy(
                    role = role,
                    allowedPages = nextPages,
                    allowedFeatures = nextFeatures
                )
            )
        }
    }

    fun updateDraftStatus(status: AccessStatus) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            cur.copy(editingUserDraft = draft.copy(status = status))
        }
    }

    fun toggleDraftPage(page: AppPageKey) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val next = if (page in draft.allowedPages) draft.allowedPages - page else draft.allowedPages + page
            cur.copy(editingUserDraft = draft.copy(allowedPages = next))
        }
    }

    fun toggleDraftFeature(feature: AppFeatureKey) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val nextFeatures = if (feature in draft.allowedFeatures) {
                draft.allowedFeatures - feature
            } else {
                draft.allowedFeatures + feature
            }
            // Automatically enable parent page when a child feature is checked
            val nextPages = if (feature !in draft.allowedFeatures && feature.parentPage != null) {
                draft.allowedPages + feature.parentPage!!
            } else {
                draft.allowedPages
            }
            cur.copy(
                editingUserDraft = draft.copy(
                    allowedPages = nextPages,
                    allowedFeatures = nextFeatures
                )
            )
        }
    }

    fun applyDraftEventsOnlyBaseline() {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            cur.copy(
                editingUserDraft = draft.copy(
                    role = UserRole.USER,
                    status = AccessStatus.ACTIVE,
                    allowedPages = setOf(AppPageKey.EVENTS_TIMELINE),
                    allowedFeatures = emptySet()
                )
            )
        }
    }

    fun applyDraftTattvaTimeOnly() {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            cur.copy(
                editingUserDraft = draft.copy(
                    role = UserRole.USER,
                    status = AccessStatus.ACTIVE,
                    allowedPages = setOf(AppPageKey.TATTVA),
                    allowedFeatures = emptySet()
                )
            )
        }
    }

    fun setDraftAllTattvaFeatures(enabled: Boolean) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val tattvaSet = AppFeatureKey.tattvaFeatures.toSet()
            val nextFeatures = if (enabled) draft.allowedFeatures + tattvaSet else draft.allowedFeatures - tattvaSet
            val nextPages = if (enabled) draft.allowedPages + AppPageKey.TATTVA else draft.allowedPages
            cur.copy(
                editingUserDraft = draft.copy(
                    allowedPages = nextPages,
                    allowedFeatures = nextFeatures
                )
            )
        }
    }

    fun setDraftSwaraTier(tier: SwaraTier) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val tierSet = AppFeatureKey.swaraTierFeatures.toSet()
            val nextFeatures = (draft.allowedFeatures - tierSet) + AppFeatureKey.featureForSwaraTier(tier)
            val nextPages = draft.allowedPages + AppPageKey.SWARA_YOGA
            cur.copy(
                editingUserDraft = draft.copy(
                    allowedPages = nextPages,
                    allowedFeatures = nextFeatures
                )
            )
        }
    }

    fun setDraftAllSwaraFeatures(enabled: Boolean) {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            val swaraSet = AppFeatureKey.swaraFeatures.toSet()
            val nextFeatures = if (enabled) draft.allowedFeatures + swaraSet else draft.allowedFeatures - swaraSet
            val nextPages = if (enabled) draft.allowedPages + AppPageKey.SWARA_YOGA else draft.allowedPages
            cur.copy(
                editingUserDraft = draft.copy(
                    allowedPages = nextPages,
                    allowedFeatures = nextFeatures
                )
            )
        }
    }

    fun applyDraftSelectAll() {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            cur.copy(
                editingUserDraft = draft.copy(
                    status = AccessStatus.ACTIVE,
                    allowedPages = AppPageKey.entries.toSet(),
                    allowedFeatures = AppFeatureKey.entries.toSet()
                )
            )
        }
    }

    fun applyDraftClearAll() {
        _uiState.update { cur ->
            val draft = cur.editingUserDraft ?: return@update cur
            cur.copy(
                editingUserDraft = draft.copy(
                    role = UserRole.USER,
                    allowedPages = emptySet(),
                    allowedFeatures = emptySet()
                )
            )
        }
    }

    fun saveEditingUserDraft() {
        val draft = _uiState.value.editingUserDraft ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val result = accessControlRepository.saveUserAccess(
                userId = draft.userId,
                email = draft.email,
                displayName = draft.displayName,
                role = draft.role,
                status = draft.status,
                allowedPages = draft.allowedPages,
                allowedFeatures = draft.allowedFeatures
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    editingUserDraft = if (result.isSuccess) null else it.editingUserDraft,
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "परमिशन सेव हो गईं: ${draft.email}" else "Permissions saved: ${draft.email}"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Failed to save permissions"
                    }
                )
            }
        }
    }

    fun quickAuthorizeEventsOnly(user: ManagedUserAccess) {
        val p = user.policy
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val result = accessControlRepository.saveUserAccess(
                userId = p.userId,
                email = p.email,
                displayName = p.displayName,
                role = UserRole.USER,
                status = AccessStatus.ACTIVE,
                allowedPages = setOf(AppPageKey.EVENTS_TIMELINE),
                allowedFeatures = emptySet()
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "इवेंट्स-ओनली अनुमति दी गई: ${p.email}" else "Authorized (Events-Only): ${p.email}"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Failed to authorize user"
                    }
                )
            }
        }
    }

    fun quickSetUserStatus(user: ManagedUserAccess, newStatus: AccessStatus) {
        val p = user.policy
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = accessControlRepository.saveUserAccess(
                userId = p.userId,
                email = p.email,
                displayName = p.displayName,
                role = p.role,
                status = newStatus,
                allowedPages = p.allowedPages.ifEmpty { setOf(AppPageKey.EVENTS_TIMELINE) },
                allowedFeatures = p.allowedFeatures
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    statusMessage = if (result.isSuccess) {
                        "${p.email} → ${newStatus.name}"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Status update failed"
                    }
                )
            }
        }
    }

    fun requestForceLogoutConfirmation(user: ManagedUserAccess) {
        _uiState.update { it.copy(confirmForceLogoutUser = user) }
    }

    fun dismissForceLogoutConfirmation() {
        _uiState.update { it.copy(confirmForceLogoutUser = null) }
    }

    fun confirmForceLogout() {
        val target = _uiState.value.confirmForceLogoutUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, confirmForceLogoutUser = null) }
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val result = accessControlRepository.forceSignOutUserSession(target.policy.userId)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "डिवाइस लॉगआउट किया गया: ${target.policy.email}"
                        else "Forced logout on device for ${target.policy.email}"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Force logout failed"
                    }
                )
            }
        }
    }

    // --- Pre-Authorize Email Invite Form ---
    fun updateInviteEmail(email: String) {
        _uiState.update { it.copy(inviteEmailInput = email) }
    }

    fun updateInviteRole(role: UserRole) {
        _uiState.update { cur ->
            cur.copy(
                inviteRole = role,
                invitePages = if (role == UserRole.ADMIN) AppPageKey.entries.toSet() else cur.invitePages,
                inviteFeatures = if (role == UserRole.ADMIN) AppFeatureKey.entries.toSet() else cur.inviteFeatures
            )
        }
    }

    fun updateInviteStatus(status: AccessStatus) {
        _uiState.update { it.copy(inviteStatus = status) }
    }

    fun toggleInvitePage(page: AppPageKey) {
        _uiState.update { cur ->
            val next = if (page in cur.invitePages) cur.invitePages - page else cur.invitePages + page
            cur.copy(invitePages = next)
        }
    }

    fun toggleInviteFeature(feature: AppFeatureKey) {
        _uiState.update { cur ->
            val nextFeatures = if (feature in cur.inviteFeatures) {
                cur.inviteFeatures - feature
            } else {
                cur.inviteFeatures + feature
            }
            val nextPages = if (feature !in cur.inviteFeatures && feature.parentPage != null) {
                cur.invitePages + feature.parentPage!!
            } else {
                cur.invitePages
            }
            cur.copy(invitePages = nextPages, inviteFeatures = nextFeatures)
        }
    }

    fun applyInviteEventsOnly() {
        _uiState.update {
            it.copy(
                inviteRole = UserRole.USER,
                inviteStatus = AccessStatus.ACTIVE,
                invitePages = setOf(AppPageKey.EVENTS_TIMELINE),
                inviteFeatures = emptySet()
            )
        }
    }

    fun applyInviteTattvaTimeOnly() {
        _uiState.update {
            it.copy(
                inviteRole = UserRole.USER,
                inviteStatus = AccessStatus.ACTIVE,
                invitePages = setOf(AppPageKey.TATTVA),
                inviteFeatures = emptySet()
            )
        }
    }

    fun setInviteAllTattvaFeatures(enabled: Boolean) {
        _uiState.update { cur ->
            val tattvaSet = AppFeatureKey.tattvaFeatures.toSet()
            val nextFeatures = if (enabled) cur.inviteFeatures + tattvaSet else cur.inviteFeatures - tattvaSet
            val nextPages = if (enabled) cur.invitePages + AppPageKey.TATTVA else cur.invitePages
            cur.copy(invitePages = nextPages, inviteFeatures = nextFeatures)
        }
    }

    fun setInviteSwaraTier(tier: SwaraTier) {
        _uiState.update { cur ->
            val tierSet = AppFeatureKey.swaraTierFeatures.toSet()
            val nextFeatures = (cur.inviteFeatures - tierSet) + AppFeatureKey.featureForSwaraTier(tier)
            val nextPages = cur.invitePages + AppPageKey.SWARA_YOGA
            cur.copy(invitePages = nextPages, inviteFeatures = nextFeatures)
        }
    }

    fun setInviteAllSwaraFeatures(enabled: Boolean) {
        _uiState.update { cur ->
            val swaraSet = AppFeatureKey.swaraFeatures.toSet()
            val nextFeatures = if (enabled) cur.inviteFeatures + swaraSet else cur.inviteFeatures - swaraSet
            val nextPages = if (enabled) cur.invitePages + AppPageKey.SWARA_YOGA else cur.invitePages
            cur.copy(invitePages = nextPages, inviteFeatures = nextFeatures)
        }
    }

    fun applyInviteSelectAll() {
        _uiState.update {
            it.copy(
                inviteStatus = AccessStatus.ACTIVE,
                invitePages = AppPageKey.entries.toSet(),
                inviteFeatures = AppFeatureKey.entries.toSet()
            )
        }
    }

    fun applyInviteClearAll() {
        _uiState.update {
            it.copy(
                inviteRole = UserRole.USER,
                invitePages = emptySet(),
                inviteFeatures = emptySet()
            )
        }
    }

    fun loadInviteIntoForm(invite: EmailInviteRule) {
        _uiState.update {
            it.copy(
                inviteEmailInput = invite.email,
                inviteRole = invite.role,
                inviteStatus = invite.status,
                invitePages = invite.allowedPages,
                inviteFeatures = invite.allowedFeatures
            )
        }
    }

    fun saveEmailInvite() {
        val s = _uiState.value
        val cleanEmail = s.inviteEmailInput.trim().lowercase()
        val isHi = s.preferences.language == AppLanguage.HINDI
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _uiState.update {
                it.copy(statusMessage = if (isHi) "कृपया सही ईमेल आईडी दर्ज करें" else "Please enter a valid email address")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = accessControlRepository.upsertEmailInvite(
                email = cleanEmail,
                role = s.inviteRole,
                status = s.inviteStatus,
                allowedPages = s.invitePages,
                allowedFeatures = s.inviteFeatures
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    inviteEmailInput = if (result.isSuccess) "" else it.inviteEmailInput,
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "ईमेल पूर्व-अनुमति सेव हो गई: $cleanEmail" else "Saved email pre-authorization: $cleanEmail"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Failed to save email invite"
                    }
                )
            }
        }
    }

    fun deleteEmailInvite(emailHash: String) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val result = accessControlRepository.deleteEmailInvite(emailHash)
            _uiState.update {
                it.copy(
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "पूर्व-अनुमति नियम हटाया गया" else "Removed pre-authorization rule"
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Delete failed"
                    }
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
