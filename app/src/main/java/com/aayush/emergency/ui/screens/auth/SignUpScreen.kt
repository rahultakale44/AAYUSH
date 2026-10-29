package com.aayush.emergency.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aayush.emergency.R
import com.aayush.emergency.data.model.UserRole
import com.aayush.emergency.ui.components.AAYUSHTextField
import com.aayush.emergency.ui.theme.*
import com.aayush.emergency.ui.viewmodel.AuthViewModel
import com.aayush.emergency.ui.viewmodel.RegisterState

/**
 * Sign up screen for new user registration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    role: UserRole,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var preferredLanguage by remember { mutableStateOf("en") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val registerState by viewModel.registerState.collectAsState()
    
    // Handle registration state changes
    LaunchedEffect(registerState) {
        when (val state = registerState) {
            is RegisterState.Success -> {
                viewModel.resetRegisterState()
                onSignUpSuccess()
            }
            is RegisterState.Error -> {
                showError = true
                errorMessage = state.message
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signup_title)) },
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
                .background(SoftBackground)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Logo
            Text(
                text = "आयुष",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Role-specific title
            val roleTitle = when (role) {
                UserRole.SENIOR_CITIZEN -> stringResource(R.string.role_senior_title)
                UserRole.TRANSPORT_COORDINATOR -> stringResource(R.string.role_transport_title)
                UserRole.HOSPITAL_STAFF -> stringResource(R.string.role_hospital_title)
            }
            
            Text(
                text = stringResource(R.string.signup_subtitle, roleTitle),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sign up form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.signup_create_account),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Error message
                    if (showError) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Full name field
                    AAYUSHTextField(
                        value = fullName,
                        onValueChange = { 
                            fullName = it
                            showError = false
                        },
                        label = stringResource(R.string.signup_fullname_label),
                        placeholder = stringResource(R.string.signup_fullname_hint),
                        imeAction = ImeAction.Next
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email field
                    AAYUSHTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            showError = false
                        },
                        label = stringResource(R.string.signup_email_label),
                        placeholder = stringResource(R.string.signup_email_hint),
                        isEmail = true,
                        imeAction = ImeAction.Next
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phone field (optional for senior)
                    if (role == UserRole.SENIOR_CITIZEN) {
                        AAYUSHTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = stringResource(R.string.signup_phone_label),
                            placeholder = stringResource(R.string.signup_phone_hint),
                            isPhone = true,
                            imeAction = ImeAction.Next
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Password field
                    AAYUSHTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            showError = false
                        },
                        label = stringResource(R.string.signup_password_label),
                        placeholder = stringResource(R.string.signup_password_hint),
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Confirm password field
                    AAYUSHTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            showError = false
                        },
                        label = stringResource(R.string.signup_confirm_password_label),
                        placeholder = stringResource(R.string.signup_confirm_password_hint),
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                viewModel.register(
                                    email = email,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    fullName = fullName,
                                    role = role,
                                    phone = phone,
                                    preferredLanguage = preferredLanguage
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sign up button
                    Button(
                        onClick = {
                            viewModel.register(
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword,
                                fullName = fullName,
                                role = role,
                                phone = phone,
                                preferredLanguage = preferredLanguage
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = registerState !is RegisterState.Loading && 
                                  fullName.isNotBlank() && 
                                  email.isNotBlank() && 
                                  password.isNotBlank() &&
                                  confirmPassword.isNotBlank()
                    ) {
                        if (registerState is RegisterState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.signup_button),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Login link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.signup_have_account),
                            color = TextSecondary
                        )
                        TextButton(onClick = onNavigateToLogin) {
                            Text(
                                text = stringResource(R.string.signup_login),
                                color = PrimaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Demo disclaimer
            Text(
                text = stringResource(R.string.auth_demo_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
