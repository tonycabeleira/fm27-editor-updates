package com.cabincrew.academy.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.data.db.QuestionProgressEntity
import com.cabincrew.academy.data.db.QuestionWithProgress
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.SettingsRepository
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.domain.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class StudySetupViewModel(repo: StudyRepository) : ViewModel() {
    val questions: StateFlow<List<QuestionWithProgress>?> =
        repo.questionsWithProgress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val categories: StateFlow<List<CategoryWithStats>> =
        repo.categoriesWithStats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class AnswerState(val selected: Int? = null, val submitted: Boolean = false)

data class StudyUiState(
    val loading: Boolean = true,
    val config: StudyConfig = StudyConfig(),
    val title: String = "",
    val questions: List<QuestionWithProgress> = emptyList(),
    val index: Int = 0,
    val answers: Map<Long, AnswerState> = emptyMap(),
    val showSummary: Boolean = false,
) {
    val current: QuestionWithProgress? get() = questions.getOrNull(index)
    val currentAnswer: AnswerState get() = current?.let { answers[it.question.id] } ?: AnswerState()
    val answeredCount: Int get() = answers.values.count { it.submitted }
    val correctCount: Int
        get() = questions.count { q -> answers[q.question.id]?.let { it.submitted && it.selected == q.question.correctIndex } == true }
}

class StudySessionViewModel(
    private val repo: StudyRepository,
    private val settings: SettingsRepository,
    handle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    init {
        val config = StudyConfig(
            mode = runCatching { StudyMode.valueOf(handle.get<String>("mode") ?: "ALL") }.getOrDefault(StudyMode.ALL),
            categoryId = handle.get<Long>("category") ?: -1L,
            difficulty = handle.get<String>("difficulty")?.takeIf { it != "-" }?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() },
            shuffle = handle.get<Boolean>("shuffle") ?: false,
            seed = handle.get<Long>("seed") ?: 0L,
        )
        val start = handle.get<Int>("start") ?: 0
        load(config, start)
    }

    private fun load(config: StudyConfig, start: Int) = viewModelScope.launch {
        val all = repo.loadQuestionsWithProgress()
        val selected = config.select(all)
        val catName = if (config.categoryId >= 0) all.firstOrNull { it.question.categoryId == config.categoryId }?.category?.name else null
        val title = buildString {
            append(if (config.mode == StudyMode.ALL) (if (config.shuffle) "Random practice" else "Study") else config.mode.title)
            if (catName != null) append(" • ").append(catName)
            if (config.difficulty != null) append(" • ").append(config.difficulty.label)
        }
        _state.value = StudyUiState(
            loading = false, config = config, title = title, questions = selected,
            index = start.coerceIn(0, (selected.size - 1).coerceAtLeast(0)),
        )
        persistPosition()
    }

    private fun persistPosition() {
        val s = _state.value
        if (s.questions.isEmpty()) return
        viewModelScope.launch { settings.setLastStudySession(s.config.encode(s.index)) }
    }

    fun select(option: Int) = _state.update { s ->
        val q = s.current ?: return@update s
        if (s.currentAnswer.submitted) s else s.copy(answers = s.answers + (q.question.id to AnswerState(selected = option)))
    }

    fun submit() {
        val s = _state.value
        val q = s.current ?: return
        val ans = s.currentAnswer
        if (ans.submitted || ans.selected == null) return
        val correct = ans.selected == q.question.correctIndex
        _state.update { it.copy(answers = it.answers + (q.question.id to ans.copy(submitted = true))) }
        viewModelScope.launch { repo.recordAnswer(q.question.id, correct) }
    }

    fun next() {
        val s = _state.value
        if (s.index < s.questions.lastIndex) {
            _state.update { it.copy(index = it.index + 1) }
            persistPosition()
        } else {
            _state.update { it.copy(showSummary = true) }
        }
    }

    fun previous() {
        if (_state.value.index > 0) {
            _state.update { it.copy(index = it.index - 1) }
            persistPosition()
        }
    }

    fun goTo(i: Int) {
        _state.update { it.copy(index = i.coerceIn(0, (it.questions.size - 1).coerceAtLeast(0)), showSummary = false) }
        persistPosition()
    }

    fun dismissSummary() = _state.update { it.copy(showSummary = false) }

    /** Restarts the session from the first question (new random order when shuffling). */
    fun restart() {
        val s = _state.value
        val cfg = if (s.config.shuffle) s.config.copy(seed = Random.nextLong()) else s.config
        val reordered = if (cfg.shuffle) s.questions.shuffled(Random(cfg.seed)) else s.questions
        _state.value = s.copy(config = cfg, questions = reordered, index = 0, answers = emptyMap(), showSummary = false)
        persistPosition()
    }

    /** Restarts with only the questions answered incorrectly in this session. */
    fun retryIncorrect() {
        val s = _state.value
        val wrong = s.questions.filter { q -> s.answers[q.question.id]?.let { it.submitted && it.selected != q.question.correctIndex } == true }
        if (wrong.isEmpty()) return
        _state.value = s.copy(questions = wrong, index = 0, answers = emptyMap(), showSummary = false, title = "Review • incorrect answers")
    }

    private fun updateProgress(transform: (QuestionProgressEntity) -> QuestionProgressEntity) {
        _state.update { s ->
            val q = s.current ?: return@update s
            val p = transform(q.progress ?: QuestionProgressEntity(q.question.id))
            s.copy(questions = s.questions.toMutableList().also { it[s.index] = q.copy(progress = p) })
        }
    }

    fun toggleBookmark() {
        val q = _state.value.current ?: return
        val v = !(q.progress?.bookmarked ?: false)
        updateProgress { it.copy(bookmarked = v) }
        viewModelScope.launch { repo.setBookmarked(q.question.id, v) }
    }

    fun toggleFavourite() {
        val q = _state.value.current ?: return
        val v = !(q.progress?.favourite ?: false)
        updateProgress { it.copy(favourite = v) }
        viewModelScope.launch { repo.setFavourite(q.question.id, v) }
    }

    fun toggleDifficult() {
        val q = _state.value.current ?: return
        val v = !(q.progress?.markedDifficult ?: false)
        updateProgress { it.copy(markedDifficult = v) }
        viewModelScope.launch { repo.setMarkedDifficult(q.question.id, v) }
    }

    fun clearResume() = viewModelScope.launch { settings.setLastStudySession(null) }
}
