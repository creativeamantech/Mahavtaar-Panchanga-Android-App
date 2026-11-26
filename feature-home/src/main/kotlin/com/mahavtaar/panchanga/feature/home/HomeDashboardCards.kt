package com.mahavtaar.panchanga.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.minimumInteractiveComponentSize
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.PlanetTransitionEvent
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.CelestialHorizonArcCanvas
import com.mahavtaar.panchanga.ui.components.ChakraPetalAccentBar
import com.mahavtaar.panchanga.ui.components.ColorDot
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.MoonPhaseCanvas
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
internal fun VedicHeroCard(
    snapshot: PanchangaSnapshot,
    nowMs: Long
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    val primaryTithi = resolveActiveAnga(snapshot.tithi, nowMs)
    val sunriseTithi = snapshot.tithi.firstOrNull()
    val remainingMs = primaryTithi?.endTimeMs?.let { (it - nowMs).coerceAtLeast(0L) }
    val ghatiPalaText = VedicGlossaryCatalog.formatGhatiPala(snapshot.sunriseMs, nowMs, lang)
    val localizedMasa = VedicLocalizer.masaName(snapshot.masaNumber, snapshot.isAdhika, snapshot.isKshaya, snapshot.masa, lang)
    val localizedPaksha = VedicLocalizer.pakshaName(snapshot.paksha, lang)
    val localizedTithi = primaryTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: strings.tithiLabel
    val localizedVaara = VedicLocalizer.vaaraName(snapshot.weekday, snapshot.vaara, lang)
    val localizedRtu = VedicLocalizer.rtuName(snapshot.rtu, lang)
    val localizedAyana = VedicLocalizer.ayanaName(snapshot.ayana, lang)
    val localizedSamvatsara = VedicLocalizer.samvatsaraName(snapshot.samvatsara, lang)
    val localizedMode = VedicLocalizer.monthSystemShort(snapshot.monthSystem, lang)
    val localizedPhase = VedicLocalizer.lunarPhaseName(snapshot.lunarPhase.phaseName, lang)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_hero_card"),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.48f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(vedic.heroGradientTop, vedic.heroGradientBottom)))
        ) {
            ChakraPetalAccentBar(height = 4.dp)
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            label = "$localizedMasa · $localizedPaksha ${strings.pakshaSuffix}",
                            textColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localizedTithi,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$localizedVaara · $localizedRtu · $localizedAyana",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MoonPhaseCanvas(
                            illuminationPercent = snapshot.lunarPhase.illuminationPercent,
                            isWaxing = snapshot.lunarPhase.isWaxing,
                            phaseLabel = localizedPhase,
                            diameter = 58.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${snapshot.lunarPhase.illuminationPercent}% ${strings.litSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dina Ghati-Pala & Sunrise Tithi telemetry pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = "${ext.dinaGhatiLabel}: $ghatiPalaText",
                        textColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                    if (sunriseTithi != null) {
                        StatusPill(
                            label = "${ext.tithiAtSunriseLabel}: ${VedicLocalizer.tithiName(sunriseTithi.number, sunriseTithi.name, lang)}",
                            textColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                if (primaryTithi != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventStartEndDateTimeView(
                            startText = VedicLocalizer.formatAngaStart(primaryTithi, snapshot.sunriseMs, snapshot.timezone, lang),
                            endText = VedicLocalizer.formatAngaEnd(primaryTithi, snapshot.nextSunriseMs, snapshot.timezone, lang),
                            accentColor = MaterialTheme.colorScheme.primary,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (remainingMs != null && remainingMs > 0L) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                label = "${VedicLocalizer.formatDurationRemaining(remainingMs, lang)} ${strings.leftSuffix}",
                                textColor = vedic.auspicious,
                                containerColor = vedic.auspiciousContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowIntervalSentence(
                            title = localizedTithi,
                            isCaution = false,
                            endMs = primaryTithi.endTimeMs,
                            nowMs = nowMs,
                            lang = lang
                        ),
                        accentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SamvatChip(strings.vikramaLabel, "${snapshot.vikramaYear}")
                    SamvatChip(strings.sakaLabel, "${snapshot.sakaYear}")
                    SamvatChip(strings.samvatsaraLabel, localizedSamvatsara)
                    SamvatChip(strings.modeLabel, localizedMode)
                }
            }
        }
    }
}

@Composable
internal fun ActiveMuhurtaStatusBanner(
    snapshot: PanchangaSnapshot,
    nowMs: Long,
    onOpenDailyPanchanga: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    fun inWindow(interval: TimingInterval?): Boolean {
        val s = interval?.startTimeMs ?: return false
        val e = interval.endTimeMs ?: return false
        return nowMs in s..e
    }

    val activeInauspicious: Pair<String, TimingInterval>? = when {
        inWindow(snapshot.rahuKala) -> VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang) to snapshot.rahuKala
        inWindow(snapshot.yamaganda) -> VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang) to snapshot.yamaganda
        inWindow(snapshot.gulikaKala) -> VedicLocalizer.muhurtaOrPeriodName("Gulika Kāla", lang) to snapshot.gulikaKala
        snapshot.durmuhurta.any { inWindow(it) } -> {
            val hit = snapshot.durmuhurta.first { inWindow(it) }
            VedicLocalizer.muhurtaOrPeriodName("Durmuhūrta", lang) to hit
        }
        snapshot.varjyam.any { inWindow(it) } -> {
            val hit = snapshot.varjyam.first { inWindow(it) }
            VedicLocalizer.muhurtaOrPeriodName("Varjyam", lang) to hit
        }
        else -> null
    }

    val activeAuspicious: Pair<String, TimingInterval>? = when {
        inWindow(snapshot.abhijitMuhurta) -> VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang) to snapshot.abhijitMuhurta
        inWindow(snapshot.vijayaMuhurta) -> ext.vijayaMuhurtaLabel to snapshot.vijayaMuhurta!!
        inWindow(snapshot.godhuliMuhurta) -> ext.godhuliMuhurtaLabel to snapshot.godhuliMuhurta!!
        inWindow(snapshot.brahmaMuhurta) -> VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang) to snapshot.brahmaMuhurta
        snapshot.amritaKala.any { inWindow(it) } -> {
            val hit = snapshot.amritaKala.first { inWindow(it) }
            VedicLocalizer.muhurtaOrPeriodName("Amṛta Kāla", lang) to hit
        }
        else -> null
    }

    val activeChog = (snapshot.gauriChoghadiyaDay + snapshot.gauriChoghadiyaNight).firstOrNull { inWindow(it) }

    if (activeInauspicious == null && activeAuspicious == null && activeChog == null && !snapshot.hasBhadra) return

    PanchangaCard(
        modifier = Modifier
            .clickable(onClick = onOpenDailyPanchanga)
            .testTag("home_active_status_banner"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val tz = snapshot.timezone
        if (activeInauspicious != null) {
            val (name, iv) = activeInauspicious
            val rem = ((iv.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
            val sStr = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
            val eStr = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
            StatusBannerRow(
                icon = Icons.Rounded.WarningAmber,
                title = "${ext.activeWindowAlert}: $name · ${VedicGlossaryCatalog.formatLiveCountdown(rem, lang)}",
                subtitle = VedicLocalizer.simpleNowIntervalSentence(name, isCaution = true, endMs = iv.endTimeMs, nowMs = nowMs, lang = lang),
                startText = sStr,
                endText = eStr,
                accent = vedic.inauspicious,
                container = vedic.inauspiciousContainer
            )
        } else if (activeAuspicious != null) {
            val (name, iv) = activeAuspicious
            val rem = ((iv.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
            val sStr = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
            val eStr = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
            StatusBannerRow(
                icon = Icons.Rounded.Verified,
                title = "${ext.activeWindowAuspicious}: $name · ${VedicGlossaryCatalog.formatLiveCountdown(rem, lang)}",
                subtitle = VedicLocalizer.simpleNowIntervalSentence(name, isCaution = false, endMs = iv.endTimeMs, nowMs = nowMs, lang = lang),
                startText = sStr,
                endText = eStr,
                accent = vedic.auspicious,
                container = vedic.auspiciousContainer
            )
        }

        if (snapshot.hasBhadra) {
            if (activeInauspicious != null || activeAuspicious != null) Spacer(modifier = Modifier.height(8.dp))
            StatusBannerRow(
                icon = Icons.Rounded.WarningAmber,
                title = ext.bhadraWarningLabel,
                subtitle = if (lang == AppLanguage.HINDI) snapshot.dishaShoolaHi else snapshot.dishaShoolaEn,
                accent = vedic.caution,
                container = vedic.cautionContainer
            )
        }

        if (activeChog != null) {
            if (activeInauspicious != null || activeAuspicious != null || snapshot.hasBhadra) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
            val start = activeChog.startTimeMs ?: nowMs
            val end = activeChog.endTimeMs ?: (nowMs + 1L)
            val span = (end - start).coerceAtLeast(1L)
            val progress = ((nowMs - start).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            val rem = (end - nowMs).coerceAtLeast(0L)
            val chogName = VedicLocalizer.muhurtaOrPeriodName(activeChog.name ?: "", lang)
            val chogAccent = if (activeChog.isAuspicious) vedic.auspicious else vedic.caution
            val sStr = activeChog.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChog.start
            val eStr = activeChog.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChog.end

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${ext.activeChoghadiyaLabel}: $chogName",
                        style = MaterialTheme.typography.labelLarge,
                        color = chogAccent,
                        fontWeight = FontWeight.Bold
                    )
                    EventStartEndDateTimeView(
                        startText = sStr,
                        endText = eStr,
                        accentColor = chogAccent,
                        compact = true
                    )
                }
                StatusPill(
                    label = VedicGlossaryCatalog.formatLiveCountdown(rem, lang),
                    textColor = chogAccent,
                    containerColor = if (activeChog.isAuspicious) vedic.auspiciousContainer else vedic.cautionContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = chogAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBannerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    container: Color,
    startText: String? = null,
    endText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                if (!startText.isNullOrBlank() && !endText.isNullOrBlank()) {
                    EventStartEndDateTimeView(
                        startText = startText,
                        endText = endText,
                        accentColor = accent,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
internal fun NextMajorIngressSpotlightCard(
    transit: PlanetTransitionEvent,
    nowMs: Long,
    onOpenPlanetaryPositions: () -> Unit,
    timezone: String = "Asia/Kolkata"
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    val diffMs = (transit.timestampMs - nowMs).coerceAtLeast(0L)
    val planetDisplay = VedicLocalizer.planetName(transit.planetId, transit.planetName, lang)
    val fromDisplay = VedicLocalizer.transitValue(transit.type, transit.fromValue, transit.fromSanskrit, lang)
    val toDisplay = VedicLocalizer.transitValue(transit.type, transit.toValue, transit.toSanskrit, lang)
    val exactTime = VedicLocalizer.formatDateTimeWithSeconds(transit.timestampMs, timezone, lang)

    PanchangaCard(
        modifier = Modifier
            .clickable(onClick = onOpenPlanetaryPositions)
            .testTag("home_next_ingress_spotlight"),
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    ) {
        SectionHeader(
            title = ext.nextMajorIngressTitle,
            subtitle = "${transit.symbol} $planetDisplay · $exactTime",
            trailing = {
                StatusPill(
                    label = VedicGlossaryCatalog.formatLiveCountdown(diffMs, lang),
                    textColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = transit.specialName?.takeIf { lang == AppLanguage.ENGLISH } ?: "$planetDisplay: $fromDisplay → $toDisplay",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        if (transit.punyaKala != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val pk = transit.punyaKala!!
                val pkStart = pk.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: pk.start
                val pkEnd = pk.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: pk.end
                StatusPill(
                    label = "${ext.punyaKalaLabel}: $pkStart – $pkEnd",
                    textColor = vedic.auspicious,
                    containerColor = vedic.auspiciousContainer
                )
                transit.mahaPunyaKala?.let { mpk ->
                    val mpkStart = mpk.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: mpk.start
                    val mpkEnd = mpk.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: mpk.end
                    StatusPill(
                        label = "${ext.mahaPunyaKalaLabel}: $mpkStart – $mpkEnd",
                        textColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SamvatChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ObservanceHighlightCard(snapshot: PanchangaSnapshot) {
    val obs = snapshot.observances.first()
    val lang = LocalAppLanguage.current
    val policy = LocalAccessPolicy.current
    PanchangaCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = VedicLocalizer.observanceTitle(obs, lang), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(VedicLocalizer.observanceTitle(obs, lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)) {
                    Text("${VedicLocalizer.observanceSubtitle(obs, lang)} · ${VedicLocalizer.observanceDeity(obs, lang)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun SunMoonTelemetryCard(snapshot: PanchangaSnapshot, nowMs: Long) {
    val strings = LocalStrings.current
    val lang = LocalAppLanguage.current
    val vedic = LocalVedicColors.current
    val totalDayMs = (snapshot.sunsetMs - snapshot.sunriseMs).coerceAtLeast(1L)
    val solarProgress = ((nowMs - snapshot.sunriseMs).toFloat() / totalDayMs.toFloat())

    fun fractionRange(interval: TimingInterval?): Pair<Float, Float>? {
        val s = interval?.startTimeMs ?: return null
        val e = interval.endTimeMs ?: return null
        val fs = ((s - snapshot.sunriseMs).toFloat() / totalDayMs.toFloat()).coerceIn(0f, 1f)
        val fe = ((e - snapshot.sunriseMs).toFloat() / totalDayMs.toFloat()).coerceIn(fs, 1f)
        return if (fe > fs) fs to fe else null
    }

    val abhijitRange = fractionRange(snapshot.abhijitMuhurta)
    val rahuRange = fractionRange(snapshot.rahuKala)

    PanchangaCard {
        SectionHeader(strings.sunAndMoonTimings, "${strings.dayLabel} ${snapshot.dayDuration} · ${strings.solarNoonLabel} ${snapshot.solarNoon}")
        Spacer(modifier = Modifier.height(8.dp))
        CelestialHorizonArcCanvas(
            solarProgress = solarProgress,
            abhijitRange = abhijitRange,
            rahuRange = rahuRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .testTag("home_celestial_horizon_arc")
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ColorDot(vedic.auspicious, size = 7.dp)
                Text(
                    text = VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ColorDot(vedic.inauspicious, size = 7.dp)
                Text(
                    text = VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val tz = snapshot.timezone
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.sunrise,
                value = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunriseMs, tz, lang),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.sunset,
                value = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunsetMs, tz, lang),
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.moonrise,
                value = snapshot.moonriseMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) }
                    ?: snapshot.moonrise ?: strings.noRiseToday,
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.moonset,
                value = snapshot.moonsetMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) }
                    ?: snapshot.moonset ?: strings.noSetToday,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun FiveAngasOverviewCard(
    snapshot: PanchangaSnapshot,
    nowMs: Long,
    onSeeAllClick: () -> Unit,
    onInspectAnga: (HomeAngaInspection) -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val activeTithi = resolveActiveAnga(snapshot.tithi, nowMs)
    val activeNak = resolveActiveAnga(snapshot.nakshatra, nowMs)
    val activeYoga = resolveActiveAnga(snapshot.yoga, nowMs)
    val activeKarana = resolveActiveAnga(snapshot.karana, nowMs)

    val localizedPaksha = VedicLocalizer.pakshaName(snapshot.paksha, lang)
    val localizedMasa = VedicLocalizer.masaName(snapshot.masaNumber, snapshot.isAdhika, snapshot.isKshaya, snapshot.masa, lang)

    fun nextAfter(segments: List<AngaSegment>, current: AngaSegment?, resolver: (AngaSegment) -> String): String? {
        if (current == null) return null
        val idx = segments.indexOfFirst { it.number == current.number && it.name == current.name }
        return segments.getOrNull(idx + 1)?.let(resolver)
    }

    val policy = LocalAccessPolicy.current
    PanchangaCard {
        SectionHeader(
            title = strings.fiveLimbsTitle,
            subtitle = VedicLocalizer.coordinateBannerLabel(snapshot.ayanamshaKey, snapshot.coordinateLabel, lang),
            trailing = {
                if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) {
                    TextButton(onClick = onSeeAllClick, modifier = Modifier.testTag("home_open_daily_panchanga")) {
                        Text(strings.details)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        val tithiName = activeTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: "—"
        AngaQuickRow(
            label = strings.tithiLabel,
            displayName = tithiName,
            segment = activeTithi,
            sunriseMs = snapshot.sunriseMs,
            nextSunriseMs = snapshot.nextSunriseMs,
            timezone = snapshot.timezone,
            nowMs = nowMs,
            onClick = activeTithi?.let { seg ->
                {
                    onInspectAnga(
                        HomeAngaInspection(
                            angaTitle = strings.anga1TithiTitle,
                            angaSubtitle = "$localizedPaksha ${strings.pakshaSuffix} · $localizedMasa",
                            displayName = tithiName,
                            segment = seg,
                            nextDisplayName = nextAfter(snapshot.tithi, seg) { VedicLocalizer.tithiName(it.number, it.name, lang) },
                            timezone = snapshot.timezone,
                            glossaryTerm = "Tithi"
                        )
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
        val nakName = activeNak?.let { VedicLocalizer.nakshatraName(it.number, it.name, lang) } ?: "—"
        AngaQuickRow(
            label = strings.nakshatraLabel,
            displayName = nakName,
            segment = activeNak,
            sunriseMs = snapshot.sunriseMs,
            nextSunriseMs = snapshot.nextSunriseMs,
            timezone = snapshot.timezone,
            nowMs = nowMs,
            onClick = activeNak?.let { seg ->
                {
                    onInspectAnga(
                        HomeAngaInspection(
                            angaTitle = strings.anga2NakshatraTitle,
                            angaSubtitle = "${strings.moonRasiPrefix}: ${VedicLocalizer.rasiByName(snapshot.moonRasi, lang)}",
                            displayName = nakName,
                            segment = seg,
                            nextDisplayName = nextAfter(snapshot.nakshatra, seg) { VedicLocalizer.nakshatraName(it.number, it.name, lang) },
                            timezone = snapshot.timezone,
                            glossaryTerm = "Nak"
                        )
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
        val yogaName = activeYoga?.let { VedicLocalizer.yogaName(it.number, it.name, lang) } ?: "—"
        AngaQuickRow(
            label = strings.yogaLabel,
            displayName = yogaName,
            segment = activeYoga,
            sunriseMs = snapshot.sunriseMs,
            nextSunriseMs = snapshot.nextSunriseMs,
            timezone = snapshot.timezone,
            nowMs = nowMs,
            onClick = activeYoga?.let { seg ->
                {
                    onInspectAnga(
                        HomeAngaInspection(
                            angaTitle = strings.anga3YogaTitle,
                            angaSubtitle = VedicLocalizer.coordinateBannerLabel(snapshot.ayanamshaKey, snapshot.coordinateLabel, lang),
                            displayName = yogaName,
                            segment = seg,
                            nextDisplayName = nextAfter(snapshot.yoga, seg) { VedicLocalizer.yogaName(it.number, it.name, lang) },
                            timezone = snapshot.timezone,
                            glossaryTerm = "Yoga"
                        )
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
        val karanaName = activeKarana?.let { VedicLocalizer.karanaName(it.name, lang) } ?: "—"
        AngaQuickRow(
            label = strings.karanaLabel,
            displayName = karanaName,
            segment = activeKarana,
            sunriseMs = snapshot.sunriseMs,
            nextSunriseMs = snapshot.nextSunriseMs,
            timezone = snapshot.timezone,
            nowMs = nowMs,
            onClick = activeKarana?.let { seg ->
                {
                    onInspectAnga(
                        HomeAngaInspection(
                            angaTitle = strings.anga4KaranaTitle,
                            angaSubtitle = strings.anga4KaranaSubtitle,
                            displayName = karanaName,
                            segment = seg,
                            nextDisplayName = nextAfter(snapshot.karana, seg) { VedicLocalizer.karanaName(it.name, lang) },
                            timezone = snapshot.timezone,
                            glossaryTerm = "Kara"
                        )
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.varaLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(VedicLocalizer.vaaraName(snapshot.weekday, snapshot.vaara, lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AngaQuickRow(
    label: String,
    displayName: String,
    segment: AngaSegment?,
    sunriseMs: Long,
    nextSunriseMs: Long,
    timezone: String,
    nowMs: Long,
    onClick: (() -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val rem = segment?.endTimeMs?.let { (it - nowMs).takeIf { d -> d > 0L } }
    val startMs = segment?.startTimeMs
    val endMs = segment?.endTimeMs
    val progress = if (startMs != null && endMs != null && endMs > startMs && nowMs in startMs..endMs) {
        ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (segment != null) {
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatAngaStart(segment, sunriseMs, timezone, lang),
                        endText = VedicLocalizer.formatAngaEnd(segment, nextSunriseMs, timezone, lang),
                        accentColor = MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    segment.swaraInfo?.let { swara ->
                        SwaraNadiBadge(nadi = swara.startNadi)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (rem != null) {
                    Text(
                        "${VedicLocalizer.formatDurationRemaining(rem, lang)} ${strings.leftSuffix}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = displayName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (progress != null) {
            Spacer(modifier = Modifier.height(5.dp))
            AuspiciousnessMeterBar(
                progress = progress,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun LiveHoraAndSwaraRow(
    dailyHoras: DailyHoras,
    liveSwara: SwaraYogaState,
    nowMs: Long,
    onOpenHora: () -> Unit,
    onOpenSwaraYoga: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val policy = LocalAccessPolicy.current
    val activeHora = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull()
    val horaRemaining = activeHora?.let { (it.endTimeMs - nowMs).coerceAtLeast(0L) }
    val showTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)
    val canOpenHora = policy.canAccessPage(AppPageKey.HORA)
    val canOpenSwara = policy.canAccessPage(AppPageKey.SWARA_YOGA)
    val activeTattva = dailyHoras.activeTattva ?: activeHora?.tattvas?.firstOrNull { nowMs in it.startTimeMs..it.endTimeMs }
    val tz = dailyHoras.timezone

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .then(if (canOpenHora) Modifier.clickable(onClick = onOpenHora) else Modifier)
                .testTag("home_hora_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, contentDescription = strings.currentHora, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.currentHora, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(activeHora?.let { "${VedicLocalizer.horaRulerName(it.ruler, lang)} ${strings.horaSuffix}" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (activeHora != null) {
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.startTimeMs, tz, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.endTimeMs, tz, lang),
                        accentColor = MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    SwaraNadiBadge(nadi = activeHora.nadi)
                }
                if (activeTattva != null) {
                    TattvaBadge(element = activeTattva.element)
                }
                if (horaRemaining != null && horaRemaining > 0L) {
                    Text("${VedicLocalizer.formatDurationRemaining(horaRemaining, lang)} ${strings.leftSuffix}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (canOpenSwara || showTattvaNadi) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .then(if (canOpenSwara) Modifier.clickable(onClick = onOpenSwaraYoga) else Modifier)
                    .testTag("home_swara_card"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Air, contentDescription = strings.swaraYogaTitle, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.swaraYogaTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SwaraNadiBadge(nadi = liveSwara.currentActiveSwara)
                    activeTattva?.let { TattvaBadge(element = it.element) }
                    Text("${strings.sunrise}: ${VedicLocalizer.nadiShortLabel(liveSwara.sunriseSwara, lang)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${strings.sunset}: ${VedicLocalizer.nadiShortLabel(liveSwara.sunsetSwara, lang)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun QuickMuhurtaSummaryCard(
    snapshot: PanchangaSnapshot,
    onOpenDailyPanchanga: () -> Unit,
    onInspectTiming: (String, TimingInterval) -> Unit = { _, _ -> }
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val policy = LocalAccessPolicy.current
    val tz = snapshot.timezone
    val brahmaLabel = VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang)
    val abhijitLabel = VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang)
    val rahuLabel = VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang)
    val yamaLabel = VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang)

    fun formatStart(iv: TimingInterval) = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
    fun formatEnd(iv: TimingInterval) = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end

    PanchangaCard {
        SectionHeader(strings.keyTimingsToday, strings.keyTimingsSubtitle, trailing = {
            if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) {
                TextButton(onClick = onOpenDailyPanchanga) {
                    Text(strings.all)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        })
        Spacer(modifier = Modifier.height(6.dp))
        TimingSummaryRow(brahmaLabel, formatStart(snapshot.brahmaMuhurta), formatEnd(snapshot.brahmaMuhurta), vedic.auspicious) {
            onInspectTiming(brahmaLabel, snapshot.brahmaMuhurta)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TimingSummaryRow(abhijitLabel, formatStart(snapshot.abhijitMuhurta), formatEnd(snapshot.abhijitMuhurta), vedic.auspicious) {
            onInspectTiming(abhijitLabel, snapshot.abhijitMuhurta)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TimingSummaryRow(rahuLabel, formatStart(snapshot.rahuKala), formatEnd(snapshot.rahuKala), vedic.inauspicious) {
            onInspectTiming(rahuLabel, snapshot.rahuKala)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TimingSummaryRow(yamaLabel, formatStart(snapshot.yamaganda), formatEnd(snapshot.yamaganda), vedic.caution) {
            onInspectTiming(yamaLabel, snapshot.yamaganda)
        }
    }
}

@Composable
private fun TimingSummaryRow(
    label: String,
    startText: String,
    endText: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = accent, fontWeight = FontWeight.Bold)
            EventStartEndDateTimeView(startText = startText, endText = endText, accentColor = accent, compact = true)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun UpcomingTransitRowCard(
    transit: PlanetTransitionEvent,
    timezone: String = "Asia/Kolkata"
) {
    val lang = LocalAppLanguage.current
    val planetDisplay = VedicLocalizer.planetName(transit.planetId, transit.planetName, lang)
    val fromDisplay = VedicLocalizer.transitValue(transit.type, transit.fromValue, transit.fromSanskrit, lang)
    val toDisplay = VedicLocalizer.transitValue(transit.type, transit.toValue, transit.toSanskrit, lang)
    val headline = if (lang == AppLanguage.HINDI) "$planetDisplay → $toDisplay" else transit.specialName ?: "${transit.planetName} → ${transit.toValue}"
    val exactTime = VedicLocalizer.formatDateTimeWithSeconds(transit.timestampMs, timezone, lang)

    PanchangaCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(transit.symbol, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("$fromDisplay → $toDisplay · $exactTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(VedicLocalizer.transitRelativeText(transit.relativeText, lang), MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}

private data class QuickModuleItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VedicModulesQuickLauncher(
    pinnedModuleKeys: List<String>,
    onUpdatePinnedModules: (List<String>) -> Unit,
    onOpenTodaySchedule: () -> Unit,
    onOpenKundli: () -> Unit,
    onOpenGunaMilan: () -> Unit,
    onOpenNavtara: () -> Unit,
    onOpenTattva: () -> Unit,
    onOpenNavagrahaMantras: () -> Unit,
    onOpenAnnualFestivals: () -> Unit,
    onOpenSwaraYoga: () -> Unit,
    onOpenPlanetaryPositions: () -> Unit,
    onOpenNotificationStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI

    val modules = buildList {
        if (policy.isAdmin) {
            add(QuickModuleItem("admin_panel", ext.adminPanelTitle, ext.adminTabUsers, Icons.Rounded.AdminPanelSettings, vedic.petalPlum, "home_mod_admin_panel", onOpenAdminPanel))
        }
        if (policy.canAccessPage(AppPageKey.TODAY_SCHEDULE)) {
            add(QuickModuleItem("schedule", strings.moreScheduleTitle, strings.filterMuhurta, Icons.Rounded.Timeline, vedic.petalSaffron, "home_mod_schedule", onOpenTodaySchedule))
        }
        if (policy.canAccessPage(AppPageKey.SWARA_YOGA)) {
            add(QuickModuleItem("swara", strings.swaraYogaTitle, strings.moreSwaraTitle, Icons.Rounded.Air, vedic.petalAzure, "home_mod_swara", onOpenSwaraYoga))
        }
        if (policy.canAccessPage(AppPageKey.TATTVA)) {
            add(QuickModuleItem("tattva", strings.moreTattvaTitle, strings.dominantTattvaLabel, Icons.Rounded.Spa, vedic.petalEmerald, "home_mod_tattva", onOpenTattva))
        }
        if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
            add(QuickModuleItem("notif_studio", strings.moreNotificationStudioTitle, strings.notifTabVoiceQuiet, Icons.Rounded.NotificationsActive, vedic.petalPlum, "home_mod_notif_studio", onOpenNotificationStudio))
        }
        if (policy.canAccessPage(AppPageKey.KUNDLI)) {
            add(QuickModuleItem("kundli", strings.moreKundliTitle, strings.tabVargas, Icons.Rounded.GridView, vedic.petalPlum, "home_mod_kundli", onOpenKundli))
        }
        if (policy.canAccessPage(AppPageKey.GUNA_MILAN)) {
            add(QuickModuleItem("guna", strings.moreGunaMilanTitle, strings.totalScoreLabel, Icons.Rounded.Favorite, vedic.petalPlum, "home_mod_guna", onOpenGunaMilan))
        }
        if (policy.canAccessPage(AppPageKey.NAVTATA)) {
            add(QuickModuleItem("navtara", strings.moreNavtaraTitle, strings.taraScoreLabel, Icons.Rounded.Stars, vedic.petalAzure, "home_mod_navtara", onOpenNavtara))
        }
        if (policy.canAccessPage(AppPageKey.NAVAGRAHA_MANTRAS)) {
            add(QuickModuleItem("mantras", strings.moreNavagrahaMantrasTitle, strings.beejMantraLabel, Icons.Rounded.SelfImprovement, vedic.petalSaffron, "home_mod_mantras", onOpenNavagrahaMantras))
        }
        if (policy.canAccessPage(AppPageKey.ANNUAL_FESTIVALS)) {
            add(QuickModuleItem("festivals", strings.moreFestivalsTitle, strings.festivalsAndVratsTitle, Icons.Rounded.Celebration, vedic.petalSaffron, "home_mod_festivals", onOpenAnnualFestivals))
        }
        if (policy.canAccessPage(AppPageKey.PLANETARY_POSITIONS)) {
            add(QuickModuleItem("planets", strings.navagrahaShort, strings.morePlanetsTitle, Icons.Rounded.Public, vedic.petalEmerald, "home_mod_planets", onOpenPlanetaryPositions))
        }
        if (policy.canAccessPage(AppPageKey.SETTINGS)) {
            add(QuickModuleItem("settings", strings.settingsTitle, strings.languageSectionTitle, Icons.Rounded.Tune, vedic.petalAzure, "home_mod_settings", onOpenSettings))
        }
    }

    if (modules.isEmpty()) return

    var showAllTools by remember { mutableStateOf(false) }
    var showPinEditor by remember { mutableStateOf(false) }

    val byKey = remember(modules) { modules.associateBy { it.key } }
    val pinned = remember(modules, pinnedModuleKeys) {
        val resolved = pinnedModuleKeys.mapNotNull { byKey[it] }.distinctBy { it.key }.take(4)
        if (resolved.isNotEmpty()) resolved else modules.take(4)
    }
    val pinnedKeysSet = remember(pinned) { pinned.map { it.key }.toSet() }
    val otherModules = remember(modules, pinnedKeysSet) { modules.filterNot { it.key in pinnedKeysSet } }

    PanchangaCard {
        SectionHeader(
            title = if (isHi) "पिन किए गए शॉर्टकट" else "Pinned Shortcuts",
            subtitle = strings.vedicModulesSubtitle,
            trailing = {
                if (modules.size > 4) {
                    TextButton(
                        onClick = { showPinEditor = true },
                        modifier = Modifier.testTag("home_edit_pins_button")
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHi) "बदलें" else "Edit")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        QuickModuleGrid(items = pinned)

        if (otherModules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAllTools = !showAllTools }
                    .minimumInteractiveComponentSize()
                    .padding(top = 6.dp)
                    .testTag("home_toggle_all_tools_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) {
                        if (showAllTools) "अन्य उपकरण छिपाएं" else "सभी अन्य उपकरण (${otherModules.size})"
                    } else {
                        if (showAllTools) "Hide Other Tools" else "All Tools (${otherModules.size} more)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (showAllTools) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = showAllTools) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    QuickModuleGrid(items = otherModules)
                }
            }
        }
    }

    if (showPinEditor) {
        var draftKeys by remember(pinnedModuleKeys) {
            mutableStateOf(pinned.map { it.key })
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPinEditor = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isHi) "होम पर 4 शॉर्टकट पिन करें (${draftKeys.size}/4)" else "Pin up to 4 Home Shortcuts (${draftKeys.size}/4)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHi) "अपने सबसे अधिक उपयोग किए जाने वाले उपकरण चुनें।" else "Choose your most-used tools for instant access on Home.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    modules.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { mod ->
                                val isPinned = mod.key in draftKeys
                                Surface(
                                    onClick = {
                                        draftKeys = if (isPinned) {
                                            if (draftKeys.size > 1) draftKeys - mod.key else draftKeys
                                        } else if (draftKeys.size < 4) {
                                            draftKeys + mod.key
                                        } else {
                                            draftKeys.drop(1) + mod.key
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("pin_picker_${mod.key}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Rounded.CheckCircle else mod.icon,
                                            contentDescription = null,
                                            tint = if (isPinned) MaterialTheme.colorScheme.primary else mod.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = mod.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isPinned) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Button(
                    onClick = {
                        onUpdatePinnedModules(draftKeys)
                        showPinEditor = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp)
                        .testTag("pin_picker_save_button")
                ) {
                    Text(if (isHi) "शॉर्टकट सहेजें" else "Save Pinned Shortcuts")
                }
            }
        }
    }
}

@Composable
private fun QuickModuleGrid(items: List<QuickModuleItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { mod ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = mod.onClick)
                            .minimumInteractiveComponentSize()
                            .testTag(mod.tag),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(mod.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(mod.icon, contentDescription = mod.title, tint = mod.accent, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mod.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mod.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun resolveActiveAnga(segments: List<AngaSegment>, nowMs: Long): AngaSegment? {
    if (segments.isEmpty()) return null
    return segments.firstOrNull { seg ->
        val s = seg.startTimeMs ?: Long.MIN_VALUE
        val e = seg.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    } ?: segments.first()
}
