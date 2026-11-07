package com.cabincrew.academy.data.importexport

import com.cabincrew.academy.data.db.Difficulty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.security.MessageDigest

/**
 * Parses and validates question files. Supports:
 *  - JSON ({"categories":[{"name":..., "questions":[...]}]} or {"questions":[{..., "category":...}]})
 *  - CSV  (comma or semicolon separated, first row is a header)
 *
 * All problems are reported as [ImportIssue]s; invalid questions are skipped, never guessed.
 */
object QuestionParser {

    private val json = Json { isLenient = true; ignoreUnknownKeys = true; allowTrailingComma = true }

    fun parse(fileName: String, content: String): ParseResult {
        val text = content.removePrefix("﻿").trim()
        if (text.isEmpty()) return ParseResult(errors = listOf(ImportIssue(fileName, "file", "The file is empty.")))
        val looksJson = text.startsWith("{") || text.startsWith("[")
        val result = if (looksJson || fileName.endsWith(".json", ignoreCase = true)) {
            parseJson(fileName, text)
        } else {
            parseCsv(fileName, text)
        }
        return checkDuplicateIds(result)
    }

    /** Validates duplicates across the combined result of several files. */
    fun checkDuplicateIds(result: ParseResult): ParseResult {
        val seen = HashMap<String, ParsedQuestion>()
        val kept = ArrayList<ParsedQuestion>()
        val errors = result.errors.toMutableList()
        for (q in result.questions) {
            val key = q.externalId.lowercase()
            val first = seen[key]
            if (first != null) {
                errors += ImportIssue(
                    q.source, q.location,
                    "Duplicate id \"${q.externalId}\" (already used at ${first.source} • ${first.location}). Question skipped."
                )
            } else {
                seen[key] = q
                kept += q
            }
        }
        return result.copy(questions = kept, errors = errors)
    }

    // ---------------------------------------------------------------- JSON

    private fun parseJson(fileName: String, text: String): ParseResult {
        val root: JsonElement = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            return ParseResult(
                errors = listOf(ImportIssue(fileName, "file", "Invalid JSON: ${e.message?.lineSequence()?.firstOrNull() ?: "syntax error"}"))
            )
        }

        val questions = ArrayList<ParsedQuestion>()
        val categories = LinkedHashMap<String, String>()
        val errors = ArrayList<ImportIssue>()
        val warnings = ArrayList<ImportIssue>()

        fun handleCategoryArray(arr: JsonArray, path: String) {
            arr.forEachIndexed { ci, el ->
                val loc = "$path[$ci]"
                val obj = el as? JsonObject
                if (obj == null) {
                    errors += ImportIssue(fileName, loc, "Each category must be an object with \"name\" and \"questions\".")
                    return@forEachIndexed
                }
                val name = obj.string("name") ?: obj.string("category")
                if (name.isNullOrBlank()) {
                    errors += ImportIssue(fileName, loc, "Category is missing a \"name\". Its questions were skipped.")
                    return@forEachIndexed
                }
                categories.putIfAbsentIgnoreCase(name.trim(), obj.string("description")?.trim().orEmpty())
                val qs = obj["questions"]
                when {
                    qs == null || qs is JsonNull ->
                        warnings += ImportIssue(fileName, loc, "Category \"$name\" has no \"questions\" array (category will still be created).")
                    qs !is JsonArray ->
                        errors += ImportIssue(fileName, "$loc.questions", "\"questions\" must be an array.")
                    else -> qs.forEachIndexed { qi, qel ->
                        parseJsonQuestion(fileName, "$loc.questions[$qi]", qel, name.trim(), errors, warnings)?.let { questions += it }
                    }
                }
            }
        }

