package com.mahavtaar.panchanga.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.panchanga.ui.components.ChakraPetalAccentBar
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTopBarTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    LanguageTogglePill(
                        onToggleLanguage = viewModel::toggleLanguage,
                        modifier = Modifier.padding(end = 12.dp)
                    )
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
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PanchangaCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MahavtaarBrandLogo(size = 112.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.appTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            ChakraPetalAccentBar(height = 3.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.aboutLeadCopy,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                item {
                    PanchangaCard {
                        SectionHeader(strings.aboutMethodologyTitle, strings.aboutMethodologySubtitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.aboutMethodologyBody,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    PanchangaCard {
                        SectionHeader(strings.aboutVersionTitle, strings.aboutAttribution)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.aboutVersionBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
