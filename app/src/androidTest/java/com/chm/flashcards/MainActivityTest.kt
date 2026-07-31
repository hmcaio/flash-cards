package com.chm.flashcards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
 * Smoke test: launching [MainActivity] wires Hilt + Navigation Compose
 * without crashing and lands on the (now real, as of F02) Set List
 * destination, empty by default in a fresh in-memory test database
 * ([com.chm.flashcards.di.TestDatabaseModule]).
 */
@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var cardSetDao: CardSetDao

    @Before
    fun init() {
        hiltRule.inject()
        // Belt-and-suspenders: the in-memory test DB is Hilt-singleton-scoped,
        // so if the test process happens to be reused across test classes,
        // this keeps this test's assumption of a clean slate valid either way.
        runBlocking { cardSetDao.getAll().first().forEach { cardSetDao.delete(it) } }
    }

    @Test
    fun launch_showsSetListEmptyState() {
        composeRule.onNodeWithText("No sets yet").assertIsDisplayed()
    }
}
