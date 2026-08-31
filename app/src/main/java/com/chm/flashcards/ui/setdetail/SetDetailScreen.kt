package com.chm.flashcards.ui.setdetail

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.Tag
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F03 Set Detail screen: replaces F01's placeholder. Shows the set's cards
 * (front + tag chips), tap a row -> Card Editor (edit mode), FAB -> Card
 * Editor (create mode), delete with confirm, "Start Practice" button
 * navigating to Session Config (still a placeholder until F05).
 * F04 adds a search text field + single-select tag filter chip row above
 * the card list, filtering it in place.
 *
 * C004: rows are now Material3 [Card]s, with a list/grid toggle (shared
 * global preference, see [SetDetailViewModel]) and the per-row Delete action
 * moved into a [DropdownMenu] behind a `MoreVert` icon button (there's no
 * Rename action here -- cards are edited via the Card Editor, not renamed
 * in place).
 */
@Composable
fun SetDetailScreen(
    onCardClick: (Uuid?) -> Unit,
    onStartPracticeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: SetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToCardEditor.collect { cardId -> onCardClick(cardId) }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToSessionConfig.collect { onStartPracticeClick() }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToHistoryList.collect { onHistoryClick() }
    }

    SetDetailScreen(
        uiState = uiState,
        onAddCardClick = viewModel::onAddCardClick,
        onCardRowClick = viewModel::onCardClick,
        onStartPracticeClick = viewModel::onStartPracticeClick,
        onHistoryClick = viewModel::onHistoryClick,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteCancel = viewModel::onDeleteCancel,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onTagFilterSelect = viewModel::onTagFilterSelect,
        onViewModeToggle = viewModel::onViewModeToggle,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SetDetailScreen(
    uiState: SetDetailUiState,
    onAddCardClick: () -> Unit,
    onCardRowClick: (Uuid) -> Unit,
    onStartPracticeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
    onDeleteConfirm: (Uuid) -> Unit,
    onDeleteCancel: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTagFilterSelect: (Uuid?) -> Unit,
    onViewModeToggle: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCardClick,
                modifier = Modifier.testTag("addCardFab"),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add card")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                uiState.setName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onStartPracticeClick,
                        enabled = uiState.hasCards,
                        modifier = Modifier.testTag("startPracticeButton"),
                    ) {
                        Text("Start Practice")
                    }
                    TextButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.testTag("historyButton"),
                    ) {
                        Text("History")
                    }
                }
                ViewModeToggleButton(viewMode = uiState.viewMode, onToggle = onViewModeToggle)
            }
            if (!uiState.hasCards) {
                Text(
                    "Add a card to start a practice session",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("startPracticeHint"),
                )
            }
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search cards") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("searchQueryField"),
            )
            if (uiState.availableTagFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .testTag("tagFilterChipRow"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableTagFilters.forEach { tag ->
                        val selected = uiState.selectedTagFilter == tag.id
                        FilterChip(
                            selected = selected,
                            onClick = { onTagFilterSelect(if (selected) null else tag.id) },
                            label = { Text(tag.name) },
                            modifier = Modifier.testTag("tagFilterChip_${tag.id}"),
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.cards.isEmpty()) {
                    val isFiltering = uiState.searchQuery.isNotBlank() || uiState.selectedTagFilter != null
                    Text(
                        if (isFiltering) "No matching cards" else "No cards yet",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("emptyCardsMessage"),
                    )
                } else {
                    CardListContent(
                        cards = uiState.cards,
                        viewMode = uiState.viewMode,
                        onCardRowClick = onCardRowClick,
                        onDeleteRequest = onDeleteRequest,
                    )
                }
            }
        }
    }

    uiState.pendingDelete?.let { pending ->
        ConfirmDeleteDialog(
            cardFront = pending.card.front,
            onConfirm = { onDeleteConfirm(pending.card.id) },
            onDismiss = onDeleteCancel,
        )
    }
}

/**
 * Icon button toggling the shared [ViewMode] preference. Shows the icon for
 * the mode a tap would switch *into*, per the common "target state" toggle
 * convention (e.g. a grid icon while currently in list mode) -- same
 * behavior as [com.chm.flashcards.ui.setlist.SetListScreen]'s toggle, since
 * this is one global preference.
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
private fun CardListContent(
    cards: List<CardWithTags>,
    viewMode: ViewMode,
    onCardRowClick: (Uuid) -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
) {
    when (viewMode) {
        ViewMode.LIST -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { it.card.id.toString() }) { cardWithTags ->
                CardRow(
                    cardWithTags = cardWithTags,
                    isGrid = false,
                    onClick = { onCardRowClick(cardWithTags.card.id) },
                    onDeleteClick = { onDeleteRequest(cardWithTags.card.id) },
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
            gridItems(cards, key = { it.card.id.toString() }) { cardWithTags ->
                CardRow(
                    cardWithTags = cardWithTags,
                    isGrid = true,
                    onClick = { onCardRowClick(cardWithTags.card.id) },
                    onDeleteClick = { onDeleteRequest(cardWithTags.card.id) },
                )
            }
        }
    }
}

/**
 * Single card row/cell, shared by list and grid mode. `isGrid` switches
 * between a full-width trailing-actions row (list) and a compact
 * corner-actions layout suited to a narrower grid cell -- both expose the
 * same tap-to-open and Delete dropdown action.
 */
