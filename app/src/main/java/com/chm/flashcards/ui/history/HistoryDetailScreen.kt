package com.chm.flashcards.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.chm.flashcards.ui.session.SessionOutcomeView
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F06 History Detail screen: reuses F05 Session Results' exact layout --
 * score header ("X/Y correct") plus [SessionOutcomeView] -- for a *past*
 * session looked up by id, rather than one just finished (spec.md "reuses
 * the same layout as SessionResultsScreen").
 */
@Composable
fun HistoryDetailScreen(
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryDetailScreen(uiState = uiState)
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun HistoryDetailScreen(uiState: HistoryDetailUiState) {
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
                modifier = Modifier.testTag("historyScoreSummaryText"),
            )
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

@Preview(name = "History Detail - mixed", showBackground = true)
@Composable
private fun HistoryDetailScreenMixedPreview() {
    FlashcardsTheme {
        HistoryDetailScreen(
            uiState = HistoryDetailUiState(
                correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
                incorrect = listOf(previewCard("What is a sealed class?")),
                isLoading = false,
            ),
        )
    }
}

@Preview(name = "History Detail - all correct", showBackground = true)
@Composable
private fun HistoryDetailScreenAllCorrectPreview() {
    FlashcardsTheme {
        HistoryDetailScreen(
            uiState = HistoryDetailUiState(
                correct = listOf(previewCard("What is a data class?"), previewCard("What is Compose?")),
                incorrect = emptyList(),
                isLoading = false,
            ),
        )
    }
}
