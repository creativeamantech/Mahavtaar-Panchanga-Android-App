package com.mahavtaar.panchanga

import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.feature.settings.WidgetGlassCustomizationSection
import com.mahavtaar.panchanga.navigation.HomeRoute
import com.mahavtaar.panchanga.navigation.HoraRoute
import com.mahavtaar.panchanga.navigation.PanchangaRoute
import com.mahavtaar.panchanga.navigation.SwaraYogaRoute
import com.mahavtaar.panchanga.navigation.TattvaRoute
import com.mahavtaar.panchanga.navigation.TodayScheduleRoute
import com.mahavtaar.panchanga.ui.theme.MahavtaarPanchangaTheme
import com.mahavtaar.panchanga.widget.CountdownTone
import com.mahavtaar.panchanga.widget.LiveWidgetDataResolver
import com.mahavtaar.panchanga.widget.RunningNowStripGlanceWidget
import com.mahavtaar.panchanga.widget.RunningNowStripGlanceWidgetReceiver
import com.mahavtaar.panchanga.widget.WidgetDeepLinkDestination
import com.mahavtaar.panchanga.widget.WidgetGlassThemeResolver
import com.mahavtaar.panchanga.widget.WidgetUpdateHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GlanceGlassWidgetsTest {

    @Test
    fun glassThemeResolverClampsOpacityEnforcesLegibilityFloorAndOverridesPerStyle() {
        // Clamps below minimum and above maximum
        assertEquals(UserPreferences.MIN_WIDGET_OPACITY, WidgetGlassThemeResolver.computeShellAlpha(0.01f), 0.0001f)
        assertEquals(UserPreferences.MAX_WIDGET_OPACITY, WidgetGlassThemeResolver.computeShellAlpha(1.5f), 0.0001f)

        // Inner tile opacity retains a readable floor (>= 0.24) even at minimum 15% shell opacity
        val minTileAlpha = WidgetGlassThemeResolver.computeTileAlpha(UserPreferences.MIN_WIDGET_OPACITY)
        assertTrue("Expected inner tile alpha >= 0.24f, was $minTileAlpha", minTileAlpha >= 0.24f)

        // AUTO uses system Day/Night resources (no forced ARGB override)
        val autoPalette = WidgetGlassThemeResolver.resolve(WidgetGlassStyle.AUTO, 0.62f)
        assertNull(autoPalette.clockOverrideArgb)
        assertTrue(autoPalette.countdownOverridesArgb.isEmpty())

        // LIGHT & DARK provide distinct high-contrast native clock & countdown overrides
        val lightPalette = WidgetGlassThemeResolver.resolve(WidgetGlassStyle.LIGHT, 0.45f)
        val darkPalette = WidgetGlassThemeResolver.resolve(WidgetGlassStyle.DARK, 0.45f)
        assertNotNull(lightPalette.clockOverrideArgb)
        assertNotNull(darkPalette.clockOverrideArgb)
        assertNotEquals(lightPalette.clockOverrideArgb, darkPalette.clockOverrideArgb)
        assertEquals(CountdownTone.entries.size, lightPalette.countdownOverridesArgb.size)
        assertEquals(CountdownTone.entries.size, darkPalette.countdownOverridesArgb.size)
        assertNotEquals(
            lightPalette.countdownOverridesArgb[CountdownTone.GOLD],
            darkPalette.countdownOverridesArgb[CountdownTone.GOLD]
        )
    }

    @Test
    fun widgetDeepLinkDestinationBuildsIntentsAndRespectsAccessPolicy() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        WidgetDeepLinkDestination.entries.forEach { dest ->
            val intent = WidgetDeepLinkDestination.buildIntent(context, dest)
            val parsed = WidgetDeepLinkDestination.fromKey(
                intent.getStringExtra(WidgetDeepLinkDestination.EXTRA_WIDGET_DESTINATION)
            )
            assertEquals(dest, parsed)
        }
        assertNull(WidgetDeepLinkDestination.fromKey("non_existent_screen"))

        val adminPolicy = AccessPolicy.ADMIN_FULL
        assertEquals(HomeRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.HOME, adminPolicy))
        assertEquals(PanchangaRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.PANCHANGA, adminPolicy))
        assertEquals(HoraRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.HORA, adminPolicy))
        assertEquals(TattvaRoute to false, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.TATTVA, adminPolicy))
        assertEquals(SwaraYogaRoute to false, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.SWARA, adminPolicy))
        assertEquals(TodayScheduleRoute to false, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.SCHEDULE, adminPolicy))

        // Restricted policy without TATTVA/SWARA/SCHEDULE falls back safely to HomeRoute
        val restrictedPolicy = AccessPolicy(
            userId = "u-1",
            email = "member@example.com",
            role = UserRole.USER,
            status = AccessStatus.ACTIVE,
            allowedPages = setOf(AppPageKey.HOME, AppPageKey.DAILY_PANCHANGA),
            allowedFeatures = emptySet()
        )
        assertEquals(PanchangaRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.PANCHANGA, restrictedPolicy))
        assertEquals(HomeRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.TATTVA, restrictedPolicy))
        assertEquals(HomeRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.SWARA, restrictedPolicy))
        assertEquals(HomeRoute to true, resolveWidgetDestinationRoute(WidgetDeepLinkDestination.SCHEDULE, restrictedPolicy))
    }

    @Test
    fun runningNowStripWidgetAndResolverProduceLiveModelSafely() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = LiveWidgetDataResolver.resolve(context)

        assertTrue(model.tithiName.isNotBlank())
        assertTrue(model.nakshatraName.isNotBlank())
        assertNotNull(model.activeHora)
        assertNotNull(model.activeTattva)
        assertEquals(WidgetGlassStyle.AUTO, model.widgetGlassStyle)
        assertEquals(UserPreferences.DEFAULT_WIDGET_OPACITY, model.widgetOpacity, 0.001f)
        assertTrue(model.nextTransitionAtMs > model.nowMs)

        val receiver = RunningNowStripGlanceWidgetReceiver()
        assertTrue(receiver.glanceAppWidget is RunningNowStripGlanceWidget)
        assertFalse(WidgetUpdateHelper.hasAnyPlacedWidgets(context))
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetGlassSettingsUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun widgetGlassCustomizationSectionSwitchesStylesAndDisplaysOpacityReadout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(
            ComponentName(context, ComponentActivity::class.java)
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        var selectedStyle by mutableStateOf(WidgetGlassStyle.AUTO)
        var selectedOpacity by mutableFloatStateOf(0.62f)

        activity.setContent {
            MahavtaarPanchangaTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WidgetGlassCustomizationSection(
                        glassStyle = selectedStyle,
                        opacity = selectedOpacity,
                        isHindi = false,
                        onSelectGlassStyle = { selectedStyle = it },
                        onChangeOpacity = { selectedOpacity = it }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("widget_glass_auto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("widget_glass_light").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("widget_glass_dark").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("62%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("widget_opacity_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("widget_glass_live_preview").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("widget_glass_light").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(WidgetGlassStyle.LIGHT, selectedStyle)

        composeRule.onNodeWithTag("widget_glass_dark").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(WidgetGlassStyle.DARK, selectedStyle)
    }
}
