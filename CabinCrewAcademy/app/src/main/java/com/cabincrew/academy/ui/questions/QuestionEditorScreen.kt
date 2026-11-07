package com.cabincrew.academy.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.theme.AppTheme

@Composable
fun QuestionEditorScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, h -> QuestionEditorViewModel(c.repository, h) }
    val s by vm.state.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var newCategoryDialog by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }

    LaunchedEffect(s.saved, s.deleted) { if (s.saved || s.deleted) onBack() }
    // Default to the first category for new questions.
    LaunchedEffect(categories, s.loading) {
        if (!s.loading && s.categoryId == null && categories.isNotEmpty()) vm.update { it.copy(categoryId = categories.first().id) }
    }

    ScreenScaffold(
        title = if (s.isNew) "New question" else "Edit question",
        onBack = onBack,
        actions = {
            if (!s.isNew) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete question") }
            Button(onClick = vm::save, modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Save")
            }
        },
    ) { padding ->
        if (s.loading) { LinearProgressIndicator(Modifier.padding(padding).fillMaxWidth()); return@ScreenScaffold }
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 900.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (s.errors.isNotEmpty()) {
                    AppCard(containerColor = AppTheme.extended.dangerContainer) {
                        s.errors.forEach { Text("• $it", color = AppTheme.extended.danger) }
                    }
                }
                AppCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = s.externalId, onValueChange = { v -> vm.update { it.copy(externalId = v) } },
                            label = { Text("Question ID") }, singleLine = true, modifier = Modifier.weight(1f),
                        )
                        ExposedDropdownMenuBox(expanded = catMenu, onExpandedChange = { catMenu = it }, modifier = Modifier.weight(2f)) {
                            OutlinedTextField(
                                value = categories.firstOrNull { it.id == s.categoryId }?.name ?: "Choose category",
                                onValueChange = {}, readOnly = true, label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenu) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                                categories.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = { vm.update { it.copy(categoryId = c.id) }; catMenu = false })
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("New category…") }, leadingIcon = { Icon(Icons.Filled.Add, null) },
                                    onClick = { catMenu = false; newCategoryDialog = true },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = s.text, onValueChange = { v -> vm.update { it.copy(text = v) } },
                        label = { Text("Question") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Difficulty.entries.forEachIndexed { i, d ->
                            SegmentedButton(
                                selected = s.difficulty == d, onClick = { vm.update { it.copy(difficulty = d) } },
                                shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                            ) { Text(d.label) }
                        }
                    }
                }

                SectionTitle("Answer options")
                Text("Select the radio button next to the correct answer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppCard {
                    s.options.forEachIndexed { i, opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = s.correctIndex == i, onClick = { vm.update { it.copy(correctIndex = i) } })
                            OutlinedTextField(
                                value = opt, onValueChange = { vm.setOption(i, it) },
                                label = { Text("Option ${'A' + i}${if (s.correctIndex == i) " (correct)" else ""}") },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.removeOption(i) }, enabled = s.options.size > 2) {
                                Icon(Icons.Filled.RemoveCircleOutline, "Remove option")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = vm::addOption, enabled = s.options.size < 10) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add option")
                    }
                }

                SectionTitle("Explanation (optional)")
                OutlinedTextField(
                    value = s.explanation, onValueChange = { v -> vm.update { it.copy(explanation = v) } },
                    label = { Text("Why is this the correct answer?") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save question") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Delete this question?", "The question and its study progress will be permanently removed.",
            "Delete", destructive = true, onConfirm = vm::delete, onDismiss = { confirmDelete = false },
        )
    }
    if (newCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategoryDialog = false },
            title = { Text("New category") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category name") }, singleLine = true) },
            confirmButton = { Button(onClick = { vm.createCategory(name); newCategoryDialog = false }, enabled = name.isNotBlank()) { Text("Create") } },
            dismissButton = { TextButton(onClick = { newCategoryDialog = false }) { Text("Cancel") } },
        )
    }
}
