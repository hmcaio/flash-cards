package com.chm.flashcards.data.importexport

import com.chm.flashcards.common.CardValidationRules
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** The only `schemaVersion` this app version knows how to import -- see F07 spec.md. */
internal const val SUPPORTED_SCHEMA_VERSION = 1

/** Guards against pathological import files, per F07 spec.md's validation order. */
internal const val MAX_SETS = 500
internal const val MAX_CARDS_PER_SET = 10_000

/**
 * Validates a candidate import JSON string against F07 spec.md's schema
 * before any DB write happens, in a fixed order (malformed -> unsupported
 * version -> too large -> invalid content -> valid), returning the first
 * failure found. Pure/stateless -- no DB or Android dependency, so it's a
 * plain JVM unit test subject ([com.chm.flashcards.data.importexport.ImportValidatorTest]).
 */
class ImportValidator @Inject constructor() {

    fun validate(json: String): ImportValidationResult {
        val library = try {
            Json.decodeFromString<LibraryExportDto>(json)
        } catch (e: SerializationException) {
            return ImportValidationResult.Malformed
        } catch (e: IllegalArgumentException) {
            return ImportValidationResult.Malformed
        }

        if (library.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return ImportValidationResult.UnsupportedVersion(library.schemaVersion)
        }

        if (library.sets.size > MAX_SETS) {
            return ImportValidationResult.TooLarge("Library has ${library.sets.size} sets (max $MAX_SETS)")
        }
        library.sets.forEach { set ->
            if (set.cards.size > MAX_CARDS_PER_SET) {
                return ImportValidationResult.TooLarge(
                    "Set \"${set.name}\" has ${set.cards.size} cards (max $MAX_CARDS_PER_SET)",
                )
            }
        }

        library.sets.forEach { set ->
            set.cards.forEach { card ->
                cardContentError(card)?.let { return ImportValidationResult.InvalidContent(it) }
            }
        }

        return ImportValidationResult.Valid(library)
    }

    private fun cardContentError(card: CardExportDto): String? = when {
        card.front.isBlank() -> "Card front cannot be blank"
        card.back.isBlank() -> "Card back cannot be blank"
        card.front.length > CardValidationRules.MAX_FRONT_BACK_LENGTH ->
            "Card front exceeds ${CardValidationRules.MAX_FRONT_BACK_LENGTH} characters"
        card.back.length > CardValidationRules.MAX_FRONT_BACK_LENGTH ->
            "Card back exceeds ${CardValidationRules.MAX_FRONT_BACK_LENGTH} characters"
        (card.notes?.length ?: 0) > CardValidationRules.MAX_NOTES_LENGTH ->
            "Card notes exceed ${CardValidationRules.MAX_NOTES_LENGTH} characters"
        card.tags.size > CardValidationRules.MAX_TAGS ->
            "Card has ${card.tags.size} tags (max ${CardValidationRules.MAX_TAGS})"
        card.tags.any { it.length > CardValidationRules.MAX_TAG_NAME_LENGTH } ->
            "A tag name exceeds ${CardValidationRules.MAX_TAG_NAME_LENGTH} characters"
        else -> null
    }
}
