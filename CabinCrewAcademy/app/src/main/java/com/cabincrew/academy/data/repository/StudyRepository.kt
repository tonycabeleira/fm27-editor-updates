package com.cabincrew.academy.data.repository

import androidx.room.withTransaction
import com.cabincrew.academy.data.db.AppDatabase
import com.cabincrew.academy.data.db.CategoryEntity
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.db.DailyActivityEntity
import com.cabincrew.academy.data.db.ExamAnswerEntity
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.data.db.ProgressTotals
import com.cabincrew.academy.data.db.QuestionEntity
import com.cabincrew.academy.data.db.QuestionProgressEntity
import com.cabincrew.academy.data.db.QuestionWithProgress
import com.cabincrew.academy.data.importexport.Exporter
import com.cabincrew.academy.data.importexport.ParseResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class ImportPlan(
    val newQuestions: Int,
    val existingQuestions: Int,
    val newCategories: List<String>,
)

data class ImportOutcome(
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val categoriesCreated: Int,
)

class DuplicateNameException(message: String) : Exception(message)

class StudyRepository(private val db: AppDatabase) {
    private val categoryDao = db.categoryDao()
    private val questionDao = db.questionDao()
    private val progressDao = db.progressDao()
    private val examDao = db.examDao()
    private val dailyDao = db.dailyActivityDao()

    // ------------------------------------------------------------ observe
    val categoriesWithStats: Flow<List<CategoryWithStats>> = categoryDao.observeWithStats()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val questionsWithProgress: Flow<List<QuestionWithProgress>> = questionDao.observeAllWithProgress()
    val totals: Flow<ProgressTotals> = progressDao.observeTotals()
    val examResults: Flow<List<ExamResultEntity>> = examDao.observeResults()
    val dailyActivity: Flow<List<DailyActivityEntity>> = dailyDao.observeAll()

    suspend fun loadQuestionsWithProgress(): List<QuestionWithProgress> = questionDao.getAllWithProgress()

