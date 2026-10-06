package com.example.presentation.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.core.config.AppEnvironment
import com.example.core.config.EnvironmentConfig
import com.example.domain.model.AuthSession
import com.example.presentation.components.BadgeStatus
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.components.ElajxStatusBadge
import com.example.presentation.theme.Charcoal100
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800

/**
 * Account & System Screen.
 *
 * Mapped to 02_UX_UI_SPEC.md (Section 3) & 06_SECURITY_PRIVACY_COMPLIANCE.md:
 * - Patient profile / session status
 * - Environment and architecture overview
 * - Privacy notice & Egyptian regulatory compliance reference
 * - Diagnostics entry point to view Bootstrap Verification Baseline
 */
@Composable
fun AccountScreen(
    session: AuthSession,
    environmentConfig: EnvironmentConfig,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onViewBaseline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ElajxSpacing.space4)
            .verticalScroll(rememberScrollState())
            .testTag("account_screen"),
        verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
    ) {
        // Header
        Text(
            text = stringResource(R.string.account_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Patient Profile / Session Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Emerald100, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Emerald800,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(ElajxSpacing.space3))
                        Column {
                            val title = when (session) {
                                is AuthSession.Authenticated -> session.fullName
                                else -> stringResource(R.string.account_guest_title)
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val phone = when (session) {
                                is AuthSession.Authenticated -> com.example.core.util.EgyptianPhoneUtil.formatMasked(session.phone)
                                else -> stringResource(R.string.auth_state_unauthenticated)
                            }
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal600
                            )
                        }
                    }

                    val (status, label) = when (session) {
                        is AuthSession.Authenticated -> BadgeStatus.SUCCESS to "Authenticated"
                        else -> BadgeStatus.WARNING to "Guest"
                    }
                    ElajxStatusBadge(status = status, label = label)
                }

                Spacer(modifier = Modifier.height(ElajxSpacing.space3))
                val description = when (session) {
                    is AuthSession.Authenticated -> stringResource(R.string.account_auth_desc)
                    else -> stringResource(R.string.account_guest_desc)
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )

                Spacer(modifier = Modifier.height(ElajxSpacing.space3))
                if (session is AuthSession.Authenticated) {
                    ElajxButton(
                        text = stringResource(R.string.auth_sign_out),
                        onClick = onSignOut,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "account_btn_sign_out"
                    )
                } else {
                    ElajxButton(
                        text = stringResource(R.string.auth_sign_in),
                        onClick = onSignIn,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "account_btn_sign_in"
                    )
                }
            }
        }

        // System Architecture Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
                Text(
                    text = stringResource(R.string.account_system_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(ElajxSpacing.space2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Active Environment", style = MaterialTheme.typography.bodyMedium, color = Charcoal600)
                    Text(text = environmentConfig.environment.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Client Security", style = MaterialTheme.typography.bodyMedium, color = Charcoal600)
                    Text(text = "Zero Secrets / Untrusted Client", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(ElajxSpacing.space1))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Backend Platform", style = MaterialTheme.typography.bodyMedium, color = Charcoal600)
                    Text(text = "Supabase PostgreSQL (RLS)", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Privacy & Compliance Card (Law 151/2020)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(ElajxSpacing.space4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(ElajxSpacing.space2))
                    Text(
                        text = stringResource(R.string.account_privacy_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(ElajxSpacing.space2))
                Text(
                    text = stringResource(R.string.account_privacy_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal600
                )
            }
        }

        // Button to Open Verification Baseline from Bootstrap
        ElajxButton(
            text = stringResource(R.string.account_view_baseline),
            onClick = onViewBaseline,
            variant = ButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
            testTag = "account_btn_view_baseline"
        )
    }
}
