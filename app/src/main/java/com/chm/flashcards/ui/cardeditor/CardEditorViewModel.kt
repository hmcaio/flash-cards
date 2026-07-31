package com.chm.flashcards.ui.cardeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.CardRepository
import com.chm.flashcards.data.repository.TagRepository
import com.chm.flashcards.ui.navigation.Screen
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

private const val MAX_FRONT_BACK_LENGTH = 1000
private const val MAX_NOTES_LENGTH = 2000
private const val MAX_TAG_NAME_LENGTH = 50
private const val MAX_TAGS = 10

/**
 * `cardId` nav arg is `null`/blank for create mode (nothing loaded), or a
 * parsed [Uuid] for edit mode (loads the existing card + its tags). Parsed
 * from [SavedStateHandle] immediately at this ViewModel entry point, per
 * [Screen]'s nav-arg convention -- `Uuid` is used everywhere below this
 * point.
 */
@HiltViewModel
class CardEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    tagRepository: TagRepository,
) : ViewModel() {

    private val setId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SET_ID]))
    private val cardId: Uuid? = savedStateHandle.get<String>(Screen.ARG_CARD_ID)
        ?.takeIf { it.isNotBlank() }
        ?.let(Uuid::parse)

    /** `true` when editing an existing card (non-null `cardId` nav arg) -- drives the screen title. */
    val isEditMode: Boolean = cardId != null

    private val _uiState = MutableStateFlow(CardEditorUiState())
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    /** Emits once the save completes -- the screen collects this to navigate back. */
    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    private var allTagNames: List<String> = emptyList()

    init {
        viewModelScope.launch {
            tagRepository.getAllTagNames().collect { names ->
                allTagNames = names
                refreshSuggestions()
            }
        }
        val id = cardId
        if (id != null) {
            viewModelScope.launch {
                cardRepository.getCard(id)?.let { existing ->
                    updateState {
                        it.copy(
                            front = existing.card.front,
                            back = existing.card.back,
                            notes = existing.card.notes.orEmpty(),
                            tags = existing.tags.map { tag -> tag.name },
                        )
                    }
                }
            }
        }
    }

    fun onFrontChange(value: String) = updateState { it.copy(front = value) }

    fun onBackChange(value: String) = updateState { it.copy(back = value) }

    fun onNotesChange(value: String) {
        if (value.length > MAX_NOTES_LENGTH) return
        updateState { it.copy(notes = value) }
    }

    /** Comma commits the in-progress input as a chip (per spec.md); any other text just updates the draft. */
    fun onTagInputChange(value: String) {
        if (value.endsWith(",")) {
            addTagChip(value.removeSuffix(","))
        } else {
            updateState { it.copy(tagInput = value) }
            refreshSuggestions()
        }
    }

    /** Enter/Done commits the current draft as a chip. */
    fun onTagInputSubmit() = addTagChip(_uiState.value.tagInput)

    fun onTagSuggestionClick(name: String) = addTagChip(name)

    fun onRemoveTag(name: String) = updateState {
        it.copy(tags = it.tags.filterNot { existing -> existing.equals(name, ignoreCase = true) })
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        val front = state.front.trim()
        val back = state.back.trim()
        val notes = state.notes.trim().ifBlank { null }
        viewModelScope.launch {
            val id = cardId
            if (id != null) {
                cardRepository.updateCard(id, front, back, notes, state.tags)
            } else {
                cardRepository.createCard(setId, front, back, notes, state.tags)
            }
            _saved.emit(Unit)
        }
    }

    /** Trims, validates (non-blank, length, cap, case-insensitive dedupe), then appends -- silently no-ops if invalid. */
    private fun addTagChip(rawName: String) {
        val name = rawName.trim()
        val current = _uiState.value.tags
        val isValid = name.isNotBlank() &&
            name.length <= MAX_TAG_NAME_LENGTH &&
            current.size < MAX_TAGS &&
            current.none { it.equals(name, ignoreCase = true) }
        updateState { state ->
            if (isValid) state.copy(tags = state.tags + name, tagInput = "") else state.copy(tagInput = "")
        }
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        val input = _uiState.value.tagInput.trim()
        val currentTags = _uiState.value.tags
        val suggestions = if (input.isBlank()) {
            emptyList()
        } else {
            allTagNames.filter { name ->
                name.contains(input, ignoreCase = true) && currentTags.none { it.equals(name, ignoreCase = true) }
            }
        }
        _uiState.update { it.copy(tagSuggestions = suggestions) }
    }

    private fun updateState(transform: (CardEditorUiState) -> CardEditorUiState) {
        _uiState.update { validate(transform(it)) }
    }

    private fun validate(state: CardEditorUiState): CardEditorUiState {
        val frontError = fieldError("Front", state.front)
        val backError = fieldError("Back", state.back)
        return state.copy(
            frontError = frontError,
            backError = backError,
            isSaveEnabled = frontError == null && backError == null,
        )
    }

    private fun fieldError(label: String, value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> "$label cannot be blank"
            trimmed.length > MAX_FRONT_BACK_LENGTH -> "$label must be $MAX_FRONT_BACK_LENGTH characters or fewer"
            else -> null
        }
    }
}
