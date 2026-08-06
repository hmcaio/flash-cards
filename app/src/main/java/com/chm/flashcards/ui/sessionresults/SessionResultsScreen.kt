package com.chm.flashcards.ui.sessionresults

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.ui.session.SessionOutcomeView
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F05 Session Results screen: score header ("X/Y correct") and two lists,
 * correct cards then incorrect cards -- the lists themselves are
 * [SessionOutcomeView] (extracted in F06 so History Detail can reuse the
 * same layout for a past session).
 */
@Composable
fun SessionResultsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SessionResultsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SessionResultsScreen(uiState = uiState, onNavigateBack = onNavigateBack)
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SessionResultsScreen(uiState: SessionResultsUiState, onNavigateBack: () -> Unit) {
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
            TextButton(onClick = onNavigateBack, modifier = Modifier.testTag("backToSetButton")) {
                Text("Back to Set")
            }
            SessionOutcomeView(
                correct = uiState.correct,
                incorrect = uiState.incorrect,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
            onNavigateBack = {},
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
            onNavigateBack = {},
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
            onNavigateBack = {},
        )
    }
}
