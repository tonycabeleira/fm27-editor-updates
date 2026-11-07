package com.cabincrew.academy.ui.exam

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.db.ExamAnswerEntity
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.data.db.QuestionEntity
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.AppSettings
import com.cabincrew.academy.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExamSetupViewModel(repo: StudyRepository, settings: SettingsRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryWithStats>?> =
        repo.categoriesWithStats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings?> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<ExamResultEntity>> =
        repo.examResults.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ExamQuestion(val question: QuestionEntity, val categoryName: String)

data class ExamUiState(
    val loading: Boolean = true,
    val questions: List<ExamQuestion> = emptyList(),
    val index: Int = 0,
    val answers: List<Int?> = emptyList(),
    val marked: Set<Int> = emptySet(),
    /** null = untimed */
    val remainingSeconds: Int? = null,
    val timeLimitSeconds: Int = 0,
    val passingScore: Int = 75,
    val scopeLabel: String = "All categories",
    val submitting: Boolean = false,
    val finishedExamId: Long? = null,
) {
    val answeredCount get() = answers.count { it != null }
    val unansweredCount get() = answers.size - answeredCount
}

class ExamSessionViewModel(
    private val repo: StudyRepository,
    handle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ExamUiState())
    val state: StateFlow<ExamUiState> = _state.asStateFlow()

    private var startedAtWall = 0L
    private var startedAtElapsed = 0L
    private var timerJob: Job? = null

    init {
        val count = handle.get<Int>("count") ?: 20
        val minutes = handle.get<Int>("minutes") ?: 30
        val pass = handle.get<Int>("pass") ?: 75
        val cats = handle.get<String>("categories").orEmpty().split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        viewModelScope.launch {
            val all = repo.loadQuestionsWithProgress()
            val pool = all.filter { cats.isEmpty() || it.question.categoryId in cats }
            val picked = pool.shuffled().take(count.coerceAtLeast(1))
            val scope = if (cats.isEmpty()) "All categories" else {
                val names = pool.mapNotNull { it.category?.name }.distinct()
                if (names.size <= 2) names.joinToString(", ") else "${names.size} categories"
            }
            startedAtWall = System.currentTimeMillis()
            startedAtElapsed = SystemClock.elapsedRealtime()
            _state.value = ExamUiState(
                loading = false,
                questions = picked.map { ExamQuestion(it.question, it.category?.name ?: "") },
                answers = List(picked.size) { null },
                remainingSeconds = if (minutes > 0) minutes * 60 else null,
                timeLimitSeconds = minutes * 60,
                passingScore = pass,
                scopeLabel = scope,
            )
            if (minutes > 0 && picked.isNotEmpty()) startTimer(minutes * 60)
        }
    }

    private fun startTimer(limit: Int) {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = ((SystemClock.elapsedRealtime() - startedAtElapsed) / 1000).toInt()
                val remaining = (limit - elapsed).coerceAtLeast(0)
                _state.update { it.copy(remainingSeconds = remaining) }
                if (remaining == 0) {
                    submit(timedOut = true)
                    break
                }
                delay(250)
            }
        }
    }

    fun select(option: Int) = _state.update { s ->
        if (s.submitting) s else s.copy(answers = s.answers.toMutableList().also { it[s.index] = option })
    }

    fun clearAnswer() = _state.update { s -> s.copy(answers = s.answers.toMutableList().also { it[s.index] = null }) }

    fun toggleMark() = _state.update { s ->
        s.copy(marked = if (s.index in s.marked) s.marked - s.index else s.marked + s.index)
    }

    fun goTo(i: Int) = _state.update { it.copy(index = i.coerceIn(0, (it.questions.size - 1).coerceAtLeast(0))) }
    fun next() = goTo(_state.value.index + 1)
    fun previous() = goTo(_state.value.index - 1)

    fun submit(timedOut: Boolean = false) {
        val s = _state.value
        if (s.submitting || s.finishedExamId != null || s.questions.isEmpty()) return
        _state.update { it.copy(submitting = true) }
        timerJob?.cancel()
        viewModelScope.launch {
            val answers = s.questions.mapIndexed { i, eq ->
                ExamAnswerEntity(
                    examId = 0,
                    position = i,
                    questionId = eq.question.id,
                    externalId = eq.question.externalId,
                    categoryName = eq.categoryName,
                    questionText = eq.question.text,
                    options = eq.question.options,
                    correctIndex = eq.question.correctIndex,
                    selectedIndex = s.answers[i],
                    markedForReview = i in s.marked,
                    explanation = eq.question.explanation,
                )
            }
            val correct = answers.count { it.isCorrect }
            val unanswered = answers.count { it.isUnanswered }
            val score = correct * 100f / answers.size
            val taken = ((SystemClock.elapsedRealtime() - startedAtElapsed) / 1000).toInt()
            val result = ExamResultEntity(
                startedAt = startedAtWall,
                finishedAt = System.currentTimeMillis(),
                timeLimitSeconds = s.timeLimitSeconds,
                timeTakenSeconds = if (s.timeLimitSeconds > 0) taken.coerceAtMost(s.timeLimitSeconds) else taken,
                totalQuestions = answers.size,
                correctCount = correct,
                incorrectCount = answers.size - correct - unanswered,
                unansweredCount = unanswered,
                scorePercent = score,
                passingScore = s.passingScore,
                passed = score >= s.passingScore,
                scopeLabel = s.scopeLabel,
                timedOut = timedOut,
            )
            val id = repo.saveExam(result, answers)
            _state.update { it.copy(finishedExamId = id) }
        }
    }
}

data class ExamResultUiState(
    val loading: Boolean = true,
    val result: ExamResultEntity? = null,
    val answers: List<ExamAnswerEntity> = emptyList(),
)

class ExamResultViewModel(private val repo: StudyRepository, handle: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(ExamResultUiState())
    val state: StateFlow<ExamResultUiState> = _state.asStateFlow()
    private val examId: Long = handle.get<Long>("examId") ?: -1L

    init {
        viewModelScope.launch {
            val data = repo.getExam(examId)
            _state.value = ExamResultUiState(false, data?.first, data?.second.orEmpty())
        }
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.deleteExam(examId); onDone() }
}
