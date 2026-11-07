package com.cabincrew.academy.ui.study

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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.domain.StudyMode
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ContentWidth
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.ResponsiveGrid
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.columnsForWidth
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme
import kotlin.random.Random

private fun StudyMode.icon(): ImageVector = when (this) {
    StudyMode.ALL -> Icons.AutoMirrored.Filled.LibraryBooks
    StudyMode.UNSTUDIED -> Icons.Filled.FiberNew
    StudyMode.INCORRECT -> Icons.Filled.Cancel
    StudyMode.BOOKMARKED -> Icons.Filled.Bookmark
    StudyMode.FAVOURITES -> Icons.Filled.Favorite
    StudyMode.DIFFICULT -> Icons.Filled.Flag
}

@Composable
fun StudySetupScreen(onStart: (StudyConfig) -> Unit, onImport: () -> Unit) {
    val vm = appViewModel { c, _ -> StudySetupViewModel(c.repository) }
    val questions by vm.questions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(StudyMode.ALL) }
    var categoryId by rememberSaveable { mutableStateOf(-1L) }
    var difficulty by rememberSaveable { mutableStateOf<Difficulty?>(null) }
    var shuffle by rememberSaveable { mutableStateOf(false) }

    val base = StudyConfig(mode, categoryId, difficulty, shuffle)
    val matching = questions?.count { base.matches(it) } ?: 0
    val wide = isWideLayout()

    ScreenScaffold(title = "Study mode", subtitle = "Learn at your own pace with instant feedback") { padding ->
        val list = questions
        if (list != null && list.isEmpty()) {
            EmptyState(
                Icons.Filled.FileUpload, "No questions yet",
                "Import your question files or create questions manually to start studying.",
                Modifier.padding(padding), actionLabel = "Import questions", onAction = onImport,
            )
            return@ScreenScaffold
        }
        ContentWidth(modifier = Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionTitle("What would you like to study?")
                ResponsiveGrid(StudyMode.entries, columns = columnsForWidth(250.dp).coerceAtMost(3)) { m ->
                    val count = questions?.count { StudyConfig(m, categoryId, difficulty).matches(it) } ?: 0
                    val selected = m == mode
                    AppCard(
                        onClick = { mode = m },
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(m.icon(), null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.titleMedium)
                                Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$count", style = MaterialTheme.typography.titleLarge)
                                RadioButton(selected = selected, onClick = { mode = m }, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                SectionTitle("Category")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = categoryId == -1L, onClick = { categoryId = -1L }, label = { Text("All categories") })
                    categories.forEach { c ->
                        FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id }, label = { Text("${c.name} (${c.questionCount})") })
                    }
                }

                SectionTitle("Difficulty level")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = difficulty == null, onClick = { difficulty = null }, label = { Text("Any") })
                    Difficulty.entries.forEach { d ->
                        FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.label) })
                    }
                }

                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shuffle, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Shuffle question order", style = MaterialTheme.typography.titleMedium)
                            Text("Practise questions in a random order", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = shuffle, onCheckedChange = { shuffle = it })
                    }
                }

                AppCard {
                    Text(
                        if (matching == 0) "No questions match these filters." else "$matching question${if (matching == 1) "" else "s"} ready",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (matching == 0) AppTheme.extended.danger else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onStart(base.copy(seed = Random.nextLong())) },
                            enabled = matching > 0,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        ) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start studying") }
                        OutlinedButton(
                            onClick = { onStart(StudyConfig(StudyMode.ALL, categoryId, difficulty, shuffle = true, seed = Random.nextLong())) },
                            enabled = (questions?.count { StudyConfig(StudyMode.ALL, categoryId, difficulty).matches(it) } ?: 0) > 0,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        ) { Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("Random practice") }
                    }
                }
                Spacer(Modifier.height(16.dp).fillMaxWidth())
            }
        }
    }
}
