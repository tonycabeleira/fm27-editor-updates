package com.cabincrew.academy.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class Difficulty(val label: String) {
    EASY("Easy"), MEDIUM("Medium"), HARD("Hard");

    companion object {
        fun parse(raw: String?): Difficulty? = when (raw?.trim()?.lowercase()) {
            null, "" -> null
            "easy", "e", "1", "low", "facil", "fácil" -> EASY
            "medium", "m", "2", "normal", "moderate", "medio", "médio" -> MEDIUM
            "hard", "h", "3", "high", "difficult", "dificil", "difícil" -> HARD
            else -> null
        }
    }
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("categoryId"), Index(value = ["externalId"], unique = true)]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The user-facing identifier from the imported file (e.g. "Q001"). Unique. */
    val externalId: String,
    val categoryId: Long,
    val text: String,
    val options: List<String>,
    /** Zero-based index into [options]. */
    val correctIndex: Int,
    val explanation: String? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "question_progress",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class QuestionProgressEntity(
    @PrimaryKey val questionId: Long,
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastCorrect: Boolean? = null,
    val lastAnsweredAt: Long? = null,
    val bookmarked: Boolean = false,
    val favourite: Boolean = false,
    val markedDifficult: Boolean = false,
)

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long,
    val timeLimitSeconds: Int,
    val timeTakenSeconds: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val scorePercent: Float,
    val passingScore: Int,
    val passed: Boolean,
    val scopeLabel: String,
    val timedOut: Boolean = false,
)

/** Snapshot of every exam question so the correction report survives later edits/deletions. */
@Entity(
    tableName = "exam_answers",
    foreignKeys = [
        ForeignKey(
            entity = ExamResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("examId")]
)
data class ExamAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val position: Int,
    val questionId: Long?,
    val externalId: String,
    val categoryName: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val selectedIndex: Int?,
    val markedForReview: Boolean,
    val explanation: String?,
) {
    val isCorrect: Boolean get() = selectedIndex == correctIndex
    val isUnanswered: Boolean get() = selectedIndex == null
}

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    /** java.time.LocalDate.toEpochDay() */
    @PrimaryKey val epochDay: Long,
    val answered: Int = 0,
    val correct: Int = 0,
)

data class QuestionWithProgress(
    @Embedded val question: QuestionEntity,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val progress: QuestionProgressEntity?,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
)

data class CategoryWithStats(
    val id: Long,
    val name: String,
    val description: String,
    val questionCount: Int,
    val studiedCount: Int,
    val answered: Int,
    val correct: Int,
) {
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
    val completion: Float get() = if (questionCount == 0) 0f else studiedCount.toFloat() / questionCount
}

data class ProgressTotals(
    val totalQuestions: Int,
    val studied: Int,
    val answered: Int,
    val correct: Int,
    val incorrect: Int,
    val bookmarked: Int,
    val favourites: Int,
    val markedDifficult: Int,
    val needsReview: Int,
)
