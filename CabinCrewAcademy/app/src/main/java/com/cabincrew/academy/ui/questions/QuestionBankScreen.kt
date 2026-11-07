package com.cabincrew.academy.ui.questions

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.data.db.QuestionWithProgress
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.DifficultyBadge
import com.cabincrew.academy.ui.components.EmptyState
import com.cabincrew.academy.ui.components.Pill
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.theme.AppTheme

@Composable
fun QuestionBankScreen(onEdit: (Long) -> Unit, onCategories: () -> Unit, onImport: () -> Unit) {
    val vm = appViewModel { c, _ -> QuestionBankViewModel(c.repository) }
    val s by vm.state.collectAsStateWithLifecycle()
    val f = s.filters
    val wide = isWideLayout()
    var categoryMenu by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Question bank",
        subtitle = if (s.loading) null else "${s.all} questions • ${s.categories.size} categories",
        actions = {
            TextButton(onClick = onCategories) { Icon(Icons.Filled.Category, null); Spacer(Modifier.width(6.dp)); Text("Categories") }
            TextButton(onClick = onImport) { Icon(Icons.Filled.ImportExport, null); Spacer(Modifier.width(6.dp)); Text("Import / Export") }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(-1) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New question") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (!s.loading && s.all == 0) {
            EmptyState(
                Icons.Filled.FileUpload, "Your question bank is empty",
                "Import JSON or CSV files with your questions, or tap \"New question\" to create one manually.",
                Modifier.padding(padding), actionLabel = "Import questions", onAction = onImport,
            )
            return@ScreenScaffold
        }
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 1100.dp).fillMaxSize(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(start = if (wide) 32.dp else 16.dp, end = if (wide) 32.dp else 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = f.query, onValueChange = vm::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by keyword, ID, answer or explanation") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = { if (f.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Clear, "Clear search") } },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box {
                            FilterChip(
                                selected = f.categoryId >= 0, onClick = { categoryMenu = true },
                                label = { Text(s.categories.firstOrNull { it.id == f.categoryId }?.name ?: "All categories") },
                                leadingIcon = { Icon(Icons.Filled.Category, null, Modifier.size(18.dp)) },
                            )
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                DropdownMenuItem(text = { Text("All categories") }, onClick = { vm.setCategory(-1); categoryMenu = false })
                                s.categories.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = { vm.setCategory(c.id); categoryMenu = false })
                                }
                            }
                        }
                        Difficulty.entries.forEach { d ->
                            FilterChip(selected = f.difficulty == d, onClick = { vm.setDifficulty(if (f.difficulty == d) null else d) }, label = { Text(d.label) })
                        }
                        FlagFilter.entries.drop(1).forEach { fl ->
                            FilterChip(selected = f.flag == fl, onClick = { vm.setFlag(if (f.flag == fl) FlagFilter.ALL else fl) }, label = { Text(fl.label) })
                        }
                    }
                }
                item {
                    Text("${s.results.size} result${if (s.results.size == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s.results.isEmpty() && !s.loading) item {
                    EmptyState(Icons.Filled.SearchOff, "No matching questions", "Try a different keyword or clear the filters.")
                }
                items(s.results, key = { it.question.id }) { q ->
                    QuestionRow(q, onClick = { onEdit(q.question.id) }, onBookmark = { vm.toggleBookmark(q) }, onFavourite = { vm.toggleFavourite(q) })
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(q: QuestionWithProgress, onClick: () -> Unit, onBookmark: () -> Unit, onFavourite: () -> Unit) {
    val ext = AppTheme.extended
    val p = q.progress
    AppCard(onClick = onClick, contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Pill(q.question.externalId, MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill(q.category?.name ?: "–", MaterialTheme.colorScheme.primary)
                    DifficultyBadge(q.question.difficulty)
                    if (p?.markedDifficult == true) Pill("Difficult", ext.warning)
                    when (p?.lastCorrect) {
                        true -> Pill("Last: correct", ext.success)
                        false -> Pill("Last: incorrect", ext.danger)
                        null -> {}
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(q.question.text, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Answer: ${'A' + q.question.correctIndex}. ${q.question.options.getOrNull(q.question.correctIndex).orEmpty()}",
                    style = MaterialTheme.typography.bodySmall, color = ext.success, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column {
                IconButton(onClick = onBookmark) {
                    Icon(if (p?.bookmarked == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Bookmark",
                        tint = if (p?.bookmarked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onFavourite) {
                    Icon(if (p?.favourite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favourite",
                        tint = if (p?.favourite == true) ext.danger else MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

