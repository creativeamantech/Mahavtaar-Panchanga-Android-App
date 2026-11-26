package com.mahavtaar.panchanga.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.feature.calendar.AnnualFestivalsScreen
import com.mahavtaar.panchanga.feature.calendar.HinduCalendarScreen
import com.mahavtaar.panchanga.feature.daily.DailyPanchangaScreen
import com.mahavtaar.panchanga.feature.daily.KundliScreen
import com.mahavtaar.panchanga.feature.daily.KundliTab
import com.mahavtaar.panchanga.feature.daily.NavagrahaMantrasScreen
import com.mahavtaar.panchanga.feature.daily.PlanetaryPositionsScreen
import com.mahavtaar.panchanga.feature.daily.TodayScheduleScreen
import com.mahavtaar.panchanga.feature.home.EventsTimelineScreen
import com.mahavtaar.panchanga.feature.home.HomeScreen
import com.mahavtaar.panchanga.feature.hora.HoraScreen
import com.mahavtaar.panchanga.feature.settings.AboutScreen
import com.mahavtaar.panchanga.feature.settings.AdminPanelScreen
import com.mahavtaar.panchanga.feature.settings.LocationManagerScreen
import com.mahavtaar.panchanga.feature.settings.MoreHubScreen
import com.mahavtaar.panchanga.feature.settings.NotificationStudioScreen
import com.mahavtaar.panchanga.feature.settings.ProfileScreen
import com.mahavtaar.panchanga.feature.settings.SettingsScreen
import com.mahavtaar.panchanga.feature.swara.NavtaraScreen
import com.mahavtaar.panchanga.feature.swara.SwaraYogaScreen
import com.mahavtaar.panchanga.feature.swara.TattvaScreen
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.localization.ExtendedStrings
import com.mahavtaar.panchanga.ui.localization.PanchangaStrings
import kotlinx.serialization.Serializable

@Serializable data object EventsTimelineRoute
@Serializable data object HomeRoute
@Serializable data object PanchangaRoute
@Serializable data object CalendarRoute
@Serializable data object HoraRoute
@Serializable data object MoreRoute
@Serializable data object TodayScheduleRoute
@Serializable data class KundliRoute(val openGunaMilan: Boolean = false)
@Serializable data object NavagrahaMantrasRoute
@Serializable data object NavtaraRoute
@Serializable data object TattvaRoute
@Serializable data object AnnualFestivalsRoute
@Serializable data object SwaraYogaRoute
@Serializable data object PlanetaryPositionsRoute
@Serializable data object LocationsRoute
@Serializable data class NotificationStudioRoute(val focusEventTypeName: String? = null)
@Serializable data object SettingsRoute
@Serializable data object AboutRoute
@Serializable data object AdminPanelRoute
@Serializable data object ProfileRoute

data class TopLevelTab(
    val route: Any,
    val labelProvider: (PanchangaStrings, ExtendedStrings) -> String,
    val icon: ImageVector,
    val testTag: String
)

