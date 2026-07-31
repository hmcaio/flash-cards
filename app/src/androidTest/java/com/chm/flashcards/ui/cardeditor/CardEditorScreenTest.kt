package com.chm.flashcards.ui.cardeditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.chm.flashcards.MainActivity
import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.entity.CardSetEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * F03 acceptance criterion: create a card (with a tag) from Set Detail's FAB
 * and see it appear back on Set Detail, reactively (no manual refresh). One
 * critical flow only, per PRD §10 "critical flows only" -- edit/delete and
 * validation/tag-dedupe are covered at the ViewModel unit-test level
 * ([CardEditorViewModelTest]).
 */
@HiltAndroidTest
class CardEditorScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var cardSetDao: CardSetDao

    @Inject
    lateinit var idGenerator: IdGenerator

    @Inject
    lateinit var timeProvider: TimeProvider

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            cardSetDao.getAll().first().forEach { cardSetDao.delete(it) }
            cardSetDao.insert(
                CardSetEntity(id = idGenerator.newId(), name = "Kotlin Basics", createdAt = timeProvider.now()),
            )
        }
    }

    @Test
    fun createCard_appearsOnSetDetail() {
        composeRule.onNodeWithText("Kotlin Basics").performClick()
        composeRule.onNodeWithTag("addCardFab").performClick()

        composeRule.onNodeWithTag("cardFrontField").performTextInput("What is a data class?")
        composeRule.onNodeWithTag("cardBackField")
            .performTextInput("A class that auto-generates equals/hashCode/toString/copy")
        composeRule.onNodeWithTag("cardTagInputField").performTextInput("Kotlin,")
        composeRule.onNodeWithTag("cardSaveButton").performClick()

        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin").assertIsDisplayed()
    }
}
