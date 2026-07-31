package com.chm.flashcards.ui.setlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * F02 acceptance criterion: create a set from the UI and see it appear in
 * the list, reactively (no manual refresh). One critical flow only, per
 * PRD §10 "critical flows only" -- rename/delete are covered at the
 * ViewModel unit-test level ([SetListViewModelTest]).
 */
@HiltAndroidTest
class SetListScreenTest {

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
    fun createSet_appearsInList() {
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()

        composeRule.onNodeWithText("Kotlin Basics").assertIsDisplayed()
    }
}
