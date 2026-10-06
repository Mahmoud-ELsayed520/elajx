package com.example.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.core.security.SecureLogger
import com.example.data.repository.MedicineRepositoryImpl
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineDetail
import com.example.domain.repository.MedicineRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Initial : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val medicines: List<Medicine>, val query: String) : SearchUiState()
    data class Empty(val query: String) : SearchUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : SearchUiState()
}

sealed class MedicineDetailUiState {
    object Idle : MedicineDetailUiState()
    object Loading : MedicineDetailUiState()
    data class Success(val detail: MedicineDetail) : MedicineDetailUiState()
    data class Error(val message: String) : MedicineDetailUiState()
}

class SearchViewModel(
    private val medicineRepository: MedicineRepository = MedicineRepositoryImpl(),
    private val coroutineDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : ViewModel() {

    private val tag = "SearchViewModel"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<MedicineDetailUiState>(MedicineDetailUiState.Idle)
    val detailState: StateFlow<MedicineDetailUiState> = _detailState.asStateFlow()

    private var searchJob: Job? = null
    private var currentSearchRequestId: Long = 0L

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()

        if (trimmed.isEmpty()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState.Initial
            return
        }

        // Debounce search input by 300ms to avoid flooding backend requests while typing
        searchJob?.cancel()
        searchJob = viewModelScope.launch(coroutineDispatcher) {
            delay(300)
            executeSearch(trimmed)
        }
    }

    fun searchNow(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()
        searchJob?.cancel()
        if (trimmed.isEmpty()) {
            _uiState.value = SearchUiState.Initial
            return
        }
        viewModelScope.launch(coroutineDispatcher) {
            executeSearch(trimmed)
        }
    }

    fun retrySearch() {
        val query = _searchQuery.value.trim()
        if (query.isNotEmpty()) {
            viewModelScope.launch(coroutineDispatcher) {
                executeSearch(query)
            }
        }
    }

    private suspend fun executeSearch(query: String) {
        val requestId = ++currentSearchRequestId
        _uiState.value = SearchUiState.Loading

        SecureLogger.d(tag, "Executing search for token: ${query.take(3)}*** (req #$requestId)")
        when (val result = medicineRepository.searchMedicines(query)) {
            is AppResult.Success -> {
                // Concurrency and race-condition safety: ignore stale out-of-order responses
                if (requestId == currentSearchRequestId) {
                    if (result.data.isEmpty()) {
                        _uiState.value = SearchUiState.Empty(query)
                    } else {
                        _uiState.value = SearchUiState.Success(result.data, query)
                    }
                }
            }
            is AppResult.Error -> {
                if (requestId == currentSearchRequestId) {
                    _uiState.value = SearchUiState.Error(result.error.message)
                }
            }
        }
    }

    fun openMedicineDetails(medicineId: String) {
        viewModelScope.launch {
            _detailState.value = MedicineDetailUiState.Loading
            when (val result = medicineRepository.getMedicineDetails(medicineId)) {
                is AppResult.Success -> {
                    _detailState.value = MedicineDetailUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _detailState.value = MedicineDetailUiState.Error(result.error.message)
                }
            }
        }
    }

    fun closeMedicineDetails() {
        _detailState.value = MedicineDetailUiState.Idle
    }
}
