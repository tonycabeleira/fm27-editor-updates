package com.cabincrew.academy.ui.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.QuestionWithProgress
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.DifficultyBadge
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.OptionCard
import com.cabincrew.academy.ui.components.OptionState
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SplitRing
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme

@Composable
fun StudySessionScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, h -> StudySessionViewModel(c.repository, c.settings, h) }
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmRestart by remember { mutableStateOf(false) }
    val q = s.current
    val ext = AppTheme.extended

    ScreenScaffold(
        title = s.title.ifEmpty { "Study" },
        subtitle = if (s.questions.isNotEmpty()) "Question ${s.index + 1} of ${s.questions.size} • ${s.correctCount}/${s.answeredCount} correct this session" else null,
        onBack = onBack,
        actions = {
            if (q != null) {
                val p = q.progress
                IconButton(onClick = vm::toggleBookmark) {
                    Icon(if (p?.bookmarked == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        if (p?.bookmarked == true) "Remove bookmark" else "Bookmark for revision",
                        tint = if (p?.bookmarked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = vm::toggleFavourite) {
                    Icon(if (p?.favourite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (p?.favourite == true) "Remove from favourites" else "Add to favourites",
                        tint = if (p?.favourite == true) ext.danger else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = vm::toggleDifficult) {
                    Icon(if (p?.markedDifficult == true) Icons.Filled.Flag else Icons.Outlined.Flag,
                        if (p?.markedDifficult == true) "Unmark difficult" else "Mark as difficult",
                        tint = if (p?.markedDifficult == true) ext.warning else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { confirmRestart = true }) { Icon(Icons.Filled.RestartAlt, "Restart session") }
            }
        },
        bottomBar = {
            if (q != null) StudyBottomBar(s, onPrev = vm::previous, onSubmit = vm::submit, onNext = vm::next)
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (s.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                return@Column
            }
            if (q == null) {
                EmptyState(
                    Icons.Filled.SearchOff, "Nothing to study here",
                    "No questions match this selection. Try a different mode or category.",
                    actionLabel = "Back", onAction = onBack,
                )
                return@Column
            }
            LinearProgressIndicator(
                progress = { (s.index + 1f) / s.questions.size },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = ext.chartTrack,
                drawStopIndicator = {},
            )
            AnimatedContent(
                targetState = s.index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it / 6 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 * dir } + fadeOut())
                },
                label = "question",
                modifier = Modifier.weight(1f),
            ) { idx ->
                val item = s.questions.getOrNull(idx) ?: return@AnimatedContent
                val ans = s.answers[item.question.id] ?: AnswerState()
                QuestionPane(item, ans, onSelect = vm::select)
            }
        }
    }

    if (confirmRestart) {
        ConfirmDialog(
            "Restart session?", "Your answers in this session will be cleared and you will start again from the first question. Saved statistics are kept.",
            "Restart", onConfirm = vm::restart, onDismiss = { confirmRestart = false },
        )
    }

    if (s.showSummary) {
        val incorrect = s.answeredCount - s.correctCount
        AlertDialog(
            onDismissRequest = vm::dismissSummary,
            title = { Text("Session complete") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    SplitRing(s.correctCount, incorrect, size = 130.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (s.answeredCount == 0) "–" else "${s.correctCount * 100 / s.answeredCount}%", style = MaterialTheme.typography.headlineSmall)
                            Text("accuracy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("${s.correctCount} correct • $incorrect incorrect • ${s.questions.size - s.answeredCount} skipped")
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (incorrect > 0) TextButton(onClick = vm::retryIncorrect) { Text("Review my $incorrect incorrect") }
                    TextButton(onClick = vm::restart) { Text("Restart session") }
                    TextButton(onClick = { vm.clearResume(); onBack() }) { Text("Finish") }
                }
            },
            dismissButton = { TextButton(onClick = vm::dismissSummary) { Text("Keep reviewing") } },
        )
    }
}

@Composable
private fun QuestionPane(item: QuestionWithProgress, ans: AnswerState, onSelect: (Int) -> Unit) {
    val wide = isWideLayout()
    val q = item.question
    val scroll = rememberScrollState()
    val optionsBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            q.options.forEachIndexed { i, text ->
                val state = when {
                    !ans.submitted -> if (ans.selected == i) OptionState.SELECTED else OptionState.NEUTRAL
                    i == q.correctIndex -> if (ans.selected == i) OptionState.CORRECT else OptionState.MISSED
                    i == ans.selected -> OptionState.INCORRECT
                    else -> OptionState.NEUTRAL
                }
                OptionCard(i, text, state, onClick = if (ans.submitted) null else ({ onSelect(i) }))
            }
        }
    }
    val feedback: @Composable () -> Unit = { FeedbackCard(item, ans) }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuestionCard(item)
                feedback()
            }
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                Eyebrow("Choose one answer")
                Spacer(Modifier.height(10.dp))
                optionsBlock()
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 820.dp).fillMaxWidth().verticalScroll(scroll).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuestionCard(item)
                optionsBlock()
                feedback()
            }
        }
    }
}

@Composable
fun QuestionCard(item: QuestionWithProgress) {
    val q = item.question
    AppCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(item.category?.name ?: "Uncategorised", MaterialTheme.colorScheme.primary)
            DifficultyBadge(q.difficulty)
            Pill("ID ${q.externalId}", MaterialTheme.colorScheme.onSurfaceVariant)
            item.progress?.takeIf { it.timesAnswered > 0 }?.let {
                Pill("Answered ${it.timesAnswered}× • ${it.timesCorrect} correct", MaterialTheme.colorScheme.tertiary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(q.text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun FeedbackCard(item: QuestionWithProgress, ans: AnswerState) {
    val q = item.question
    val ext = AppTheme.extended
    AnimatedVisibility(ans.submitted, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
        val correct = ans.selected == q.correctIndex
        AppCard(containerColor = if (correct) ext.successContainer else ext.dangerContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null, tint = if (correct) ext.success else ext.danger, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (correct) "Correct!" else "Incorrect", style = MaterialTheme.typography.titleLarge, color = if (correct) ext.success else ext.danger)
            }
            if (!correct) {
                Spacer(Modifier.height(8.dp))
                Text("Correct answer: ${'A' + q.correctIndex}. ${q.options[q.correctIndex]}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (!q.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                Row {
                    Icon(Icons.Filled.Lightbulb, null, tint = ext.gold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Explanation", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(q.explanation, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyBottomBar(s: StudyUiState, onPrev: () -> Unit, onSubmit: () -> Unit, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onPrev, enabled = s.index > 0, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Previous")
            }
            Spacer(Modifier.weight(1f))
            val ans = s.currentAnswer
            if (!ans.submitted) {
                TextButton(onClick = onNext) { Text(if (s.index == s.questions.lastIndex) "Finish" else "Skip") }
                Button(onClick = onSubmit, enabled = ans.selected != null, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                    Text("Check answer")
                }
            } else {
                Button(onClick = onNext, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                    Text(if (s.index == s.questions.lastIndex) "Finish" else "Next question")
                    Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

