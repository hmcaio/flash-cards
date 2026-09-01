package com.chm.flashcards.ui.sessionconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chm.flashcards.data.repository.Tag
import com.chm.flashcards.ui.components.TagFilterChipRow
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * F05 Session Config screen: slider for how many cards to practice (1..setSize,
 * default `min(10, setSize)`), Start button -> Session Play. Only reachable when
 * the set has at least one card (spec.md "Start Practice" is disabled otherwise
 * on Set Detail), so there's no "0 cards" empty state here.
 *
 * C002 addition: an optional multi-select tag filter chip row (OR semantics),
 * above the slider. Selecting any tag narrows `setSize`/`selectedCount` to the
 * filtered pool; no tags selected (the default) keeps every card in the set
 * eligible, unchanged from before. C008 extracted the chip row itself into
 * the shared [TagFilterChipRow] composable, also used by Set Detail's
 * single-select tag filter (`ui/setdetail/SetDetailScreen.kt`).
 */
@Composable
fun SessionConfigScreen(
    onStartSession: () -> Unit,
    viewModel: SessionConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToSessionPlay.collect { onStartSession() }
    }

    SessionConfigScreen(
        uiState = uiState,
        onCountChange = viewModel::onCountChange,
        onTagToggle = viewModel::onTagToggle,
        onStartClick = viewModel::onStartClick,
    )
}

/** Stateless content, hoisted out of the [hiltViewModel]-backed overload above so it's previewable. */
@Composable
private fun SessionConfigScreen(
    uiState: SessionConfigUiState,
    onCountChange: (Int) -> Unit,
    onTagToggle: (Uuid) -> Unit,
    onStartClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text("Practice session", style = MaterialTheme.typography.headlineSmall)
            if (uiState.availableTags.isNotEmpty()) {
                TagFilterChipRow(
                    tags = uiState.availableTags,
                    isSelected = { it in uiState.selectedTagIds },
                    onToggle = onTagToggle,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Text(
                "${uiState.selectedCount} of ${uiState.setSize} cards",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("selectedCountText"),
            )
            Slider(
                value = uiState.selectedCount.toFloat(),
                onValueChange = { onCountChange(it.toInt()) },
                valueRange = 1f..maxOf(1, uiState.setSize).toFloat(),
                steps = maxOf(0, uiState.setSize - 2),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cardCountSlider"),
            )
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("startSessionButton"),
            ) {
                Text("Start")
            }
        }
    }
}

// --- Previews -------------------------------------------------------------

private val kotlinTag = Tag(Uuid.random(), "Kotlin")
private val basicsTag = Tag(Uuid.random(), "Basics")
private val previewAvailableTags = listOf(kotlinTag, basicsTag)

@Preview(name = "Session Config - default", showBackground = true)
@Composable
private fun SessionConfigScreenDefaultPreview() {
    FlashcardsTheme {
        SessionConfigScreen(
            uiState = SessionConfigUiState(setSize = 20, selectedCount = 10),
            onCountChange = {},
            onTagToggle = {},
            onStartClick = {},
        )
    }
}

@Preview(name = "Session Config - small set", showBackground = true)
@Composable
private fun SessionConfigScreenSmallSetPreview() {
    FlashcardsTheme {
        SessionConfigScreen(
            uiState = SessionConfigUiState(setSize = 3, selectedCount = 3),
            onCountChange = {},
            onTagToggle = {},
            onStartClick = {},
        )
    }
}

@Preview(name = "Session Config - min selected", showBackground = true)
@Composable
private fun SessionConfigScreenMinSelectedPreview() {
    FlashcardsTheme {
        SessionConfigScreen(
            uiState = SessionConfigUiState(setSize = 20, selectedCount = 1),
            onCountChange = {},
            onTagToggle = {},
            onStartClick = {},
        )
    }
}

/** C002: tag filter chip row visible, no tag selected yet -- full set still eligible. */
@Preview(name = "Session Config - tag filter available", showBackground = true)
@Composable
private fun SessionConfigScreenTagFilterAvailablePreview() {
    FlashcardsTheme {
        SessionConfigScreen(
            uiState = SessionConfigUiState(
                setSize = 20,
                selectedCount = 10,
                availableTags = previewAvailableTags,
            ),
            onCountChange = {},
            onTagToggle = {},
            onStartClick = {},
        )
    }
}

/** C002: "Kotlin" tag selected, narrowing the eligible pool -- setSize/selectedCount reflect the filtered count. */
@Preview(name = "Session Config - tag filter selected", showBackground = true)
@Composable
private fun SessionConfigScreenTagFilterSelectedPreview() {
    FlashcardsTheme {
        SessionConfigScreen(
            uiState = SessionConfigUiState(
                setSize = 7,
                selectedCount = 7,
                availableTags = previewAvailableTags,
                selectedTagIds = setOf(kotlinTag.id),
            ),
            onCountChange = {},
            onTagToggle = {},
            onStartClick = {},
        )
    }
}
