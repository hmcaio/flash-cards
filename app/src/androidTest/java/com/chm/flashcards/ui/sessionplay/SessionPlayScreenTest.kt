package com.chm.flashcards.ui.sessionplay

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
 * Backing out of an in-progress session discards it (nothing is persisted
 * until Results, per F05 spec) -- the system back gesture during Session
 * Play must show a "Leave session?" confirm dialog rather than silently
 * navigating away, per this chore. Covers: dialog shown on back, "Cancel"
 * stays on Session Play, "Leave" actually navigates back (to Session
 * Config, one pop up the still-uncollapsed stack -- collapsing only
 * happens once Results is reached, see `EndToEndPracticeFlowTest`).
 */
@HiltAndroidTest
class SessionPlayScreenTest {

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

    private fun reachSessionPlay() {
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("addCardFab").performClick()
        composeRule.onNodeWithTag("cardFrontField").performTextInput("What is a data class?")
        composeRule.onNodeWithTag("cardBackField").performTextInput("Auto equals/hashCode/toString/copy")
        composeRule.onNodeWithTag("cardSaveButton").performClick()

        composeRule.onNodeWithTag("startPracticeButton").performClick()
        composeRule.onNodeWithTag("startSessionButton").performClick()
        composeRule.onNodeWithText("1/1").assertIsDisplayed()
    }

    @Test
    fun systemBack_showsLeaveConfirmDialog_notImmediateNavigation() {
        reachSessionPlay()

        Espresso.pressBack()

        composeRule.onNodeWithText("Leave session?").assertIsDisplayed()
        // Still on Session Play underneath the dialog -- not navigated away yet.
        composeRule.onNodeWithText("1/1").assertIsDisplayed()
    }

    @Test
    fun leaveConfirmDialog_cancel_staysOnSessionPlay() {
        reachSessionPlay()

        Espresso.pressBack()
        composeRule.onNodeWithTag("leaveSessionCancelButton").performClick()

        composeRule.onNodeWithText("Leave session?").assertDoesNotExist()
        composeRule.onNodeWithText("1/1").assertIsDisplayed()
    }

    @Test
    fun leaveConfirmDialog_leave_navigatesBackToSessionConfig() {
        reachSessionPlay()

        Espresso.pressBack()
        composeRule.onNodeWithTag("leaveSessionConfirmButton").performClick()

        // One pop up the (still-uncollapsed) stack lands on Session Config, not Set Detail --
        // the popUpTo collapse only happens once a session actually reaches Results.
        composeRule.onNodeWithTag("startSessionButton").assertIsDisplayed()
    }
}
