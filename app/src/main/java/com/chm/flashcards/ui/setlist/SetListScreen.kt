package com.chm.flashcards.ui.setlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.ui.theme.FlashcardsTheme
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * F02 Set List screen: list of sets (name + live card count), create/rename/delete,
 * empty state, tap a set to navigate to Set Detail (still a placeholder until F03).
 */
@Composable
fun SetListScreen(
    onSetClick: (Uuid) -> Unit,
    viewModel: SetListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToSetDetail.collect { id -> onSetClick(id) }
    }

    SetListScreen(
        uiState = uiState,
        onCreateClick = viewModel::onCreateClick,
        onCreateNameChange = viewModel::onCreateNameChange,
        onCreateConfirm = viewModel::onCreateConfirm,
        onCreateDialogDismiss = viewModel::onCreateDialogDismiss,
        onSetClick = viewModel::onSetClick,
        onRename = viewModel::onRename,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteCancel = viewModel::onDeleteCancel,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SetListScreen(
    uiState: SetListUiState,
    onCreateClick: () -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreateConfirm: () -> Unit,
    onCreateDialogDismiss: () -> Unit,
    onSetClick: (Uuid) -> Unit,
    onRename: (Uuid, String) -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
    onDeleteConfirm: (Uuid) -> Unit,
    onDeleteCancel: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                modifier = Modifier.testTag("createSetFab"),
            ) {
                Text("+")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.sets.isEmpty()) {
                Text("No sets yet", modifier = Modifier.align(Alignment.Center))
            } else {
                SetListContent(
                    sets = uiState.sets,
                    onSetClick = onSetClick,
                    onRename = onRename,
                    onDeleteRequest = onDeleteRequest,
                )
            }
        }
    }

    if (uiState.isCreateDialogOpen) {
        NameInputDialog(
            title = "New set",
            name = uiState.createNameInput,
            error = uiState.createNameError,
            onNameChange = onCreateNameChange,
            onConfirm = onCreateConfirm,
            onDismiss = onCreateDialogDismiss,
            nameFieldTag = "createSetNameField",
            confirmButtonTag = "createSetConfirmButton",
        )
    }

    uiState.pendingDelete?.let { pending ->
        ConfirmDialog(
            title = "Delete set?",
            message = "This set and its ${pending.cardCount} cards will be deleted",
            confirmLabel = "Delete",
            onConfirm = { onDeleteConfirm(pending.id) },
            onDismiss = onDeleteCancel,
        )
    }
}

@Composable
private fun SetListContent(
    sets: List<CardSetWithCount>,
    onSetClick: (Uuid) -> Unit,
    onRename: (Uuid, String) -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<CardSetWithCount?>(null) }
    var renameText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sets, key = { it.id.toString() }) { set ->
            SetListRow(
                set = set,
                onClick = { onSetClick(set.id) },
                onRenameClick = {
                    renameTarget = set
                    renameText = set.name
                },
                onDeleteClick = { onDeleteRequest(set.id) },
            )
            HorizontalDivider()
        }
    }

    renameTarget?.let { target ->
        NameInputDialog(
            title = "Rename set",
            name = renameText,
            error = null,
            onNameChange = { renameText = it },
            onConfirm = {
                onRename(target.id, renameText)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
            nameFieldTag = "renameSetNameField",
            confirmButtonTag = "renameSetConfirmButton",
        )
    }
}

@Composable
private fun SetListRow(
    set: CardSetWithCount,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(set.name, style = MaterialTheme.typography.titleMedium)
            Text("${set.cardCount} cards", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onRenameClick) { Text("Rename") }
        TextButton(onClick = onDeleteClick) { Text("Delete") }
    }
}

/** Shared by the create-set and rename-set flows -- both are just "name in, name out". */
@Composable
private fun NameInputDialog(
    title: String,
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    nameFieldTag: String,
    confirmButtonTag: String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.testTag(nameFieldTag),
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(confirmButtonTag)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Shared confirm/cancel dialog shape -- currently only delete uses it. */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// --- Previews -------------------------------------------------------------
// Sample data only -- previews aren't real app logic, so unlike the ViewModel
// they call Uuid.random()/Instant.now() directly rather than going through
// IdGenerator/TimeProvider.

private val previewSets = listOf(
    CardSetWithCount(Uuid.random(), "Kotlin Basics", Instant.now(), cardCount = 12),
    CardSetWithCount(Uuid.random(), "Android Jetpack", Instant.now(), cardCount = 0),
    CardSetWithCount(Uuid.random(), "Spanish Vocabulary", Instant.now(), cardCount = 47),
)

@Preview(name = "Set List - populated", showBackground = true)
@Composable
private fun SetListScreenPopulatedPreview() {
    FlashcardsTheme {
        SetListScreen(
            uiState = SetListUiState(sets = previewSets),
            onCreateClick = {},
            onCreateNameChange = {},
            onCreateConfirm = {},
            onCreateDialogDismiss = {},
            onSetClick = {},
            onRename = { _, _ -> },
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
        )
    }
}

@Preview(name = "Set List - empty", showBackground = true)
@Composable
private fun SetListScreenEmptyPreview() {
    FlashcardsTheme {
        SetListScreen(
            uiState = SetListUiState(),
            onCreateClick = {},
            onCreateNameChange = {},
            onCreateConfirm = {},
            onCreateDialogDismiss = {},
            onSetClick = {},
            onRename = { _, _ -> },
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
        )
    }
}

@Preview(name = "Set List row", showBackground = true)
@Composable
private fun SetListRowPreview() {
    FlashcardsTheme {
        SetListRow(set = previewSets[0], onClick = {}, onRenameClick = {}, onDeleteClick = {})
    }
}

@Preview(name = "Name input dialog", showBackground = true)
@Composable
private fun NameInputDialogPreview() {
    FlashcardsTheme {
        NameInputDialog(
            title = "New set",
            name = "Kotlin Basics",
            error = null,
            onNameChange = {},
            onConfirm = {},
            onDismiss = {},
            nameFieldTag = "createSetNameField",
            confirmButtonTag = "createSetConfirmButton",
        )
    }
}

@Preview(name = "Name input dialog - error", showBackground = true)
@Composable
private fun NameInputDialogErrorPreview() {
    FlashcardsTheme {
        NameInputDialog(
            title = "New set",
            name = "   ",
            error = "Name can't be blank",
            onNameChange = {},
            onConfirm = {},
            onDismiss = {},
            nameFieldTag = "createSetNameField",
            confirmButtonTag = "createSetConfirmButton",
        )
    }
}

@Preview(name = "Delete confirm dialog", showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    FlashcardsTheme {
        ConfirmDialog(
            title = "Delete set?",
            message = "This set and its 12 cards will be deleted",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
