package com.aayush.emergency.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aayush.emergency.R
import com.aayush.emergency.data.model.UserRole
import com.aayush.emergency.ui.components.ScreenHeader

/**
 * Role selection screen for three-device demo
 * 
 * DEMO MECHANISM - NOT REAL AUTHENTICATION
 * Allows switching between three role interfaces for competition demonstration
 */
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.role_selection_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ScreenHeader(
            title = stringResource(R.string.role_selection_title),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Senior Citizen Role
        RoleCard(
            title = stringResource(R.string.role_senior_title),
            description = stringResource(R.string.role_senior_description),
            icon = Icons.Default.Person,
            onClick = { onRoleSelected(UserRole.SENIOR_CITIZEN) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transport Coordinator Role
        RoleCard(
            title = stringResource(R.string.role_transport_title),
            description = stringResource(R.string.role_transport_description),
            icon = Icons.Default.LocalShipping,
            onClick = { onRoleSelected(UserRole.TRANSPORT_COORDINATOR) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hospital Staff Role
        RoleCard(
            title = stringResource(R.string.role_hospital_title),
            description = stringResource(R.string.role_hospital_description),
            icon = Icons.Default.LocalHospital,
            onClick = { onRoleSelected(UserRole.HOSPITAL_STAFF) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.role_selection_demo_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
