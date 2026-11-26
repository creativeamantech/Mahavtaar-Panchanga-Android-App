package com.mahavtaar.panchanga

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mahavtaar.panchanga.core.access.UserAccessCacheService
import com.mahavtaar.panchanga.database.access.toAccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppGateState
import com.mahavtaar.panchanga.domain.model.AppThemeMode
import com.mahavtaar.panchanga.domain.model.ROOT_ADMIN_EMAIL
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AccessControlRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.firebase.AuthScreen
import com.mahavtaar.panchanga.firebase.PendingApprovalScreen
import com.mahavtaar.panchanga.firebase.ResolvingAccessScreen
import com.mahavtaar.panchanga.firebase.SessionDisplacedScreen
import com.mahavtaar.panchanga.firebase.SuspendedAccessScreen
import com.mahavtaar.panchanga.firebase.authStateFlow
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.feature.settings.QuickSetupScreen
import com.mahavtaar.panchanga.navigation.HomeRoute
import com.mahavtaar.panchanga.navigation.HoraRoute
import com.mahavtaar.panchanga.navigation.PanchangaNavHost
import com.mahavtaar.panchanga.navigation.PanchangaRoute
import com.mahavtaar.panchanga.navigation.SwaraYogaRoute
import com.mahavtaar.panchanga.navigation.TattvaRoute
import com.mahavtaar.panchanga.navigation.TodayScheduleRoute
import com.mahavtaar.panchanga.navigation.isTopLevelRoute
import com.mahavtaar.panchanga.navigation.navigateTopLevel
import com.mahavtaar.panchanga.navigation.visibleTopLevelTabs
import com.mahavtaar.panchanga.notifications.PanchangaNotificationScheduler
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.access.ProvideAccessPolicy
import com.mahavtaar.panchanga.ui.localization.AccessControlStrings
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.theme.MahavtaarPanchangaTheme
import com.mahavtaar.panchanga.widget.WidgetDeepLinkDestination
import com.mahavtaar.panchanga.widget.WidgetUpdateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var notificationScheduler: PanchangaNotificationScheduler

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var cloudAccountRepository: CloudAccountRepository

    @Inject
    lateinit var accessControlRepository: AccessControlRepository

    @Inject
    lateinit var userAccessCacheService: UserAccessCacheService

    private var pendingWidgetDestination by mutableStateOf<WidgetDeepLinkDestination?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
        captureWidgetDestination(intent)

        setContent {
            val prefs by remember {
                preferencesRepository.preferences.catch { emit(UserPreferences()) }
            }.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )
            val currentUser by remember {
                firebaseAuth.authStateFlow().catch { emit(null) }
            }.collectAsStateWithLifecycle(
                initialValue = runCatching { firebaseAuth.currentUser }.getOrNull()
            )

            val windowSizeClass = calculateWindowSizeClass(this)
            val isDarkTheme = when (prefs.themeMode) {
                AppThemeMode.NIGHT_SKY -> true
                AppThemeMode.PARCHMENT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var displacedByDeviceName by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(isDarkTheme) {
                runCatching {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkTheme) {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        },
                        navigationBarStyle = if (isDarkTheme) {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        }
                    )
                }
            }

            LaunchedEffect(currentUser?.uid) {
                if (currentUser == null) {
                    runCatching { cloudAccountRepository.stopSync() }
                }
            }

            LaunchedEffect(
                prefs.widgetGlassStyle,
                prefs.widgetOpacity,
                prefs.language,
                prefs.selectedLocation.name,
                prefs.use24HourFormat
            ) {
                WidgetUpdateHelper.triggerAsyncUpdate(this@MainActivity)
            }

            MahavtaarPanchangaTheme(
                themeMode = prefs.themeMode,
                language = prefs.language
            ) {
                val ac = AccessControlStrings.forLanguage(prefs.language)
                val toggleLanguage = {
                    lifecycleScope.launch {
                        preferencesRepository.setLanguage(prefs.language.toggled())
                    }
                    Unit
                }
                val performSignOut = {
                    lifecycleScope.launch {
                        runCatching {
                            CredentialManager.create(this@MainActivity)
                                .clearCredentialState(ClearCredentialStateRequest())
                        }
                        runCatching { userAccessCacheService.clearAllCachedAccess() }
                        cloudAccountRepository.signOut()
                    }
                    Unit
                }

                val activeUser = currentUser
                val displacedDevice = displacedByDeviceName

                when {
                    displacedDevice != null -> {
                        SessionDisplacedScreen(
                            activeDeviceName = displacedDevice,
                            onSignInAgain = { displacedByDeviceName = null },
                            onToggleLanguage = toggleLanguage
                        )
                    }
                    activeUser == null -> {
                        AuthScreen(
                            onAuthSuccess = { displacedByDeviceName = null },
                            onToggleLanguage = toggleLanguage
                        )
                    }
                    else -> {
                        key(activeUser.uid) {
                            val initialUserGateState = remember(
                                activeUser.uid,
                                activeUser.email,
                                activeUser.displayName,
                                activeUser.isEmailVerified
                            ) {
                                val cached = userAccessCacheService
                                    .getInstantCachedAccess(activeUser.uid)
                                    ?.takeIf { it.userId == activeUser.uid }
                                val isRootAdmin = activeUser.isEmailVerified &&
                                    activeUser.email?.trim()?.equals(ROOT_ADMIN_EMAIL, ignoreCase = true) == true
                                val cachedPolicy = cached?.toAccessPolicy()
                                when {
                                    isRootAdmin -> AppGateState.Authorized(
                                        policy = AccessPolicy.ADMIN_FULL.copy(
                                            userId = activeUser.uid,
                                            email = activeUser.email?.trim()?.lowercase().orEmpty(),
                                            displayName = activeUser.displayName
                                        ),
                                        sessionToken = cached?.sessionToken.orEmpty()
                                    )
                                    cached != null && cachedPolicy != null && cachedPolicy.status == AccessStatus.SUSPENDED -> AppGateState.Suspended(
                                        email = activeUser.email ?: cached.email
                                    )
                                    cached != null && cachedPolicy != null && cachedPolicy.status == AccessStatus.ACTIVE &&
                                        (cachedPolicy.isAdmin || cachedPolicy.allowedPages.isNotEmpty()) -> AppGateState.Authorized(
                                        policy = cachedPolicy,
                                        sessionToken = cached.sessionToken
                                    )
                                    cached != null && cachedPolicy != null -> AppGateState.PendingApproval(
                                        uid = activeUser.uid,
                                        email = activeUser.email ?: cached.email,
                                        displayName = activeUser.displayName ?: cached.displayName
                                    )
                                    else -> AppGateState.ResolvingAccess(activeUser.email.orEmpty())
                                }
                            }

                            val gateFlow = remember(
                                activeUser.uid,
                                activeUser.email,
                                activeUser.displayName,
                                activeUser.isEmailVerified,
                                initialUserGateState
                            ) {
                                accessControlRepository.observeGateState(
                                    uid = activeUser.uid,
                                    email = activeUser.email,
                                    displayName = activeUser.displayName,
                                    isEmailVerified = activeUser.isEmailVerified
                                ).catch { emit(initialUserGateState) }
                            }

                            val gateState by gateFlow.collectAsStateWithLifecycle(
                                initialValue = initialUserGateState
                            )

                            var isCheckingPending by remember { mutableStateOf(false) }
                            var pendingStatusMessage by remember { mutableStateOf<String?>(null) }

                            LaunchedEffect(gateState, activeUser.uid) {
                                when (val g = gateState) {
                                    is AppGateState.Authorized -> {
                                        displacedByDeviceName = null
                                        runCatching { cloudAccountRepository.startSyncForUser(activeUser.uid) }
                                    }
                                    is AppGateState.SessionDisplaced -> {
                                        displacedByDeviceName = g.activeDeviceName
                                        runCatching { userAccessCacheService.clearAllCachedAccess() }
                                        runCatching { cloudAccountRepository.signOut() }
                                    }
                                    else -> {
                                        runCatching { cloudAccountRepository.stopSync() }
                                    }
                                }
                            }

                            LaunchedEffect(
                                gateState is AppGateState.Authorized,
                                prefs.selectedLocation.name,
                                prefs.selectedLocation.latitude,
                                prefs.selectedLocation.longitude,
                                prefs.language,
                                prefs.ayanamsha,
                                prefs.monthSystem
                            ) {
                                if (gateState is AppGateState.Authorized) {
                                    runCatching { notificationScheduler.rescheduleNow() }
                                }
                            }

                            when (val currentGate = gateState) {
                                is AppGateState.Unauthenticated -> {
                                    AuthScreen(
                                        onAuthSuccess = { displacedByDeviceName = null },
                                        onToggleLanguage = toggleLanguage
                                    )
                                }
                                is AppGateState.ResolvingAccess -> {
                                    ResolvingAccessScreen(
                                        email = activeUser.email.orEmpty(),
                                        onToggleLanguage = toggleLanguage
                                    )
                                }
                                is AppGateState.SessionDisplaced -> {
                                    SessionDisplacedScreen(
                                        activeDeviceName = currentGate.activeDeviceName,
                                        onSignInAgain = { displacedByDeviceName = null },
                                        onToggleLanguage = toggleLanguage
                                    )
                                }
                                is AppGateState.PendingApproval -> {
                                    PendingApprovalScreen(
                                        email = currentGate.email.ifBlank { activeUser.email.orEmpty() },
                                        uid = currentGate.uid.ifBlank { activeUser.uid },
                                        isChecking = isCheckingPending,
                                        statusMessage = pendingStatusMessage,
                                        onCheckStatus = {
                                            lifecycleScope.launch {
                                                isCheckingPending = true
                                                pendingStatusMessage = null
                                                accessControlRepository.refreshAccessGateOnce(
                                                    uid = activeUser.uid,
                                                    email = activeUser.email,
                                                    displayName = activeUser.displayName,
                                                    isEmailVerified = activeUser.isEmailVerified
                                                ).onSuccess { refreshed ->
                                                    if (refreshed is AppGateState.PendingApproval) {
                                                        pendingStatusMessage = ac.stillPendingMsg
                                                    }
                                                }.onFailure { err ->
                                                    pendingStatusMessage = err.localizedMessage
                                                }
                                                isCheckingPending = false
                                            }
                                        },
                                        onSignOut = performSignOut,
                                        onToggleLanguage = toggleLanguage
                                    )
                                }
                                is AppGateState.Suspended -> {
                                    SuspendedAccessScreen(
                                        email = currentGate.email.ifBlank { activeUser.email.orEmpty() },
                                        onSignOut = performSignOut,
                                        onToggleLanguage = toggleLanguage
                                    )
                                }
                                is AppGateState.Authorized -> {
                                    ProvideAccessPolicy(policy = currentGate.policy) {
                                        if (!prefs.hasCompletedOnboarding) {
                                            QuickSetupScreen(
                                                onFinished = { notificationScheduler.rescheduleNow() }
                                            )
                                        } else {
                                            PanchangaRootShell(
                                                useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
                                                pendingWidgetDestination = pendingWidgetDestination,
                                                onConsumeWidgetDestination = { pendingWidgetDestination = null },
                                                onNotificationsChanged = { notificationScheduler.rescheduleNow() },
                                                onSignOut = performSignOut
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureWidgetDestination(intent)
    }

    private fun captureWidgetDestination(intent: Intent?) {
        val raw = intent?.getStringExtra(WidgetDeepLinkDestination.EXTRA_WIDGET_DESTINATION)
        val parsed = WidgetDeepLinkDestination.fromKey(raw)
        if (parsed != null) {
            pendingWidgetDestination = parsed
            intent?.removeExtra(WidgetDeepLinkDestination.EXTRA_WIDGET_DESTINATION)
        }
    }
}

internal fun resolveWidgetDestinationRoute(
    destination: WidgetDeepLinkDestination,
    policy: AccessPolicy
): Pair<Any, Boolean> = when (destination) {
    WidgetDeepLinkDestination.HOME -> HomeRoute to true
    WidgetDeepLinkDestination.PANCHANGA ->
        if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) PanchangaRoute to true else HomeRoute to true
    WidgetDeepLinkDestination.HORA ->
        if (policy.canAccessPage(AppPageKey.HORA)) HoraRoute to true else HomeRoute to true
    WidgetDeepLinkDestination.TATTVA ->
        if (policy.canAccessPage(AppPageKey.TATTVA)) TattvaRoute to false else HomeRoute to true
    WidgetDeepLinkDestination.SWARA ->
        if (policy.canAccessPage(AppPageKey.SWARA_YOGA)) SwaraYogaRoute to false else HomeRoute to true
    WidgetDeepLinkDestination.SCHEDULE ->
        if (policy.canAccessPage(AppPageKey.TODAY_SCHEDULE)) TodayScheduleRoute to false else HomeRoute to true
}

@Composable
private fun PanchangaRootShell(
    useNavigationRail: Boolean,
    pendingWidgetDestination: WidgetDeepLinkDestination?,
    onConsumeWidgetDestination: () -> Unit,
    onNotificationsChanged: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val tabs = remember(policy) { visibleTopLevelTabs(policy) }
    val showTopLevelChrome = tabs.size > 1 && tabs.any { currentDestination.isTopLevelRoute(it.route) }

    LaunchedEffect(pendingWidgetDestination, policy) {
        val target = pendingWidgetDestination ?: return@LaunchedEffect
        val (route, isTopLevel) = resolveWidgetDestinationRoute(target, policy)
        if (isTopLevel) {
            navController.navigateTopLevel(route)
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
        onConsumeWidgetDestination()
    }

    if (useNavigationRail) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelChrome) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = currentDestination.isTopLevelRoute(tab.route)
                            val label = tab.labelProvider(strings, ext)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navController.navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
                PanchangaNavHost(
                    navController = navController,
                    onNotificationsChanged = onNotificationsChanged,
                    onSignOut = onSignOut,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showTopLevelChrome) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = currentDestination.isTopLevelRoute(tab.route)
                            val label = tab.labelProvider(strings, ext)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            PanchangaNavHost(
                navController = navController,
                onNotificationsChanged = onNotificationsChanged,
                onSignOut = onSignOut,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
