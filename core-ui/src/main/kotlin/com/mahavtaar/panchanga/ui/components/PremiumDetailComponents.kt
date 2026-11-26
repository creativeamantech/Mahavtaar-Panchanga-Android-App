package com.mahavtaar.panchanga.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@Composable
fun AtmosphericHeroCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
    showCrownBar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val vedic = LocalVedicColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.2.dp, accentBorder),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(vedic.heroGradientTop, vedic.heroGradientBottom)
                    )
                )
        ) {
            if (showCrownBar) {
                ChakraPetalAccentBar(height = 3.5.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun ExpandableInfoCard(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    leadingBadge: (@Composable () -> Unit)? = null,
    trailingBadge: (@Composable () -> Unit)? = null,
    summaryContent: (@Composable ColumnScope.() -> Unit)? = null,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "expand_chevron"
    )
    val borderStroke = BorderStroke(
        width = if (expanded) 1.25.dp else 1.dp,
        color = if (expanded) accentColor.copy(alpha = 0.50f) else MaterialTheme.colorScheme.outlineVariant
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderStroke
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingBadge != null) {
                    leadingBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailingBadge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingBadge()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            if (summaryContent != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = if (expanded) 10.dp else 14.dp),
                    content = summaryContent
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(180)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    expandedContent()
                }
            }
        }
    }
}

