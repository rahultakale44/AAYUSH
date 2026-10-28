package com.aayush.emergency

import com.aayush.emergency.ui.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppViewModel
 * Tests Phase 1 language selection state management
 */
class AppViewModelTest {
    
    private lateinit var viewModel: AppViewModel
    
    @Before
    fun setup() {
        viewModel = AppViewModel()
    }
    
    @Test
    fun defaultLanguage_isEnglish() {
        assertEquals("en", viewModel.selectedLanguage.value)
    }
    
    @Test
    fun setLanguage_toMarathi_updatesState() {
        viewModel.setLanguage("mr")
        assertEquals("mr", viewModel.selectedLanguage.value)
    }
    
    @Test
    fun setLanguage_toHindi_updatesState() {
        viewModel.setLanguage("hi")
        assertEquals("hi", viewModel.selectedLanguage.value)
    }
    
    @Test
    fun setLanguage_backToEnglish_updatesState() {
        viewModel.setLanguage("mr")
        viewModel.setLanguage("en")
        assertEquals("en", viewModel.selectedLanguage.value)
    }
}
