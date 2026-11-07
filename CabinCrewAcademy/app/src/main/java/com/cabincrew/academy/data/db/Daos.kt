package com.cabincrew.academy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT c.id, c.name, c.description,
            (SELECT COUNT(*) FROM questions q WHERE q.categoryId = c.id) AS questionCount,
            (SELECT COUNT(*) FROM questions q JOIN question_progress p ON p.questionId = q.id
                WHERE q.categoryId = c.id AND p.timesAnswered > 0) AS studiedCount,
            (SELECT COALESCE(SUM(p.timesAnswered), 0) FROM questions q JOIN question_progress p ON p.questionId = q.id
                WHERE q.categoryId = c.id) AS answered,
            (SELECT COALESCE(SUM(p.timesCorrect), 0) FROM questions q JOIN question_progress p ON p.questionId = q.id
                WHERE q.categoryId = c.id) AS correct
        FROM categories c
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeWithStats(): Flow<List<CategoryWithStats>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface QuestionDao {
    @Transaction
    @Query("SELECT * FROM questions ORDER BY categoryId, id")
    fun observeAllWithProgress(): Flow<List<QuestionWithProgress>>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY categoryId, id")
    suspend fun getAllWithProgress(): List<QuestionWithProgress>

    @Query("SELECT * FROM questions ORDER BY categoryId, id")
    suspend fun getAll(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE externalId = :externalId LIMIT 1")
    suspend fun getByExternalId(externalId: String): QuestionEntity?

    @Query("SELECT externalId FROM questions")
    suspend fun getAllExternalIds(): List<String>

    @Query("SELECT COUNT(*) FROM questions")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM question_progress WHERE questionId = :questionId")
    suspend fun get(questionId: Long): QuestionProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: QuestionProgressEntity)

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM questions) AS totalQuestions,
            (SELECT COUNT(*) FROM question_progress WHERE timesAnswered > 0) AS studied,
            (SELECT COALESCE(SUM(timesAnswered), 0) FROM question_progress) AS answered,
            (SELECT COALESCE(SUM(timesCorrect), 0) FROM question_progress) AS correct,
            (SELECT COALESCE(SUM(timesIncorrect), 0) FROM question_progress) AS incorrect,
            (SELECT COUNT(*) FROM question_progress WHERE bookmarked = 1) AS bookmarked,
            (SELECT COUNT(*) FROM question_progress WHERE favourite = 1) AS favourites,
            (SELECT COUNT(*) FROM question_progress WHERE markedDifficult = 1) AS markedDifficult,
            (SELECT COUNT(*) FROM question_progress WHERE lastCorrect = 0) AS needsReview
        """
    )
    fun observeTotals(): Flow<ProgressTotals>

    /** Clears answer statistics but keeps bookmarks, favourites and difficult flags. */
    @Query(
        """UPDATE question_progress SET timesAnswered = 0, timesCorrect = 0, timesIncorrect = 0,
           lastCorrect = NULL, lastAnsweredAt = NULL"""
    )
    suspend fun resetAnswerStats()

    @Query("DELETE FROM question_progress")
    suspend fun deleteAll()
}

@Dao
interface ExamDao {
    @Insert
    suspend fun insertResult(result: ExamResultEntity): Long

    @Insert
    suspend fun insertAnswers(answers: List<ExamAnswerEntity>)

    @Query("SELECT * FROM exam_results ORDER BY finishedAt DESC")
    fun observeResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE id = :id")
    suspend fun getResult(id: Long): ExamResultEntity?

    @Query("SELECT * FROM exam_answers WHERE examId = :examId ORDER BY position")
    suspend fun getAnswers(examId: Long): List<ExamAnswerEntity>

    @Query("DELETE FROM exam_results WHERE id = :id")
    suspend fun deleteResult(id: Long)

    @Query("DELETE FROM exam_results")
    suspend fun deleteAll()
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE epochDay = :day")
    suspend fun get(day: Long): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activity ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<DailyActivityEntity>>

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()
}
