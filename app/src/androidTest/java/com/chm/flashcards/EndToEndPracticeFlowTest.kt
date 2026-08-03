package com.chm.flashcards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.chm.flashcards.data.dao.CardSetDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
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
 */
@HiltAndroidTest
class EndToEndPracticeFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var cardSetDao: CardSetDao

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { cardSetDao.getAll().first().forEach { cardSetDao.delete(it) } }
    }

    @Test
    fun createSetAddCardRunSessionSeeResult() {
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
        composeRule.onNodeWithTag("correctButton").performClick()

        // Session Results: 1/1 correct, the card listed under Correct.
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
    }
}
