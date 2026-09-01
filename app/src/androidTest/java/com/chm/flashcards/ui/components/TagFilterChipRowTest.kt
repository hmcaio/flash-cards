package com.chm.flashcards.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.chm.flashcards.MainActivity
import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.dao.CardTagCrossRefDao
import com.chm.flashcards.data.dao.TagDao
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardSetEntity
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * C008 acceptance test for [TagFilterChipRow]'s "support large quantities of filters" fix.
 * Exercises the real [com.chm.flashcards.ui.setdetail.SetDetailScreen] call site (rather than
 * hosting the composable in isolation) since it's already wired to a real card set + real tags
 * via Hilt/Room, matching the DAO-seeding precedent set by
 * [com.chm.flashcards.ui.setdetail.SetDetailScreenTest] and
 * [com.chm.flashcards.EndToEndPracticeFlowTest].
 *
 * Seeds 18 tags -- all attached to one card, so all 18 surface in `availableTagFilters` -- far
 * more than fit on one screen without wrapping. The two assertions below are the actual proof
 * the chore's intent is met:
 *  1. the row renders without crashing/hanging with that many chips (would previously have been
 *     an endless single-line horizontal scroll), and
 *  2. a chip far down the wrapped/scrolled list -- the *last* of the 18 -- is still reachable via
 *     [performScrollTo] inside the row's own bounded-height internal scroll, and clicking it
 *     still drives real single-select filtering behavior (narrows the card list), proving the
 *     row isn't just rendering but is fully interactive at this scale.
 */
@HiltAndroidTest
class TagFilterChipRowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var cardSetDao: CardSetDao

    @Inject
    lateinit var cardDao: CardDao

    @Inject
    lateinit var tagDao: TagDao

    @Inject
    lateinit var crossRefDao: CardTagCrossRefDao

    @Inject
    lateinit var idGenerator: IdGenerator

    @Inject
    lateinit var timeProvider: TimeProvider

    private lateinit var lastTagId: Uuid

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            cardSetDao.getAll().first().forEach { cardSetDao.delete(it) }

            val set = CardSetEntity(id = idGenerator.newId(), name = "Many Tags Set", createdAt = timeProvider.now())
            cardSetDao.insert(set)

            val taggedCardId = idGenerator.newId()
            cardDao.insert(
                CardEntity(
                    id = taggedCardId,
                    setId = set.id,
                    front = "Card with many tags",
                    back = "Back text",
                    notes = null,
                ),
            )
            cardDao.insert(
                CardEntity(
                    id = idGenerator.newId(),
                    setId = set.id,
                    front = "Untagged card",
                    back = "Back text 2",
                    notes = null,
                ),
            )

            val tagCount = 18
            val tagIds = (1..tagCount).map { i ->
                val tagId = idGenerator.newId()
                tagDao.insert(TagEntity(id = tagId, name = "Tag %02d".format(i)))
                crossRefDao.insert(CardTagCrossRef(cardId = taggedCardId, tagId = tagId))
                tagId
            }
            lastTagId = tagIds.last()
        }
    }

    @Test
    fun manyTags_rowRendersAndLastChipIsReachableAndClickable() {
        composeRule.onNodeWithText("Many Tags Set").performClick()

        // Both cards visible before any filter is applied.
        composeRule.onNodeWithText("Card with many tags").assertIsDisplayed()
        composeRule.onNodeWithText("Untagged card").assertIsDisplayed()

        // The container itself renders without crashing/hanging despite 18 chips.
        composeRule.onNodeWithTag("tagFilterChipRow").assertIsDisplayed()

        // The last of the 18 chips -- deep into the wrapped rows, past what a single screen
        // height would show -- is reachable via the row's own internal vertical scroll, and
        // is a real, clickable chip.
        composeRule.onNodeWithTag("tagFilterChip_$lastTagId").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tagFilterChip_$lastTagId").performClick()

        // Clicking it drives real single-select filtering: only the tagged card remains.
        composeRule.onNodeWithText("Card with many tags").assertIsDisplayed()
        composeRule.onNodeWithText("Untagged card").assertDoesNotExist()
    }
}