@Composable
fun TelemetryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AuspiciousnessMeterBar(
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingValue: String? = null
) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        target = progress.coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 280),
        label = "meter_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank() || !trailingValue.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!trailingValue.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trailingValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceAtLeast(0.02f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.72f), accentColor)
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorBottomSheetFrame(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Rounded.AutoAwesome,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    statusLabel: String? = null,
    sheetTestTag: String = "inspector_bottom_sheet",
    onDismiss: () -> Unit,
    heroVisual: (@Composable () -> Unit)? = null,
    footerActions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalExtendedStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(sheetTestTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
        ) {
            ChakraPetalAccentBar(height = 3.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!statusLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusPill(
                            label = statusLabel,
                            textColor = accentColor,
                            containerColor = accentColor.copy(alpha = 0.15f)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("inspector_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = ext.closeLabel
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (heroVisual != null) {
                    heroVisual()
                }
                content()
                if (footerActions != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    footerActions()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TimingWindowInspectorSheet(
    displayTitle: String,
    interval: TimingInterval,
    nowMs: Long,
    onDismiss: () -> Unit,
    onOpenGlossary: ((String) -> Unit)? = null,
    timezone: String = "Asia/Kolkata"
) {
    val lang = LocalAppLanguage.current
    val vedic = LocalVedicColors.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val showGlossary = policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)
    val showDescriptions = policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)

    val rawName = (interval.name ?: displayTitle).lowercase()
    val isInauspicious = !interval.isAuspicious && (
        rawName.contains("rahu") || rawName.contains("rāhu") || rawName.contains("राहु") ||
            rawName.contains("yama") || rawName.contains("यम") ||
            rawName.contains("gulika") || rawName.contains("गुलिक") ||
            rawName.contains("durmu") || rawName.contains("दुर्मु") ||
            rawName.contains("varj") || rawName.contains("वर्ज") ||
            rawName.contains("udvega") || rawName.contains("उद्वेग") ||
            rawName.contains("roga") || rawName.contains("रोग") ||
            rawName.contains("kaal") || rawName.contains("kāla") || rawName.contains("काल")
        )
    val isAuspicious = interval.isAuspicious || !isInauspicious
    val accent = if (isAuspicious) vedic.auspicious else vedic.inauspicious
    val container = if (isAuspicious) vedic.auspiciousContainer else vedic.inauspiciousContainer

    val startMs = interval.startTimeMs
    val endMs = interval.endTimeMs
    val durationMs = if (startMs != null && endMs != null && endMs > startMs) endMs - startMs else null
    val isActiveNow = startMs != null && endMs != null && nowMs in startMs..endMs
    val progress = if (isActiveNow && startMs != null && endMs != null && endMs > startMs) {
        ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val statusText = when {
        isActiveNow && endMs != null -> {
            val rem = VedicGlossaryCatalog.formatLiveCountdown((endMs - nowMs).coerceAtLeast(0L), lang)
            if (lang == AppLanguage.HINDI) "अभी सक्रिय · $rem शेष" else "Active Now · $rem left"
        }
        startMs != null && nowMs < startMs -> {
            val until = VedicGlossaryCatalog.formatLiveCountdown((startMs - nowMs).coerceAtLeast(0L), lang)
            if (lang == AppLanguage.HINDI) "$until में आरम्भ" else "Starts in $until"
        }
        else -> if (isAuspicious) ext.auspiciousLegend else ext.inauspiciousLegend
    }

    val glossaryQuery = when {
        rawName.contains("rahu") || rawName.contains("rāhu") || rawName.contains("राहु") -> "rahu_kala"
        rawName.contains("yama") || rawName.contains("gulika") || rawName.contains("यम") || rawName.contains("गुलिक") -> "yamaganda"
        rawName.contains("abhijit") || rawName.contains("अभिजित") -> "abhijit"
        rawName.contains("brahma") || rawName.contains("amrit") || rawName.contains("amṛta") || rawName.contains("ब्रह्म") || rawName.contains("अमृत") -> "amrit_kalam"
        else -> "choghadiya"
    }

    val guidance = if (lang == AppLanguage.HINDI) {
        if (isAuspicious) {
            "यह शुभ मुहूर्त माङ्गलिक आरम्भ, संकल्प, पूजा, अध्ययन तथा सकारात्मक निर्णयों के लिए उत्तम माना जाता है।"
        } else {
            "इस कालखण्ड में नवीन शुभ आरम्भ व महत्वपूर्ण अनुबन्ध टालना श्रेष्ठ है; पूर्व-आरम्भ कार्य तथा जप-ध्यान जारी रख सकते हैं।"
        }
    } else {
        if (isAuspicious) {
            "Favorable Vedic window suited for auspicious starts, sacred vows, spiritual practice, study, and constructive agreements."
        } else {
            "Traditionally avoided for launching fresh ventures or ceremonies; ongoing routine duties, meditation, and quiet review remain fine."
        }
    }

    val startDisplay = startMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.start
    val endDisplay = endMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: interval.end

    InspectorBottomSheetFrame(
        title = displayTitle,
        subtitle = "$startDisplay – $endDisplay",
        icon = if (isAuspicious) Icons.Rounded.Verified else Icons.Rounded.WarningAmber,
        accentColor = accent,
        statusLabel = statusText,
        sheetTestTag = "timing_inspector_sheet",
        onDismiss = onDismiss,
        footerActions = if (onOpenGlossary != null && showGlossary) {
            {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenGlossary(glossaryQuery)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("inspector_open_glossary_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.HINDI) "वैदिक शब्दकोश में पढ़ें" else "Read in Vedic Glossary")
                }
            }
        } else null
    ) {
        if (isActiveNow) {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowIntervalSentence(
                    title = displayTitle,
                    isCaution = !isAuspicious,
                    endMs = endMs,
                    nowMs = nowMs,
                    lang = lang
                ),
                accentColor = accent,
                containerColor = container
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "आरम्भ" else "Starts",
                value = startDisplay,
                valueColor = accent,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "समाप्ति" else "Ends",
                value = endDisplay,
                valueColor = accent,
                modifier = Modifier.weight(1f)
            )
        }
        if (durationMs != null) {
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "कुल अवधि" else "Duration",
                value = VedicLocalizer.formatDurationRemaining(durationMs, lang),
                caption = if (isActiveNow) {
                    if (lang == AppLanguage.HINDI) "वर्तमान में प्रगति पर" else "Currently in progress"
                } else null,
                valueColor = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isActiveNow) {
            AuspiciousnessMeterBar(
                progress = progress,
                accentColor = accent,
                label = if (lang == AppLanguage.HINDI) "कालखण्ड प्रगति" else "Window Progress",
                trailingValue = "${(progress * 100).toInt()}%"
            )
        }
        if (showDescriptions || showGlossary) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = container.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "शास्त्रीय मार्गदर्शन" else "Classical Guidance",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = guidance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AngaInspectorSheet(
    angaTitle: String,
    angaSubtitle: String,
    displayName: String,
    segment: AngaSegment,
    nextDisplayName: String?,
    nowMs: Long,
    timezone: String,
    glossaryTerm: String,
    onDismiss: () -> Unit,
    onOpenGlossary: ((String) -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val showGlossary = policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)

    val glossaryEntry = remember(glossaryTerm) {
        val q = glossaryTerm.trim().lowercase()
        VedicGlossaryCatalog.entries.firstOrNull { entry ->
            entry.id.lowercase().contains(q) || entry.termEn.lowercase().contains(q)
        }
    }

    val startMs = segment.startTimeMs
    val endMs = segment.endTimeMs
    val progress = if (startMs != null && endMs != null && endMs > startMs) {
        ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
    } else null
    val remMs = endMs?.let { (it - nowMs).takeIf { d -> d > 0L } }

    InspectorBottomSheetFrame(
        title = "$displayName (#${segment.number})",
        subtitle = "$angaTitle · $angaSubtitle",
        icon = Icons.Rounded.AutoAwesome,
        accentColor = MaterialTheme.colorScheme.primary,
        statusLabel = remMs?.let { "${VedicLocalizer.formatDurationRemaining(it, lang)} ${strings.leftSuffix}" } ?: angaTitle,
        sheetTestTag = "anga_inspector_sheet",
        onDismiss = onDismiss,
        footerActions = if (onOpenGlossary != null && showGlossary) {
            {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenGlossary(glossaryTerm)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("inspector_open_glossary_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.HINDI) "वैदिक शब्दकोश में पढ़ें" else "Read in Vedic Glossary")
                }
            }
        } else null
    ) {
        val isAngaActiveNow = (startMs == null || nowMs >= startMs) && (endMs == null || nowMs <= endMs)
        if (isAngaActiveNow) {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowIntervalSentence(
                    title = "$angaTitle: $displayName",
                    isCaution = false,
                    endMs = endMs,
                    nowMs = nowMs,
                    lang = lang
                ),
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
        if (progress != null) {
            AuspiciousnessMeterBar(
                progress = progress,
                accentColor = MaterialTheme.colorScheme.primary,
                label = if (lang == AppLanguage.HINDI) "वर्तमान अंग प्रगति" else "Elapsed Limb Progress",
                trailingValue = "${(progress * 100).toInt()}%"
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryStatTile(
                label = strings.startsLabel,
                value = startMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                    ?: segment.starts
                    ?: strings.beforeSunrise,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.endsLabel,
                value = endMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) }
                    ?: segment.ends
                    ?: strings.continuesPastNextSunrise,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        if (!nextDisplayName.isNullOrBlank()) {
            TelemetryStatTile(
                label = strings.nextTransitionPrefix,
                value = nextDisplayName,
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }
        segment.swaraInfo?.let { swara ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "स्वर नाड़ी संधि संकेत" else "Swara Boundary Polarity",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "आरम्भ संधि" else "Start Sandhi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SwaraNadiBadge(nadi = swara.startNadi)
                            swara.startEvent?.let { ev ->
                                EventStartEndDateTimeView(
                                    startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, timezone, lang),
                                    endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, timezone, lang),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    compact = true
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "समाप्ति संधि" else "End Sandhi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SwaraNadiBadge(nadi = swara.endNadi)
                            swara.endEvent?.let { ev ->
                                EventStartEndDateTimeView(
                                    startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, timezone, lang),
                                    endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, timezone, lang),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showGlossary && glossaryEntry != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) glossaryEntry.termHi else glossaryEntry.termEn,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (lang == AppLanguage.HINDI) glossaryEntry.definitionHi else glossaryEntry.definitionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == AppLanguage.HINDI) glossaryEntry.usageTipHi else glossaryEntry.usageTipEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = vedic.auspicious
                    )
                }
            }
        }
    }
}
