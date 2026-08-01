package com.chm.flashcards.ui.setdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
 */
@Composable
fun SetDetailScreen(
    onCardClick: (Uuid?) -> Unit,
    onStartPracticeClick: () -> Unit,
    viewModel: SetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToCardEditor.collect { cardId -> onCardClick(cardId) }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToSessionConfig.collect { onStartPracticeClick() }
    }

    SetDetailScreen(
        uiState = uiState,
        onAddCardClick = viewModel::onAddCardClick,
        onCardRowClick = viewModel::onCardClick,
        onStartPracticeClick = viewModel::onStartPracticeClick,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteCancel = viewModel::onDeleteCancel,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onTagFilterSelect = viewModel::onTagFilterSelect,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SetDetailScreen(
    uiState: SetDetailUiState,
    onAddCardClick: () -> Unit,
    onCardRowClick: (Uuid) -> Unit,
    onStartPracticeClick: () -> Unit,
    onDeleteRequest: (Uuid) -> Unit,
    onDeleteConfirm: (Uuid) -> Unit,
    onDeleteCancel: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTagFilterSelect: (Uuid?) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCardClick,
                modifier = Modifier.testTag("addCardFab"),
            ) {
                Text("+")
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
            Button(
                onClick = onStartPracticeClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("startPracticeButton"),
            ) {
                Text("Start Practice")
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.cards, key = { it.card.id.toString() }) { cardWithTags ->
                            CardRow(
                                cardWithTags = cardWithTags,
                                onClick = { onCardRowClick(cardWithTags.card.id) },
                                onDeleteClick = { onDeleteRequest(cardWithTags.card.id) },
                            )
                            HorizontalDivider()
                        }
                    }
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

@Composable
private fun CardRow(
    cardWithTags: CardWithTags,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cardRow_${cardWithTags.card.id}")
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        TextButton(onClick = onDeleteClick, modifier = Modifier.testTag("deleteCardButton_${cardWithTags.card.id}")) {
            Text("Delete")
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

@Preview(name = "Set Detail - populated", showBackground = true)
@Composable
private fun SetDetailScreenPopulatedPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(
                setName = "Kotlin Basics",
                cards = previewCards,
                isLoading = false,
                availableTagFilters = previewTagFilters,
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
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
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
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
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
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
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
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
            ),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onSearchQueryChange = {},
            onTagFilterSelect = {},
        )
    }
}

@Preview(name = "Card row", showBackground = true)
@Composable
private fun CardRowPreview() {
    FlashcardsTheme {
        CardRow(cardWithTags = previewCards[0], onClick = {}, onDeleteClick = {})
    }
}

@Preview(name = "Delete card confirm dialog", showBackground = true)
@Composable
private fun ConfirmDeleteDialogPreview() {
    FlashcardsTheme {
        ConfirmDeleteDialog(cardFront = "What is a data class?", onConfirm = {}, onDismiss = {})
    }
}
