package com.chm.flashcards.ui.cardeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.ui.theme.FlashcardsTheme

/**
 * F03 Card Editor screen: front/back/notes fields, tag chip input with
 * autocomplete-from-existing + create-on-type (comma or Enter commits a
 * chip), save/cancel. Used for both create (`cardId` nav arg blank) and edit
 * (`cardId` present) -- see [CardEditorViewModel].
 */
@Composable
fun CardEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onNavigateBack() }
    }

    CardEditorScreen(
        uiState = uiState,
        isEditMode = viewModel.isEditMode,
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onNotesChange = viewModel::onNotesChange,
        onTagInputChange = viewModel::onTagInputChange,
        onTagInputSubmit = viewModel::onTagInputSubmit,
        onTagSuggestionClick = viewModel::onTagSuggestionClick,
        onRemoveTag = viewModel::onRemoveTag,
        onSaveClick = viewModel::onSave,
        onCancelClick = onNavigateBack,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun CardEditorScreen(
    uiState: CardEditorUiState,
    isEditMode: Boolean,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onTagInputSubmit: () -> Unit,
    onTagSuggestionClick: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancelClick, modifier = Modifier.testTag("cardCancelButton")) {
                    Text("Cancel")
                }
                Text(
                    if (isEditMode) "Edit card" else "New card",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = onSaveClick,
                    enabled = uiState.isSaveEnabled,
                    modifier = Modifier.testTag("cardSaveButton"),
                ) {
                    Text("Save")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.front,
                    onValueChange = onFrontChange,
                    label = { Text("Front") },
                    isError = uiState.frontError != null,
                    supportingText = { uiState.frontError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cardFrontField"),
                )
                OutlinedTextField(
                    value = uiState.back,
                    onValueChange = onBackChange,
                    label = { Text("Back") },
                    isError = uiState.backError != null,
                    supportingText = { uiState.backError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cardBackField"),
                )
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cardNotesField"),
                )
                TagChipInput(
                    tags = uiState.tags,
                    tagInput = uiState.tagInput,
                    tagSuggestions = uiState.tagSuggestions,
                    onTagInputChange = onTagInputChange,
                    onTagInputSubmit = onTagInputSubmit,
                    onTagSuggestionClick = onTagSuggestionClick,
                    onRemoveTag = onRemoveTag,
                )
            }
        }
    }
}

/** Tag chip row + input field + suggestions dropdown -- candidate for extraction/reuse per plan.md's refactor step. */
@Composable
private fun TagChipInput(
    tags: List<String>,
    tagInput: String,
    tagSuggestions: List<String>,
    onTagInputChange: (String) -> Unit,
    onTagInputSubmit: () -> Unit,
    onTagSuggestionClick: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    Column {
        Text("Tags", style = MaterialTheme.typography.labelLarge)
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    TagChip(text = tag, onRemove = { onRemoveTag(tag) })
                }
            }
        }
        OutlinedTextField(
            value = tagInput,
            onValueChange = onTagInputChange,
            label = { Text("Add tag (comma or enter to add)") },
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = { onTagInputSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cardTagInputField"),
        )
        if (tagSuggestions.isNotEmpty()) {
            Column(modifier = Modifier.testTag("tagSuggestionsList")) {
                tagSuggestions.forEach { suggestion ->
                    Text(
                        suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagSuggestionClick(suggestion) }
                            .padding(vertical = 8.dp)
                            .testTag("tagSuggestion_$suggestion"),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .clickable(onClick = onRemove)
            .testTag("tagChip_$text"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Text("×", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// --- Previews -------------------------------------------------------------
// Sample data only -- previews aren't real app logic, so unlike the ViewModel
// they construct UiState directly rather than going through the ViewModel.

@Preview(name = "Card Editor - create", showBackground = true)
@Composable
private fun CardEditorScreenCreatePreview() {
    FlashcardsTheme {
        CardEditorScreen(
            uiState = CardEditorUiState(isSaveEnabled = false),
            isEditMode = false,
            onFrontChange = {},
            onBackChange = {},
            onNotesChange = {},
            onTagInputChange = {},
            onTagInputSubmit = {},
            onTagSuggestionClick = {},
            onRemoveTag = {},
            onSaveClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "Card Editor - edit, populated", showBackground = true)
@Composable
private fun CardEditorScreenEditPreview() {
    FlashcardsTheme {
        CardEditorScreen(
            uiState = CardEditorUiState(
                front = "What is a data class?",
                back = "A class that auto-generates equals/hashCode/toString/copy",
                notes = "Common Kotlin interview question",
                tags = listOf("Kotlin", "Basics"),
                isSaveEnabled = true,
            ),
            isEditMode = true,
            onFrontChange = {},
            onBackChange = {},
            onNotesChange = {},
            onTagInputChange = {},
            onTagInputSubmit = {},
            onTagSuggestionClick = {},
            onRemoveTag = {},
            onSaveClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "Card Editor - validation error", showBackground = true)
@Composable
private fun CardEditorScreenErrorPreview() {
    FlashcardsTheme {
        CardEditorScreen(
            uiState = CardEditorUiState(
                front = "",
                back = "",
                frontError = "Front cannot be blank",
                backError = "Back cannot be blank",
                isSaveEnabled = false,
            ),
            isEditMode = false,
            onFrontChange = {},
            onBackChange = {},
            onNotesChange = {},
            onTagInputChange = {},
            onTagInputSubmit = {},
            onTagSuggestionClick = {},
            onRemoveTag = {},
            onSaveClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "Tag chip input - with suggestions", showBackground = true)
@Composable
private fun TagChipInputPreview() {
    FlashcardsTheme {
        TagChipInput(
            tags = listOf("Kotlin"),
            tagInput = "Co",
            tagSuggestions = listOf("Compose", "Coroutines"),
            onTagInputChange = {},
            onTagInputSubmit = {},
            onTagSuggestionClick = {},
            onRemoveTag = {},
        )
    }
}