fun visibleTopLevelTabs(policy: AccessPolicy): List<TopLevelTab> {
    if (policy.isAdmin) {
        return listOf(
            TopLevelTab(HomeRoute, { s, _ -> s.navHome }, Icons.Rounded.Home, "nav_tab_home"),
            TopLevelTab(PanchangaRoute, { s, _ -> s.navPanchanga }, Icons.Rounded.AutoAwesome, "nav_tab_panchanga"),
            TopLevelTab(CalendarRoute, { s, _ -> s.navCalendar }, Icons.Rounded.CalendarMonth, "nav_tab_calendar"),
            TopLevelTab(HoraRoute, { s, _ -> s.navHora }, Icons.Rounded.Schedule, "nav_tab_hora"),
            TopLevelTab(MoreRoute, { s, _ -> s.navMore }, Icons.Rounded.GridView, "nav_tab_more")
        )
    }

    val primaryTabs = mutableListOf<TopLevelTab>()
    if (policy.canAccessPage(AppPageKey.EVENTS_TIMELINE)) {
        primaryTabs += TopLevelTab(
            route = EventsTimelineRoute,
            labelProvider = { _, ext -> ext.eventsTabLabel },
            icon = Icons.Rounded.EventAvailable,
            testTag = "nav_tab_events"
        )
    }
    if (policy.canAccessPage(AppPageKey.HOME)) {
        primaryTabs += TopLevelTab(
            route = HomeRoute,
            labelProvider = { s, _ -> s.navHome },
            icon = Icons.Rounded.Home,
            testTag = "nav_tab_home"
        )
    }
    if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) {
        primaryTabs += TopLevelTab(
            route = PanchangaRoute,
            labelProvider = { s, _ -> s.navPanchanga },
            icon = Icons.Rounded.AutoAwesome,
            testTag = "nav_tab_panchanga"
        )
    }
    if (policy.canAccessPage(AppPageKey.CALENDAR)) {
        primaryTabs += TopLevelTab(
            route = CalendarRoute,
            labelProvider = { s, _ -> s.navCalendar },
            icon = Icons.Rounded.CalendarMonth,
            testTag = "nav_tab_calendar"
        )
    }
    if (policy.canAccessPage(AppPageKey.HORA)) {
        primaryTabs += TopLevelTab(
            route = HoraRoute,
            labelProvider = { s, _ -> s.navHora },
            icon = Icons.Rounded.Schedule,
            testTag = "nav_tab_hora"
        )
    }

    val hasSecondaryPages = listOf(
        AppPageKey.TODAY_SCHEDULE,
        AppPageKey.KUNDLI,
        AppPageKey.GUNA_MILAN,
        AppPageKey.NAVAGRAHA_MANTRAS,
        AppPageKey.NAVTATA,
        AppPageKey.TATTVA,
        AppPageKey.ANNUAL_FESTIVALS,
        AppPageKey.SWARA_YOGA,
        AppPageKey.PLANETARY_POSITIONS,
        AppPageKey.LOCATIONS,
        AppPageKey.ABOUT
    ).any { policy.canAccessPage(it) } ||
        policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY) ||
        policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)

    if (hasSecondaryPages || primaryTabs.size > 4) {
        val cappedPrimary = primaryTabs.take(4)
        return cappedPrimary + TopLevelTab(
            route = MoreRoute,
            labelProvider = { s, _ -> s.navMore },
            icon = Icons.Rounded.GridView,
            testTag = "nav_tab_more"
        )
    }

    if (primaryTabs.isEmpty()) {
        primaryTabs += TopLevelTab(
            route = EventsTimelineRoute,
            labelProvider = { _, ext -> ext.eventsTabLabel },
            icon = Icons.Rounded.EventAvailable,
            testTag = "nav_tab_events"
        )
    }

    return primaryTabs + TopLevelTab(
        route = SettingsRoute,
        labelProvider = { s, _ -> s.settingsTitle },
        icon = Icons.Rounded.Tune,
        testTag = "nav_tab_settings"
    )
}

private fun routeForLandingPage(page: AppPageKey): Any = when (page) {
    AppPageKey.EVENTS_TIMELINE -> EventsTimelineRoute
    AppPageKey.HOME -> HomeRoute
    AppPageKey.DAILY_PANCHANGA -> PanchangaRoute
    AppPageKey.CALENDAR -> CalendarRoute
    AppPageKey.HORA -> HoraRoute
    AppPageKey.TODAY_SCHEDULE -> TodayScheduleRoute
    AppPageKey.PLANETARY_POSITIONS -> PlanetaryPositionsRoute
    AppPageKey.NAVTATA -> NavtaraRoute
    AppPageKey.KUNDLI -> KundliRoute(openGunaMilan = false)
    AppPageKey.GUNA_MILAN -> KundliRoute(openGunaMilan = true)
    AppPageKey.SWARA_YOGA -> SwaraYogaRoute
    AppPageKey.TATTVA -> TattvaRoute
    AppPageKey.NAVAGRAHA_MANTRAS -> NavagrahaMantrasRoute
    AppPageKey.ANNUAL_FESTIVALS -> AnnualFestivalsRoute
    AppPageKey.LOCATIONS -> LocationsRoute
    AppPageKey.SETTINGS -> SettingsRoute
    AppPageKey.ABOUT -> AboutRoute
}

fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun androidx.navigation.NavDestination?.isTopLevelRoute(route: Any): Boolean =
    this?.hasRoute(route::class) == true

