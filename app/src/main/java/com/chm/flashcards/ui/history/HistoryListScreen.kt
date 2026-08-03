package com.chm.flashcards.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.dao.PracticeSessionListItem
import com.chm.flashcards.ui.theme.FlashcardsTheme
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid

/**
 * F06 History List screen: past sessions for one set, newest first, each row
 * showing date + derived score -- tap a row to see its full correct/incorrect
 * breakdown ([HistoryDetailScreen]). Empty state ("No sessions yet") when the
 * set has never been practiced (spec.md acceptance criteria).
 */
@Composable
fun HistoryListScreen(
    onSessionClick: (Uuid) -> Unit,
    viewModel: HistoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHistoryDetail.collect { sessionId -> onSessionClick(sessionId) }
    }

    HistoryListScreen(uiState = uiState, onSessionClick = viewModel::onSessionClick)
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun HistoryListScreen(uiState: HistoryListUiState, onSessionClick: (Uuid) -> Unit) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.sessions.isEmpty()) {
                Text(
                    "No sessions yet",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("emptyHistoryMessage"),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.sessions, key = { it.sessionId.toString() }) { session ->
                        HistorySessionRow(session = session, onClick = { onSessionClick(session.sessionId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionRow(session: PracticeSessionListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("historySessionRow_${session.sessionId}")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatSessionDate(session.startedAt), style = MaterialTheme.typography.bodyLarge)
        Text(
            "${session.correctCount}/${session.totalCount}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("historySessionScore_${session.sessionId}"),
        )
    }
}

private val sessionDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

private fun formatSessionDate(startedAt: Instant): String = sessionDateFormatter.format(startedAt)

// --- Previews -------------------------------------------------------------

private val previewSessions = listOf(
    PracticeSessionListItem(
        sessionId = Uuid.random(),
        startedAt = Instant.parse("2026-02-01T10:15:00Z"),
        correctCount = 4,
        totalCount = 5,
    ),
    PracticeSessionListItem(
        sessionId = Uuid.random(),
        startedAt = Instant.parse("2026-01-20T09:00:00Z"),
        correctCount = 2,
        totalCount = 10,
    ),
)

@Preview(name = "History List - populated", showBackground = true)
@Composable
private fun HistoryListScreenPopulatedPreview() {
    FlashcardsTheme {
        HistoryListScreen(uiState = HistoryListUiState(sessions = previewSessions, isLoading = false), onSessionClick = {})
    }
}

@Preview(name = "History List - empty", showBackground = true)
@Composable
private fun HistoryListScreenEmptyPreview() {
    FlashcardsTheme {
        HistoryListScreen(uiState = HistoryListUiState(sessions = emptyList(), isLoading = false), onSessionClick = {})
    }
}
