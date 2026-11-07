package com.cabincrew.academy.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.ui.Routes
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.BarChart
import com.cabincrew.academy.ui.components.ContentWidth
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.HeroBanner
import com.cabincrew.academy.ui.components.LineChart
import com.cabincrew.academy.ui.components.MeterBar
import com.cabincrew.academy.ui.components.ProgressRing
import com.cabincrew.academy.ui.components.ResponsiveGrid
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.StatTile
import com.cabincrew.academy.ui.components.columnsForWidth
import com.cabincrew.academy.ui.components.formatPercent
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme
import java.text.DateFormat
import java.time.LocalTime
import java.util.Date

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, onNavigateTop: (String) -> Unit) {
    val vm = appViewModel { c, _ -> DashboardViewModel(c.repository, c.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val wide = isWideLayout()
    val hasQuestions = state.totals.totalQuestions > 0

    val continueStudying = {
        val r = state.resume
        if (r != null) onNavigate(Routes.study(r.first, r.second)) else onNavigateTop(Routes.STUDY_SETUP)
    }
    val randomTest = {
        val s = state.settings
        onNavigate(Routes.exam(s.examQuestionCount, s.examDurationMinutes, s.passingScore, emptyList()))
    }

    ContentWidth(maxWidth = 1280.dp) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Hero(state, wide, hasQuestions, continueStudying, randomTest) { onNavigate(Routes.IMPORT) }

            if (!state.loading && !hasQuestions) {
                GettingStarted(onImport = { onNavigate(Routes.IMPORT) }, onAdd = { onNavigate(Routes.editQuestion()) }, onCategories = { onNavigate(Routes.CATEGORIES) })
            }

            // Key figures
            val tiles = listOf<@Composable () -> Unit>(
                { StatTile(Icons.AutoMirrored.Filled.LibraryBooks, "Questions available", state.totals.totalQuestions.toString()) },
                { StatTile(Icons.Filled.CheckCircle, "Completed", "${state.totals.studied}", accent = AppTheme.extended.success, supporting = "${formatPercent(state.studyProgress)} of the bank studied") },
                { StatTile(Icons.AutoMirrored.Filled.TrendingUp, "Average accuracy", if (state.totals.answered == 0) "–" else formatPercent(state.accuracy), accent = MaterialTheme.colorScheme.tertiary, supporting = "${state.totals.correct} correct • ${state.totals.incorrect} incorrect") },
                { StatTile(Icons.Filled.EmojiEvents, "Best exam score", state.bestExam?.let { "${it.toInt()}%" } ?: "–", accent = AppTheme.extended.gold, supporting = state.averageExam?.let { "Average ${it.toInt()}% over ${state.exams.size} exam${if (state.exams.size == 1) "" else "s"}" } ?: "No exams taken yet") },
            )
            ResponsiveGrid(tiles, columns = if (wide) 4 else columnsForWidth(220.dp).coerceAtMost(2)) { it() }

            // Daily goal + activity + exam trend
            val panels = listOf<@Composable () -> Unit>(
                { DailyGoalCard(state) },
                { WeekCard(state) },
                { ExamTrendCard(state, onOpenExams = { onNavigateTop(Routes.STATISTICS) }) },
            )
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    panels.forEach { Box(Modifier.weight(1f)) { it() } }
                }
            } else {
                ResponsiveGrid(panels, columns = columnsForWidth(320.dp).coerceAtMost(2), fillLastRow = true) { it() }
            }

            if (state.categories.isNotEmpty()) {
                SectionTitle("Study categories") {
                    TextButton(onClick = { onNavigate(Routes.CATEGORIES) }) { Text("Manage") }
                }
                ResponsiveGrid(state.categories, columns = columnsForWidth(280.dp)) { cat ->
                    AppCard(
                        onClick = { onNavigate(Routes.study(StudyConfig(categoryId = cat.id))) },
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Category, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(cat.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${cat.questionCount} questions • ${if (cat.answered == 0) "no answers yet" else "${formatPercent(cat.accuracy)} accuracy"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        MeterBar(cat.completion)
                        Spacer(Modifier.height(4.dp))
                        Text("${formatPercent(cat.completion)} studied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SectionTitle("Recent activity")
            AppCard(contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)) {
                if (state.recent.isEmpty()) {
                    Text(
                        "Your study sessions and exam results will appear here.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                } else {
                    state.recent.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = AppTheme.extended.cardBorder)
                        ActivityRow(item) { item.examId?.let { onNavigate(Routes.examResult(it)) } }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Hero(
    state: DashboardState,
    wide: Boolean,
    hasQuestions: Boolean,
    onContinue: () -> Unit,
    onRandomTest: () -> Unit,
    onImport: () -> Unit,
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val name = state.settings.userName.ifBlank { null }
    val gold = AppTheme.extended.gold
    HeroBanner {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flight, null, tint = gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Eyebrow("Cabin Crew Academy", color = gold)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (name != null) "$greeting, $name" else "$greeting. Welcome aboard.",
                    style = if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (hasQuestions) "You have studied ${state.totals.studied} of ${state.totals.totalQuestions} questions. Keep your preparation on course."
                    else "Your question bank is empty. Import your study materials to begin your preparation.",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(20.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasQuestions) {
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF1A1300)),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp))
                            Text(if (state.resume != null) "Continue studying" else "Start studying")
                        }
                        OutlinedButton(
                            onClick = onRandomTest,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                        ) {
                            Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("Start a random test")
                        }
                    } else {
                        Button(
                            onClick = onImport,
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF1A1300)),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                        ) { Icon(Icons.Filled.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Import questions") }
                    }
                }
            }
            if (wide || hasQuestions) {
                Spacer(Modifier.width(20.dp))
                ProgressRing(
                    state.studyProgress, size = if (wide) 150.dp else 104.dp, strokeWidth = if (wide) 14.dp else 10.dp,
                    color = gold, trackColor = Color.White.copy(alpha = 0.15f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatPercent(state.studyProgress), style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("studied", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GettingStarted(onImport: () -> Unit, onAdd: () -> Unit, onCategories: () -> Unit) {
    AppCard {
        Eyebrow("Getting started")
        Spacer(Modifier.height(8.dp))
        Text("Load your study materials", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Import your questions from JSON or CSV files (several files at once are supported), or create them manually. " +
                "Templates and the exact format are available on the Import screen.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImport) { Icon(Icons.Filled.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Import files") }
            OutlinedButton(onClick = onAdd) { Text("Create a question") }
            OutlinedButton(onClick = onCategories) { Text("Set up categories") }
        }
    }
}

@Composable
private fun PanelHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DailyGoalCard(state: DashboardState) {
    val goal = state.settings.dailyGoal
    val ext = AppTheme.extended
    AppCard(Modifier.height(270.dp)) {
        PanelHeader("Daily goal", Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(state.todayAnswered.toFloat() / goal, size = 110.dp, strokeWidth = 11.dp, color = if (state.todayAnswered >= goal) ext.success else MaterialTheme.colorScheme.secondary) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.todayAnswered}", style = MaterialTheme.typography.headlineSmall)
                    Text("of $goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = if (state.streak.current > 0) ext.warning else MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text("${state.streak.current}-day streak", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text("Best: ${state.streak.best} days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        state.todayAnswered >= goal -> "Goal reached today. Excellent work!"
                        state.streak.studiedToday -> "${goal - state.todayAnswered} more to reach today's goal."
                        state.streak.current > 0 -> "Study today to keep your streak alive."
                        else -> "Answer a question to start a streak."
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun WeekCard(state: DashboardState) {
    AppCard(Modifier.height(270.dp)) {
        PanelHeader("Last 7 days", Icons.Filled.Quiz, MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(12.dp))
        BarChart(
            state.last7Days, chartHeight = 170.dp,
            maxValue = maxOf(state.settings.dailyGoal.toFloat(), state.last7Days.maxOfOrNull { it.value } ?: 1f),
            goalLine = state.settings.dailyGoal.toFloat(),
        )
    }
}

@Composable
private fun ExamTrendCard(state: DashboardState, onOpenExams: () -> Unit) {
    AppCard(Modifier.height(270.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { PanelHeader("Exam scores", Icons.Filled.Timer, AppTheme.extended.gold) }
            if (state.exams.isNotEmpty()) TextButton(onClick = onOpenExams) { Text("History") }
        }
        Spacer(Modifier.height(8.dp))
        if (state.exams.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Take a mock exam to see your score trend.\nDashed line = pass mark (${state.settings.passingScore}%).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val values = state.exams.take(12).reversed().map { it.scorePercent }
            LineChart(values, threshold = state.settings.passingScore.toFloat(), chartHeight = 140.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Last ${values.size} exam${if (values.size == 1) "" else "s"} • pass mark ${state.settings.passingScore}%",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
    val ext = AppTheme.extended
    val color = when (item.passed) { true -> ext.success; false -> ext.danger; null -> MaterialTheme.colorScheme.tertiary }
    val rowModifier = if (item.isExam) Modifier.fillMaxWidth().padding(vertical = 2.dp) else Modifier.fillMaxWidth()
    androidx.compose.material3.Surface(onClick = onClick, enabled = item.isExam, color = Color.Transparent, modifier = rowModifier) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (item.isExam) Icons.Filled.Timer else Icons.AutoMirrored.Filled.MenuBook, null, tint = color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (item.isExam) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.time))
                else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.time)),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

