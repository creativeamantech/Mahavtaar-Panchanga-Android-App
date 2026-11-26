package com.mahavtaar.panchanga.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.ui.R
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun MahavtaarBrandLogo(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Image(
        painter = painterResource(id = R.drawable.ic_panchanga_logo),
        contentDescription = strings.appTitle,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

@Composable
fun ChakraPetalAccentBar(
    modifier: Modifier = Modifier,
    height: Dp = 3.dp
) {
    val vedic = LocalVedicColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        vedic.petalPlum,
                        vedic.petalAzure,
                        vedic.petalEmerald,
                        vedic.petalSaffron
                    )
                )
            )
    )
}

@Composable
fun PanchangaCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    textColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    border: BorderStroke? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = border
    ) {
        Text(
            text = label,
            style = textStyle,
            color = textColor,
            fontWeight = fontWeight,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SwaraNadiBadge(
    nadi: SwaraNadi,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val color = when (nadi) {
        SwaraNadi.IDA -> vedic.idaNadi
        SwaraNadi.PINGALA -> vedic.pingalaNadi
        SwaraNadi.SUSHUMNA -> vedic.sushumnaNadi
    }
    StatusPill(
        label = VedicLocalizer.nadiBadgeLabel(nadi, lang),
        textColor = color,
        containerColor = color.copy(alpha = 0.18f),
        fontWeight = FontWeight.Bold,
        textStyle = MaterialTheme.typography.labelMedium,
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.55f)),
        modifier = modifier
    )
}

@Composable
fun TattvaBadge(
    element: TattvaElement,
    showElementWord: Boolean = true,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val color = when (element) {
        TattvaElement.PRITHVI -> vedic.petalSaffron
        TattvaElement.JALA -> vedic.idaNadi
        TattvaElement.TEJAS -> vedic.inauspicious
        TattvaElement.VAYU -> vedic.auspicious
        TattvaElement.AKASH -> vedic.sushumnaNadi
    }
    val baseName = VedicLocalizer.tattvaName(element, lang)
    val label = if (showElementWord) {
        if (lang == AppLanguage.HINDI) "$baseName तत्व" else "$baseName Tattva"
    } else {
        baseName
    }
    StatusPill(
        label = label,
        textColor = color,
        containerColor = color.copy(alpha = 0.18f),
        fontWeight = FontWeight.Bold,
        textStyle = MaterialTheme.typography.labelMedium,
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.55f)),
        modifier = modifier
    )
}

@Composable
fun EventStartEndDateTimeView(
    startText: String,
    endText: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val startLabel = if (lang == AppLanguage.HINDI) "आरम्भ:" else "Start:"
    val endLabel = if (lang == AppLanguage.HINDI) "समाप्ति:" else "End:"
    val labelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val valueStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = startLabel,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = startText,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = endLabel,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = endText,
                style = valueStyle,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveNowClarityBanner(
    sentence: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val nowTag = if (lang == AppLanguage.HINDI) "अभी" else "NOW"
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor
            ) {
                Text(
                    text = nowTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateLocationHeaderBar(
    cityName: String,
    timezoneLabel: String,
    liveClockText: String,
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onLocationClick: () -> Unit,
    onToggleLanguage: (() -> Unit)? = null,
    coordinateSubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // City pill + quick EN/हि language toggle + Live city clock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onLocationClick)
                        .testTag("header_location_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = strings.changeLocationCd,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cityName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onToggleLanguage != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleLanguage)
                            .testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = strings.switchLanguageCd,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "हि / EN" else "EN / हि",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = liveClockText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (!coordinateSubtitle.isNullOrBlank()) "$timezoneLabel · $coordinateSubtitle" else timezoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Date switcher row
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousDay,
                    modifier = Modifier.testTag("date_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = strings.previousDayCd,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("date_picker_trigger"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = strings.pickDateCd,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = VedicLocalizer.formatHeaderDate(selectedDate, lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onSelectToday,
                        modifier = Modifier.testTag("date_today_button")
                    ) {
                        Text(strings.today, style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.testTag("date_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = strings.nextDayCd,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSelectDate(picked)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm")
                ) {
                    Text(strings.select)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun LoadingStateBox(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val resolvedMessage = message ?: LocalStrings.current.loadingEphemeris
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = resolvedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    PanchangaCard(modifier = modifier) {
        Text(
            text = strings.calculationError,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag("error_retry_button")
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.retry)
        }
    }
}

@Composable
fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun LanguageTogglePill(
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onToggleLanguage)
            .testTag("language_toggle_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = strings.switchLanguageCd,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (lang == AppLanguage.HINDI) "हि / EN" else "EN / हि",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
