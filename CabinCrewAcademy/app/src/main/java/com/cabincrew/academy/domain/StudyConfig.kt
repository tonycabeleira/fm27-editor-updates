package com.cabincrew.academy.domain

import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.data.db.QuestionWithProgress

enum class StudyMode(val title: String, val description: String) {
    ALL("All questions", "Work through the whole question bank"),
    UNSTUDIED("Not yet studied", "Questions you have never answered"),
    INCORRECT("Incorrect answers", "Questions you got wrong last time"),
    BOOKMARKED("Bookmarked", "Questions saved for later revision"),
    FAVOURITES("Favourites", "Your favourite questions"),
    DIFFICULT("Marked difficult", "Questions you flagged as difficult"),
}

/** Everything needed to rebuild a study session, so "Continue studying" can resume it. */
data class StudyConfig(
    val mode: StudyMode = StudyMode.ALL,
    /** -1 = all categories */
    val categoryId: Long = -1,
    val difficulty: Difficulty? = null,
    val shuffle: Boolean = false,
    val seed: Long = 0,
) {
    fun matches(q: QuestionWithProgress): Boolean {
        if (categoryId >= 0 && q.question.categoryId != categoryId) return false
        if (difficulty != null && q.question.difficulty != difficulty) return false
        val p = q.progress
        return when (mode) {
            StudyMode.ALL -> true
            StudyMode.UNSTUDIED -> p == null || p.timesAnswered == 0
            StudyMode.INCORRECT -> p?.lastCorrect == false
            StudyMode.BOOKMARKED -> p?.bookmarked == true
            StudyMode.FAVOURITES -> p?.favourite == true
            StudyMode.DIFFICULT -> p?.markedDifficult == true
        }
    }

    fun select(all: List<QuestionWithProgress>): List<QuestionWithProgress> {
        val filtered = all.filter { matches(it) }
            .sortedWith(compareBy({ it.category?.name?.lowercase() }, { it.question.id }))
        return if (shuffle) filtered.shuffled(kotlin.random.Random(seed)) else filtered
    }

    fun encode(index: Int): String = listOf(mode.name, categoryId, difficulty?.name ?: "-", shuffle, seed, index).joinToString("|")

    companion object {
        fun decode(raw: String?): Pair<StudyConfig, Int>? {
            val parts = raw?.split("|") ?: return null
            if (parts.size != 6) return null
            return runCatching {
                StudyConfig(
                    mode = StudyMode.valueOf(parts[0]),
                    categoryId = parts[1].toLong(),
                    difficulty = parts[2].takeIf { it != "-" }?.let { Difficulty.valueOf(it) },
                    shuffle = parts[3].toBoolean(),
                    seed = parts[4].toLong(),
                ) to parts[5].toInt()
            }.getOrNull()
        }
    }
}
