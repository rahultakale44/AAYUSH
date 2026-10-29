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
import com.aayush.emergency.ui.viewmodel.LoginState

/**
 * Login screen for role-specific authentication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    role: UserRole,
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val loginState by viewModel.loginState.collectAsState()
    
    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                viewModel.resetLoginState()
                onLoginSuccess()
            }
            is LoginState.Error -> {
                showError = true
                errorMessage = state.message
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
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
            Spacer(modifier = Modifier.height(32.dp))
            
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
                text = roleTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Login form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_welcome),
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
                    
                    // Email field
                    AAYUSHTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            showError = false
                        },
                        label = stringResource(R.string.login_email_label),
                        placeholder = stringResource(R.string.login_email_hint),
                        isEmail = true,
                        imeAction = ImeAction.Next
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password field
                    AAYUSHTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            showError = false
                        },
                        label = stringResource(R.string.login_password_label),
                        placeholder = stringResource(R.string.login_password_hint),
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Forgot password link
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(R.string.login_forgot_password),
                            color = PrimaryTeal
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Login button
                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = loginState !is LoginState.Loading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.login_button),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Demo access button
                    OutlinedButton(
                        onClick = { viewModel.demoLogin(role) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = loginState !is LoginState.Loading
                    ) {
                        Text(
                            text = stringResource(R.string.login_demo_access),
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryTeal
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sign up link
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_no_account),
                            color = TextSecondary
                        )
                        TextButton(onClick = onNavigateToSignUp) {
                            Text(
                                text = stringResource(R.string.login_sign_up),
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
