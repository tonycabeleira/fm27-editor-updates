package com.cabincrew.academy

import com.cabincrew.academy.data.importexport.QuestionParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals

/** Neutral placeholder questions used only by UI tests – not study content. */
object TestFixtures {
    fun seed(container: AppContainer) = runBlocking {
        val cats = listOf("Category One", "Category Two", "Category Three")
        val sb = StringBuilder("""{"categories":[""")
        cats.forEachIndexed { ci, c ->
            if (ci > 0) sb.append(",")
            sb.append("""{"name":"$c","questions":[""")
            (1..8).forEach { qi ->
                if (qi > 1) sb.append(",")
                val diff = listOf("easy", "medium", "hard")[qi % 3]
                sb.append("""{"id":"T$ci$qi","question":"Sample fixture question $qi for $c?","options":["First option","Second option","Third option","Fourth option"],"correctAnswer":${qi % 4},"explanation":"Fixture explanation text for question $qi.","difficulty":"$diff"}""")
            }
            sb.append("]}")
        }
        sb.append("]}")
        val parsed = QuestionParser.parse("fixture.json", sb.toString())
        assertEquals(24, parsed.questions.size)
        container.repository.applyImport(parsed, overwriteExisting = false)
        container.settings.setUserName("Alex")
        container.settings.setExamQuestionCount(10)
    }
}
