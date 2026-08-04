package com.chm.flashcards.ui.importexport

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.chm.flashcards.MainActivity
import com.chm.flashcards.data.dao.CardSetDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * F07 plan.md step 37 acceptance test: export the library to a (stubbed via
 * Espresso-Intents, backed by the in-memory [com.chm.flashcards.data.importexport.InMemoryDocumentStore]
 * from [com.chm.flashcards.di.TestDocumentIoModule]) SAF file, then import
 * that same file back with "Add as new sets" and confirm the library
 * duplicates (same content, doubled set count) rather than actually driving
 * the real system file picker UI.
 */
@HiltAndroidTest
class ImportExportScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var cardSetDao: CardSetDao

    private val exportUri: Uri = Uri.parse("content://com.chm.flashcards.test/flashcards-export.json")

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { cardSetDao.getAll().first().forEach { cardSetDao.delete(it) } }

        Intents.init()
        Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(exportUri)))
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(exportUri)))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun exportThenImportAddAsNewSets_duplicatesLibrary() {
        // Build a small library: one set, one tagged card.
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()
        composeRule.onNodeWithText("Kotlin Basics").performClick()

        composeRule.onNodeWithTag("addCardFab").performClick()
        composeRule.onNodeWithTag("cardFrontField").performTextInput("What is a data class?")
        composeRule.onNodeWithTag("cardBackField").performTextInput("Auto equals/hashCode/toString/copy")
        composeRule.onNodeWithTag("cardTagInputField").performTextInput("Kotlin,")
        composeRule.onNodeWithTag("cardSaveButton").performClick()
        composeRule.onNodeWithText("What is a data class?").assertIsDisplayed()

        // Back to Set List, into Import/Export.
        Espresso.pressBack()
        composeRule.onNodeWithTag("importExportButton").performClick()

        // Export -- SAF picker result stubbed via Espresso-Intents, actual write goes to the
        // in-memory test DocumentWriter.
        composeRule.onNodeWithTag("exportButton").performClick()
        composeRule.onNodeWithTag("importExportSuccess").assertIsDisplayed()

        // Import the same file back with "Add as new sets".
        composeRule.onNodeWithTag("importButton").performClick()
        composeRule.onNodeWithTag("addAsNewSetsButton").performClick()
        composeRule.onNodeWithTag("importExportSuccess").assertIsDisplayed()

        // Back to Set List: the set now appears twice (original + imported copy).
        Espresso.pressBack()
        composeRule.onAllNodesWithText("Kotlin Basics").assertCountEquals(2)
    }
}
