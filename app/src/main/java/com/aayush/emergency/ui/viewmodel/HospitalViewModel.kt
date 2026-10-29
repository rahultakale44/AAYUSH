package com.aayush.emergency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aayush.emergency.data.model.*
import com.aayush.emergency.data.repository.SharedCaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Hospital Operations Dashboard
 */
class HospitalViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sharedRepository = SharedCaseRepository.getInstance(application)
    
    private val _cases = MutableStateFlow<List<EmergencyCase>>(emptyList())
    val cases: StateFlow<List<EmergencyCase>> = _cases.asStateFlow()
    
    private val _staff = MutableStateFlow<List<HospitalStaff>>(emptyList())
    val staff: StateFlow<List<HospitalStaff>> = _staff.asStateFlow()
    
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
     * Check server connection and load initial data
     */
    fun checkServerAndLoadData() {
        viewModelScope.launch {
            _isLoading.value = true
            sharedRepository.checkServerConnection()
            loadCases()
            loadStaff()
            _isLoading.value = false
        }
    }
    
    /**
     * Load all emergency cases
     */
    fun loadCases() {
        viewModelScope.launch {
            val result = sharedRepository.getCases()
            result.onSuccess { caseList ->
                _cases.value = caseList.sortedByDescending { it.createdAt }
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = "Failed to load cases: ${error.message}"
            }
        }
    }
    
    /**
     * Load hospital staff
     */
    private fun loadStaff() {
        viewModelScope.launch {
            val staffList = sharedRepository.getHospitalStaff()
            _staff.value = staffList
        }
    }
    
    /**
     * Select a case for detailed view
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
     * Clear selected case
     */
    fun clearSelectedCase() {
        _selectedCase.value = null
    }
    
    /**
     * Acknowledge case
     */
    fun acknowledgeCase(caseId: Long) {
        viewModelScope.launch {
            val result = sharedRepository.updateHospitalStatus(
                caseId,
                HospitalCaseStatus.ACKNOWLEDGED
            )
            result.onSuccess {
                loadCases()
                if (_selectedCase.value?.id == caseId) {
                    selectCase(caseId)
                }
            }.onFailure { error ->
                _errorMessage.value = "Failed to acknowledge case: ${error.message}"
            }
        }
    }
    
    /**
     * Assign staff to case
     */
    fun assignStaff(caseId: Long, staff: HospitalStaff) {
        viewModelScope.launch {
            val assignment = StaffAssignment(
                staffId = staff.id,
                staffName = staff.displayName,
                service = staff.service,
                assignedAt = System.currentTimeMillis()
            )
            
            val result = sharedRepository.assignStaff(caseId, assignment)
            result.onSuccess {
                loadCases()
                if (_selectedCase.value?.id == caseId) {
                    selectCase(caseId)
                }
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to assign staff"
            }
        }
    }
    
    /**
     * Remove staff assignment
     */
    fun removeStaffAssignment(caseId: Long, staffId: String) {
        viewModelScope.launch {
            val result = sharedRepository.removeStaffAssignment(caseId, staffId)
            result.onSuccess {
                loadCases()
                if (_selectedCase.value?.id == caseId) {
                    selectCase(caseId)
                }
            }
        }
    }
    
    /**
     * Update hospital case status
     */
    fun updateCaseStatus(caseId: Long, status: HospitalCaseStatus) {
        viewModelScope.launch {
            val result = sharedRepository.updateHospitalStatus(caseId, status)
            result.onSuccess {
                loadCases()
                if (_selectedCase.value?.id == caseId) {
                    selectCase(caseId)
                }
            }.onFailure { error ->
                _errorMessage.value = "Failed to update status: ${error.message}"
            }
        }
    }
    
    /**
     * Get summary statistics
     */
    fun getSummary(): HospitalSummary {
        val caseList = _cases.value
        return HospitalSummary(
            totalCases = caseList.size,
            criticalCases = caseList.count { 
                it.session.triageResult?.urgencyLevel in listOf(
                    UrgencyLevel.IMMEDIATE,
                    UrgencyLevel.URGENT
                )
            },
            newCases = caseList.count { it.hospitalStatus == HospitalCaseStatus.NEW },
            activeCases = caseList.count { 
                it.hospitalStatus in listOf(
                    HospitalCaseStatus.ACKNOWLEDGED,
                    HospitalCaseStatus.STAFF_ASSIGNED,
                    HospitalCaseStatus.CARE_IN_PROGRESS
                )
            },
            availableStaff = _staff.value.count { it.availability == StaffAvailability.AVAILABLE }
        )
    }
    
    /**
     * Filter cases by urgency
     */
    fun filterByUrgency(urgency: UrgencyLevel?): List<EmergencyCase> {
        return if (urgency == null) {
            _cases.value
        } else {
            _cases.value.filter { it.session.triageResult?.urgencyLevel == urgency }
        }
    }
    
    /**
     * Filter cases by status
     */
    fun filterByStatus(status: HospitalCaseStatus?): List<EmergencyCase> {
        return if (status == null) {
            _cases.value
        } else {
            _cases.value.filter { it.hospitalStatus == status }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Hospital dashboard summary statistics
 */
data class HospitalSummary(
    val totalCases: Int = 0,
    val criticalCases: Int = 0,
    val newCases: Int = 0,
    val activeCases: Int = 0,
    val availableStaff: Int = 0
)
