package com.chm.flashcards.ui.setlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.ui.theme.FlashcardsTheme
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * F02 Set List screen: list of sets (name + live card count), create/rename/delete,
 * empty state, tap a set to navigate to Set Detail (still a placeholder until F03).
 *
 * C004: rows are now Material3 [Card]s, with a list/grid toggle (shared global
 * preference, see [SetListViewModel]) and per-row Rename/Delete actions moved
 * into a [DropdownMenu] behind a `MoreVert` icon button.
 */
@Composable
fun SetListScreen(
    onSetClick: (Uuid) -> Unit,
    onImportExportClick: () -> Unit,
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
        onImportExportClick = onImportExportClick,
        onViewModeToggle = viewModel::onViewModeToggle,
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
    onImportExportClick: () -> Unit,
    onViewModeToggle: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                modifier = Modifier.testTag("createSetFab"),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create set")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewModeToggleButton(viewMode = uiState.viewMode, onToggle = onViewModeToggle)
                TextButton(
                    onClick = onImportExportClick,
                    modifier = Modifier.testTag("importExportButton"),
                ) {
                    Text("Import / Export")
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.sets.isEmpty()) {
                    Text("No sets yet", modifier = Modifier.align(Alignment.Center))
                } else {
                    SetListContent(
                        sets = uiState.sets,
                        viewMode = uiState.viewMode,
                        onSetClick = onSetClick,
                        onRename = onRename,
                        onDeleteRequest = onDeleteRequest,
                    )
                }
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

/**
 * Icon button toggling the shared [ViewMode] preference. Shows the icon for
 * the mode a tap would switch *into*, per the common "target state" toggle
 * convention (e.g. a grid icon while currently in list mode).
 */
@Composable
private fun ViewModeToggleButton(viewMode: ViewMode, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.testTag("viewModeToggle")) {
        if (viewMode == ViewMode.GRID) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ViewList, contentDescription = "Switch to list view")
        } else {
            Icon(imageVector = Icons.Default.GridView, contentDescription = "Switch to grid view")
        }
    }
}

@Composable
private fun SetListContent(
    sets: List<CardSetWithCount>,
    viewMode: ViewMode,
    onSetClick: (Uuid) -> Unit,
    onRename: (Uuid, String) -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<CardSetWithCount?>(null) }
    var renameText by remember { mutableStateOf("") }

    val startRename: (CardSetWithCount) -> Unit = { set ->
        renameTarget = set
        renameText = set.name
    }

    when (viewMode) {
        ViewMode.LIST -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sets, key = { it.id.toString() }) { set ->
                SetListItem(
                    set = set,
                    isGrid = false,
                    onClick = { onSetClick(set.id) },
                    onRenameClick = { startRename(set) },
                    onDeleteClick = { onDeleteRequest(set.id) },
                )
            }
        }

        ViewMode.GRID -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems(sets, key = { it.id.toString() }) { set ->
                SetListItem(
                    set = set,
                    isGrid = true,
                    onClick = { onSetClick(set.id) },
                    onRenameClick = { startRename(set) },
                    onDeleteClick = { onDeleteRequest(set.id) },
                )
            }
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

/**
 * Single set row/cell, shared by list and grid mode. `isGrid` switches
 * between a full-width trailing-actions row (list) and a compact
 * corner-actions layout suited to a narrower grid cell -- both expose the
 * same tap-to-open and Rename/Delete dropdown actions.
 */
@Composable
private fun SetListItem(
    set: CardSetWithCount,
    isGrid: Boolean,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setListItem_${set.id}"),
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SetActionsMenu(setId = set.id, onRenameClick = onRenameClick, onDeleteClick = onDeleteClick)
                }
                Text(set.name, style = MaterialTheme.typography.titleMedium)
                Text("${set.cardCount} cards", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(set.name, style = MaterialTheme.typography.titleMedium)
                    Text("${set.cardCount} cards", style = MaterialTheme.typography.bodySmall)
                }
                SetActionsMenu(setId = set.id, onRenameClick = onRenameClick, onDeleteClick = onDeleteClick)
            }
        }
    }
}

@Composable
private fun SetActionsMenu(setId: Uuid, onRenameClick: () -> Unit, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("setMenuButton_$setId")) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Set actions")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    expanded = false
                    onRenameClick()
                },
                modifier = Modifier.testTag("renameMenuItem_$setId"),
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
                modifier = Modifier.testTag("deleteMenuItem_$setId"),
            )
        }
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

@Preview(name = "Set List - grid (default)", showBackground = true)
@Composable
private fun SetListScreenGridPreview() {
    FlashcardsTheme {
        SetListScreen(
            uiState = SetListUiState(sets = previewSets, viewMode = ViewMode.GRID),
            onCreateClick = {},
            onCreateNameChange = {},
            onCreateConfirm = {},
            onCreateDialogDismiss = {},
            onSetClick = {},
            onRename = { _, _ -> },
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onImportExportClick = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set List - list", showBackground = true)
@Composable
private fun SetListScreenListPreview() {
    FlashcardsTheme {
        SetListScreen(
            uiState = SetListUiState(sets = previewSets, viewMode = ViewMode.LIST),
            onCreateClick = {},
            onCreateNameChange = {},
            onCreateConfirm = {},
            onCreateDialogDismiss = {},
            onSetClick = {},
            onRename = { _, _ -> },
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onImportExportClick = {},
            onViewModeToggle = {},
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
            onImportExportClick = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set List row - dropdown open", showBackground = true)
@Composable
private fun SetListRowMenuOpenPreview() {
    FlashcardsTheme {
        Box {
            SetListItem(set = previewSets[0], isGrid = false, onClick = {}, onRenameClick = {}, onDeleteClick = {})
            // DropdownMenu's open/closed state is transient per-item state, so it can't be
            // statically forced open from here -- this preview just shows the closed row;
            // see the chore notes for why a "menu open" preview isn't forced.
        }
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
