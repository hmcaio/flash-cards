package com.chm.flashcards.ui.setlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.data.preferences.ViewModePreferences
import com.chm.flashcards.data.repository.CardSetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_NAME_LENGTH = 100

@HiltViewModel
class SetListViewModel @Inject constructor(
    private val repository: CardSetRepository,
    private val viewModePreferences: ViewModePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetListUiState())
    val uiState: StateFlow<SetListUiState> = _uiState.asStateFlow()

    private val _navigateToSetDetail = MutableSharedFlow<Uuid>()
    val navigateToSetDetail: SharedFlow<Uuid> = _navigateToSetDetail.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllSetsWithCount().collect { sets ->
                _uiState.update { it.copy(sets = sets) }
            }
        }
        viewModelScope.launch {
            viewModePreferences.viewMode.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
    }

    fun onViewModeToggle() {
        val newMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch { viewModePreferences.setViewMode(newMode) }
    }

    fun onCreateClick() {
        _uiState.update {
            it.copy(isCreateDialogOpen = true, createNameInput = "", createNameError = null)
        }
    }

    fun onCreateNameChange(name: String) {
        _uiState.update { it.copy(createNameInput = name, createNameError = null) }
    }

    fun onCreateConfirm() {
        val trimmed = _uiState.value.createNameInput.trim()
        val error = validateName(trimmed)
        if (error != null) {
            _uiState.update { it.copy(createNameError = error) }
            return
        }
        viewModelScope.launch {
            repository.createSet(trimmed)
            _uiState.update {
                it.copy(isCreateDialogOpen = false, createNameInput = "", createNameError = null)
            }
        }
    }

    fun onCreateDialogDismiss() {
        _uiState.update {
            it.copy(isCreateDialogOpen = false, createNameInput = "", createNameError = null)
        }
    }

    fun onRename(id: Uuid, newName: String) {
        val trimmed = newName.trim()
        if (validateName(trimmed) != null) return
        viewModelScope.launch { repository.renameSet(id, trimmed) }
    }

    fun onDeleteRequest(id: Uuid) {
        val target = _uiState.value.sets.find { it.id == id } ?: return
        _uiState.update { it.copy(pendingDelete = target) }
    }

    fun onDeleteConfirm(id: Uuid) {
        viewModelScope.launch {
            repository.deleteSet(id)
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    fun onDeleteCancel() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onSetClick(id: Uuid) {
        viewModelScope.launch { _navigateToSetDetail.emit(id) }
    }

    private fun validateName(name: String): String? = when {
        name.isBlank() -> "Name cannot be blank"
        name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer"
        else -> null
    }
}
