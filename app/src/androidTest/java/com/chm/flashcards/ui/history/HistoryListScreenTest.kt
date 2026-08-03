package com.chm.flashcards.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.chm.flashcards.MainActivity
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
 * F06 / spec.md acceptance criteria: after completing a practice session
 * (F05's flow), it appears at the top of History List for that set with the
 * correct score, and tapping it shows the same correct/incorrect card lists
 * as the original Session Results screen.
 */
@HiltAndroidTest
class HistoryListScreenTest {

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
    fun afterCompletingSession_appearsInHistoryList() {
        // Create a set with one card.
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("addCardFab").performClick()
        composeRule.onNodeWithTag("cardFrontField").performTextInput("What is a data class?")
        composeRule.onNodeWithTag("cardBackField")
            .performTextInput("A class that auto-generates equals/hashCode/toString/copy")
        composeRule.onNodeWithTag("cardSaveButton").performClick()

        // Run one session, answered correctly, all the way to Session Results.
        composeRule.onNodeWithTag("startPracticeButton").performClick()
        composeRule.onNodeWithTag("startSessionButton").performClick()
        composeRule.onNodeWithTag("flipCard").performClick()
        composeRule.onNodeWithTag("correctButton").performClick()
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()

        // Back to Set Detail: Session Results -> Session Play -> Session Config -> Set Detail.
        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()

        // History List: the just-finished session shows up with its score.
        composeRule.onNodeWithTag("historyButton").performClick()
        composeRule.onNodeWithTag("emptyHistoryMessage").assertDoesNotExist()
        composeRule.onNodeWithText("1/1").assertIsDisplayed()

        // History Detail: same correct/incorrect breakdown as Session Results.
        composeRule.onNodeWithText("1/1").performClick()
        composeRule.onNodeWithText("1/1 correct").assertIsDisplayed()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
    }
}
