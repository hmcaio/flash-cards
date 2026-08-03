package com.chm.flashcards.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F06: shared correct/incorrect card list, extracted out of F05's
 * `SessionResultsScreen` so History Detail (F06) can render a past session
 * with the exact same layout as a just-finished one (spec.md "History Detail
 * reuses Session Results layout"). Two sections, correct cards then
 * incorrect, each hidden entirely when empty -- same behavior as the
 * pre-extraction inline version.
 */
@Composable
fun SessionOutcomeView(correct: List<Card>, incorrect: List<Card>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (correct.isNotEmpty()) {
            item { SectionHeader("Correct") }
            items(correct, key = { "correct_${it.id}" }) { card ->
                ResultCardRow(card)
                HorizontalDivider()
            }
        }
        if (incorrect.isNotEmpty()) {
            item { SectionHeader("Incorrect") }
            items(incorrect, key = { "incorrect_${it.id}" }) { card ->
                ResultCardRow(card)
                HorizontalDivider()
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

@Preview(name = "Session Outcome - mixed", showBackground = true)
@Composable
private fun SessionOutcomeViewMixedPreview() {
    FlashcardsTheme {
        SessionOutcomeView(
            correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
            incorrect = listOf(previewCard("What is a sealed class?")),
        )
    }
}

@Preview(name = "Session Outcome - all correct", showBackground = true)
@Composable
private fun SessionOutcomeViewAllCorrectPreview() {
    FlashcardsTheme {
        SessionOutcomeView(
            correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
            incorrect = emptyList(),
        )
    }
}

@Preview(name = "Session Outcome - all incorrect", showBackground = true)
@Composable
private fun SessionOutcomeViewAllIncorrectPreview() {
    FlashcardsTheme {
        SessionOutcomeView(
            correct = emptyList(),
            incorrect = listOf(previewCard("What is a sealed class?")),
        )
    }
}
