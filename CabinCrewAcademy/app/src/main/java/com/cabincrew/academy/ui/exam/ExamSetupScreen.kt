package com.cabincrew.academy.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ContentWidth
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.HeroBanner
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.formatDuration
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun ExamSetupScreen(
    onStart: (count: Int, minutes: Int, pass: Int, categories: List<Long>) -> Unit,
    onOpenResult: (Long) -> Unit,
    onImport: () -> Unit,
) {
    val vm = appViewModel { c, _ -> ExamSetupViewModel(c.repository, c.settings) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    var initialised by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableIntStateOf(20) }
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    var pass by rememberSaveable { mutableIntStateOf(75) }
    var selected by rememberSaveable { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(settings) {
        val s = settings
        if (s != null && !initialised) {
            count = s.examQuestionCount; minutes = s.examDurationMinutes; pass = s.passingScore; initialised = true
        }
    }

    val cats = categories
    val available = cats?.filter { selected.isEmpty() || it.id in selected }?.sumOf { it.questionCount } ?: 0
    val effectiveCount = count.coerceAtMost(available)
    val wide = isWideLayout()

    ScreenScaffold(title = "Exam simulator", subtitle = "Realistic timed recruitment assessment") { padding ->
        if (cats != null && cats.sumOf { it.questionCount } == 0) {
            EmptyState(
                Icons.Filled.FileUpload, "No questions available",
                "Import your question bank before taking a mock exam.", Modifier.padding(padding),
                actionLabel = "Import questions", onAction = onImport,
            )
            return@ScreenScaffold
        }
        ContentWidth(modifier = Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroBanner {
                    Eyebrow("Assessment briefing", color = AppTheme.extended.gold)
                    Spacer(Modifier.height(8.dp))
                    Text("$effectiveCount questions • ${if (minutes == 0) "no time limit" else "$minutes minutes"} • pass mark $pass%",
                        style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Questions are drawn at random from your bank. Answers are not revealed until you submit. " +
                            "You can move between questions and mark any for review before submitting.",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                    )
                }

                val settingsCards = listOf<@Composable () -> Unit>(
                    {
                        SliderCard(Icons.Filled.Quiz, "Number of questions", "$effectiveCount of $available available",
                            count.toFloat(), 1f..maxOf(available, 1).coerceAtMost(200).toFloat().coerceAtLeast(2f)) { count = it.roundToInt() }
                    },
                    {
                        SliderCard(Icons.Filled.Timer, "Time limit", if (minutes == 0) "No limit" else "$minutes min",
                            minutes.toFloat(), 0f..180f) { minutes = (it / 5f).roundToInt() * 5 }
                    },
                    {
                        SliderCard(Icons.Filled.Percent, "Passing score", "$pass%", pass.toFloat(), 10f..100f) { pass = it.roundToInt() }
                    },
                )
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    settingsCards.forEach { Column(Modifier.weight(1f)) { it() } }
                } else settingsCards.forEach { it() }

                SectionTitle("Categories included")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selected.isEmpty(), onClick = { selected = emptyList() }, label = { Text("All categories") })
                    cats.orEmpty().filter { it.questionCount > 0 }.forEach { c ->
                        FilterChip(
                            selected = c.id in selected,
                            onClick = { selected = if (c.id in selected) selected - c.id else selected + c.id },
                            label = { Text("${c.name} (${c.questionCount})") },
                        )
                    }
                }

                Button(
                    onClick = { onStart(effectiveCount, minutes, pass, selected) },
                    enabled = effectiveCount > 0,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                ) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Begin examination", style = MaterialTheme.typography.titleMedium) }

                if (history.isNotEmpty()) {
                    SectionTitle("Previous exams")
                    AppCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
                        history.take(10).forEachIndexed { i, r ->
                            if (i > 0) HorizontalDivider(color = AppTheme.extended.cardBorder)
                            ExamHistoryRow(r) { onOpenResult(r.id) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SliderCard(
    icon: ImageVector,
    title: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = current.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
fun ExamHistoryRow(r: ExamResultEntity, onClick: () -> Unit) {
    val ext = AppTheme.extended
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${r.scorePercent.toInt()}%", style = MaterialTheme.typography.titleLarge, color = if (r.passed) ext.success else ext.danger, modifier = Modifier.width(72.dp))
            Column(Modifier.weight(1f)) {
                Text("${r.correctCount}/${r.totalQuestions} correct • ${r.scopeLabel}", style = MaterialTheme.typography.titleSmall)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.finishedAt)) + " • " + formatDuration(r.timeTakenSeconds),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(if (r.passed) "PASSED" else "NOT PASSED", if (r.passed) ext.success else ext.danger)
        }
    }
}
