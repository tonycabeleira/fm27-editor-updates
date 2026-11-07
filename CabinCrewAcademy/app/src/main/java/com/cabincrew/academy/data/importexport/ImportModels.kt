package com.cabincrew.academy.data.importexport

import com.cabincrew.academy.data.db.Difficulty

/** A question that has been read from a file and passed validation. */
data class ParsedQuestion(
    val externalId: String,
    val categoryName: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String?,
    val difficulty: Difficulty,
    /** Name of the file this question came from (for messages). */
    val source: String,
    /** Human readable location, e.g. "row 12" or "categories[0].questions[3]". */
    val location: String,
)

data class ImportIssue(
    val source: String,
    val location: String,
    val message: String,
) {
    override fun toString(): String = "$source • $location: $message"
}

data class ParseResult(
    val questions: List<ParsedQuestion> = emptyList(),
    /** Category name -> description (descriptions only available from JSON). */
    val categories: Map<String, String> = emptyMap(),
    val errors: List<ImportIssue> = emptyList(),
    val warnings: List<ImportIssue> = emptyList(),
) {
    operator fun plus(other: ParseResult): ParseResult = ParseResult(
        questions = questions + other.questions,
        categories = categories + other.categories.filterKeys { k -> categories.keys.none { it.equals(k, true) } },
        errors = errors + other.errors,
        warnings = warnings + other.warnings,
    )
}

const val MIN_OPTIONS = 2
const val MAX_OPTIONS = 10