@Composable
private fun CardRow(
    cardWithTags: CardWithTags,
    isGrid: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cardRow_${cardWithTags.card.id}"),
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CardActionsMenu(cardId = cardWithTags.card.id, onDeleteClick = onDeleteClick)
                }
                Text(cardWithTags.card.front, style = MaterialTheme.typography.bodyLarge)
                if (cardWithTags.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        cardWithTags.tags.forEach { tag -> TagBadge(tag.name) }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cardWithTags.card.front, style = MaterialTheme.typography.bodyLarge)
                    if (cardWithTags.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            cardWithTags.tags.forEach { tag -> TagBadge(tag.name) }
                        }
                    }
                }
                CardActionsMenu(cardId = cardWithTags.card.id, onDeleteClick = onDeleteClick)
            }
        }
    }
}

/**
 * A single-item dropdown ("Delete" only -- there's no Rename action for
 * cards). Kept as the same `MoreVert` + [DropdownMenu] interaction pattern
 * as [com.chm.flashcards.ui.setlist.SetListScreen]'s two-item menu, for UI
 * consistency across both screens per this chore's intent, rather than a
 * bare icon button that deletes directly.
 */
@Composable
private fun CardActionsMenu(cardId: Uuid, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("cardMenuButton_$cardId")) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Card actions")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
                modifier = Modifier.testTag("deleteMenuItem_$cardId"),
            )
        }
    }
}

@Composable
private fun TagBadge(name: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(cardFront: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete card?") },
        text = { Text("\"$cardFront\" will be deleted") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// --- Previews -------------------------------------------------------------
// Sample data only -- previews aren't real app logic, so unlike the ViewModel
// they call Uuid.random() directly rather than going through IdGenerator.

private val kotlinTag = Tag(Uuid.random(), "Kotlin")
private val basicsTag = Tag(Uuid.random(), "Basics")

private val previewCardDataClass = CardWithTags(
    card = Card(Uuid.random(), Uuid.random(), "What is a data class?", "Auto equals/hashCode/toString/copy", null),
    tags = listOf(kotlinTag, basicsTag),
)
private val previewCardCompose = CardWithTags(
    card = Card(Uuid.random(), Uuid.random(), "What is Compose?", "A declarative UI toolkit", null),
    tags = emptyList(),
)
private val previewCards = listOf(previewCardDataClass, previewCardCompose)

/** Tags used by [previewCards] -- mirrors how [SetDetailViewModel] derives `availableTagFilters`. */
private val previewTagFilters = previewCards.flatMap { it.tags }.distinctBy { it.id }

@Preview(name = "Set Detail - grid (default)", showBackground = true)
@Composable
private fun SetDetailScreenGridPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = previewCards,
                isLoading = false,
                availableTagFilters = previewTagFilters,
                hasCards = true,
                viewMode = ViewMode.GRID,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set Detail - list", showBackground = true)
@Composable
private fun SetDetailScreenListPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = previewCards,
                isLoading = false,
                availableTagFilters = previewTagFilters,
                hasCards = true,
                viewMode = ViewMode.LIST,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set Detail - empty", showBackground = true)
@Composable
private fun SetDetailScreenEmptyPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(setName = "Kotlin Basics", cards = emptyList(), isLoading = false),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set Detail - search query typed", showBackground = true)
@Composable
private fun SetDetailScreenSearchQueryPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = listOf(previewCardCompose),
                isLoading = false,
                searchQuery = "Compose",
                availableTagFilters = previewTagFilters,
                hasCards = true,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set Detail - tag filter selected", showBackground = true)
@Composable
private fun SetDetailScreenTagFilterSelectedPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = listOf(previewCardDataClass),
                isLoading = false,
                selectedTagFilter = kotlinTag.id,
                availableTagFilters = previewTagFilters,
                hasCards = true,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Set Detail - no matching results", showBackground = true)
@Composable
private fun SetDetailScreenNoResultsPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = emptyList(),
                isLoading = false,
                searchQuery = "xyz",
                availableTagFilters = previewTagFilters,
                hasCards = true,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onHistoryClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
            onViewModeToggle = {},
        )
    }
}

@Preview(name = "Card row", showBackground = true)
@Composable
private fun CardRowPreview() {
    FlashcardsTheme {
        CardRow(cardWithTags = previewCards[0], isGrid = false, onClick = {}, onDeleteClick = {})
    }
}

@Preview(name = "Card cell - grid", showBackground = true)
@Composable
private fun CardGridCellPreview() {
    FlashcardsTheme {
        CardRow(cardWithTags = previewCards[0], isGrid = true, onClick = {}, onDeleteClick = {})
    }
}

@Preview(name = "Delete card confirm dialog", showBackground = true)
@Composable
private fun ConfirmDeleteDialogPreview() {
    FlashcardsTheme {
        ConfirmDeleteDialog(cardFront = "What is a data class?", onConfirm = {}, onDismiss = {})
    }
}
