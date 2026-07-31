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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
 * Search/tag-filter UI is F04's job, not this screen's.
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.cards.isEmpty()) {
                    Text("No cards yet", modifier = Modifier.align(Alignment.Center))
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

private val previewCards = listOf(
    CardWithTags(
        card = Card(Uuid.random(), Uuid.random(), "What is a data class?", "Auto equals/hashCode/toString/copy", null),
        tags = listOf(Tag(Uuid.random(), "Kotlin"), Tag(Uuid.random(), "Basics")),
    ),
    CardWithTags(
        card = Card(Uuid.random(), Uuid.random(), "What is Compose?", "A declarative UI toolkit", null),
        tags = emptyList(),
    ),
)

@Preview(name = "Set Detail - populated", showBackground = true)
@Composable
private fun SetDetailScreenPopulatedPreview() {
    FlashcardsTheme {
        SetDetailScreen(
            uiState = SetDetailUiState(setName = "Kotlin Basics", cards = previewCards, isLoading = false),
            onAddCardClick = {},
            onCardRowClick = {},
            onStartPracticeClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
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
