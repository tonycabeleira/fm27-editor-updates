package com.cabincrew.academy

import com.cabincrew.academy.data.db.DailyActivityEntity
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.data.importexport.CsvReader
import com.cabincrew.academy.data.importexport.QuestionParser
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.domain.StudyMode
import com.cabincrew.academy.domain.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionParserTest {

    private val validJson = """
        {
          "categories": [
            {
              "name": "Aviation Knowledge",
              "questions": [
                { "id": "Q001", "question": "Example question?", "options": ["A","B","C","D"], "correctAnswer": 1, "explanation": "Because." },
                { "id": "Q002", "question": "Second?", "options": ["Yes","No"], "correctAnswer": 0, "difficulty": "hard" }
              ]
            },
            { "name": "First Aid", "questions": [] }
          ]
        }
    """.trimIndent()

    @Test fun parsesValidJson() {
        val r = QuestionParser.parse("q.json", validJson)
        assertEquals(emptyList<Any>(), r.errors)
        assertEquals(2, r.questions.size)
        assertEquals(1, r.questions[0].correctIndex)
        assertEquals("Because.", r.questions[0].explanation)
        assertEquals(Difficulty.HARD, r.questions[1].difficulty)
        assertEquals(setOf("Aviation Knowledge", "First Aid"), r.categories.keys)
    }

    @Test fun reportsInvalidQuestionsWithLocation() {
        val json = """
            {"categories":[{"name":"X","questions":[
              {"id":"A1","question":"Q?","options":["a","b"],"correctAnswer":2},
              {"id":"A2","question":"","options":["a"],"correctAnswer":0},
              {"id":"A3","question":"ok?","options":["a","b","c"],"correctAnswer":2}
            ]}]}
        """.trimIndent()
        val r = QuestionParser.parse("bad.json", json)
        assertEquals(1, r.questions.size)
        assertEquals(2, r.errors.size)
        assertTrue(r.errors[0].location.contains("A1"))
        assertTrue(r.errors[0].message.contains("0..1"))
    }

    @Test fun invalidJsonSyntaxIsReported() {
        val r = QuestionParser.parse("broken.json", "{ \"categories\": [ ")
        assertTrue(r.questions.isEmpty())
        assertEquals(1, r.errors.size)
        assertTrue(r.errors[0].message.startsWith("Invalid JSON"))
    }

    @Test fun duplicateIdsAreRejected() {
        val json = """{"categories":[{"name":"X","questions":[
            {"id":"D1","question":"a?","options":["1","2"],"correctAnswer":0},
            {"id":"d1","question":"b?","options":["1","2"],"correctAnswer":1}]}]}"""
        val r = QuestionParser.parse("dup.json", json)
        assertEquals(1, r.questions.size)
        assertTrue(r.errors.single().message.contains("Duplicate id"))
    }

    @Test fun flatQuestionsArrayIsSupported() {
        val json = """{"questions":[{"category":"English","question":"a?","options":["x","y"],"correctAnswer":1}]}"""
        val r = QuestionParser.parse("flat.json", json)
        assertEquals(1, r.questions.size)
        assertEquals("English", r.questions[0].categoryName)
        assertTrue(r.questions[0].externalId.startsWith("AUTO-"))
    }

    @Test fun parsesCsvWithQuotesSemicolonsAndLetters() {
        val csv = "﻿id;category;question;option1;option2;option3;option4;correctAnswer;explanation;difficulty\r\n" +
            "C1;Customer Service;\"What is \"\"service\"\"; really?\";A;B;C;;C;\"Multi\nline\";easy\r\n" +
            "C2;Customer Service;Two options;Yes;No;;;0;;\r\n"
        val r = QuestionParser.parse("q.csv", csv)
        assertEquals(emptyList<Any>(), r.errors)
        assertEquals(2, r.questions.size)
        val q = r.questions[0]
        assertEquals("What is \"service\"; really?", q.text)
        assertEquals(listOf("A", "B", "C"), q.options)
        assertEquals(2, q.correctIndex)
        assertEquals("Multi\nline", q.explanation)
        assertEquals(Difficulty.EASY, q.difficulty)
        assertEquals(2, r.questions[1].options.size)
    }

    @Test fun csvMissingColumnsIsReported() {
        val r = QuestionParser.parse("q.csv", "question,option1\nfoo,bar\n")
        assertTrue(r.questions.isEmpty())
        assertTrue(r.errors.single().message.contains("Missing column"))
    }

    @Test fun csvOutOfRangeAnswerIsReported() {
        val r = QuestionParser.parse("q.csv", "category,question,option1,option2,correctAnswer\nX,Q?,a,b,5\n")
        assertTrue(r.questions.isEmpty())
        assertTrue(r.errors.single().location.contains("row 2"))
    }

    @Test fun csvEscapeRoundTrip() {
        val values = listOf("plain", "with,comma", "with \"quote\"", "multi\nline")
        val line = values.joinToString(",") { CsvReader.escape(it) }
        assertEquals(values, CsvReader.read(line, ',').single())
    }

    @Test fun shippedTemplatesAreValid() {
        val dir = java.io.File("../docs/templates")
        val files = dir.listFiles().orEmpty().filter { it.extension == "json" || it.extension == "csv" }
        assertEquals(3, files.size)
        files.forEach { f ->
            val r = QuestionParser.parse(f.name, f.readText())
            assertEquals("${f.name}: ${r.errors}", 0, r.errors.size)
            assertEquals(1, r.questions.size)
            assertEquals(1, r.questions[0].correctIndex)
        }
        // In-app templates must be valid too.
        assertEquals(0, QuestionParser.parse("t.json", com.cabincrew.academy.data.importexport.Exporter.JSON_TEMPLATE).errors.size)
        assertEquals(0, QuestionParser.parse("t.csv", com.cabincrew.academy.data.importexport.Exporter.CSV_TEMPLATE).errors.size)
    }

    @Test fun exportRoundTrip() {
        val cat = com.cabincrew.academy.data.db.CategoryEntity(id = 1, name = "Cat, \"quoted\"")
        val qs = listOf(
            com.cabincrew.academy.data.db.QuestionEntity(id = 1, externalId = "X1", categoryId = 1, text = "Line1\nLine2, with comma?",
                options = listOf("a", "b; c", "d \"e\""), correctIndex = 2, explanation = "why", difficulty = Difficulty.HARD),
        )
        listOf(
            com.cabincrew.academy.data.importexport.Exporter.toJson(listOf(cat), qs) to "e.json",
            com.cabincrew.academy.data.importexport.Exporter.toCsv(listOf(cat), qs) to "e.csv",
        ).forEach { (text, name) ->
            val r = QuestionParser.parse(name, text)
            assertEquals("$name ${r.errors}", 0, r.errors.size)
            val q = r.questions.single()
            assertEquals("X1", q.externalId)
            assertEquals(cat.name, q.categoryName)
            assertEquals(qs[0].text, q.text)
            assertEquals(qs[0].options, q.options)
            assertEquals(2, q.correctIndex)
            assertEquals(Difficulty.HARD, q.difficulty)
        }
    }

    @Test fun autoIdIsStable() {
        assertEquals(QuestionParser.autoId("Cat", "Text?"), QuestionParser.autoId(" cat ", "text?"))
    }

    @Test fun studyConfigRoundTrip() {
        val c = StudyConfig(StudyMode.INCORRECT, 3, Difficulty.HARD, true, 42)
        val decoded = StudyConfig.decode(c.encode(7))
        assertNotNull(decoded)
        assertEquals(c, decoded!!.first)
        assertEquals(7, decoded.second)
    }

    @Test fun streaks() {
        val today = 1000L
        val days = listOf(1000L, 999L, 998L, 990L, 991L, 992L, 993L).map { DailyActivityEntity(it, answered = 3) }
        val s = Streaks.compute(days, today)
        assertEquals(3, s.current)
        assertEquals(4, s.best)
        assertTrue(s.studiedToday)
        val yesterdayOnly = Streaks.compute(listOf(DailyActivityEntity(999L, 2)), today)
        assertEquals(1, yesterdayOnly.current)
    }
}
