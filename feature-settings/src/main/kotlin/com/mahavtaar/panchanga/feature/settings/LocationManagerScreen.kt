package com.mahavtaar.panchanga.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.PermissionRationaleDialog
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.export.TimezoneCatalog
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val context = LocalContext.current
    val canCustomCoords = policy.hasFeature(AppFeatureKey.CUSTOM_COORDINATES)
    val snackbarHostState = remember { SnackbarHostState() }
    var editingLocation by remember { mutableStateOf<CityLocation?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showGpsRationale by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectCurrentGpsLocation()
        }
    }

    if (showGpsRationale) {
        PermissionRationaleDialog(
            title = if (isHi) "लोकेशन अनुमति" else "Use Your Location",
            reason = if (isHi) {
                "आपके सटीक सूर्योदय, सूर्यास्त, होरा और तत्व समय की गणना के लिए लोकेशन आवश्यक है।"
            } else {
                "Location lets Mahavtaar calculate exact local Sunrise, Sunset, Hora, and Tattva times for where you are."
            },
            withoutPermissionNote = if (isHi) {
                "इसके बिना आप नीचे सूची से कोई भी शहर चुन सकते हैं।"
            } else {
                "Without GPS access, you can pick any city manually from the list below."
            },
            allowLabel = if (isHi) "अनुमति दें" else "Allow GPS",
            notNowLabel = if (isHi) "अभी नहीं" else "Not Now",
            onAllow = {
                showGpsRationale = false
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showGpsRationale = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.selectLocationTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("locations_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
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
                    .widthIn(max = 720.dp)
                    .testTag("locations_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "location_action_buttons") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) {
                                    viewModel.detectCurrentGpsLocation()
                                } else {
                                    showGpsRationale = true
                                }
                            },
                            enabled = !state.isDetectingGps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("detect_gps_button")
                        ) {
                            if (state.isDetectingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isDetectingGps) strings.acquiringGps else strings.useCurrentGps)
                        }

                        if (canCustomCoords) {
                            OutlinedButton(
                                onClick = {
                                    editingLocation = null
                                    showCustomDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("add_custom_location_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ext.addCustomLocationButton)
                            }
                        }
                    }
                }

                item(key = "search_box") {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        label = { Text(strings.searchCityHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = strings.searchCityHint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input")
                    )
                }

                if (state.savedLocations.isNotEmpty() && state.searchQuery.isBlank()) {
                    item(key = "saved_header") {
                        SectionHeader(title = strings.savedLocationsTitle, subtitle = strings.savedLocationsSubtitle)
                    }
                    items(items = state.savedLocations, key = { "saved_${it.name}" }) { loc ->
                        val active = state.preferences.selectedLocation
                        val isSelected = active.name == loc.name &&
                            kotlin.math.abs(active.latitude - loc.latitude) < 1e-4 &&
                            kotlin.math.abs(active.longitude - loc.longitude) < 1e-4
                        CityRowItem(
                            city = loc,
                            isSelected = isSelected,
                            onSelect = {
                                viewModel.selectLocation(loc)
                                onNavigateBack()
                            },
                            onSetDefault = if (canCustomCoords) {
                                { viewModel.setDefaultLocation(loc) }
                            } else null,
                            onEdit = if (canCustomCoords) {
                                {
                                    editingLocation = loc
                                    showCustomDialog = true
                                }
                            } else null,
                            onDelete = if (canCustomCoords) {
                                { viewModel.removeSavedLocation(loc.name) }
                            } else null
                        )
                    }
                }

                item(key = "results_header") {
                    SectionHeader(
                        title = if (state.searchQuery.isBlank()) strings.popularCitiesTitle else strings.matchingCitiesTitle,
                        subtitle = strings.offlineCatalogSubtitle
                    )
                }

                items(items = state.searchResults, key = { "res_${it.name}_${it.latitude}" }) { city ->
                    val active = state.preferences.selectedLocation
                    val isSelected = active.name == city.name &&
                        kotlin.math.abs(active.latitude - city.latitude) < 1e-4 &&
                        kotlin.math.abs(active.longitude - city.longitude) < 1e-4
                    CityRowItem(
                        city = city,
                        isSelected = isSelected,
                        onSelect = {
                            viewModel.selectLocation(city)
                            onNavigateBack()
                        },
                        onSetDefault = if (canCustomCoords) {
                            { viewModel.setDefaultLocation(city) }
                        } else null,
                        onEdit = null,
                        onDelete = null
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (canCustomCoords && showCustomDialog) {
        CustomLocationEditorDialog(
            initialLocation = editingLocation ?: state.preferences.selectedLocation,
            isEditingExisting = editingLocation != null,
            onDismiss = { showCustomDialog = false },
            onSave = { name, lat, lon, tz, makeDefault ->
                viewModel.saveCustomLocation(
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    timezone = tz,
                    makeDefault = makeDefault,
                    originalNameToReplace = editingLocation?.name
                )
                showCustomDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CityRowItem(
    city: CityLocation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onSetDefault: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onSelect)
            .testTag("location_row_${city.name}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(city.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${city.timezone} · ${"%.5f°, %.5f°".format(Locale.US, city.latitude, city.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (city.isDefault || city.isCustom || city.isGpsResolved) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (city.isDefault) {
                            StatusPill(
                                label = ext.defaultBadgeLabel,
                                textColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        if (city.isCustom) {
                            StatusPill(
                                label = ext.customBadgeLabel,
                                textColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        if (city.isGpsResolved) {
                            val accText = city.accuracyMeters?.let { " ±${it.roundToInt()}m" }.orEmpty()
                            StatusPill(
                                label = "${ext.gpsBadgeLabel}$accText",
                                textColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }
                }
            }
            if (!city.isDefault && onSetDefault != null) {
                IconButton(
                    onClick = onSetDefault,
                    modifier = Modifier.testTag("pin_default_btn_${city.name}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = ext.pinAsDefaultLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_location_btn_${city.name}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = ext.editLocationTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = strings.activeCityCd, tint = MaterialTheme.colorScheme.primary)
            }
            if (onDelete != null && !isSelected) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.removeLocationCd)
                }
            }
        }
    }
}

@Composable
private fun CustomLocationEditorDialog(
    initialLocation: CityLocation,
    isEditingExisting: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lon: Double, timezone: String, makeDefault: Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    var name by remember { mutableStateOf(if (isEditingExisting) initialLocation.name else "") }
    var latText by remember { mutableStateOf("%.5f".format(Locale.US, initialLocation.latitude)) }
    var lonText by remember { mutableStateOf("%.5f".format(Locale.US, initialLocation.longitude)) }
    var selectedTz by remember { mutableStateOf(initialLocation.timezone) }
    var tzQuery by remember { mutableStateOf("") }
    var showTzList by remember { mutableStateOf(false) }
    var makeDefault by remember { mutableStateOf(initialLocation.isDefault) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val tzOptions = remember(tzQuery) { TimezoneCatalog.filterOptions(tzQuery, limit = 40) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditingExisting) ext.editLocationTitle else ext.addCustomLocationButton) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text(ext.locationLabelHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_loc_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = {
                            latText = it
                            errorMsg = null
                        },
                        label = { Text(ext.latitudeHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_loc_lat_input")
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = {
                            lonText = it
                            errorMsg = null
                        },
                        label = { Text(ext.longitudeHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_loc_lon_input")
                    )
                }

                OutlinedButton(
                    onClick = { showTzList = !showTzList },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("custom_loc_tz_picker_button")
                ) {
                    Text(
                        text = "${ext.timezonePickerTitle}: $selectedTz",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showTzList) {
                    OutlinedTextField(
                        value = tzQuery,
                        onValueChange = { tzQuery = it },
                        label = { Text(ext.searchTimezoneHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_loc_tz_search")
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tzOptions.forEach { opt ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (opt.id == selectedTz) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTz = opt.id
                                        showTzList = false
                                    }
                            ) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ext.pinAsDefaultLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it },
                        modifier = Modifier.testTag("custom_loc_default_switch")
                    )
                }

                errorMsg?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val parsedLat = TimezoneCatalog.parseLatitude(latText)
                    val parsedLon = TimezoneCatalog.parseLongitude(lonText)
                    when {
                        cleanName.isBlank() -> errorMsg = ext.invalidLocationNameMsg
                        parsedLat == null -> errorMsg = ext.invalidLatitudeMsg
                        parsedLon == null -> errorMsg = ext.invalidLongitudeMsg
                        else -> onSave(cleanName, parsedLat, parsedLon, selectedTz, makeDefault)
                    }
                },
                modifier = Modifier.testTag("custom_loc_save_button")
            ) {
                Text(ext.saveAndApplyLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
