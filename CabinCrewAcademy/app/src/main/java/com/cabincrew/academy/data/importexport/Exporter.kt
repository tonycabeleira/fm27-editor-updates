package com.cabincrew.academy.data.importexport

import com.cabincrew.academy.data.db.CategoryEntity
import com.cabincrew.academy.data.db.QuestionEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject

object Exporter {
    private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

    fun toJson(categories: List<CategoryEntity>, questions: List<QuestionEntity>): String {
        val byCat = questions.groupBy { it.categoryId }
        val root: JsonObject = buildJsonObject {
            put("format", "cabin-crew-academy")
            put("version", 1)
            put("exportedAt", java.time.OffsetDateTime.now().toString())
            putJsonArray("categories") {
                categories.forEach { c ->
                    addJsonObject {
                        put("name", c.name)
                        if (c.description.isNotBlank()) put("description", c.description)
                        putJsonArray("questions") {
                            byCat[c.id].orEmpty().forEach { q ->
                                addJsonObject {
                                    put("id", q.externalId)
                                    put("question", q.text)
                                    putJsonArray("options") { q.options.forEach { add(it) } }
                                    put("correctAnswer", q.correctIndex)
                                    q.explanation?.let { put("explanation", it) }
                                    put("difficulty", q.difficulty.name.lowercase())
                                }
                            }
                        }
                    }
                }
            }
        }
        return prettyJson.encodeToString(JsonObject.serializer(), root)
    }

    fun toCsv(categories: List<CategoryEntity>, questions: List<QuestionEntity>): String {
        val maxOptions = (questions.maxOfOrNull { it.options.size } ?: 4).coerceAtLeast(4)
        val catName = categories.associate { it.id to it.name }
        val sb = StringBuilder()
        val header = listOf("id", "category", "question") + (1..maxOptions).map { "option$it" } +
            listOf("correctAnswer", "explanation", "difficulty")
        sb.append(header.joinToString(",")).append("\r\n")
        questions.forEach { q ->
            val cells = listOf(q.externalId, catName[q.categoryId].orEmpty(), q.text) +
                (0 until maxOptions).map { q.options.getOrElse(it) { "" } } +
                listOf(q.correctIndex.toString(), q.explanation.orEmpty(), q.difficulty.name.lowercase())
            sb.append(cells.joinToString(",") { CsvReader.escape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** Placeholder templates – these contain no study content, only the structure to fill in. */
    val JSON_TEMPLATE = """
{
  "categories": [
    {
      "name": "Aviation Knowledge",
      "description": "Optional description of the category",
      "questions": [
        {
          "id": "Q001",
          "question": "Example question?",
          "options": [
            "Option A",
            "Option B",
            "Option C",
            "Option D"
          ],
          "correctAnswer": 1,
          "explanation": "Explanation of the correct answer.",
          "difficulty": "medium"
        }
      ]
    }
  ]
}
""".trimStart()

    val CSV_TEMPLATE =
        "id,category,question,option1,option2,option3,option4,correctAnswer,explanation,difficulty\r\n" +
            "Q001,Aviation Knowledge,Example question?,Option A,Option B,Option C,Option D,1,Explanation of the correct answer.,medium\r\n"
}
