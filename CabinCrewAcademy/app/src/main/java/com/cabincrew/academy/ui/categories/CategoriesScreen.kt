package com.cabincrew.academy.ui.categories

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.db.CategoryWithStats
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.domain.StudyConfig
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.MeterBar
import com.cabincrew.academy.ui.components.ResponsiveGrid
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.columnsForWidth
import com.cabincrew.academy.ui.components.formatPercent
import com.cabincrew.academy.ui.components.rememberSnackbarHost
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Example subjects offered as a one-tap starting point. Only names are created – no questions. */
val SUGGESTED_CATEGORIES = listOf(
    "Aviation Knowledge", "Safety and Emergency Procedures", "First Aid", "Customer Service", "English",
    "General Knowledge", "Numerical Reasoning", "Verbal Reasoning", "Personality and Situational Judgement",
)

class CategoriesViewModel(private val repo: StudyRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryWithStats>?> =
        repo.categoriesWithStats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun create(name: String, description: String) = viewModelScope.launch {
        runCatching { repo.createCategory(name, description) }
            .onSuccess { _messages.emit("Category \"${name.trim()}\" created") }
            .onFailure { _messages.emit(it.message ?: "Could not create category") }
    }

    fun rename(id: Long, name: String, description: String) = viewModelScope.launch {
        runCatching { repo.renameCategory(id, name, description) }
            .onSuccess { _messages.emit("Category updated") }
            .onFailure { _messages.emit(it.message ?: "Could not rename category") }
    }

    fun delete(c: CategoryWithStats) = viewModelScope.launch {
        repo.deleteCategory(c.id)
        _messages.emit("Deleted \"${c.name}\" and ${c.questionCount} question(s)")
    }

    fun createSuggested() = viewModelScope.launch {
        SUGGESTED_CATEGORIES.forEach { runCatching { repo.createCategory(it) } }
        _messages.emit("Suggested categories created")
    }
}

@Composable
fun CategoriesScreen(onBack: () -> Unit, onStudyCategory: (StudyConfig) -> Unit) {
    val vm = appViewModel { c, _ -> CategoriesViewModel(c.repository) }
    val cats by vm.categories.collectAsStateWithLifecycle()
    val snackbar = rememberSnackbarHost()
    var editing by remember { mutableStateOf<CategoryWithStats?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryWithStats?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    ScreenScaffold(
        title = "Categories",
        subtitle = "Organise your questions by subject",
        onBack = onBack,
        snackbarHostState = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New category") })
        },
    ) { padding ->
        val list = cats
        if (list != null && list.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    Icons.Filled.Category, "No categories yet",
                    "Create your own subjects, or start with the suggested cabin crew subjects (names only – no questions are added). " +
                        "Categories are also created automatically when you import files.",
                )
                OutlinedButton(onClick = vm::createSuggested) { Text("Create suggested categories") }
            }
            return@ScreenScaffold
        }
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 1200.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                ResponsiveGrid(list.orEmpty(), columns = columnsForWidth(320.dp)) { c ->
                    AppCard(contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                if (c.description.isNotBlank()) Text(c.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, "Rename") }
                            IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, "Delete") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${c.questionCount} questions • ${c.studiedCount} studied • ${if (c.answered == 0) "no answers yet" else "${formatPercent(c.accuracy)} accuracy"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.padding(end = 12.dp)) { MeterBar(c.completion) }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { onStudyCategory(StudyConfig(categoryId = c.id)) }, enabled = c.questionCount > 0) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Study this category")
                        }
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (creating) CategoryDialog("New category", "", "", onDismiss = { creating = false }) { n, d -> vm.create(n, d) }
    editing?.let { c -> CategoryDialog("Edit category", c.name, c.description, onDismiss = { editing = null }) { n, d -> vm.rename(c.id, n, d) } }
    deleting?.let { c ->
        ConfirmDialog(
            "Delete \"${c.name}\"?",
            if (c.questionCount > 0) "This will permanently delete the category and its ${c.questionCount} question(s), including their study progress. Consider exporting a backup first."
            else "This empty category will be deleted.",
            "Delete", destructive = true, onConfirm = { vm.delete(c) }, onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CategoryDialog(title: String, initialName: String, initialDescription: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, description); onDismiss() }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
