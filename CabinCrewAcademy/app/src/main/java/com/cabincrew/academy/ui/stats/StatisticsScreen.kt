package com.cabincrew.academy.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.db.DailyActivityEntity
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.data.db.ProgressTotals
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.AppSettings
import com.cabincrew.academy.data.settings.SettingsRepository
import com.cabincrew.academy.domain.StreakInfo
import com.cabincrew.academy.domain.Streaks
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.domain.StudyMode
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.BarChart
import com.cabincrew.academy.ui.components.BarDatum
import com.cabincrew.academy.ui.components.ContentWidth
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.LineChart
import com.cabincrew.academy.ui.components.MeterBar
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.ResponsiveGrid
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.SplitRing
import com.cabincrew.academy.ui.components.StatTile
import com.cabincrew.academy.ui.components.columnsForWidth
import com.cabincrew.academy.ui.components.formatPercent
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.exam.ExamHistoryRow
import com.cabincrew.academy.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsState(
    val loading: Boolean = true,
    val totals: ProgressTotals = ProgressTotals(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val categories: List<CategoryWithStats> = emptyList(),
    val exams: List<ExamResultEntity> = emptyList(),
    val days: List<DailyActivityEntity> = emptyList(),
    val streak: StreakInfo = StreakInfo(0, 0, false),
    val settings: AppSettings = AppSettings(),
)

class StatisticsViewModel(repo: StudyRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<StatsState> = combine(
        repo.totals, repo.categoriesWithStats, repo.examResults, repo.dailyActivity, settings.settings,
    ) { t, c, e, d, s -> StatsState(false, t, c, e, d, Streaks.compute(d), s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}

@Composable
fun StatisticsScreen(onOpenExam: (Long) -> Unit, onStudy: (StudyConfig) -> Unit) {
    val vm = appViewModel { c, _ -> StatisticsViewModel(c.repository, c.settings) }
    val s by vm.state.collectAsStateWithLifecycle()
    val wide = isWideLayout()
    val ext = AppTheme.extended
    val t = s.totals
    val accuracy = if (t.answered == 0) 0f else t.correct.toFloat() / t.answered
    val best = s.exams.maxOfOrNull { it.scorePercent }
    val avg = if (s.exams.isEmpty()) null else s.exams.map { it.scorePercent }.average()
    val passRate = if (s.exams.isEmpty()) null else s.exams.count { it.passed }.toFloat() / s.exams.size

    ScreenScaffold(title = "Statistics", subtitle = "Track your progress and find your weak spots") { padding ->
        if (!s.loading && t.totalQuestions == 0 && s.exams.isEmpty()) {
            EmptyState(Icons.Filled.Insights, "No statistics yet", "Import questions and start studying – your progress will be charted here.", Modifier.padding(padding))
            return@ScreenScaffold
        }
        ContentWidth(maxWidth = 1280.dp, modifier = Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val tiles = listOf<@Composable () -> Unit>(
                    { StatTile(Icons.Filled.CheckCircle, "Questions completed", "${t.studied} / ${t.totalQuestions}", accent = ext.success, supporting = "${if (t.totalQuestions == 0) 0 else t.studied * 100 / t.totalQuestions}% of the bank") },
                    { StatTile(Icons.AutoMirrored.Filled.TrendingUp, "Accuracy", if (t.answered == 0) "–" else formatPercent(accuracy), accent = MaterialTheme.colorScheme.tertiary, supporting = "${t.answered} answers in total") },
                    { StatTile(Icons.Filled.EmojiEvents, "Best exam", best?.let { "${it.toInt()}%" } ?: "–", accent = ext.gold, supporting = avg?.let { "Average ${it.toInt()}%" } ?: "No exams yet") },
                    { StatTile(Icons.Filled.Timer, "Exams taken", s.exams.size.toString(), supporting = passRate?.let { "Pass rate ${formatPercent(it)}" } ?: "Pass mark ${s.settings.passingScore}%") },
                    { StatTile(Icons.Filled.LocalFireDepartment, "Study streak", "${s.streak.current} day${if (s.streak.current == 1) "" else "s"}", accent = ext.warning, supporting = "Best streak ${s.streak.best} days") },
                    { StatTile(Icons.Filled.Cancel, "Incorrect answers", t.incorrect.toString(), accent = ext.danger, supporting = "${t.needsReview} question(s) to review") },
                )
                ResponsiveGrid(tiles, columns = if (wide) 3 else columnsForWidth(240.dp).coerceAtMost(2)) { it() }

                val charts = listOf<@Composable () -> Unit>(
                    {
                        AppCard(Modifier.height(300.dp)) {
                            Text("Correct vs incorrect", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                SplitRing(t.correct, t.incorrect, size = 160.dp) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (t.answered == 0) "–" else formatPercent(accuracy), style = MaterialTheme.typography.headlineMedium)
                                        Text("accuracy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(Modifier.width(24.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LegendRow(ext.success, "Correct", t.correct.toString())
                                    LegendRow(ext.danger, "Incorrect", t.incorrect.toString())
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    },
                    {
                        AppCard(Modifier.height(300.dp)) {
                            Text("Exam score trend", style = MaterialTheme.typography.titleMedium)
                            Text("Dashed line: pass mark (${s.settings.passingScore}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            if (s.exams.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No exams taken yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else LineChart(s.exams.take(20).reversed().map { it.scorePercent }, threshold = s.settings.passingScore.toFloat(), chartHeight = 200.dp)
                        }
                    },
                )
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { charts.forEach { Box(Modifier.weight(1f)) { it() } } }
                else charts.forEach { it() }

                AppCard {
                    Text("Questions answered – last 14 days", style = MaterialTheme.typography.titleMedium)
                    Text("Gold dashed line: daily goal (${s.settings.dailyGoal})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val today = LocalDate.now()
                    val byDay = s.days.associateBy { it.epochDay }
                    val data = (13 downTo 0).map { back ->
                        val d = today.minusDays(back.toLong())
                        BarDatum(if (back == 0) "Today" else "${d.dayOfMonth}/${d.monthValue}", (byDay[d.toEpochDay()]?.answered ?: 0).toFloat(), back == 0)
                    }
                    BarChart(data, chartHeight = 180.dp, maxValue = maxOf(s.settings.dailyGoal.toFloat(), data.maxOf { it.value }), goalLine = s.settings.dailyGoal.toFloat())
                }

                if (s.categories.isNotEmpty()) {
                    SectionTitle("Performance by category")
                    Text("Sorted from weakest to strongest accuracy. Categories without answers are listed last.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val sorted = s.categories.sortedWith(compareBy({ it.answered == 0 }, { it.accuracy }))
                    val weakest = sorted.filter { it.answered > 0 }.take(2).map { it.id }.toSet()
                    AppCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        sorted.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = ext.cardBorder)
                            Column(Modifier.padding(vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (c.id in weakest && sorted.count { it.answered > 0 } > 1) { Pill("Weakest", ext.danger); Spacer(Modifier.width(8.dp)) }
                                    Text(if (c.answered == 0) "–" else formatPercent(c.accuracy), style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { onStudy(StudyConfig(mode = StudyMode.ALL, categoryId = c.id)) }, enabled = c.questionCount > 0) { Text("Practise") }
                                }
                                MeterBar(
                                    if (c.answered == 0) 0f else c.accuracy,
                                    color = when {
                                        c.answered == 0 -> MaterialTheme.colorScheme.outline
                                        c.accuracy * 100 >= s.settings.passingScore -> ext.success
                                        c.accuracy >= 0.5f -> ext.warning
                                        else -> ext.danger
                                    },
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${c.studiedCount}/${c.questionCount} studied • ${c.correct}/${c.answered} correct answers",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionTitle("Revision lists")
                val lists = listOf(
                    RevisionItem(Icons.Filled.Cancel, "To review (last answer wrong)", t.needsReview, ext.danger, StudyMode.INCORRECT),
                    RevisionItem(Icons.Filled.Bookmark, "Bookmarked", t.bookmarked, MaterialTheme.colorScheme.secondary, StudyMode.BOOKMARKED),
                    RevisionItem(Icons.Filled.Flag, "Marked difficult", t.markedDifficult, ext.warning, StudyMode.DIFFICULT),
                    RevisionItem(Icons.Filled.Favorite, "Favourites", t.favourites, ext.danger, StudyMode.FAVOURITES),
                )
                ResponsiveGrid(lists, columns = if (wide) 4 else 2) { item ->
                    AppCard(onClick = if (item.count > 0) ({ onStudy(StudyConfig(mode = item.mode)) }) else null, contentPadding = PaddingValues(16.dp)) {
                        Icon(item.icon, null, tint = item.color)
                        Spacer(Modifier.height(8.dp))
                        Text(item.count.toString(), style = MaterialTheme.typography.headlineSmall)
                        Text(item.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        if (item.count > 0) Text("Tap to study", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (s.exams.isNotEmpty()) {
                    SectionTitle("Examination history")
                    AppCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
                        s.exams.forEachIndexed { i, r ->
                            if (i > 0) HorizontalDivider(color = ext.cardBorder)
                            ExamHistoryRow(r) { onOpenExam(r.id) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class RevisionItem(val icon: ImageVector, val label: String, val count: Int, val color: Color, val mode: StudyMode)

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).padding(0.dp)) { androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(color) } }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
