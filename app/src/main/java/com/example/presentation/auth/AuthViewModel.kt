package com.example.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.core.util.EgyptianPhoneUtil
import com.example.domain.model.AuthSession
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION,
    SUCCESS
}

data class AuthUiState(
    val step: AuthStep = AuthStep.PHONE_INPUT,
    val phoneInput: String = "",
    val normalizedPhone: String = "",
    val otpInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resendCooldownSeconds: Int = 0
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onPhoneInputChanged(input: String) {
        _uiState.update { it.copy(phoneInput = input, errorMessage = null) }
    }

    fun onOtpInputChanged(input: String) {
        // Limit to 6 digits
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _uiState.update { it.copy(otpInput = input, errorMessage = null) }
        }
    }

    fun sendOtp() {
        val phone = _uiState.value.phoneInput
        val normalizeResult = EgyptianPhoneUtil.normalize(phone)
        if (normalizeResult is AppResult.Error) {
            _uiState.update { it.copy(errorMessage = normalizeResult.error.message) }
            return
        }
        val normalized = (normalizeResult as AppResult.Success).data

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.sendOtp(normalized)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            normalizedPhone = normalized,
                            step = AuthStep.OTP_VERIFICATION,
                            otpInput = "",
                            errorMessage = null
                        )
                    }
                    startCountdownTimer(60)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun resendOtp() {
        if (_uiState.value.resendCooldownSeconds > 0) return
        val normalized = _uiState.value.normalizedPhone
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.sendOtp(normalized)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    startCountdownTimer(60)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        val otp = _uiState.value.otpInput
        if (!EgyptianPhoneUtil.isValidOtp(otp)) {
            _uiState.update { it.copy(errorMessage = "OTP must be exactly 6 digits") }
            return
        }

        val normalized = _uiState.value.normalizedPhone
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.verifyOtp(normalized, otp)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, step = AuthStep.SUCCESS, errorMessage = null) }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun resetToPhoneInput() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                step = AuthStep.PHONE_INPUT,
                otpInput = "",
                errorMessage = null,
                resendCooldownSeconds = 0
            )
        }
    }

    private fun startCountdownTimer(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
                if (remaining > 0) delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