@Composable
private fun RequirePageAccess(
    allowed: Boolean,
    navController: NavHostController,
    fallbackRoute: Any,
    content: @Composable () -> Unit
) {
    if (allowed) {
        content()
    } else {
        LaunchedEffect(fallbackRoute) {
            navController.navigate(fallbackRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }
}

@Composable
fun PanchangaNavHost(
    navController: NavHostController,
    onNotificationsChanged: () -> Unit,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val policy = LocalAccessPolicy.current
    val landingRoute = routeForLandingPage(policy.defaultLandingPage())

    NavHost(
        navController = navController,
        startDestination = landingRoute,
        modifier = modifier
    ) {
        composable<EventsTimelineRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.EVENTS_TIMELINE) || policy.allowedPages.isEmpty(),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                EventsTimelineScreen(
                    onOpenLocations = {
                        if (policy.canAccessPage(AppPageKey.LOCATIONS)) {
                            navController.navigate(LocationsRoute)
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(SettingsRoute)
                    },
                    onOpenAdminPanel = {
                        if (policy.isAdmin) {
                            navController.navigate(AdminPanelRoute)
                        }
                    },
                    onOpenProfile = {
                        navController.navigate(ProfileRoute)
                    }
                )
            }
        }

        composable<HomeRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.HOME),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                HomeScreen(
                    onOpenDailyPanchanga = { navController.navigateTopLevel(PanchangaRoute) },
                    onOpenHora = { navController.navigateTopLevel(HoraRoute) },
                    onOpenSwaraYoga = { navController.navigate(SwaraYogaRoute) },
                    onOpenPlanetaryPositions = { navController.navigate(PlanetaryPositionsRoute) },
                    onOpenLocations = { navController.navigate(LocationsRoute) },
                    onOpenTodaySchedule = { navController.navigate(TodayScheduleRoute) },
                    onOpenKundli = { navController.navigate(KundliRoute(openGunaMilan = false)) },
                    onOpenGunaMilan = { navController.navigate(KundliRoute(openGunaMilan = true)) },
                    onOpenNavagrahaMantras = { navController.navigate(NavagrahaMantrasRoute) },
                    onOpenNavtara = { navController.navigate(NavtaraRoute) },
                    onOpenTattva = { navController.navigate(TattvaRoute) },
                    onOpenAnnualFestivals = { navController.navigate(AnnualFestivalsRoute) },
                    onOpenNotificationStudio = { navController.navigate(NotificationStudioRoute()) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenAdminPanel = {
                        if (policy.isAdmin) navController.navigate(AdminPanelRoute)
                    },
                    onOpenProfile = {
                        navController.navigate(ProfileRoute)
                    }
                )
            }
        }

        composable<PanchangaRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.DAILY_PANCHANGA),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                DailyPanchangaScreen(
                    onOpenLocations = {
                        if (policy.canAccessPage(AppPageKey.LOCATIONS)) {
                            navController.navigate(LocationsRoute)
                        }
                    }
                )
            }
        }

        composable<CalendarRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.CALENDAR),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                HinduCalendarScreen(
                    onOpenDailyPanchanga = {
                        if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) {
                            navController.navigateTopLevel(PanchangaRoute)
                        }
                    },
                    onOpenAnnualFestivals = {
                        if (policy.canAccessPage(AppPageKey.ANNUAL_FESTIVALS)) {
                            navController.navigate(AnnualFestivalsRoute)
                        }
                    },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute())
                        }
                    }
                )
            }
        }

        composable<HoraRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.HORA),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                HoraScreen(
                    onOpenLocations = {
                        if (policy.canAccessPage(AppPageKey.LOCATIONS)) {
                            navController.navigate(LocationsRoute)
                        }
                    },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute(VedicAlertEventType.PLANETARY_HORA.name))
                        }
                    }
                )
            }
        }

        composable<MoreRoute> {
            MoreHubScreen(
                onOpenTodaySchedule = { navController.navigate(TodayScheduleRoute) },
                onOpenKundli = { navController.navigate(KundliRoute(openGunaMilan = false)) },
                onOpenGunaMilan = { navController.navigate(KundliRoute(openGunaMilan = true)) },
                onOpenNavagrahaMantras = { navController.navigate(NavagrahaMantrasRoute) },
                onOpenNavtara = { navController.navigate(NavtaraRoute) },
                onOpenTattva = { navController.navigate(TattvaRoute) },
                onOpenAnnualFestivals = { navController.navigate(AnnualFestivalsRoute) },
                onOpenSwaraYoga = { navController.navigate(SwaraYogaRoute) },
                onOpenPlanetaryPositions = { navController.navigate(PlanetaryPositionsRoute) },
                onOpenLocations = { navController.navigate(LocationsRoute) },
                onOpenNotificationStudio = { navController.navigate(NotificationStudioRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenAbout = { navController.navigate(AboutRoute) },
                onOpenAdminPanel = {
                    if (policy.isAdmin) navController.navigate(AdminPanelRoute)
                },
                onOpenProfile = { navController.navigate(ProfileRoute) },
                onSignOut = onSignOut
            )
        }

        composable<TodayScheduleRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.TODAY_SCHEDULE),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                TodayScheduleScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute())
                        }
                    }
                )
            }
        }

        composable<KundliRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<KundliRoute>()
            val requiredPage = if (route.openGunaMilan) AppPageKey.GUNA_MILAN else AppPageKey.KUNDLI
            RequirePageAccess(
                allowed = policy.canAccessPage(requiredPage) ||
                    policy.canAccessPage(AppPageKey.KUNDLI) ||
                    policy.canAccessPage(AppPageKey.GUNA_MILAN),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                val initialTab = if (route.openGunaMilan || !policy.canAccessPage(AppPageKey.KUNDLI)) {
                    KundliTab.GUNA_MILAN
                } else {
                    KundliTab.CHARTS
                }
                KundliScreen(
                    onNavigateBack = { navController.popBackStack() },
                    initialTab = initialTab
                )
            }
        }

        composable<NavagrahaMantrasRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.NAVAGRAHA_MANTRAS),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                NavagrahaMantrasScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable<NavtaraRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.NAVTATA),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                NavtaraScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable<TattvaRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.TATTVA),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                TattvaScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute(VedicAlertEventType.HORA_SUB_TATTVA.name))
                        }
                    },
                    onOpenLocations = {
                        if (policy.canAccessPage(AppPageKey.LOCATIONS)) {
                            navController.navigate(LocationsRoute)
                        }
                    }
                )
            }
        }

        composable<AnnualFestivalsRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.ANNUAL_FESTIVALS),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                AnnualFestivalsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute(VedicAlertEventType.VRAT_OBSERVANCE.name))
                        }
                    }
                )
            }
        }

        composable<SwaraYogaRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.SWARA_YOGA),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                SwaraYogaScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenNotificationStudio = {
                        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                            navController.navigate(NotificationStudioRoute(VedicAlertEventType.SWARA_SUNRISE_NADI.name))
                        }
                    }
                )
            }
        }

        composable<PlanetaryPositionsRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.PLANETARY_POSITIONS),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                PlanetaryPositionsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable<LocationsRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.LOCATIONS),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                LocationManagerScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable<NotificationStudioRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NotificationStudioRoute>()
            RequirePageAccess(
                allowed = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                NotificationStudioScreen(
                    onNavigateBack = { navController.popBackStack() },
                    focusEventTypeName = route.focusEventTypeName
                )
            }
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigateTopLevel(landingRoute)
                    }
                },
                onNotificationsChanged = onNotificationsChanged,
                onOpenNotificationStudio = {
                    if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                        navController.navigate(NotificationStudioRoute())
                    }
                },
                onOpenAdminPanel = {
                    if (policy.isAdmin) navController.navigate(AdminPanelRoute)
                },
                onOpenProfile = { navController.navigate(ProfileRoute) },
                onSignOut = onSignOut
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = onSignOut,
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenNavtara = {
                    if (policy.canAccessPage(AppPageKey.NAVTATA)) {
                        navController.navigate(NavtaraRoute)
                    }
                }
            )
        }

        composable<AboutRoute> {
            RequirePageAccess(
                allowed = policy.canAccessPage(AppPageKey.ABOUT),
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable<AdminPanelRoute> {
            RequirePageAccess(
                allowed = policy.isAdmin,
                navController = navController,
                fallbackRoute = landingRoute
            ) {
                AdminPanelScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
