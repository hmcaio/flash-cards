package com.chm.flashcards.ui.setdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
 * F04 acceptance criterion: typing in the search box filters the visible
 * card list, and selecting a tag filter chip narrows it further -- combined
 * with search text, per spec.md's AND semantics. One critical flow covering
 * both search and tag-filter interaction; edge cases (blank query, combined
 * filters, `availableTagFilters` scoping) are covered at the ViewModel
 * unit-test level ([SetDetailViewModelTest]) and DAO level
 * ([com.chm.flashcards.data.dao.CardDaoTest]).
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

    private lateinit var kotlinCardId: Uuid
    private lateinit var kotlinTagId: Uuid

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            cardSetDao.getAll().first().forEach { cardSetDao.delete(it) }
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
            cardDao.insert(
                CardEntity(
                    id = idGenerator.newId(),
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
}
