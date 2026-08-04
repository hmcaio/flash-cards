package com.chm.flashcards.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.importexport.SetExportDto
import com.chm.flashcards.data.repository.ImportMode
import com.chm.flashcards.ui.theme.FlashcardsTheme

/**
 * F07 Import/Export screen: Export writes the whole library to a
 * SAF-picked (`ACTION_CREATE_DOCUMENT`) file, Import reads a SAF-picked
 * (`ACTION_OPEN_DOCUMENT`) file, validates it, and on success prompts for
 * Replace-all vs. Add-as-new-sets before writing; a validation failure shows
 * an inline error instead. Reached from Set List's "Import / Export" entry
 * point (see `SetListScreen`).
 */
@Composable
fun ImportExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::onExportUriSelected) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onImportUriSelected) }

    ImportExportScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onExportClick = { exportLauncher.launch("flashcards-export.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onImportModeConfirmed = viewModel::onImportModeConfirmed,
        onImportModeCancelled = viewModel::onImportModeCancelled,
        onMessageDismissed = viewModel::onMessageDismissed,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun ImportExportScreen(
    uiState: ImportExportUiState,
    onNavigateBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onImportModeConfirmed: (ImportMode) -> Unit,
    onImportModeCancelled: () -> Unit,
    onMessageDismissed: () -> Unit,
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
                TextButton(onClick = onNavigateBack, modifier = Modifier.testTag("importExportBackButton")) {
                    Text("Back")
                }
                Text("Import / Export", style = MaterialTheme.typography.titleMedium)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Export your whole library to a JSON file, or import one to add or replace your sets.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onExportClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exportButton"),
                ) {
                    Text("Export library")
                }
                Button(
                    onClick = onImportClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("importButton"),
                ) {
                    Text("Import library")
                }
                // Inline text rather than a transient snackbar -- simplest thing that works for
                // a personal, single-user app. Tap-to-dismiss via onMessageDismissed.
                uiState.errorMessage?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable(onClick = onMessageDismissed)
                            .testTag("importExportError"),
                    )
                }
                uiState.successMessage?.let { message ->
                    Text(
                        message,
                        modifier = Modifier
                            .clickable(onClick = onMessageDismissed)
                            .testTag("importExportSuccess"),
                    )
                }
            }
        }
    }

    uiState.pendingImport?.let { library ->
        ImportModeDialog(
            setCount = library.sets.size,
            onReplaceAll = { onImportModeConfirmed(ImportMode.ReplaceAll) },
            onAddAsNewSets = { onImportModeConfirmed(ImportMode.AddAsNewSets) },
            onDismiss = onImportModeCancelled,
        )
    }
}

@Composable
private fun ImportModeDialog(
    setCount: Int,
    onReplaceAll: () -> Unit,
    onAddAsNewSets: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import $setCount set${if (setCount == 1) "" else "s"}") },
        text = { Text("Replace your entire library, or add these as new sets alongside what you already have?") },
        confirmButton = {
            TextButton(onClick = onAddAsNewSets, modifier = Modifier.testTag("addAsNewSetsButton")) {
                Text("Add as new sets")
            }
        },
        dismissButton = {
            TextButton(onClick = onReplaceAll, modifier = Modifier.testTag("replaceAllButton")) {
                Text("Replace all")
            }
        },
    )
}

// --- Previews -------------------------------------------------------------

@Preview(name = "Import/Export - idle", showBackground = true)
@Composable
private fun ImportExportScreenIdlePreview() {
    FlashcardsTheme {
        ImportExportScreen(
            uiState = ImportExportUiState(),
            onNavigateBack = {},
            onExportClick = {},
            onImportClick = {},
            onImportModeConfirmed = {},
            onImportModeCancelled = {},
            onMessageDismissed = {},
        )
    }
}

@Preview(name = "Import/Export - mode choice dialog", showBackground = true)
@Composable
private fun ImportExportScreenModeChoicePreview() {
    FlashcardsTheme {
        ImportExportScreen(
            uiState = ImportExportUiState(
                pendingImport = LibraryExportDto(
                    exportedAt = "2026-07-28T12:00:00Z",
                    sets = listOf(
                        SetExportDto(name = "Kotlin Basics", cards = emptyList()),
                    ),
                ),
            ),
            onNavigateBack = {},
            onExportClick = {},
            onImportClick = {},
            onImportModeConfirmed = {},
            onImportModeCancelled = {},
            onMessageDismissed = {},
        )
    }
}

@Preview(name = "Import/Export - error", showBackground = true)
@Composable
private fun ImportExportScreenErrorPreview() {
    FlashcardsTheme {
        ImportExportScreen(
            uiState = ImportExportUiState(errorMessage = "The selected file isn't a valid export (malformed JSON)"),
            onNavigateBack = {},
            onExportClick = {},
            onImportClick = {},
            onImportModeConfirmed = {},
            onImportModeCancelled = {},
            onMessageDismissed = {},
        )
    }
}

@Preview(name = "Import/Export - success", showBackground = true)
@Composable
private fun ImportExportScreenSuccessPreview() {
    FlashcardsTheme {
        ImportExportScreen(
            uiState = ImportExportUiState(successMessage = "Library imported"),
            onNavigateBack = {},
            onExportClick = {},
            onImportClick = {},
            onImportModeConfirmed = {},
            onImportModeCancelled = {},
            onMessageDismissed = {},
        )
    }
}
