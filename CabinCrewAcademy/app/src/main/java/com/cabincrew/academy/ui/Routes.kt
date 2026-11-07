package com.cabincrew.academy.ui

import com.cabincrew.academy.domain.StudyConfig

object Routes {
    const val DASHBOARD = "dashboard"
    const val STUDY_SETUP = "study_setup"
    const val EXAM_SETUP = "exam_setup"
    const val QUESTIONS = "questions"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val IMPORT = "import"

    const val STUDY = "study?mode={mode}&category={category}&difficulty={difficulty}&shuffle={shuffle}&seed={seed}&start={start}"
    fun study(c: StudyConfig, start: Int = 0) =
        "study?mode=${c.mode.name}&category=${c.categoryId}&difficulty=${c.difficulty?.name ?: "-"}" +
            "&shuffle=${c.shuffle}&seed=${c.seed}&start=$start"

    const val EXAM = "exam?count={count}&minutes={minutes}&pass={pass}&categories={categories}"
    fun exam(count: Int, minutes: Int, pass: Int, categories: List<Long>) =
        "exam?count=$count&minutes=$minutes&pass=$pass&categories=${categories.joinToString(",")}"

    const val EXAM_RESULT = "exam_result/{examId}"
    fun examResult(id: Long) = "exam_result/$id"

    const val EDIT_QUESTION = "question_edit?id={id}"
    fun editQuestion(id: Long = -1) = "question_edit?id=$id"
}
