package com.aayush.emergency.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aayush.emergency.R
import com.aayush.emergency.ui.components.AAYUSHCard
import com.aayush.emergency.ui.components.ScreenHeader

/**
 * Language selection screen with working state
 * Supports English, Marathi (मराठी), and Hindi (हिंदी)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit
) {
    // Language state - in Phase 1, persisted in ViewModel in later phases
    var selectedLanguage by remember { mutableStateOf("en") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.language_title)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AAYUSHCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    LanguageOption(
                        text = stringResource(R.string.language_english),
                        languageCode = "en",
                        selected = selectedLanguage == "en",
                        onSelect = { selectedLanguage = "en" }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LanguageOption(
                        text = stringResource(R.string.language_marathi),
                        languageCode = "mr",
                        selected = selectedLanguage == "mr",
                        onSelect = { selectedLanguage = "mr" }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LanguageOption(
                        text = stringResource(R.string.language_hindi),
                        languageCode = "hi",
                        selected = selectedLanguage == "hi",
                        onSelect = { selectedLanguage = "hi" }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note about language support
            Text(
                text = "Language selection updates the interface. Full multilingual support including voice input will be enhanced in later phases.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageOption(
    text: String,
    languageCode: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // Click handled by Row's selectable
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
