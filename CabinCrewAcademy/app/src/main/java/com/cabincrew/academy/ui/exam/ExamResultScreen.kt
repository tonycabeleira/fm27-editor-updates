package com.cabincrew.academy.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.ExamAnswerEntity
import com.cabincrew.academy.data.db.ExamResultEntity
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.HeroBanner
import com.cabincrew.academy.ui.components.OptionCard
import com.cabincrew.academy.ui.components.OptionState
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.ProgressRing
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.formatDuration
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date

private enum class ReviewFilter(val label: String) { ALL("All"), INCORRECT("Incorrect"), UNANSWERED("Unanswered"), CORRECT("Correct"), MARKED("Marked") }

@Composable
fun ExamResultScreen(onBack: () -> Unit, onNewExam: () -> Unit) {
    val vm = appViewModel { c, h -> ExamResultViewModel(c.repository, h) }
    val s by vm.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.ALL) }
    var confirmDelete by remember { mutableStateOf(false) }
    val wide = isWideLayout()

    ScreenScaffold(
        title = "Exam results",
        subtitle = s.result?.let { DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(it.finishedAt)) },
        onBack = onBack,
        actions = {
            if (s.result != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete this result") }
        },
    ) { padding ->
        val r = s.result
        when {
            s.loading -> LinearProgressIndicator(Modifier.padding(padding).fillMaxWidth())
            r == null -> EmptyState(Icons.AutoMirrored.Filled.HelpOutline, "Result not found", "This exam result no longer exists.", Modifier.padding(padding), "Back", onBack)
            else -> {
                val shown = s.answers.filter {
                    when (filter) {
                        ReviewFilter.ALL -> true
                        ReviewFilter.INCORRECT -> !it.isCorrect && !it.isUnanswered
                        ReviewFilter.UNANSWERED -> it.isUnanswered
                        ReviewFilter.CORRECT -> it.isCorrect
                        ReviewFilter.MARKED -> it.markedForReview
                    }
                }
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        Modifier.widthIn(max = 1000.dp).fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = if (wide) 32.dp else 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item { ScoreHeader(r, wide) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = onNewExam, modifier = Modifier.weight(1f).height(52.dp)) {
                                    Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(8.dp)); Text("Take another exam")
                                }
                            }
                        }
                        item {
                            SectionTitle("Correction report")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReviewFilter.entries.forEach { f ->
                                    val count = when (f) {
                                        ReviewFilter.ALL -> s.answers.size
                                        ReviewFilter.INCORRECT -> r.incorrectCount
                                        ReviewFilter.UNANSWERED -> r.unansweredCount
                                        ReviewFilter.CORRECT -> r.correctCount
                                        ReviewFilter.MARKED -> s.answers.count { it.markedForReview }
                                    }
                                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text("${f.label} ($count)") })
                                }
                            }
                        }
                        if (shown.isEmpty()) item {
                            Text("No questions in this view.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                        }
                        items(shown, key = { it.id }) { a -> AnswerReviewCard(a) }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Delete this result?", "This exam will be removed from your history and statistics. Question progress already recorded is kept.",
            "Delete", destructive = true, onConfirm = { vm.delete(onBack) }, onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ScoreHeader(r: ExamResultEntity, wide: Boolean) {
    val ext = AppTheme.extended
    HeroBanner {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(r.scorePercent / 100f, size = if (wide) 160.dp else 120.dp, strokeWidth = 14.dp,
                color = if (r.passed) ext.gold else ext.danger, trackColor = Color.White.copy(alpha = 0.15f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${"%.0f".format(r.scorePercent)}%", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("score", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(r.scopeLabel, color = ext.gold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.passed) Icons.Filled.EmojiEvents else Icons.Filled.Cancel, null, tint = if (r.passed) ext.gold else Color.White, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (r.passed) "PASSED" else "NOT PASSED", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (r.passed) "Congratulations – you met the ${r.passingScore}% pass mark."
                    else "The pass mark is ${r.passingScore}%. Review the corrections below and try again.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("${r.correctCount} correct", Color(0xFF7EE2B0))
                    Pill("${r.incorrectCount} incorrect", Color(0xFFFF9B8F))
                    Pill("${r.unansweredCount} unanswered", Color.White)
                    Pill("${formatDuration(r.timeTakenSeconds)}${if (r.timeLimitSeconds > 0) " / ${formatDuration(r.timeLimitSeconds)}" else ""}", Color.White)
                    if (r.timedOut) Pill("Time expired", Color(0xFFFFC266))
                }
            }
        }
    }
}

@Composable
private fun AnswerReviewCard(a: ExamAnswerEntity) {
    val ext = AppTheme.extended
    val (icon, color, label) = when {
        a.isUnanswered -> Triple(Icons.AutoMirrored.Filled.HelpOutline, ext.warning, "Not answered")
        a.isCorrect -> Triple(Icons.Filled.CheckCircle, ext.success, "Correct")
        else -> Triple(Icons.Filled.Cancel, ext.danger, "Incorrect")
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text("Question ${a.position + 1} • $label", style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
            if (a.markedForReview) Icon(Icons.Filled.Flag, "Marked for review", tint = ext.warning, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (a.categoryName.isNotEmpty()) Pill(a.categoryName, MaterialTheme.colorScheme.tertiary)
            Pill("ID ${a.externalId}", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Text(a.questionText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            a.options.forEachIndexed { i, text ->
                val state = when {
                    i == a.correctIndex && a.selectedIndex == i -> OptionState.CORRECT
                    i == a.correctIndex -> OptionState.MISSED
                    i == a.selectedIndex -> OptionState.INCORRECT
                    else -> OptionState.NEUTRAL
                }
                OptionCard(i, text, state, onClick = null)
            }
        }
        if (!a.explanation.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Row {
                Icon(Icons.Filled.Lightbulb, null, tint = ext.gold)
                Spacer(Modifier.width(8.dp))
                Text(a.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

