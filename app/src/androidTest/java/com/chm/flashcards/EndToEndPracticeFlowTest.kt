package com.chm.flashcards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
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
 * F05 / PRD §10 end-to-end acceptance test: the full chain first exercisable
 * end to end as of this feature -- create a set, add a card, run a practice
 * session on it, and land on the results screen showing the outcome. Also
 * covers the "Start Practice" disabled-until-a-card-exists edge case
 * (spec.md) along the way, since the set starts empty.
 *
 * C001 extends this: once Session Results is reached, both the screen's own
 * "Back to Set" button (createSetAddCardRunSessionSeeResult) and the system
 * back gesture (systemBackFromSessionResultsLandsOnSetDetail) must land
 * directly on Set Detail -- not back on Session Play/Config -- because
 * FlashCardsNavHost collapses that sub-stack via `popUpTo` when navigating
 * to Results.
 */
@HiltAndroidTest
class EndToEndPracticeFlowTest {

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

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { cardSetDao.getAll().first().forEach { cardSetDao.delete(it) } }
    }

    @Test
    fun createSetAddCardRunSessionSeeResult() {
        runSessionToResults()

        // Session Results: 1/1 correct, the card listed under Correct.
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()

        // C001: tapping the screen's own "Back to Set" button is a single pop that lands
        // directly on Set Detail (not Session Play/Config), thanks to the popUpTo collapse
        // wired in FlashCardsNavHost when navigating to Session Results.
        composeRule.onNodeWithTag("backToSetButton").performClick()
        composeRule.onNodeWithTag("startPracticeButton").assertIsDisplayed()
    }

    @Test
    fun systemBackFromSessionResultsLandsOnSetDetail() {
        runSessionToResults()
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()

        // C001: the hardware/system back gesture must also resolve to a single pop landing
        // on Set Detail, not Session Play/Config -- this is what actually proves the
        // popUpTo collapse works, as opposed to just the button's own popBackStack() call.
        Espresso.pressBack()
        composeRule.onNodeWithTag("startPracticeButton").assertIsDisplayed()
    }

    /**
     * Shared setup for the two C001 tests above: create a set, add one card, run a
     * one-card practice session, and land on Session Results. Duplicated verbatim from
     * [createSetAddCardRunSessionSeeResult]'s original body so both tests exercise the
     * exact same real path to Results before diverging on button-tap vs. system-back.
     */
    private fun runSessionToResults() {
        // Create a set.
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()
        composeRule.onNodeWithText("Kotlin Basics").assertIsDisplayed()

        // Enter the set -- no cards yet, so Start Practice is disabled.
        composeRule.onNodeWithText("Kotlin Basics").performClick()
        composeRule.onNodeWithTag("startPracticeButton").assertIsNotEnabled()

        // Add one card.
        composeRule.onNodeWithTag("addCardFab").performClick()
        composeRule.onNodeWithTag("cardFrontField").performTextInput("What is a data class?")
        composeRule.onNodeWithTag("cardBackField")
            .performTextInput("A class that auto-generates equals/hashCode/toString/copy")
        composeRule.onNodeWithTag("cardSaveButton").performClick()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()

        // Start Practice is now enabled (exactly one card in the set).
        composeRule.onNodeWithTag("startPracticeButton").performClick()

        // Session Config: default selected count is min(10, 1) = 1, so Start needs no slider input.
        composeRule.onNodeWithText("1 of 1 cards").assertIsDisplayed()
        composeRule.onNodeWithTag("startSessionButton").performClick()

        // Session Play: one card, front shown first.
        composeRule.onNodeWithText("1/1").assertIsDisplayed()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
        composeRule.onNodeWithTag("flipCard").performClick()
        composeRule.onNodeWithText("A class that auto-generates equals/hashCode/toString/copy").assertIsDisplayed()

        // C003: Correct/Incorrect are icon buttons now -- confirm they still expose a
        // meaningful contentDescription (not just relying on the testTag to find them).
        composeRule.onNodeWithContentDescription("Incorrect").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Correct").assertIsDisplayed()
        composeRule.onNodeWithTag("correctButton").performClick()
    }

    /**
     * C002 end-to-end acceptance test: a set with one tagged and one untagged
     * card -- selecting the tag's filter chip on Session Config must narrow
     * the count/slider to the single matching card, and the resulting session
     * (Play + Results) must never surface the untagged card. This is the
     * proof that the filter reaches all the way down to
     * `WeightedCardSelector`, not just that the UI compiles -- seeding data
     * directly via the injected DAOs (same precedent as
     * `ui/setdetail/SetDetailScreenTest.kt`) so the tag id is known upfront
     * for the `tagFilterChip_$tagId` test tag.
     */
    @Test
    fun tagFilterOnSessionConfig_narrowsSessionToOnlyTaggedCard() {
        lateinit var kotlinTagId: Uuid
        runBlocking {
            val set = CardSetEntity(id = idGenerator.newId(), name = "Tagged Set", createdAt = timeProvider.now())
            cardSetDao.insert(set)

            val taggedCardId = idGenerator.newId()
            cardDao.insert(
                CardEntity(
                    id = taggedCardId,
                    setId = set.id,
                    front = "Tagged card front",
                    back = "Tagged card back",
                    notes = null,
                ),
            )
            cardDao.insert(
                CardEntity(
                    id = idGenerator.newId(),
                    setId = set.id,
                    front = "Untagged card front",
                    back = "Untagged card back",
                    notes = null,
                ),
            )

            kotlinTagId = idGenerator.newId()
            tagDao.insert(TagEntity(id = kotlinTagId, name = "Kotlin"))
            crossRefDao.insert(CardTagCrossRef(cardId = taggedCardId, tagId = kotlinTagId))
        }

        composeRule.onNodeWithText("Tagged Set").performClick()
        composeRule.onNodeWithTag("startPracticeButton").performClick()

        // Before filtering, both cards in the set are eligible.
        composeRule.onNodeWithText("2 of 2 cards").assertIsDisplayed()

        // Select the "Kotlin" tag filter chip -- narrows the pool to the one tagged card.
        composeRule.onNodeWithTag("tagFilterChip_$kotlinTagId").performClick()
        composeRule.onNodeWithText("1 of 1 cards").assertIsDisplayed()

        composeRule.onNodeWithTag("startSessionButton").performClick()

        // Session Play: only the tagged card ever appears.
        composeRule.onNodeWithText("1/1").assertIsDisplayed()
        composeRule.onNodeWithText("Tagged card front").assertIsDisplayed()
        composeRule.onNodeWithText("Untagged card front").assertDoesNotExist()
        composeRule.onNodeWithTag("flipCard").performClick()
        composeRule.onNodeWithText("Tagged card back").assertIsDisplayed()
        composeRule.onNodeWithTag("correctButton").performClick()

        // Session Results: only the tagged card is listed -- the untagged card never reached the selector.
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()
        composeRule.onNodeWithText("Tagged card front").assertIsDisplayed()
        composeRule.onNodeWithText("Untagged card front").assertDoesNotExist()
    }
}
