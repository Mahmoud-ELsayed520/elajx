package com.example.presentation.screens.prescription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxEmptyState
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Amber100
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800

/**
 * Prescription Scanner Screen.
 *
 * Mapped to 02_UX_UI_SPEC.md (Section 6 & 9) & 05_AI_SPEC.md:
 * - Step 1: Explain that app assists with reading the prescription and user must review results.
 * - AI Safety boundary: AI is an assistant, not a doctor or pharmacist.
 * - Empty state: "لم تفحص روشتة بعد" / "You haven't scanned a prescription yet".
 */
@Composable
fun PrescriptionScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .verticalScroll(rememberScrollState())
            .testTag("prescription_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.prescription_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            ElajxStatusBadge(
                status = BadgeStatus.INFO,
                label = "Phase 6 AI Pipeline",
                testTag = "prescription_badge"
            )
        }

        // Educational Intro Card: Step 1 of Prescription UX
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Emerald100),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Emerald800,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(ElajxSpacing.space2))
                    Text(
                        text = stringResource(R.string.prescription_intro_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Emerald800
                    )
                }
                Spacer(modifier = Modifier.height(ElajxSpacing.space2))
                Text(
                    text = stringResource(R.string.prescription_intro_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal900
                )
            }
        }

        // Safety Disclaimer Card (Mandatory per 05_AI_SPEC.md)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Amber100),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
                Text(
                    text = stringResource(R.string.prescription_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber600,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Empty State: "لم تفحص روشتة بعد"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            ElajxEmptyState(
                title = stringResource(R.string.prescription_empty_title),
                description = stringResource(R.string.prescription_empty_desc),
                icon = Icons.Default.Description,
                actionText = stringResource(R.string.prescription_cta),
                onActionClick = { /* Deferred to Phase 6 camera/upload implementation */ },
                testTag = "prescription_empty_state"
            )
        }
    }
}
