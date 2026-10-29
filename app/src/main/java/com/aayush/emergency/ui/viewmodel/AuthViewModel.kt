package com.aayush.emergency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aayush.emergency.data.model.UserRole
import com.aayush.emergency.data.model.UserSession
import com.aayush.emergency.data.repository.AuthRepository
import com.aayush.emergency.data.repository.SeedDataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication flows
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository.getInstance(application)
    private val seedDataRepository = SeedDataRepository.getInstance(application)
    
    val currentSession: StateFlow<UserSession?> = authRepository.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    
    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)
    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()
    
    init {
        // Initialize seed data on first launch
        viewModelScope.launch {
            authRepository.initializeSeedData()
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            val session = authRepository.login(email, password)
            
            _loginState.value = if (session != null) {
                // Initialize seed data for demo senior account
                if (session.role == com.aayush.emergency.data.model.UserRole.SENIOR_CITIZEN && session.isDemo) {
                    seedDataRepository.initializeSeniorSeedData(session.userId)
                }
                LoginState.Success(session)
            } else {
                LoginState.Error("Invalid email or password")
            }
        }
    }
    
    /**
     * Demo access login
     */
    fun demoLogin(role: UserRole) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            val session = authRepository.demoLogin(role)
            
            _loginState.value = if (session != null) {
                // Initialize seed data for demo senior account
                if (session.role == com.aayush.emergency.data.model.UserRole.SENIOR_CITIZEN) {
                    seedDataRepository.initializeSeniorSeedData(session.userId)
                }
                LoginState.Success(session)
            } else {
                LoginState.Error("Demo account not found")
            }
        }
    }
    
    /**
     * Register new user
     */
    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        role: UserRole,
        phone: String = "",
        preferredLanguage: String = "en"
    ) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            
            // Validate inputs
            when {
                email.isBlank() -> {
                    _registerState.value = RegisterState.Error("Email is required")
                    return@launch
                }
                password.isBlank() -> {
                    _registerState.value = RegisterState.Error("Password is required")
                    return@launch
                }
                password != confirmPassword -> {
                    _registerState.value = RegisterState.Error("Passwords do not match")
                    return@launch
                }
                fullName.isBlank() -> {
                    _registerState.value = RegisterState.Error("Full name is required")
                    return@launch
                }
            }
            
            val result = authRepository.register(
                email = email,
                password = password,
                fullName = fullName,
                role = role,
                phone = phone,
                preferredLanguage = preferredLanguage
            )
            
            _registerState.value = result.fold(
                onSuccess = { session -> RegisterState.Success(session) },
                onFailure = { error -> RegisterState.Error(error.message ?: "Registration failed") }
            )
        }
    }
    
    /**
     * Request password reset
     */
    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            
            if (email.isBlank()) {
                _resetState.value = ResetState.Error("Email is required")
                return@launch
            }
            
            val exists = authRepository.requestPasswordReset(email)
            
            _resetState.value = if (exists) {
                ResetState.Success("Password reset instructions sent to $email (Demo: No actual email sent)")
            } else {
                ResetState.Error("Email not found")
            }
        }
    }
    
    /**
     * Logout
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
    
    /**
     * Reset login state
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
    
    /**
     * Reset register state
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
    
    /**
     * Reset password reset state
     */
    fun resetResetState() {
        _resetState.value = ResetState.Idle
    }
}

/**
 * Login state
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val session: UserSession) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Registration state
 */
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val session: UserSession) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

/**
 * Password reset state
 */
sealed class ResetState {
    object Idle : ResetState()
    object Loading : ResetState()
    data class Success(val message: String) : ResetState()
    data class Error(val message: String) : ResetState()
}