        when (root) {
            is JsonObject -> {
                val cats = root["categories"]
                val flat = root["questions"]
                if (cats == null && flat == null) {
                    errors += ImportIssue(fileName, "root", "Expected a \"categories\" array (or a \"questions\" array). See the import guide for the format.")
                }
                if (cats != null) {
                    if (cats is JsonArray) handleCategoryArray(cats, "categories")
                    else errors += ImportIssue(fileName, "categories", "\"categories\" must be an array.")
                }
                if (flat != null) {
                    if (flat is JsonArray) {
                        flat.forEachIndexed { qi, qel ->
                            val cat = (qel as? JsonObject)?.string("category")
                            if (cat.isNullOrBlank()) {
                                errors += ImportIssue(fileName, "questions[$qi]", "Question has no \"category\" field. Skipped.")
                            } else {
                                categories.putIfAbsentIgnoreCase(cat.trim(), "")
                                parseJsonQuestion(fileName, "questions[$qi]", qel, cat.trim(), errors, warnings)?.let { questions += it }
                            }
                        }
                    } else errors += ImportIssue(fileName, "questions", "\"questions\" must be an array.")
                }
            }
            is JsonArray -> handleCategoryArray(root, "root")
            else -> errors += ImportIssue(fileName, "root", "The JSON root must be an object.")
        }
        return ParseResult(questions, categories, errors, warnings)
    }

    private fun parseJsonQuestion(
        fileName: String,
        loc: String,
        el: JsonElement,
        categoryName: String,
        errors: MutableList<ImportIssue>,
        warnings: MutableList<ImportIssue>,
    ): ParsedQuestion? {
        val obj = el as? JsonObject ?: run {
            errors += ImportIssue(fileName, loc, "Question must be an object.")
            return null
        }
        val text = (obj.string("question") ?: obj.string("text"))?.trim()
        val optionsEl = obj["options"] ?: obj["answers"]
        val options: List<String>? = (optionsEl as? JsonArray)?.map { (it as? JsonPrimitive)?.content?.trim().orEmpty() }
        val correctEl = obj["correctAnswer"] ?: obj["correct"] ?: obj["answer"]
        val idRaw = (obj["id"] as? JsonPrimitive)?.content?.trim()
        val label = if (!idRaw.isNullOrBlank()) "$loc (id $idRaw)" else loc

        val problems = mutableListOf<String>()
        if (text.isNullOrBlank()) problems += "missing \"question\" text"
        if (options == null) problems += "missing \"options\" array"
        else {
            if (options.size < MIN_OPTIONS) problems += "needs at least $MIN_OPTIONS options (found ${options.size})"
            if (options.size > MAX_OPTIONS) problems += "has more than $MAX_OPTIONS options"
            options.forEachIndexed { i, o -> if (o.isBlank()) problems += "option $i is empty" }
        }
        var correct: Int? = null
        when {
            correctEl == null || correctEl is JsonNull -> problems += "missing \"correctAnswer\""
            correctEl is JsonPrimitive -> {
                correct = correctEl.intOrNull ?: correctEl.content.trim().toIntOrNull() ?: letterIndex(correctEl.content)
                if (correct == null) problems += "\"correctAnswer\" must be a number (0 = first option); got \"${correctEl.content}\""
            }
            else -> problems += "\"correctAnswer\" must be a number"
        }
        if (correct != null && options != null && correct !in options.indices) {
            problems += "\"correctAnswer\" is $correct but valid values are 0..${options.size - 1} (index starts at 0)"
        }
        if (problems.isNotEmpty()) {
            errors += ImportIssue(fileName, label, problems.joinToString("; ").replaceFirstChar { it.uppercase() } + ".")
            return null
        }
        val diffRaw = (obj["difficulty"] as? JsonPrimitive)?.content
        val difficulty = Difficulty.parse(diffRaw)
        if (diffRaw != null && difficulty == null) {
            warnings += ImportIssue(fileName, label, "Unknown difficulty \"$diffRaw\" – using Medium.")
        }
        val explanation = obj.string("explanation")?.trim()?.takeIf { it.isNotEmpty() }
        val id = if (idRaw.isNullOrBlank()) autoId(categoryName, text!!) else idRaw
        return ParsedQuestion(
            externalId = id,
            categoryName = categoryName,
            text = text!!,
            options = options!!,
            correctIndex = correct!!,
            explanation = explanation,
            difficulty = difficulty ?: Difficulty.MEDIUM,
            source = fileName,
            location = label,
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun MutableMap<String, String>.putIfAbsentIgnoreCase(key: String, value: String) {
        val existing = keys.firstOrNull { it.equals(key, ignoreCase = true) }
        if (existing == null) put(key, value)
        else if (this[existing].isNullOrBlank() && value.isNotBlank()) put(existing, value)
    }

    // ---------------------------------------------------------------- CSV

    private val optionHeader = Regex("^(option|opt|answer)[ _-]?([a-j]|\\d{1,2})$", RegexOption.IGNORE_CASE)

    private fun parseCsv(fileName: String, text: String): ParseResult {
        val firstLine = text.lineSequence().first()
        val delimiter = when {
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            firstLine.count { it == '\t' } > firstLine.count { it == ',' } -> '\t'
            else -> ','
        }
        val rows = try {
            CsvReader.read(text, delimiter)
        } catch (e: IllegalArgumentException) {
            return ParseResult(errors = listOf(ImportIssue(fileName, "file", e.message ?: "Invalid CSV")))
        }
        if (rows.isEmpty()) return ParseResult(errors = listOf(ImportIssue(fileName, "file", "No rows found.")))

        val header = rows.first().map { it.trim().removePrefix("﻿").lowercase() }
        fun col(vararg names: String) = header.indexOfFirst { h -> names.any { it.equals(h, true) } }
        val idCol = col("id", "questionid")
        val catCol = col("category", "subject", "categoria")
        val qCol = col("question", "text", "pergunta")
        val correctCol = col("correctanswer", "correct", "answer", "correct_answer")
        val explCol = col("explanation", "explicacao", "explicação")
        val diffCol = col("difficulty", "level")
        val optionCols = header.mapIndexedNotNull { i, h ->
            optionHeader.matchEntire(h)?.let { m ->
                val key = m.groupValues[2]
                val order = key.toIntOrNull() ?: (key.lowercase()[0] - 'a' + 1)
                order to i
            }
        }.sortedBy { it.first }.map { it.second }

        val headerErrors = mutableListOf<String>()
        if (catCol < 0) headerErrors += "\"category\""
        if (qCol < 0) headerErrors += "\"question\""
        if (correctCol < 0) headerErrors += "\"correctAnswer\""
        if (optionCols.size < MIN_OPTIONS) headerErrors += "at least two option columns (\"option1\", \"option2\", …)"
        if (headerErrors.isNotEmpty()) {
            return ParseResult(
                errors = listOf(
                    ImportIssue(fileName, "header (row 1)", "Missing column(s): ${headerErrors.joinToString(", ")}. Found: ${rows.first().joinToString(", ")}")
                )
            )
        }

        val questions = ArrayList<ParsedQuestion>()
        val categories = LinkedHashMap<String, String>()
        val errors = ArrayList<ImportIssue>()
        val warnings = ArrayList<ImportIssue>()

        rows.drop(1).forEachIndexed { idx, row ->
            val rowNo = idx + 2
            if (row.all { it.isBlank() }) return@forEachIndexed
            fun cell(i: Int) = if (i in row.indices) row[i].trim() else ""
            val id = if (idCol >= 0) cell(idCol) else ""
            val loc = if (id.isNotEmpty()) "row $rowNo (id $id)" else "row $rowNo"
            val category = cell(catCol)
            val question = cell(qCol)
            val rawOptions = optionCols.map { cell(it) }
            val lastFilled = rawOptions.indexOfLast { it.isNotEmpty() }
            val options = if (lastFilled >= 0) rawOptions.subList(0, lastFilled + 1) else emptyList()
            val correctRaw = cell(correctCol)

            val problems = mutableListOf<String>()
            if (category.isEmpty()) problems += "category is empty"
            if (question.isEmpty()) problems += "question is empty"
            if (options.size < MIN_OPTIONS) problems += "needs at least $MIN_OPTIONS options"
            options.forEachIndexed { i, o -> if (o.isEmpty()) problems += "option ${i + 1} is empty but a later option is filled" }
            val correct = correctRaw.toIntOrNull() ?: letterIndex(correctRaw)
            if (correctRaw.isEmpty()) problems += "correctAnswer is empty"
            else if (correct == null) problems += "correctAnswer \"$correctRaw\" is not a number (0 = first option) or a letter A–J"
            else if (correct !in options.indices) problems += "correctAnswer is $correctRaw but only ${options.size} option(s) are filled (index starts at 0)"
            if (problems.isNotEmpty()) {
                errors += ImportIssue(fileName, loc, problems.joinToString("; ").replaceFirstChar { it.uppercase() } + ".")
                return@forEachIndexed
            }
            val diffRaw = if (diffCol >= 0) cell(diffCol) else ""
            val diff = Difficulty.parse(diffRaw)
            if (diffRaw.isNotEmpty() && diff == null) warnings += ImportIssue(fileName, loc, "Unknown difficulty \"$diffRaw\" – using Medium.")
            categories.putIfAbsentIgnoreCase(category, "")
            questions += ParsedQuestion(
                externalId = id.ifEmpty { autoId(category, question) },
                categoryName = categories.keys.first { it.equals(category, true) },
                text = question,
                options = options,
                correctIndex = correct!!,
                explanation = (if (explCol >= 0) cell(explCol) else "").ifEmpty { null },
                difficulty = diff ?: Difficulty.MEDIUM,
                source = fileName,
                location = loc,
            )
        }
        return ParseResult(questions, categories, errors, warnings)
    }

    private fun letterIndex(raw: String): Int? {
        val t = raw.trim()
        if (t.length != 1) return null
        val c = t.uppercase()[0]
        return if (c in 'A'..'J') c - 'A' else null
    }

    /** Stable id for questions without one, so re-importing the same file is detected as duplicates. */
    fun autoId(category: String, text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("${category.trim().lowercase()}|${text.trim().lowercase()}".toByteArray())
        return "AUTO-" + digest.take(5).joinToString("") { "%02X".format(it) }
    }
}

/** Minimal RFC 4180 CSV reader supporting quoted fields, escaped quotes and multi-line values. */
object CsvReader {
    fun read(text: String, delimiter: Char): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        var line = 1
        var quoteStartLine = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"'); i++
                    } else inQuotes = false
                } else {
                    if (c == '\n') line++
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> { inQuotes = true; quoteStartLine = line }
                    delimiter -> { row.add(field.toString()); field.setLength(0) }
                    '\r' -> {}
                    '\n' -> {
                        row.add(field.toString()); field.setLength(0)
                        rows.add(row); row = ArrayList(); line++
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (inQuotes) throw IllegalArgumentException("Unclosed quotation mark starting on line $quoteStartLine.")
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString()); rows.add(row)
        }
        return rows
    }

    fun escape(value: String, delimiter: Char = ','): String =
        if (value.any { it == delimiter || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value
}
