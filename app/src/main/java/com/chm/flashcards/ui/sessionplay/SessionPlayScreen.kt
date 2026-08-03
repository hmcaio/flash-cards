package com.chm.flashcards.ui.sessionplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlin.uuid.Uuid

/**
 * F05 Session Play screen: one card at a time, tap to flip (front -> back +
 * notes), then Correct/Incorrect buttons (only shown once flipped, so the
 * user has actually seen the answer before grading themselves), progress
 * indicator ("3/10"). Navigates to Session Results once the last card is
 * answered.
 */
@Composable
fun SessionPlayScreen(
    onSessionComplete: (Uuid) -> Unit,
    viewModel: SessionPlayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToResults.collect { sessionId -> onSessionComplete(sessionId) }
    }

    SessionPlayScreen(
        uiState = uiState,
        onFlip = viewModel::onFlip,
        onAnswer = viewModel::onAnswer,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SessionPlayScreen(
    uiState: SessionPlayUiState,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            val card = uiState.currentCard
            if (card == null) {
                Text("No cards to practice", modifier = Modifier.testTag("noCardsMessage"))
                return@Column
            }

            Text(
                "${uiState.currentIndex + 1}/${uiState.totalCount}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("sessionProgressText"),
            )
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable(onClick = onFlip)
                    .testTag("flipCard"),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(card.front, style = MaterialTheme.typography.headlineSmall)
                    if (uiState.isFlipped) {
                        Text(
                            card.back,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        card.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            if (uiState.isFlipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onAnswer(false) },
                        modifier = Modifier.testTag("incorrectButton"),
                    ) {
                        Text("Incorrect")
                    }
                    Button(
                        onClick = { onAnswer(true) },
                        modifier = Modifier.testTag("correctButton"),
                    ) {
                        Text("Correct")
                    }
                }
            } else {
                Text(
                    "Tap the card to flip",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

// --- Previews -------------------------------------------------------------

private val previewCard = com.chm.flashcards.data.repository.Card(
    id = Uuid.random(),
    setId = Uuid.random(),
    front = "What is a data class?",
    back = "A class that auto-generates equals/hashCode/toString/copy",
    notes = "Introduced in Kotlin 1.0",
)

@Preview(name = "Session Play - front shown", showBackground = true)
@Composable
private fun SessionPlayScreenFrontPreview() {
    FlashcardsTheme {
        SessionPlayScreen(
            uiState = SessionPlayUiState(currentIndex = 2, totalCount = 10, currentCard = previewCard, isFlipped = false),
            onFlip = {},
            onAnswer = {},
        )
    }
}

@Preview(name = "Session Play - flipped to back", showBackground = true)
@Composable
private fun SessionPlayScreenFlippedPreview() {
    FlashcardsTheme {
        SessionPlayScreen(
            uiState = SessionPlayUiState(currentIndex = 2, totalCount = 10, currentCard = previewCard, isFlipped = true),
            onFlip = {},
            onAnswer = {},
        )
    }
}

@Preview(name = "Session Play - near end", showBackground = true)
@Composable
private fun SessionPlayScreenNearEndPreview() {
    FlashcardsTheme {
        SessionPlayScreen(
            uiState = SessionPlayUiState(currentIndex = 9, totalCount = 10, currentCard = previewCard, isFlipped = false),
            onFlip = {},
            onAnswer = {},
        )
    }
}
