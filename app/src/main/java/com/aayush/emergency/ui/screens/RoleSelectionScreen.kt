package com.aayush.emergency.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aayush.emergency.R
import com.aayush.emergency.data.model.UserRole
import com.aayush.emergency.ui.theme.*

/**
 * Premium Role Selection Screen
 * Three distinct role cards with icon, clear descriptions
 * Follows the reference design with premium healthcare aesthetic
 */
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // AAYUSH logo/brand
        Text(
            text = "आयुष",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = stringResource(R.string.role_selection_question),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = DeepNavy,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.role_selection_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Senior Citizen Role Card
        PremiumRoleCard(
            title = stringResource(R.string.role_senior_title),
            description = stringResource(R.string.role_senior_description),
            icon = Icons.Default.Person,
            iconBackgroundColor = SafeGreen.copy(alpha = 0.15f),
            iconTint = SafeGreen,
            onClick = { onRoleSelected(UserRole.SENIOR_CITIZEN) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transport/Emergency Response Role Card
        PremiumRoleCard(
            title = stringResource(R.string.role_transport_title),
            description = stringResource(R.string.role_transport_description),
            icon = Icons.Default.LocalShipping,
            iconBackgroundColor = PrimaryTeal.copy(alpha = 0.15f),
            iconTint = DeepTeal,
            onClick = { onRoleSelected(UserRole.TRANSPORT_COORDINATOR) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hospital Staff Role Card
        PremiumRoleCard(
            title = stringResource(R.string.role_hospital_title),
            description = stringResource(R.string.role_hospital_description),
            icon = Icons.Default.LocalHospital,
            iconBackgroundColor = WarningAmber.copy(alpha = 0.15f),
            iconTint = Color(0xFFD97706),
            onClick = { onRoleSelected(UserRole.HOSPITAL_STAFF) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PremiumRoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy,
                    fontSize = 20.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
