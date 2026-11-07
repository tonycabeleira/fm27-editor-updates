package com.cabincrew.academy.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.DifficultyBadge
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.OptionCard
import com.cabincrew.academy.ui.components.OptionState
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.formatDuration
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme

@Composable
fun ExamSessionScreen(onExit: () -> Unit, onFinished: (Long) -> Unit) {
    val vm = appViewModel { c, h -> ExamSessionViewModel(c.repository, h) }
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var showNavigator by remember { mutableStateOf(false) }
    val wide = isWideLayout()
    val ext = AppTheme.extended

    LaunchedEffect(s.finishedExamId) { s.finishedExamId?.let(onFinished) }
    BackHandler(enabled = !s.loading && s.questions.isNotEmpty() && s.finishedExamId == null) { confirmExit = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { if (s.questions.isEmpty()) onExit() else confirmExit = true }) { Icon(Icons.Filled.Close, "Leave exam") }
                },
                title = {
                    Column {
                        Text("Mock examination")
                        if (s.questions.isNotEmpty()) Text(
                            "Question ${s.index + 1} of ${s.questions.size} • ${s.answeredCount} answered",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    s.remainingSeconds?.let { TimerChip(it, s.timeLimitSeconds) }
                    Spacer(Modifier.width(8.dp))
                    if (!wide && s.questions.isNotEmpty()) IconButton(onClick = { showNavigator = true }) { Icon(Icons.Filled.GridView, "Question overview") }
                    Button(onClick = { confirmSubmit = true }, enabled = s.questions.isNotEmpty() && !s.submitting, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Submit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (s.questions.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = vm::previous, enabled = s.index > 0, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null); if (wide) { Spacer(Modifier.width(6.dp)); Text("Previous") }
                        }
                        val marked = s.index in s.marked
                        FilledTonalButton(
                            onClick = vm::toggleMark,
                            colors = if (marked) ButtonDefaults.filledTonalButtonColors(containerColor = ext.gold.copy(alpha = 0.25f)) else ButtonDefaults.filledTonalButtonColors(),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            Icon(if (marked) Icons.Filled.Flag else Icons.Outlined.Flag, null, tint = if (marked) ext.gold else MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(6.dp)); Text(if (marked) "Marked" else "Mark for review")
                        }
                        Spacer(Modifier.weight(1f))
                        if (s.answers.getOrNull(s.index) != null) TextButton(onClick = vm::clearAnswer) { Text("Clear") }
                        Button(onClick = vm::next, enabled = s.index < s.questions.lastIndex, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)) {
                            if (wide) { Text("Next"); Spacer(Modifier.width(6.dp)) }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.loading || s.submitting -> Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (s.submitting) Text("Marking your exam…", Modifier.padding(24.dp))
                }
                s.questions.isEmpty() -> EmptyState(
                    Icons.Filled.Timer, "No questions available",
                    "There are no questions in the selected categories.", actionLabel = "Back", onAction = onExit,
                )
                else -> Row(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = s.index, transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "examQ", modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { idx ->
                        ExamQuestionView(s, idx, vm::select)
                    }
                    if (wide) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                        ) {
                            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                                QuestionNavigator(s) { vm.goTo(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNavigator) {
        ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                QuestionNavigator(s) { vm.goTo(it); showNavigator = false }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave the exam?") },
            text = { Text("Your answers will be lost and this attempt will not be recorded.") },
            confirmButton = {
                Button(onClick = { confirmExit = false; onExit() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Leave exam") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Continue exam") } },
        )
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit examination?") },
            text = {
                Column {
                    Text("You have answered ${s.answeredCount} of ${s.questions.size} questions.")
                    if (s.unansweredCount > 0) Text("${s.unansweredCount} unanswered question(s) will be marked incorrect.", color = ext.danger)
                    if (s.marked.isNotEmpty()) Text("${s.marked.size} question(s) are still marked for review.", color = ext.warning)
                    Spacer(Modifier.height(8.dp))
                    Text("Once submitted, answers cannot be changed.")
                }
            },
            confirmButton = { Button(onClick = { confirmSubmit = false; vm.submit() }) { Text("Submit now") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Review answers") } },
        )
    }
}

@Composable
private fun TimerChip(remaining: Int, limit: Int) {
    val ext = AppTheme.extended
    val low = remaining <= 60 || (limit > 0 && remaining <= limit / 10)
    val color = if (low) ext.danger else MaterialTheme.colorScheme.primary
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Timer, "Time remaining", tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(formatDuration(remaining), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExamQuestionView(s: ExamUiState, idx: Int, onSelect: (Int) -> Unit) {
    val eq = s.questions.getOrNull(idx) ?: return
    val q = eq.question
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 860.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Question ${idx + 1}", MaterialTheme.colorScheme.primary)
                    if (eq.categoryName.isNotEmpty()) Pill(eq.categoryName, MaterialTheme.colorScheme.tertiary)
                    DifficultyBadge(q.difficulty)
                    if (idx in s.marked) Pill("Marked for review", AppTheme.extended.gold)
                }
                Spacer(Modifier.height(14.dp))
                Text(q.text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium))
            }
            Eyebrow("Select one answer")
            q.options.forEachIndexed { i, text ->
                OptionCard(i, text, if (s.answers.getOrNull(idx) == i) OptionState.SELECTED else OptionState.NEUTRAL, onClick = { onSelect(i) })
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuestionNavigator(s: ExamUiState, onGo: (Int) -> Unit) {
    val ext = AppTheme.extended
    val cs = MaterialTheme.colorScheme
    Text("Question overview", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text("${s.answeredCount} answered • ${s.unansweredCount} unanswered • ${s.marked.size} marked",
        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    Spacer(Modifier.height(14.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        s.questions.indices.forEach { i ->
            val answered = s.answers.getOrNull(i) != null
            val marked = i in s.marked
            val current = i == s.index
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { onGo(i) },
                    shape = CircleShape,
                    color = if (answered) cs.primary else cs.surfaceVariant,
                    border = when {
                        current -> BorderStroke(3.dp, ext.gold)
                        marked -> BorderStroke(2.dp, ext.warning)
                        else -> null
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = if (answered) cs.onPrimary else cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (marked) Icon(Icons.Filled.Flag, null, tint = ext.warning, modifier = Modifier.size(14.dp).align(Alignment.TopEnd))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Legend(cs.primary, "Answered")
    Legend(cs.surfaceVariant, "Not answered")
    Legend(ext.warning, "Marked for review", ring = true)
    Legend(ext.gold, "Current question", ring = true)
}

@Composable
private fun Legend(color: Color, label: String, ring: Boolean = false) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(14.dp).clip(CircleShape)
                .background(if (ring) Color.Transparent else color)
                .then(if (ring) Modifier.background(color.copy(alpha = 0.35f)) else Modifier)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
