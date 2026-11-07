package com.cabincrew.academy.ui.questions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryEntity
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.data.db.QuestionEntity
import com.cabincrew.academy.data.db.QuestionWithProgress
import com.cabincrew.academy.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FlagFilter(val label: String) {
    ALL("All"), BOOKMARKED("Bookmarked"), FAVOURITES("Favourites"), DIFFICULT("Marked difficult"),
    INCORRECT("Answered incorrectly"), UNSTUDIED("Not studied")
}

data class BankFilters(
    val query: String = "",
    val categoryId: Long = -1,
    val difficulty: Difficulty? = null,
    val flag: FlagFilter = FlagFilter.ALL,
)

data class BankState(
    val loading: Boolean = true,
    val all: Int = 0,
    val results: List<QuestionWithProgress> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filters: BankFilters = BankFilters(),
)

class QuestionBankViewModel(private val repo: StudyRepository) : ViewModel() {
    private val filters = MutableStateFlow(BankFilters())

    val state: StateFlow<BankState> = combine(repo.questionsWithProgress, repo.categories, filters) { qs, cats, f ->
        val terms = f.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val results = qs.filter { q ->
            (f.categoryId < 0 || q.question.categoryId == f.categoryId) &&
                (f.difficulty == null || q.question.difficulty == f.difficulty) &&
                when (f.flag) {
                    FlagFilter.ALL -> true
                    FlagFilter.BOOKMARKED -> q.progress?.bookmarked == true
                    FlagFilter.FAVOURITES -> q.progress?.favourite == true
                    FlagFilter.DIFFICULT -> q.progress?.markedDifficult == true
                    FlagFilter.INCORRECT -> q.progress?.lastCorrect == false
                    FlagFilter.UNSTUDIED -> (q.progress?.timesAnswered ?: 0) == 0
                } &&
                (terms.isEmpty() || run {
                    val hay = buildString {
                        append(q.question.text).append(' ').append(q.question.externalId).append(' ')
                        q.question.options.forEach { append(it).append(' ') }
                        append(q.question.explanation.orEmpty())
                    }.lowercase()
                    terms.all { it in hay }
                })
        }
        BankState(false, qs.size, results, cats, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankState())

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun setCategory(id: Long) = filters.update { it.copy(categoryId = id) }
    fun setDifficulty(d: Difficulty?) = filters.update { it.copy(difficulty = d) }
    fun setFlag(f: FlagFilter) = filters.update { it.copy(flag = f) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteQuestion(id) }
    fun toggleBookmark(q: QuestionWithProgress) = viewModelScope.launch { repo.setBookmarked(q.question.id, !(q.progress?.bookmarked ?: false)) }
    fun toggleFavourite(q: QuestionWithProgress) = viewModelScope.launch { repo.setFavourite(q.question.id, !(q.progress?.favourite ?: false)) }
}

data class EditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val id: Long = 0,
    val externalId: String = "",
    val categoryId: Long? = null,
    val text: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val errors: List<String> = emptyList(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class QuestionEditorViewModel(private val repo: StudyRepository, handle: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    val categories: StateFlow<List<CategoryEntity>> = repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        val id = handle.get<Long>("id") ?: -1L
        viewModelScope.launch {
            val q = if (id > 0) repo.getQuestion(id) else null
            _state.value = if (q != null) EditorState(
                loading = false, isNew = false, id = q.id, externalId = q.externalId, categoryId = q.categoryId,
                text = q.text, options = q.options, correctIndex = q.correctIndex, explanation = q.explanation.orEmpty(),
                difficulty = q.difficulty,
            ) else EditorState(loading = false, externalId = repo.nextManualId())
        }
    }

    fun update(transform: (EditorState) -> EditorState) = _state.update { transform(it).copy(errors = emptyList()) }

    fun setOption(i: Int, value: String) = update { s -> s.copy(options = s.options.toMutableList().also { it[i] = value }) }
    fun addOption() = update { s -> if (s.options.size >= 10) s else s.copy(options = s.options + "") }
    fun removeOption(i: Int) = update { s ->
        if (s.options.size <= 2) s else {
            val newCorrect = when {
                s.correctIndex == i -> 0
                s.correctIndex > i -> s.correctIndex - 1
                else -> s.correctIndex
            }
            s.copy(options = s.options.toMutableList().also { it.removeAt(i) }, correctIndex = newCorrect)
        }
    }

    fun createCategory(name: String) = viewModelScope.launch {
        runCatching { repo.createCategory(name) }
            .onSuccess { id -> update { it.copy(categoryId = id) } }
            .onFailure { e -> _state.update { it.copy(errors = listOf(e.message ?: "Could not create category")) } }
    }

    fun save() {
        val s = _state.value
        val errors = buildList {
            if (s.externalId.isBlank()) add("Enter a question ID.")
            if (s.categoryId == null) add("Choose a category.")
            if (s.text.isBlank()) add("Enter the question text.")
            if (s.options.any { it.isBlank() }) add("Fill in every answer option (or remove empty ones).")
            if (s.options.size < 2) add("At least two options are required.")
            if (s.correctIndex !in s.options.indices) add("Select the correct answer.")
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        viewModelScope.launch {
            runCatching {
                repo.saveQuestion(
                    QuestionEntity(
                        id = s.id, externalId = s.externalId.trim(), categoryId = s.categoryId!!, text = s.text.trim(),
                        options = s.options.map { it.trim() }, correctIndex = s.correctIndex,
                        explanation = s.explanation.trim().ifEmpty { null }, difficulty = s.difficulty,
                    )
                )
            }.onSuccess { _state.update { it.copy(saved = true) } }
                .onFailure { e -> _state.update { it.copy(errors = listOf(e.message ?: "Could not save")) } }
        }
    }

    fun delete() = viewModelScope.launch {
        repo.deleteQuestion(_state.value.id)
        _state.update { it.copy(deleted = true) }
    }
}
