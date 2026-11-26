package com.mahavtaar.panchanga.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ConfirmActionDialog
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.ResponsiveChoiceGroup
import com.mahavtaar.panchanga.ui.components.VedicGlossaryBottomSheet
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

private enum class MoreCategory {
    ADMIN,
    DAILY_PRACTICE,
    ASTROLOGY_CHARTS,
    APP_PREFERENCES
}

private data class MoreHubEntry(
    val id: String,
    val category: MoreCategory,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val tag: String,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun MoreHubScreen(
    onOpenTodaySchedule: () -> Unit,
    onOpenKundli: () -> Unit,
    onOpenGunaMilan: () -> Unit,
    onOpenNavagrahaMantras: () -> Unit,
    onOpenNavtara: () -> Unit,
    onOpenTattva: () -> Unit,
    onOpenAnnualFestivals: () -> Unit,
    onOpenSwaraYoga: () -> Unit,
    onOpenPlanetaryPositions: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val vedic = LocalVedicColors.current

    var showGlossary by remember { mutableStateOf(false) }
    var showConfirmSignOut by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allEntries = buildList {
        if (policy.isAdmin) {
            add(
                MoreHubEntry(
                    id = "dest_admin_panel",
                    category = MoreCategory.ADMIN,
                    icon = Icons.Rounded.AdminPanelSettings,
                    title = ext.adminPanelTitle,
                    subtitle = ext.adminPanelSubtitle,
                    keywords = "admin access users rbac एडमिन",
                    tag = "more_open_admin_panel",
                    accent = vedic.petalPlum,
                    onClick = onOpenAdminPanel
                )
            )
        }
        // 1. Daily Practice
        if (policy.canAccessPage(AppPageKey.TODAY_SCHEDULE)) {
            add(
                MoreHubEntry(
                    id = "dest_schedule",
                    category = MoreCategory.DAILY_PRACTICE,
                    icon = Icons.Rounded.Timeline,
                    title = strings.moreScheduleTitle,
                    subtitle = strings.moreScheduleSubtitle,
                    keywords = "schedule choghadiya rahu kalam timeline muhurta चौघड़िया राहु काल समय",
                    tag = "more_open_schedule",
                    accent = vedic.petalSaffron,
                    onClick = onOpenTodaySchedule
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.SWARA_YOGA)) {
            add(
                MoreHubEntry(
                    id = "dest_swara",
                    category = MoreCategory.DAILY_PRACTICE,
                    icon = Icons.Rounded.Air,
                    title = strings.moreSwaraTitle,
                    subtitle = strings.moreSwaraSubtitle,
                    keywords = "swara yoga shiva swarodaya nadi ida pingala sushumna स्वर नाड़ी इड़ा पिंगला",
                    tag = "more_open_swara",
                    accent = vedic.petalAzure,
                    onClick = onOpenSwaraYoga
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.TATTVA)) {
            add(
                MoreHubEntry(
                    id = "dest_tattva",
                    category = MoreCategory.DAILY_PRACTICE,
                    icon = Icons.Rounded.Spa,
                    title = strings.moreTattvaTitle,
                    subtitle = strings.moreTattvaSubtitle,
                    keywords = "tattva pancha prithvi jala agni vayu akasha तत्व पंचतत्व",
                    tag = "more_open_tattva",
                    accent = vedic.petalEmerald,
                    onClick = onOpenTattva
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.NAVTATA)) {
            add(
                MoreHubEntry(
                    id = "dest_navtara",
                    category = MoreCategory.DAILY_PRACTICE,
                    icon = Icons.Rounded.Stars,
                    title = strings.moreNavtaraTitle,
                    subtitle = strings.moreNavtaraSubtitle,
                    keywords = "navtara tara bala nakshatra नवतारा तारा बल नक्षत्र",
                    tag = "more_open_navtara",
                    accent = vedic.petalAzure,
                    onClick = onOpenNavtara
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.ANNUAL_FESTIVALS)) {
            add(
                MoreHubEntry(
                    id = "dest_festivals",
                    category = MoreCategory.DAILY_PRACTICE,
                    icon = Icons.Rounded.Celebration,
                    title = strings.moreFestivalsTitle,
                    subtitle = strings.moreFestivalsSubtitle,
                    keywords = "festivals vrat ekadashi pradosh sankashti पर्व व्रत एकादशी प्रदोष त्योहार",
                    tag = "more_open_festivals",
                    accent = vedic.petalSaffron,
                    onClick = onOpenAnnualFestivals
                )
            )
        }

        // 2. Astrology & Charts
        if (policy.canAccessPage(AppPageKey.KUNDLI)) {
            add(
                MoreHubEntry(
                    id = "dest_kundli",
                    category = MoreCategory.ASTROLOGY_CHARTS,
                    icon = Icons.Rounded.GridView,
                    title = strings.moreKundliTitle,
                    subtitle = strings.moreKundliSubtitle,
                    keywords = "kundli janma lagna dasha horoscope chart कुंडली जन्म लग्न दशा",
                    tag = "more_open_kundli",
                    accent = vedic.petalPlum,
                    onClick = onOpenKundli
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.GUNA_MILAN)) {
            add(
                MoreHubEntry(
                    id = "dest_guna",
                    category = MoreCategory.ASTROLOGY_CHARTS,
                    icon = Icons.Rounded.Favorite,
                    title = strings.moreGunaMilanTitle,
                    subtitle = strings.moreGunaMilanSubtitle,
                    keywords = "guna milan ashtakoota marriage compatibility matchmaking गुण मिलान विवाह",
                    tag = "more_open_guna_milan",
                    accent = vedic.petalPlum,
                    onClick = onOpenGunaMilan
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.PLANETARY_POSITIONS)) {
            add(
                MoreHubEntry(
                    id = "dest_planets",
                    category = MoreCategory.ASTROLOGY_CHARTS,
                    icon = Icons.Rounded.Public,
                    title = strings.morePlanetsTitle,
                    subtitle = strings.morePlanetsSubtitle,
                    keywords = "planets graha gochar transit ephemeris ग्रह गोचर",
                    tag = "more_open_planets",
                    accent = vedic.petalEmerald,
                    onClick = onOpenPlanetaryPositions
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.NAVAGRAHA_MANTRAS)) {
            add(
                MoreHubEntry(
                    id = "dest_mantras",
                    category = MoreCategory.ASTROLOGY_CHARTS,
                    icon = Icons.Rounded.SelfImprovement,
                    title = strings.moreNavagrahaMantrasTitle,
                    subtitle = strings.moreNavagrahaMantrasSubtitle,
                    keywords = "navagraha mantra japa remedies stotra नवग्रह मंत्र जप उपाय",
                    tag = "more_open_mantras",
                    accent = vedic.petalSaffron,
                    onClick = onOpenNavagrahaMantras
                )
            )
        }

        // 3. App & Preferences
        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
            add(
                MoreHubEntry(
                    id = "dest_notification_studio",
                    category = MoreCategory.APP_PREFERENCES,
                    icon = Icons.Rounded.NotificationsActive,
                    title = strings.moreNotificationStudioTitle,
                    subtitle = strings.moreNotificationStudioSubtitle,
                    keywords = "alerts notifications dnd quiet hours voice chime alarm अलर्ट सूचना ध्वनि क्वाइट",
                    tag = "more_open_notification_studio",
                    accent = vedic.petalPlum,
                    onClick = onOpenNotificationStudio
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.LOCATIONS)) {
            add(
                MoreHubEntry(
                    id = "dest_locations",
                    category = MoreCategory.APP_PREFERENCES,
                    icon = Icons.Rounded.LocationOn,
                    title = strings.moreLocationsTitle,
                    subtitle = "${strings.currentLocationPrefix}: ${state.preferences.selectedLocation.name} (${state.preferences.selectedLocation.timezone})",
                    keywords = "location city gps timezone coordinates स्थान शहर जीपीएस",
                    tag = "more_open_locations",
                    accent = vedic.petalEmerald,
                    onClick = onOpenLocations
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.SETTINGS)) {
            add(
                MoreHubEntry(
                    id = "dest_settings",
                    category = MoreCategory.APP_PREFERENCES,
                    icon = Icons.Rounded.Tune,
                    title = strings.moreSettingsTitle,
                    subtitle = strings.moreSettingsSubtitle,
                    keywords = "settings theme language ayanamsha backup dnd quiet setup सेटिंग्स भाषा थीम बैकअप",
                    tag = "more_open_settings",
                    accent = vedic.petalAzure,
                    onClick = onOpenSettings
                )
            )
        }
        if (policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)) {
            add(
                MoreHubEntry(
                    id = "dest_glossary",
                    category = MoreCategory.APP_PREFERENCES,
                    icon = Icons.Rounded.MenuBook,
                    title = ext.vedicGlossaryTitle,
                    subtitle = ext.vedicGlossarySubtitle,
                    keywords = "glossary dictionary meaning terms help शब्दकोश अर्थ जानकारी",
                    tag = "more_open_glossary",
                    accent = vedic.petalAzure,
                    onClick = { showGlossary = true }
                )
            )
        }
        if (policy.canAccessPage(AppPageKey.ABOUT)) {
            add(
                MoreHubEntry(
                    id = "dest_about",
                    category = MoreCategory.APP_PREFERENCES,
                    icon = Icons.Rounded.Info,
                    title = strings.moreAboutTitle,
                    subtitle = strings.moreAboutSubtitle,
                    keywords = "about version info app बारे में जानकारी",
                    tag = "more_open_about",
                    accent = vedic.petalSaffron,
                    onClick = onOpenAbout
                )
            )
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredEntries = remember(allEntries, normalizedQuery) {
        if (normalizedQuery.isEmpty()) allEntries
        else allEntries.filter { entry ->
            entry.title.lowercase().contains(normalizedQuery) ||
                entry.subtitle.lowercase().contains(normalizedQuery) ||
                entry.keywords.lowercase().contains(normalizedQuery)
        }
    }
    val groupedEntries = remember(filteredEntries) {
        filteredEntries.groupBy { it.category }
    }

    if (showConfirmSignOut) {
        ConfirmActionDialog(
            title = if (isHi) "साइन आउट करें?" else "Sign Out?",
            message = if (isHi) {
                "क्या आप अपने खाते से साइन आउट करना चाहते हैं? आपकी सहेजी गई सेटिंग्स सुरक्षित रहेंगी।"
            } else {
                "Are you sure you want to sign out? Your saved preferences stay safely backed up."
            },
            confirmLabel = ext.signOutButton,
            cancelLabel = strings.cancel,
            isDestructive = true,
            onConfirm = {
                showConfirmSignOut = false
                onSignOut?.invoke() ?: viewModel.signOut()
            },
            onDismiss = { showConfirmSignOut = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("more_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MahavtaarBrandLogo(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.exploreAndConfigure, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${state.preferences.selectedLocation.name} · ${VedicLocalizer.ayanamshaLabel(state.preferences.ayanamsha, lang)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search bar across all More Hub tools & settings
            item(key = "more_search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (isHi) "टूल या सेटिंग खोजें (जैसे कुंडली, स्वर, तत्व, अलर्ट)..."
                            else "Search tools & settings (Kundli, Swara, Tattva, Alerts)..."
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("more_search_clear")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = if (isHi) "साफ़ करें" else "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("more_search_input")
                )
            }

            if (normalizedQuery.isEmpty()) {
                item(key = "google_profile_card") {
                    val user = state.cloudUser
                    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
                        ?: user?.email?.substringBefore("@")
                        ?: ext.profileTitle
                    val initials = displayName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "ॐ" }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable(onClick = onOpenProfile)
                            .testTag("more_profile_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user?.email ?: if (isHi) "प्रोफ़ाइल और क्लाउड बैकअप खोलें" else "Open Profile & Cloud Backup",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = ext.profileTitle)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenProfile,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("more_open_profile_button")
                                ) {
                                    Text(ext.profileTitle)
                                }
                                OutlinedButton(
                                    onClick = { showConfirmSignOut = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("more_logout_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ext.signOutButton)
                                }
                            }
                        }
                    }
                }

                // Large-font-safe bilingual switcher
                item(key = "quick_language") {
                    ResponsiveChoiceGroup(
                        options = AppLanguage.entries,
                        selected = state.preferences.language,
                        labelFor = { it.nativeLabel },
                        tagFor = { "more_lang_${it.code}" },
                        onSelect = viewModel::setLanguage
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
                item(key = "more_empty_search") {
                    PanchangaCard {
                        Text(
                            text = if (isHi) {
                                "\"$searchQuery\" के लिए कोई टूल या सेटिंग नहीं मिली।"
                            } else {
                                "No tools or settings match \"$searchQuery\"."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val categoryOrder = listOf(
                    MoreCategory.ADMIN,
                    MoreCategory.DAILY_PRACTICE,
                    MoreCategory.ASTROLOGY_CHARTS,
                    MoreCategory.APP_PREFERENCES
                )
                categoryOrder.forEach { category ->
                    val entries = groupedEntries[category].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "sec_${category.name}") {
                            MoreSectionLabel(
                                title = when (category) {
                                    MoreCategory.ADMIN -> ext.adminRoleAdmin
                                    MoreCategory.DAILY_PRACTICE -> if (isHi) "दैनिक साधना व समय" else "Daily Practice & Timing"
                                    MoreCategory.ASTROLOGY_CHARTS -> if (isHi) "ज्योतिष व कुंडली" else "Astrology & Charts"
                                    MoreCategory.APP_PREFERENCES -> if (isHi) "ऐप और प्राथमिकताएँ" else "App & Preferences"
                                }
                            )
                        }
                        items(entries, key = { it.id }) { entry ->
                            MoreDestinationCard(
                                icon = entry.icon,
                                title = entry.title,
                                subtitle = entry.subtitle,
                                tag = entry.tag,
                                accent = entry.accent,
                                onClick = entry.onClick
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showGlossary && policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)) {
        VedicGlossaryBottomSheet(onDismiss = { showGlossary = false })
    }
}

@Composable
private fun MoreSectionLabel(title: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MoreDestinationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
