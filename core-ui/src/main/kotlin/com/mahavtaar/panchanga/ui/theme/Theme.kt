package com.mahavtaar.panchanga.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppThemeMode
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.extendedStringsFor
import com.mahavtaar.panchanga.ui.localization.stringsFor

@Immutable
data class ExtendedVedicColors(
    val auspicious: Color,
    val auspiciousContainer: Color,
    val inauspicious: Color,
    val inauspiciousContainer: Color,
    val caution: Color,
    val cautionContainer: Color,
    val idaNadi: Color,
    val pingalaNadi: Color,
    val sushumnaNadi: Color,
    val heroGradientTop: Color,
    val heroGradientBottom: Color,
    val petalPlum: Color = PetalPlumLight,
    val petalAzure: Color = PetalAzureLight,
    val petalEmerald: Color = PetalEmeraldLight,
    val petalSaffron: Color = PetalSaffronLight
)

val LocalVedicColors = staticCompositionLocalOf {
    ExtendedVedicColors(
        auspicious = AuspiciousTealLight,
        auspiciousContainer = Color(0xFFDDF6E5),
        inauspicious = InauspiciousRoseLight,
        inauspiciousContainer = Color(0xFFFCE3E6),
        caution = CautionAmberLight,
        cautionContainer = Color(0xFFFFF0D9),
        idaNadi = IdaCyanLight,
        pingalaNadi = PingalaFlameLight,
        sushumnaNadi = SushumnaVioletLight,
        heroGradientTop = Color(0xFFFFF7E6),
        heroGradientBottom = Color(0xFFFAF0FA),
        petalPlum = PetalPlumLight,
        petalAzure = PetalAzureLight,
        petalEmerald = PetalEmeraldLight,
        petalSaffron = PetalSaffronLight
    )
}

private val RoyalNightColorScheme = darkColorScheme(
    primary = RoyalNightGold,
    onPrimary = Color(0xFF241600),
    primaryContainer = RoyalNightGoldContainer,
    onPrimaryContainer = Color(0xFFFFE7AC),
    secondary = RoyalNightPlumOrchid,
    onSecondary = Color(0xFF320038),
    secondaryContainer = RoyalNightPlumContainer,
    onSecondaryContainer = Color(0xFFFCD7FF),
    tertiary = RoyalNightAzure,
    onTertiary = Color(0xFF00273D),
    tertiaryContainer = RoyalNightAzureContainer,
    onTertiaryContainer = Color(0xFFD1EEFF),
    background = RoyalNightBg,
    onBackground = RoyalNightTextPrimary,
    surface = RoyalNightSurface,
    onSurface = RoyalNightTextPrimary,
    surfaceVariant = RoyalNightSurfaceElevated,
    onSurfaceVariant = RoyalNightTextSecondary,
    outline = RoyalNightOutline,
    outlineVariant = RoyalNightOutlineVariant,
    error = InauspiciousRoseDark,
    onError = Color(0xFF36000A)
)

private val IvoryGoldColorScheme = lightColorScheme(
    primary = IvoryPrimaryPlum,
    onPrimary = Color.White,
    primaryContainer = IvoryPrimaryContainer,
    onPrimaryContainer = Color(0xFF3D003E),
    secondary = IvorySecondaryGold,
    onSecondary = Color.White,
    secondaryContainer = IvorySecondaryContainer,
    onSecondaryContainer = Color(0xFF4A2B00),
    tertiary = IvoryTertiaryAzure,
    onTertiary = Color.White,
    tertiaryContainer = IvoryTertiaryContainer,
    onTertiaryContainer = Color(0xFF002847),
    background = IvoryBg,
    onBackground = IvoryTextPrimary,
    surface = IvorySurface,
    onSurface = IvoryTextPrimary,
    surfaceVariant = IvorySurfaceElevated,
    onSurfaceVariant = IvoryTextSecondary,
    outline = IvoryOutline,
    outlineVariant = IvoryOutlineVariant,
    error = InauspiciousRoseLight,
    onError = Color.White
)

private val RoyalNightExtended = ExtendedVedicColors(
    auspicious = AuspiciousTealDark,
    auspiciousContainer = Color(0xFF103724),
    inauspicious = InauspiciousRoseDark,
    inauspiciousContainer = Color(0xFF3E131D),
    caution = CautionAmberDark,
    cautionContainer = Color(0xFF3D270C),
    idaNadi = IdaCyanDark,
    pingalaNadi = PingalaFlameDark,
    sushumnaNadi = SushumnaVioletDark,
    heroGradientTop = Color(0xFF28133D),
    heroGradientBottom = Color(0xFF160C24),
    petalPlum = PetalPlumDark,
    petalAzure = PetalAzureDark,
    petalEmerald = PetalEmeraldDark,
    petalSaffron = PetalSaffronDark
)

private val IvoryGoldExtended = ExtendedVedicColors(
    auspicious = AuspiciousTealLight,
    auspiciousContainer = Color(0xFFDDF6E5),
    inauspicious = InauspiciousRoseLight,
    inauspiciousContainer = Color(0xFFFCE3E6),
    caution = CautionAmberLight,
    cautionContainer = Color(0xFFFFF0D9),
    idaNadi = IdaCyanLight,
    pingalaNadi = PingalaFlameLight,
    sushumnaNadi = SushumnaVioletLight,
    heroGradientTop = Color(0xFFFFF7E6),
    heroGradientBottom = Color(0xFFFAF0FA),
    petalPlum = PetalPlumLight,
    petalAzure = PetalAzureLight,
    petalEmerald = PetalEmeraldLight,
    petalSaffron = PetalSaffronLight
)

val PanchangaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MahavtaarPanchangaTheme(
    themeMode: AppThemeMode = AppThemeMode.PARCHMENT,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.NIGHT_SKY -> true
        AppThemeMode.PARCHMENT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) RoyalNightColorScheme else IvoryGoldColorScheme
    val vedicColors = if (isDark) RoyalNightExtended else IvoryGoldExtended
    val typography = if (language == AppLanguage.HINDI) HindiPanchangaTypography else PanchangaTypography
    val strings = stringsFor(language)
    val extStrings = extendedStringsFor(language)

    CompositionLocalProvider(
        LocalVedicColors provides vedicColors,
        LocalAppLanguage provides language,
        LocalStrings provides strings,
        LocalExtendedStrings provides extStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = PanchangaShapes,
            content = content
        )
    }
}