    // ------------------------------------------------------------ categories
    suspend fun createCategory(name: String, description: String = ""): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Category name cannot be empty." }
        if (categoryDao.findByName(trimmed) != null) throw DuplicateNameException("A category called \"$trimmed\" already exists.")
        return categoryDao.insert(CategoryEntity(name = trimmed, description = description.trim()))
    }

    suspend fun renameCategory(id: Long, newName: String, description: String) {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty()) { "Category name cannot be empty." }
        val existing = categoryDao.findByName(trimmed)
        if (existing != null && existing.id != id) throw DuplicateNameException("A category called \"$trimmed\" already exists.")
        val cat = categoryDao.getById(id) ?: return
        categoryDao.update(cat.copy(name = trimmed, description = description.trim()))
    }

    /** Deletes the category and (cascade) all of its questions and their progress. */
    suspend fun deleteCategory(id: Long) {
        categoryDao.getById(id)?.let { categoryDao.delete(it) }
    }

    // ------------------------------------------------------------ questions
    suspend fun getQuestion(id: Long): QuestionEntity? = questionDao.getById(id)

    suspend fun saveQuestion(question: QuestionEntity): Long {
        val clash = questionDao.getByExternalId(question.externalId)
        if (clash != null && clash.id != question.id) {
            throw DuplicateNameException("Another question already uses the id \"${question.externalId}\".")
        }
        return if (question.id == 0L) {
            questionDao.insert(question.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            questionDao.update(question.copy(updatedAt = System.currentTimeMillis()))
            question.id
        }
    }

    suspend fun deleteQuestion(id: Long) = questionDao.deleteById(id)

    suspend fun nextManualId(): String {
        val ids = questionDao.getAllExternalIds().toSet()
        var n = ids.size + 1
        while (true) {
            val candidate = "M%04d".format(n)
            if (candidate !in ids) return candidate
            n++
        }
    }

    // ------------------------------------------------------------ progress
    private suspend fun progressOf(questionId: Long) = progressDao.get(questionId) ?: QuestionProgressEntity(questionId)

    suspend fun recordAnswer(questionId: Long, correct: Boolean) = db.withTransaction {
        recordAnswerInternal(questionId, correct)
        bumpDaily(1, if (correct) 1 else 0)
    }

    private suspend fun recordAnswerInternal(questionId: Long, correct: Boolean) {
        val p = progressOf(questionId)
        progressDao.upsert(
            p.copy(
                timesAnswered = p.timesAnswered + 1,
                timesCorrect = p.timesCorrect + if (correct) 1 else 0,
                timesIncorrect = p.timesIncorrect + if (correct) 0 else 1,
                lastCorrect = correct,
                lastAnsweredAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun bumpDaily(answered: Int, correct: Int) {
        if (answered == 0) return
        val today = LocalDate.now().toEpochDay()
        val d = dailyDao.get(today) ?: DailyActivityEntity(today)
        dailyDao.upsert(d.copy(answered = d.answered + answered, correct = d.correct + correct))
    }

    suspend fun setBookmarked(questionId: Long, value: Boolean) = db.withTransaction {
        progressDao.upsert(progressOf(questionId).copy(bookmarked = value))
    }

    suspend fun setFavourite(questionId: Long, value: Boolean) = db.withTransaction {
        progressDao.upsert(progressOf(questionId).copy(favourite = value))
    }

    suspend fun setMarkedDifficult(questionId: Long, value: Boolean) = db.withTransaction {
        progressDao.upsert(progressOf(questionId).copy(markedDifficult = value))
    }

    /** Clears study statistics, exam history and daily activity. Questions, bookmarks and favourites are kept. */
    suspend fun resetProgress() = db.withTransaction {
        progressDao.resetAnswerStats()
        examDao.deleteAll()
        dailyDao.deleteAll()
    }

    suspend fun deleteAllQuestions() = db.withTransaction {
        questionDao.deleteAll()
        categoryDao.deleteAll()
    }

    // ------------------------------------------------------------ exams
    suspend fun saveExam(result: ExamResultEntity, answers: List<ExamAnswerEntity>): Long = db.withTransaction {
        val id = examDao.insertResult(result)
        examDao.insertAnswers(answers.map { it.copy(examId = id) })
        var answered = 0
        var correct = 0
        answers.forEach { a ->
            if (a.selectedIndex != null && a.questionId != null) {
                recordAnswerInternal(a.questionId, a.isCorrect)
                answered++
                if (a.isCorrect) correct++
            }
        }
        bumpDaily(answered, correct)
        id
    }

    suspend fun getExam(id: Long): Pair<ExamResultEntity, List<ExamAnswerEntity>>? {
        val r = examDao.getResult(id) ?: return null
        return r to examDao.getAnswers(id)
    }

    suspend fun deleteExam(id: Long) = examDao.deleteResult(id)

    // ------------------------------------------------------------ import / export
    suspend fun planImport(parsed: ParseResult): ImportPlan {
        val existing = questionDao.getAllExternalIds().map { it.lowercase() }.toHashSet()
        val existingCount = parsed.questions.count { it.externalId.lowercase() in existing }
        val existingCats = categoryDao.getAll().map { it.name.lowercase() }.toHashSet()
        return ImportPlan(
            newQuestions = parsed.questions.size - existingCount,
            existingQuestions = existingCount,
            newCategories = parsed.categories.keys.filter { it.lowercase() !in existingCats },
        )
    }

    /**
     * Writes the parsed questions to the database inside one transaction.
     * @param overwriteExisting when false, questions whose id already exists are left untouched.
     * Overwriting keeps the internal id so study progress is preserved.
     */
    suspend fun applyImport(parsed: ParseResult, overwriteExisting: Boolean): ImportOutcome = db.withTransaction {
        var created = 0
        val catIds = HashMap<String, Long>()
        suspend fun categoryId(name: String): Long {
            val key = name.lowercase()
            catIds[key]?.let { return it }
            val existing = categoryDao.findByName(name)
            val id = existing?.id ?: run {
                created++
                categoryDao.insert(CategoryEntity(name = name, description = parsed.categories[name].orEmpty()))
            }
            catIds[key] = id
            return id
        }
        parsed.categories.keys.forEach { categoryId(it) }
        var added = 0
        var updated = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        parsed.questions.forEach { q ->
            val catId = categoryId(q.categoryName)
            val existing = questionDao.getByExternalId(q.externalId)
            if (existing == null) {
                questionDao.insert(
                    QuestionEntity(
                        externalId = q.externalId, categoryId = catId, text = q.text, options = q.options,
                        correctIndex = q.correctIndex, explanation = q.explanation, difficulty = q.difficulty,
                        createdAt = now, updatedAt = now,
                    )
                )
                added++
            } else if (overwriteExisting) {
                questionDao.update(
                    existing.copy(
                        categoryId = catId, text = q.text, options = q.options, correctIndex = q.correctIndex,
                        explanation = q.explanation, difficulty = q.difficulty, updatedAt = now,
                    )
                )
                updated++
            } else skipped++
        }
        ImportOutcome(added, updated, skipped, created)
    }

    suspend fun exportJson(): String = Exporter.toJson(categoryDao.getAll(), questionDao.getAll())
    suspend fun exportCsv(): String = Exporter.toCsv(categoryDao.getAll(), questionDao.getAll())
}
