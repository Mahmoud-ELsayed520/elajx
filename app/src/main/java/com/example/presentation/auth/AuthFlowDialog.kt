package com.example.presentation.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.util.EgyptianPhoneUtil
import com.example.presentation.components.ButtonVariant
import com.example.presentation.components.ElajxButton
import com.example.presentation.theme.Charcoal600
import com.example.presentation.theme.Charcoal900
import com.example.presentation.theme.ElajxSpacing
import com.example.presentation.theme.Emerald100
import com.example.presentation.theme.Emerald800

/**
 * Authentication Dialog & Flow Composable.
 *
 * Implements Phase 2 Phone OTP flow per 02_UX_UI_SPEC.md (Section 6 & 13):
 * - Egyptian phone input (+20)
 * - 6-digit OTP code entry
 * - 60-second resend timer
 * - Law 151/2020 privacy reassurance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthFlowDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("auth_flow_dialog"),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.auth_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.step == AuthStep.OTP_VERIFICATION) {
                                    viewModel.resetToPhoneInput()
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("auth_nav_back")
                        ) {
                            Icon(
                                imageVector = if (uiState.step == AuthStep.OTP_VERIFICATION) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.Default.Close
                                },
                                contentDescription = "Close"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(ElajxSpacing.space4)
            ) {
                when (uiState.step) {
                    AuthStep.PHONE_INPUT -> {
                        PhoneInputStep(
                            phoneInput = uiState.phoneInput,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            onPhoneChanged = { viewModel.onPhoneInputChanged(it) },
                            onSubmit = { viewModel.sendOtp() }
                        )
                    }
                    AuthStep.OTP_VERIFICATION -> {
                        OtpVerificationStep(
                            normalizedPhone = uiState.normalizedPhone,
                            otpInput = uiState.otpInput,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            cooldownSeconds = uiState.resendCooldownSeconds,
                            onOtpChanged = { viewModel.onOtpInputChanged(it) },
                            onVerify = { viewModel.verifyOtp(onSuccess = onSuccess) },
                            onResend = { viewModel.resendOtp() },
                            onChangePhone = { viewModel.resetToPhoneInput() }
                        )
                    }
                    AuthStep.SUCCESS -> {
                        // Dismiss handled by onSuccess callback
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneInputStep(
    phoneInput: String,
    isLoading: Boolean,
    errorMessage: String?,
    onPhoneChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_phone_step_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Emerald100, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Emerald800,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(ElajxSpacing.space3))
                Column {
                    Text(
                        text = stringResource(R.string.auth_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.auth_phone_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
            }

            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_phone_input"),
                placeholder = { Text(stringResource(R.string.auth_phone_hint)) },
                prefix = {
                    Text(
                        text = stringResource(R.string.auth_phone_prefix) + "  ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = errorMessage != null,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("auth_error_message")
                )
            }

            ElajxButton(
                text = stringResource(R.string.auth_btn_send_otp),
                onClick = onSubmit,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "auth_btn_send_otp"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Emerald800,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(ElajxSpacing.space1))
                Text(
                    text = stringResource(R.string.auth_privacy_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = Charcoal600
                )
            }
        }
    }
}

@Composable
private fun OtpVerificationStep(
    normalizedPhone: String,
    otpInput: String,
    isLoading: Boolean,
    errorMessage: String?,
    cooldownSeconds: Int,
    onOtpChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangePhone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_otp_step_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(ElajxSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(ElajxSpacing.space4)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Emerald100, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Emerald800,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(ElajxSpacing.space3))
                Column {
                    Text(
                        text = stringResource(R.string.auth_otp_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.auth_otp_subtitle,
                            EgyptianPhoneUtil.formatMasked(normalizedPhone)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600
                    )
                }
            }

            OutlinedTextField(
                value = otpInput,
                onValueChange = onOtpChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_otp_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.auth_otp_hint),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = errorMessage != null,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("auth_otp_error_message")
                )
            }

            ElajxButton(
                text = stringResource(R.string.auth_btn_verify),
                onClick = onVerify,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "auth_btn_verify_otp"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cooldownSeconds > 0) {
                    Text(
                        text = stringResource(R.string.auth_resend_countdown, cooldownSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal600,
                        modifier = Modifier.testTag("auth_resend_timer")
                    )
                } else {
                    ElajxButton(
                        text = stringResource(R.string.auth_btn_resend),
                        onClick = onResend,
                        variant = ButtonVariant.GHOST,
                        testTag = "auth_btn_resend"
                    )
                }

                ElajxButton(
                    text = stringResource(R.string.auth_change_phone),
                    onClick = onChangePhone,
                    variant = ButtonVariant.GHOST,
                    testTag = "auth_btn_change_phone"
                )
            }
        }
    }
}
