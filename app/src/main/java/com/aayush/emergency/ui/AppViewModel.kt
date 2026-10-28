package com.aayush.emergency.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main ViewModel managing app-wide state
 */
class AppViewModel : ViewModel() {
    
    // Language state: "en", "mr" (Marathi), "hi" (Hindi)
    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()
    
    /**
     * Update the selected language
     * @param languageCode Language code: "en", "mr", or "hi"
     */
    fun setLanguage(languageCode: String) {
        _selectedLanguage.value = languageCode
    }
}
