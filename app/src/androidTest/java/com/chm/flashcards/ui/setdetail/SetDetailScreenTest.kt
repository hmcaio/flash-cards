package com.chm.flashcards.ui.setdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.data.preferences.ViewModePreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * F04 acceptance criterion: typing in the search box filters the visible
 * card list, and selecting a tag filter chip narrows it further -- combined
 * with search text, per spec.md's AND semantics. One critical flow covering
 * both search and tag-filter interaction; edge cases (blank query, combined
 * filters, `availableTagFilters` scoping) are covered at the ViewModel
 * unit-test level ([SetDetailViewModelTest]) and DAO level
 * ([com.chm.flashcards.data.dao.CardDaoTest]).
 *
 * C004 additions: the per-card Delete action now goes through the row's
 * dropdown menu instead of an inline button, plus the grid/list toggle --
 * see [com.chm.flashcards.ui.setlist.SetListScreenTest]'s class doc for why
 * the real, DataStore-backed [ViewModePreferences] is used directly here
 * rather than swapped for a test double.
 */
@HiltAndroidTest
class SetDetailScreenTest {

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

    @Inject
    lateinit var viewModePreferences: ViewModePreferences

    private lateinit var kotlinCardId: Uuid
    private lateinit var composeCardId: Uuid
    private lateinit var kotlinTagId: Uuid

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            cardSetDao.getAll().first().forEach { cardSetDao.delete(it) }
            viewModePreferences.setViewMode(ViewMode.GRID)
            val set = CardSetEntity(id = idGenerator.newId(), name = "Kotlin Basics", createdAt = timeProvider.now())
            cardSetDao.insert(set)

            kotlinCardId = idGenerator.newId()
            cardDao.insert(
                CardEntity(
                    id = kotlinCardId,
                    setId = set.id,
                    front = "What is a data class?",
                    back = "Auto equals/hashCode/toString/copy",
                    notes = null,
                ),
            )
            composeCardId = idGenerator.newId()
            cardDao.insert(
                CardEntity(
                    id = composeCardId,
                    setId = set.id,
                    front = "What is Compose?",
                    back = "A declarative UI toolkit",
                    notes = null,
                ),
            )

            kotlinTagId = idGenerator.newId()
            tagDao.insert(TagEntity(id = kotlinTagId, name = "Kotlin"))
            crossRefDao.insert(CardTagCrossRef(cardId = kotlinCardId, tagId = kotlinTagId))
        }
    }

    @Test
    fun typingInSearchBox_filtersVisibleCards() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
        composeRule.onNodeWithText("What is Compose?").assertIsDisplayed()

        composeRule.onNodeWithTag("searchQueryField").performTextInput("Compose")

        composeRule.onNodeWithText("What is Compose?").assertIsDisplayed()
        composeRule.onNodeWithText("What is a data class?").assertDoesNotExist()
    }

    @Test
    fun selectingTagFilterChip_filtersToTaggedCards() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
        composeRule.onNodeWithText("What is Compose?").assertIsDisplayed()

        composeRule.onNodeWithTag("tagFilterChip_$kotlinTagId").performClick()

        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
        composeRule.onNodeWithText("What is Compose?").assertDoesNotExist()
    }

    @Test
    fun dropdownMenu_deleteAction_opensConfirmDialog_andDeletes() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("cardMenuButton_$composeCardId").performClick()
        composeRule.onNodeWithTag("deleteMenuItem_$composeCardId").performClick()
        composeRule.onNodeWithText("Delete card?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("What is Compose?").assertDoesNotExist()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
    }

    @Test
    fun defaultViewMode_isGrid_twoCardsRenderSideBySide() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        val topA = composeRule.onNodeWithTag("cardRow_$kotlinCardId").fetchSemanticsNode().boundsInRoot.top
        val topB = composeRule.onNodeWithTag("cardRow_$composeCardId").fetchSemanticsNode().boundsInRoot.top

        assertEquals(topA, topB, 1f)
    }

    @Test
    fun toggleViewMode_switchesToListLayout_cardsStacked() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("viewModeToggle").performClick()
        waitForToggleDescription("Switch to grid view") // i.e. now showing LIST mode

        val topA = composeRule.onNodeWithTag("cardRow_$kotlinCardId").fetchSemanticsNode().boundsInRoot.top
        val topB = composeRule.onNodeWithTag("cardRow_$composeCardId").fetchSemanticsNode().boundsInRoot.top

        assert(topB > topA + 1f) { "expected list mode to stack cards vertically, got topA=$topA topB=$topB" }
    }

    @Test
    fun toggleViewMode_persistsThroughRealDataStore_sharedWithSetList() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("viewModeToggle").performClick()
        waitForToggleDescription("Switch to grid view") // i.e. now showing LIST mode

        // Reads through the real, DataStore-backed ViewModePreferences directly (bypassing
        // the ViewModel's own cached UiState) to prove the write reached the persisted
        // preference -- and that it's the SAME global value Set List reads (single
        // DataStore-backed preference, not two independent per-screen ones).
        val persisted = runBlocking { viewModePreferences.viewMode.first() }
        assertEquals(ViewMode.LIST, persisted)
    }

    /**
     * The toggle click only *starts* the write (`viewModelScope.launch { setViewMode(...) }`);
     * the DataStore write itself completes on its own dispatcher, asynchronously from Compose's
     * click handling -- see [com.chm.flashcards.ui.setlist.SetListScreenTest]'s copy of this
     * helper for the full explanation.
     */
    private fun waitForToggleDescription(description: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
