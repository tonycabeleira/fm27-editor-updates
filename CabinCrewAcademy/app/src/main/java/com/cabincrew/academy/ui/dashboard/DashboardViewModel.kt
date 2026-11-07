package com.cabincrew.academy.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.data.db.ProgressTotals
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.AppSettings
import com.cabincrew.academy.data.settings.SettingsRepository
import com.cabincrew.academy.domain.StreakInfo
import com.cabincrew.academy.domain.Streaks
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.ui.components.BarDatum
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class ActivityItem(val time: Long, val title: String, val detail: String, val isExam: Boolean, val passed: Boolean? = null, val examId: Long? = null)

data class DashboardState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val totals: ProgressTotals = ProgressTotals(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val categories: List<CategoryWithStats> = emptyList(),
    val exams: List<ExamResultEntity> = emptyList(),
    val todayAnswered: Int = 0,
    val streak: StreakInfo = StreakInfo(0, 0, false),
    val last7Days: List<BarDatum> = emptyList(),
    val resume: Pair<StudyConfig, Int>? = null,
    val recent: List<ActivityItem> = emptyList(),
) {
    val studyProgress: Float get() = if (totals.totalQuestions == 0) 0f else totals.studied.toFloat() / totals.totalQuestions
    val accuracy: Float get() = if (totals.answered == 0) 0f else totals.correct.toFloat() / totals.answered
    val bestExam: Float? get() = exams.maxOfOrNull { it.scorePercent }
    val averageExam: Float? get() = if (exams.isEmpty()) null else exams.map { it.scorePercent }.average().toFloat()
}

class DashboardViewModel(repo: StudyRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<DashboardState> = combine(
        repo.totals, repo.categoriesWithStats, repo.examResults, repo.dailyActivity, settings.settings,
    ) { totals, cats, exams, days, s ->
        val today = LocalDate.now()
        val byDay = days.associateBy { it.epochDay }
        val last7 = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            BarDatum(
                label = if (back == 0) "Today" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                value = (byDay[d.toEpochDay()]?.answered ?: 0).toFloat(),
                highlight = back == 0,
            )
        }
        val recent = buildList {
            exams.take(8).forEach {
                add(
                    ActivityItem(
                        time = it.finishedAt,
                        title = "Exam • ${it.scorePercent.toInt()}% ${if (it.passed) "Passed" else "Not passed"}",
                        detail = "${it.correctCount}/${it.totalQuestions} correct • ${it.scopeLabel}",
                        isExam = true, passed = it.passed, examId = it.id,
                    )
                )
            }
            days.filter { it.answered > 0 }.take(8).forEach {
                val date = LocalDate.ofEpochDay(it.epochDay)
                val acc = if (it.answered == 0) 0 else it.correct * 100 / it.answered
                add(
                    ActivityItem(
                        time = date.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        title = "Answered ${it.answered} question${if (it.answered == 1) "" else "s"}",
                        detail = "$acc% correct",
                        isExam = false,
                    )
                )
            }
        }.sortedByDescending { it.time }.take(6)
        DashboardState(
            loading = false,
            settings = s,
            totals = totals,
            categories = cats,
            exams = exams,
            todayAnswered = byDay[today.toEpochDay()]?.answered ?: 0,
            streak = Streaks.compute(days),
            last7Days = last7,
            resume = StudyConfig.decode(s.lastStudySession),
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}
