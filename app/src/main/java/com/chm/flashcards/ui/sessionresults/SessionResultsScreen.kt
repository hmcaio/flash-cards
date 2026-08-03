package com.chm.flashcards.ui.sessionresults

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F05 Session Results screen: score header ("X/Y correct") and two lists,
 * correct cards then incorrect cards.
 */
@Composable
fun SessionResultsScreen(
    viewModel: SessionResultsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SessionResultsScreen(uiState = uiState)
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SessionResultsScreen(uiState: SessionResultsUiState) {
    val total = uiState.correct.size + uiState.incorrect.size
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                "${uiState.correct.size}/$total correct",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("scoreSummaryText"),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (uiState.correct.isNotEmpty()) {
                    item { SectionHeader("Correct") }
                    items(uiState.correct, key = { "correct_${it.id}" }) { card ->
                        ResultCardRow(card)
                        HorizontalDivider()
                    }
                }
                if (uiState.incorrect.isNotEmpty()) {
                    item { SectionHeader("Incorrect") }
                    items(uiState.incorrect, key = { "incorrect_${it.id}" }) { card ->
                        ResultCardRow(card)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultCardRow(card: Card) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(card.front, style = MaterialTheme.typography.bodyLarge)
        Text(card.back, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- Previews -------------------------------------------------------------

private fun previewCard(front: String) = Card(
    id = Uuid.random(),
    setId = Uuid.random(),
    front = front,
    back = "Back for $front",
    notes = null,
)

@Preview(name = "Session Results - mixed", showBackground = true)
@Composable
private fun SessionResultsScreenMixedPreview() {
    FlashcardsTheme {
        SessionResultsScreen(
            uiState = SessionResultsUiState(
                correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
                incorrect = listOf(previewCard("What is a sealed class?")),
            ),
        )
    }
}

@Preview(name = "Session Results - all correct", showBackground = true)
@Composable
private fun SessionResultsScreenAllCorrectPreview() {
    FlashcardsTheme {
        SessionResultsScreen(
            uiState = SessionResultsUiState(
                correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
                incorrect = emptyList(),
            ),
        )
    }
}

@Preview(name = "Session Results - all incorrect", showBackground = true)
@Composable
private fun SessionResultsScreenAllIncorrectPreview() {
    FlashcardsTheme {
        SessionResultsScreen(
            uiState = SessionResultsUiState(
                correct = emptyList(),
                incorrect = listOf(previewCard("What is a sealed class?")),
            ),
        )
    }
}
