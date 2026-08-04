package com.chm.flashcards.data.repository

import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.TransactionRunner
import com.chm.flashcards.data.importexport.CardExportDto
import com.chm.flashcards.data.importexport.ImportValidationResult
import com.chm.flashcards.data.importexport.ImportValidator
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.importexport.SetExportDto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ImportExportRepositoryImpl @Inject constructor(
    private val cardSetRepository: CardSetRepository,
    private val cardRepository: CardRepository,
    private val timeProvider: TimeProvider,
    private val importValidator: ImportValidator,
    private val transactionRunner: TransactionRunner,
) : ImportExportRepository {

    override fun validateImport(json: String): ImportValidationResult = importValidator.validate(json)

    override suspend fun exportLibrary(): String {
        val sets = cardSetRepository.getAllSets().first()
        val setDtos = sets.map { set ->
            val cardsWithTags = cardRepository.getCardsBySetId(set.id).first()
            SetExportDto(
                name = set.name,
                cards = cardsWithTags.map { cardWithTags ->
                    CardExportDto(
                        front = cardWithTags.card.front,
                        back = cardWithTags.card.back,
                        notes = cardWithTags.card.notes,
                        tags = cardWithTags.tags.map { it.name },
                    )
                },
            )
        }
        val library = LibraryExportDto(exportedAt = timeProvider.now().toString(), sets = setDtos)
        return Json.encodeToString(library)
    }

    override suspend fun importLibrary(parsed: LibraryExportDto, mode: ImportMode) {
        transactionRunner.runInTransaction {
            if (mode == ImportMode.ReplaceAll) {
                cardSetRepository.getAllSets().first().forEach { existing -> cardSetRepository.deleteSet(existing.id) }
            }
            parsed.sets.forEach { setDto -> importSet(setDto) }
        }
    }

    private suspend fun importSet(setDto: SetExportDto) {
        val createdSet = cardSetRepository.createSet(setDto.name)
        setDto.cards.forEach { cardDto ->
            cardRepository.createCard(
                setId = createdSet.id,
                front = cardDto.front,
                back = cardDto.back,
                notes = cardDto.notes,
                tagNames = cardDto.tags,
            )
        }
    }
}
