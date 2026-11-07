package com.cabincrew.academy.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.AppSettings
import com.cabincrew.academy.data.settings.SettingsRepository
import com.cabincrew.academy.data.settings.ThemeMode
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.rememberSnackbarHost
import com.cabincrew.academy.ui.importexport.ExportKind
import com.cabincrew.academy.ui.importexport.ImportExportViewModel
import com.cabincrew.academy.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

class SettingsViewModel(private val settingsRepo: SettingsRepository, private val repo: StudyRepository) : ViewModel() {
    val settings: StateFlow<AppSettings?> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun setTheme(m: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(m) }
    fun setName(n: String) = viewModelScope.launch { settingsRepo.setUserName(n) }
    fun setDuration(v: Int) = viewModelScope.launch { settingsRepo.setExamDuration(v) }
    fun setCount(v: Int) = viewModelScope.launch { settingsRepo.setExamQuestionCount(v) }
    fun setPassing(v: Int) = viewModelScope.launch { settingsRepo.setPassingScore(v) }
    fun setDailyGoal(v: Int) = viewModelScope.launch { settingsRepo.setDailyGoal(v) }
    fun resetProgress() = viewModelScope.launch {
        repo.resetProgress(); settingsRepo.setLastStudySession(null)
        _messages.emit("Progress, exam history and streaks have been reset")
    }
    fun deleteAllQuestions() = viewModelScope.launch {
        repo.deleteAllQuestions(); settingsRepo.setLastStudySession(null)
        _messages.emit("All questions and categories deleted")
    }
}

@Composable
fun SettingsScreen(onImportExport: () -> Unit, onCategories: () -> Unit) {
    val vm = appViewModel { c, _ -> SettingsViewModel(c.settings, c.repository) }
    val exporter = appViewModel { c, _ -> ImportExportViewModel(c.repository, c.appContext) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val exportState by exporter.state.collectAsStateWithLifecycle()
    val snackbar = rememberSnackbarHost()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.JSON.mime)) { it?.let { u -> exporter.export(u, ExportKind.JSON) } }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(exportState.message) { exportState.message?.let { snackbar.showSnackbar(it); exporter.consumeMessage() } }

    ScreenScaffold(title = "Settings", snackbarHostState = snackbar) { padding ->
        val s = settings ?: return@ScreenScaffold
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 860.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionTitle("Profile & appearance")
                AppCard {
                    var name by remember(s.userName) { mutableStateOf(s.userName) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value = name, onValueChange = { name = it; vm.setName(it) },
                            label = { Text("Your name (shown on the dashboard)") }, singleLine = true, modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(selected = s.themeMode == m, onClick = { vm.setTheme(m) }, shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)) {
                                Text(m.label)
                            }
                        }
                    }
                }

                SectionTitle("Examination")
                AppCard {
                    SettingSlider("Exam duration", s.examDurationMinutes, 0f..180f, 5, { if (it == 0) "No time limit" else "$it minutes" }, vm::setDuration)
                    SettingSlider("Questions per test", s.examQuestionCount, 5f..200f, 5, { "$it questions" }, vm::setCount)
                    SettingSlider("Passing score", s.passingScore, 10f..100f, 1, { "$it%" }, vm::setPassing)
                }

                SectionTitle("Study goals")
                AppCard {
                    SettingSlider("Daily goal", s.dailyGoal, 5f..200f, 5, { "$it questions per day" }, vm::setDailyGoal)
                }

                SectionTitle("Data")
                AppCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    ActionRow(Icons.Filled.FileUpload, "Import database", "Add questions from JSON or CSV files", onClick = onImportExport)
                    HorizontalDivider(color = AppTheme.extended.cardBorder)
                    ActionRow(Icons.Filled.FileDownload, "Export database", "Save all questions as a JSON backup") {
                        exportJson.launch("cabin-crew-questions-${LocalDate.now()}.json")
                    }
                    HorizontalDivider(color = AppTheme.extended.cardBorder)
                    ActionRow(Icons.Filled.Category, "Manage categories", "Create, rename and delete subjects", onClick = onCategories)
                    HorizontalDivider(color = AppTheme.extended.cardBorder)
                    ActionRow(Icons.Filled.RestartAlt, "Reset progress", "Clear study statistics, exam history and streaks", tint = AppTheme.extended.warning) { confirmReset = true }
                    HorizontalDivider(color = AppTheme.extended.cardBorder)
                    ActionRow(Icons.Filled.DeleteForever, "Delete all questions", "Remove every question and category", tint = AppTheme.extended.danger) { confirmDeleteAll = true }
                }

                SectionTitle("About")
                AppCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    ActionRow(Icons.Filled.Info, "About Cabin Crew Academy", "Version, privacy and offline operation") { showAbout = true }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmReset) ConfirmDialog(
        "Reset all progress?",
        "Answer statistics, exam history, daily activity and streaks will be permanently cleared. Your questions, categories, bookmarks and favourites are kept.",
        "Reset progress", destructive = true, onConfirm = vm::resetProgress, onDismiss = { confirmReset = false },
    )
    if (confirmDeleteAll) ConfirmDialog(
        "Delete ALL questions?",
        "Every question and category, together with their study progress, will be permanently deleted. Export a backup first if you might need them again.",
        "Delete everything", destructive = true, onConfirm = vm::deleteAllQuestions, onDismiss = { confirmDeleteAll = false },
    )
    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        icon = { Icon(Icons.Filled.Flight, null, tint = AppTheme.extended.gold) },
        title = { Text("Cabin Crew Academy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${com.cabincrew.academy.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                Text("A professional study and examination simulator for cabin crew recruitment preparation.")
                Text("• Works 100% offline – no account, no subscription, no internet connection required.")
                Text("• All questions, progress and statistics are stored only on this device.")
                Text("• Back up your question bank any time with Settings → Export database.")
            }
        },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
    )
}

@Composable
private fun SettingSlider(title: String, value: Int, range: ClosedFloatingPointRange<Float>, step: Int, format: (Int) -> String, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(format(local.toInt()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = local.coerceIn(range.start, range.endInclusive),
            onValueChange = { local = ((it / step).roundToInt() * step).toFloat().coerceIn(range.start, range.endInclusive) },
            valueRange = range,
            onValueChangeFinished = { onCommit(local.toInt()) },
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, tint: Color = MaterialTheme.colorScheme.secondary, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
