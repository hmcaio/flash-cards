package com.chm.flashcards.ui.setlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.chm.flashcards.MainActivity
import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.entity.CardSetEntity
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
 * F02 acceptance criterion: create a set from the UI and see it appear in
 * the list, reactively (no manual refresh). One critical flow only, per
 * PRD §10 "critical flows only" -- rename/delete are covered at the
 * ViewModel unit-test level ([SetListViewModelTest]).
 *
 * C004 additions: the Rename/Delete flows now go through the per-row
 * dropdown menu instead of inline buttons (proving the new interaction path
 * actually works, not just that the old tags still exist), plus the
 * grid/list toggle and its persistence through the real, DataStore-backed
 * [ViewModePreferences] -- not swapped out for a test double here (unlike
 * [com.chm.flashcards.di.TestDatabaseModule]/[com.chm.flashcards.di.TestDocumentIoModule])
 * since a Preferences DataStore is just a small file under the test app's
 * own sandboxed storage; the real implementation is exercised directly.
 * `@Before` explicitly resets it to [ViewMode.GRID] (this chore's default)
 * the same way existing tests already defensively clear leftover
 * [CardSetDao] rows, since instrumented tests in the same run can share a
 * process/app sandbox.
 */
@HiltAndroidTest
class SetListScreenTest {

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

    @Inject
    lateinit var viewModePreferences: ViewModePreferences

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            cardSetDao.getAll().first().forEach { cardSetDao.delete(it) }
            viewModePreferences.setViewMode(ViewMode.GRID)
        }
    }

    private fun seedSet(name: String): Uuid {
        val id = idGenerator.newId()
        runBlocking { cardSetDao.insert(CardSetEntity(id = id, name = name, createdAt = timeProvider.now())) }
        return id
    }

    /**
     * The toggle click only *starts* the write (`viewModelScope.launch { setViewMode(...) }`);
     * the DataStore write itself completes on its own dispatcher, asynchronously from Compose's
     * click handling. Waiting for the toggle icon's content description to flip is how a test
     * observes that the full round trip (write -> DataStore flow emission -> collected into
     * UiState -> recomposition) has actually landed, instead of racing straight into an
     * assertion right after `performClick()`.
     */
    private fun waitForToggleDescription(description: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun createSet_appearsInList() {
        composeRule.onNodeWithTag("createSetFab").performClick()
        composeRule.onNodeWithTag("createSetNameField").performTextInput("Kotlin Basics")
        composeRule.onNodeWithTag("createSetConfirmButton").performClick()

        composeRule.onNodeWithText("Kotlin Basics").assertIsDisplayed()
    }

    @Test
    fun dropdownMenu_renameAction_opensRenameDialog_andRenames() {
        val id = seedSet("Kotlin Basics")

        composeRule.onNodeWithTag("setMenuButton_$id").performClick()
        composeRule.onNodeWithTag("renameMenuItem_$id").performClick()
        composeRule.onNodeWithTag("renameSetNameField").performTextClearance()
        composeRule.onNodeWithTag("renameSetNameField").performTextInput("Renamed Set")
        composeRule.onNodeWithTag("renameSetConfirmButton").performClick()

        composeRule.onNodeWithText("Renamed Set").assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin Basics").assertDoesNotExist()
    }

    @Test
    fun dropdownMenu_deleteAction_opensConfirmDialog_andDeletes() {
        seedSet("Kotlin Basics")
        val id = seedSet("To Delete")

        composeRule.onNodeWithTag("setMenuButton_$id").performClick()
        composeRule.onNodeWithTag("deleteMenuItem_$id").performClick()
        composeRule.onNodeWithText("Delete set?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("To Delete").assertDoesNotExist()
        composeRule.onNodeWithText("Kotlin Basics").assertIsDisplayed()
    }

    @Test
    fun defaultViewMode_isGrid_twoSetsRenderSideBySide() {
        val idA = seedSet("Kotlin Basics")
        val idB = seedSet("Android Jetpack")

        val topA = composeRule.onNodeWithTag("setListItem_$idA").fetchSemanticsNode().boundsInRoot.top
        val topB = composeRule.onNodeWithTag("setListItem_$idB").fetchSemanticsNode().boundsInRoot.top

        assertEquals(topA, topB, 1f)
    }

    @Test
    fun toggleViewMode_switchesToListLayout_itemsStacked() {
        val idA = seedSet("Kotlin Basics")
        val idB = seedSet("Android Jetpack")

        composeRule.onNodeWithTag("viewModeToggle").performClick()
        waitForToggleDescription("Switch to grid view") // i.e. now showing LIST mode

        val topA = composeRule.onNodeWithTag("setListItem_$idA").fetchSemanticsNode().boundsInRoot.top
        val topB = composeRule.onNodeWithTag("setListItem_$idB").fetchSemanticsNode().boundsInRoot.top

        assert(topB > topA + 1f) { "expected list mode to stack items vertically, got topA=$topA topB=$topB" }
    }

    @Test
    fun toggleViewMode_persistsThroughRealDataStore() {
        composeRule.onNodeWithTag("viewModeToggle").performClick()
        waitForToggleDescription("Switch to grid view") // i.e. now showing LIST mode

        // Reads through the real, DataStore-backed ViewModePreferences directly
        // (bypassing the ViewModel's own cached UiState) to prove the write actually
        // reached the persisted preference, not just in-memory ViewModel state.
        val persisted = runBlocking { viewModePreferences.viewMode.first() }
        assertEquals(ViewMode.LIST, persisted)
    }

    @Test
    fun toggleViewModeOnSetList_reflectsOnSetDetail_sharedGlobalPreference() {
        seedSet("Kotlin Basics")

        composeRule.onNodeWithTag("viewModeToggle").performClick() // Set List: switch to LIST
        waitForToggleDescription("Switch to grid view")

        composeRule.onNodeWithText("Kotlin Basics").performClick() // navigate to Set Detail

        // Set Detail's own toggle reads the SAME preference -- its icon/description must
        // already reflect LIST mode without any toggle interaction on this screen, proving
        // it's one shared global preference rather than two independent per-screen ones.
        composeRule.onNodeWithContentDescription("Switch to grid view").assertIsDisplayed()
    }
}
