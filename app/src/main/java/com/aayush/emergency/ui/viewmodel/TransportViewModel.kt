package com.aayush.emergency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aayush.emergency.data.model.*
import com.aayush.emergency.data.repository.SharedCaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Transport Coordinator Dashboard
 * Manages simulated ambulance/transport coordination
 */
class TransportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sharedRepository = SharedCaseRepository.getInstance(application)
    
    private val _cases = MutableStateFlow<List<EmergencyCase>>(emptyList())
    val cases: StateFlow<List<EmergencyCase>> = _cases.asStateFlow()
    
    private val _selectedCase = MutableStateFlow<EmergencyCase?>(null)
    val selectedCase: StateFlow<EmergencyCase?> = _selectedCase.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val serverAvailable: StateFlow<Boolean> = sharedRepository.serverAvailable
    
    init {
        checkServerAndLoadData()
    }
    
    /**
     * Check server and load cases
     */
    fun checkServerAndLoadData() {
        viewModelScope.launch {
            _isLoading.value = true
            sharedRepository.checkServerConnection()
            loadCases()
            _isLoading.value = false
        }
    }
    
    /**
     * Load all cases
     */
    fun loadCases() {
        viewModelScope.launch {
            val result = sharedRepository.getCases()
            result.onSuccess { caseList ->
                // Show cases that need transport
                _cases.value = caseList
                    .filter { it.session.triageResult != null }
                    .sortedByDescending { it.createdAt }
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = "Failed to load cases: ${error.message}"
            }
        }
    }
    
    /**
     * Select case
     */
    fun selectCase(caseId: Long) {
        viewModelScope.launch {
            val result = sharedRepository.getCase(caseId)
            result.onSuccess { case ->
                _selectedCase.value = case
            }
        }
    }
    
    /**
     * Clear selection
     */
    fun clearSelectedCase() {
        _selectedCase.value = null
    }
    
    /**
     * Update transport status
     * SIMULATED - does not dispatch real ambulance
     */
    fun updateTransportStatus(caseId: Long, status: TransportStatus) {
        viewModelScope.launch {
            val result = sharedRepository.updateTransportStatus(caseId, status)
            result.onSuccess {
                loadCases()
                if (_selectedCase.value?.id == caseId) {
                    selectCase(caseId)
                }
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = "Failed to update transport: ${error.message}"
            }
        }
    }
    
    /**
     * Get transport summary
     */
    fun getTransportSummary(): TransportSummary {
        val caseList = _cases.value
        return TransportSummary(
            pendingRequests = caseList.count { 
                it.transportStatus in listOf(TransportStatus.NONE, TransportStatus.REQUESTED) 
            },
            enRoute = caseList.count { it.transportStatus == TransportStatus.EN_ROUTE },
            transporting = caseList.count { it.transportStatus == TransportStatus.TRANSPORTING },
            completed = caseList.count { it.transportStatus == TransportStatus.COMPLETED }
        )
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Transport coordinator summary
 */
data class TransportSummary(
    val pendingRequests: Int = 0,
    val enRoute: Int = 0,
    val transporting: Int = 0,
    val completed: Int = 0
)
