package com.chm.flashcards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chm.flashcards.data.repository.Tag
import com.chm.flashcards.ui.theme.FlashcardsTheme
import kotlin.uuid.Uuid

/**
 * C008: shared tag filter chip row, extracted out of [com.chm.flashcards.ui.setdetail.SetDetailScreen]'s
 * (single-select) and [com.chm.flashcards.ui.sessionconfig.SessionConfigScreen]'s (multi-select,
 * OR semantics) previously near-identical private implementations. Both screens had the same
 * scaling problem: a single-line `Row` wrapped in `Modifier.horizontalScroll(...)`, which becomes
 * an endless horizontal scroll with no wrapping once a set/session has many tags.
 *
 * This version uses [FlowRow] so chips wrap onto multiple lines instead of scrolling
 * horizontally, bounded by [maxHeight] + an internal [Modifier.verticalScroll] so a very large
 * number of tags caps its own height (with its own vertical scroll) rather than pushing the
 * rest of the screen's content down or off-screen.
 *
 * Selection semantics are left entirely to the caller via [isSelected]/[onToggle], so this one
 * composable serves both the single-select (Set Detail) and multi-select (Session Config) call
 * sites: single-select passes `isSelected = { it == selectedTagFilter }` and toggles by clearing
 * the selection when the same tag is tapped again; multi-select passes `isSelected = { it in
 * selectedTagIds }` and forwards `onToggle` directly.
 *
 * Preserves the exact `testTag`s both prior implementations used
 * (`"tagFilterChipRow"` on the container, `"tagFilterChip_${tag.id}"` on each chip) so existing
 * instrumented tests keep passing unmodified.
 */
@Composable
fun TagFilterChipRow(
    tags: List<Tag>,
    isSelected: (Uuid) -> Boolean,
    onToggle: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 120.dp,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .testTag("tagFilterChipRow"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val selected = isSelected(tag.id)
            FilterChip(
                selected = selected,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) },
                modifier = Modifier.testTag("tagFilterChip_${tag.id}"),
            )
        }
    }
}

// --- Previews -------------------------------------------------------------

private val previewTags = listOf(
    Tag(Uuid.random(), "Kotlin"),
    Tag(Uuid.random(), "Basics"),
    Tag(Uuid.random(), "Compose"),
)

private val manyPreviewTags = (1..30).map { i -> Tag(Uuid.random(), "Tag $i") }

@Preview(name = "Tag filter chip row - a handful of tags", showBackground = true)
@Composable
private fun TagFilterChipRowPreview() {
    val selectedId = previewTags[0].id
    FlashcardsTheme {
        TagFilterChipRow(
            tags = previewTags,
            isSelected = { it == selectedId },
            onToggle = {},
        )
    }
}

/**
 * The important preview for this chore: 30+ tags demonstrate wrapping onto multiple lines plus
 * the bounded max height with internal vertical scroll, instead of an endless horizontal scroll
 * or the row growing to push the rest of the screen off-screen.
 */
@Preview(name = "Tag filter chip row - large quantity of tags (30+)", showBackground = true)
@Composable
private fun TagFilterChipRowManyTagsPreview() {
    FlashcardsTheme {
        TagFilterChipRow(
            tags = manyPreviewTags,
            isSelected = { false },
            onToggle = {},
        )
    }
}
