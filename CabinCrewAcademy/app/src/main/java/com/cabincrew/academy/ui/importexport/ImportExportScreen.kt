package com.cabincrew.academy.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cabincrew.academy.data.importexport.ImportIssue
import com.cabincrew.academy.ui.appViewModel
import com.cabincrew.academy.ui.components.AppCard
import com.cabincrew.academy.ui.components.ConfirmDialog
import com.cabincrew.academy.ui.components.Eyebrow
import com.cabincrew.academy.ui.components.ScreenScaffold
import com.cabincrew.academy.ui.components.SectionTitle
import com.cabincrew.academy.ui.components.isWideLayout
import com.cabincrew.academy.ui.components.rememberSnackbarHost
import com.cabincrew.academy.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun ImportExportScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> ImportExportViewModel(c.repository, c.appContext) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = rememberSnackbarHost()
    var confirmOverwrite by remember { mutableStateOf(false) }
    val wide = isWideLayout()

    LaunchedEffect(s.message) { s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.parseFiles(it) }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.JSON.mime)) { it?.let { u -> vm.export(u, ExportKind.JSON) } }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.CSV.mime)) { it?.let { u -> vm.export(u, ExportKind.CSV) } }
    val templateJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.JSON.mime)) { it?.let { u -> vm.saveTemplate(u, ExportKind.JSON) } }
    val templateCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.CSV.mime)) { it?.let { u -> vm.saveTemplate(u, ExportKind.CSV) } }
    val stamp = LocalDate.now().toString()

    ScreenScaffold(title = "Import & export", subtitle = "Manage your question database", onBack = onBack, snackbarHostState = snackbar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 1000.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionTitle("Import questions")
                AppCard {
                    Text("Select one or more JSON or CSV files", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Files are validated first – nothing is saved until you confirm. Questions whose ID already exists are never overwritten unless you explicitly choose to.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { pickFiles.launch(arrayOf("*/*")) }, enabled = !s.busy) {
                        Icon(Icons.Filled.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Choose files")
                    }
                    if (s.busy) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }

                s.parse?.let { parse ->
                    val plan = s.plan
                    AppCard {
                        Eyebrow("Validation report")
                        Spacer(Modifier.height(6.dp))
                        Text(s.files.joinToString(", "), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Figure("Valid questions", parse.questions.size.toString(), AppTheme.extended.success)
                            if (plan != null) {
                                Figure("New", plan.newQuestions.toString(), MaterialTheme.colorScheme.primary)
                                Figure("Already in database", plan.existingQuestions.toString(), AppTheme.extended.warning)
                                Figure("New categories", plan.newCategories.size.toString(), MaterialTheme.colorScheme.tertiary)
                            }
                            Figure("Errors", parse.errors.size.toString(), if (parse.errors.isEmpty()) MaterialTheme.colorScheme.outline else AppTheme.extended.danger)
                            Figure("Warnings", parse.warnings.size.toString(), if (parse.warnings.isEmpty()) MaterialTheme.colorScheme.outline else AppTheme.extended.warning)
                        }
                        if (plan != null && plan.newCategories.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Categories to create: ${plan.newCategories.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (parse.errors.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            IssueList("Errors – these questions will NOT be imported", parse.errors, Icons.Filled.ErrorOutline, AppTheme.extended.danger)
                        }
                        if (parse.warnings.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            IssueList("Warnings", parse.warnings, Icons.Filled.Warning, AppTheme.extended.warning)
                        }
                        Spacer(Modifier.height(18.dp))
                        HorizontalDivider(color = AppTheme.extended.cardBorder)
                        Spacer(Modifier.height(14.dp))
                        if (parse.questions.isEmpty() && parse.categories.isEmpty()) {
                            Text("Nothing can be imported. Fix the errors above and choose the file again.", color = AppTheme.extended.danger)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.import(overwriteExisting = false) }, enabled = !s.busy) {
                                    Text(
                                        if ((plan?.existingQuestions ?: 0) > 0) "Import ${plan?.newQuestions} new (skip ${plan?.existingQuestions} existing)"
                                        else "Import ${parse.questions.size} question(s)"
                                    )
                                }
                                if ((plan?.existingQuestions ?: 0) > 0) {
                                    OutlinedButton(
                                        onClick = { confirmOverwrite = true }, enabled = !s.busy,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.extended.danger),
                                    ) { Text("Import and overwrite ${plan?.existingQuestions} existing") }
                                }
                                TextButton(onClick = vm::reset) { Text("Cancel") }
                            }
                        }
                    }
                }

                s.outcome?.let { o ->
                    AppCard(containerColor = AppTheme.extended.successContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = AppTheme.extended.success)
                            Spacer(Modifier.width(10.dp))
                            Text("Import complete", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${o.added} added • ${o.updated} updated • ${o.skipped} skipped (already existed) • ${o.categoriesCreated} categories created")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = vm::reset) { Text("Import more files") }
                    }
                }

                SectionTitle("Export database")
                AppCard {
                    Text("Back up or share your entire question bank", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Exports every category and question. The JSON file can be imported again on any device running Cabin Crew Academy.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportJson.launch("cabin-crew-questions-$stamp.json") }) {
                            Icon(Icons.Filled.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Export as JSON")
                        }
                        OutlinedButton(onClick = { exportCsv.launch("cabin-crew-questions-$stamp.csv") }) {
                            Icon(Icons.Filled.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Export as CSV")
                        }
                    }
                }

                SectionTitle("File format guide")
                AppCard {
                    Text("Empty templates", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { templateJson.launch("questions-template.json") }) {
                            Icon(Icons.Filled.Description, null); Spacer(Modifier.width(8.dp)); Text("Save JSON template")
                        }
                        OutlinedButton(onClick = { templateCsv.launch("questions-template.csv") }) {
                            Icon(Icons.Filled.Description, null); Spacer(Modifier.width(8.dp)); Text("Save CSV template")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    GuideText("JSON", """{
  "categories": [
    {
      "name": "Aviation Knowledge",
      "questions": [
        {
          "id": "Q001",
          "question": "Example question?",
          "options": ["Option A", "Option B", "Option C", "Option D"],
          "correctAnswer": 1,
          "explanation": "Optional explanation.",
          "difficulty": "medium"
        }
      ]
    }
  ]
}""")
                    Spacer(Modifier.height(12.dp))
                    GuideText("CSV (first row = header, comma or semicolon separated)",
                        "id,category,question,option1,option2,option3,option4,correctAnswer,explanation,difficulty\n" +
                            "Q001,Aviation Knowledge,Example question?,Option A,Option B,Option C,Option D,1,Optional explanation.,medium")
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "correctAnswer is the position of the correct option starting at 0 (0 = first option, 1 = second…). In CSV you may also use a letter A–J.",
                        "Between 2 and 10 options per question. In CSV, add more columns (option5, option6…) as needed and leave unused ones empty.",
                        "id must be unique. If omitted, a stable ID is generated from the category and question text.",
                        "explanation and difficulty (easy / medium / hard) are optional.",
                        "Categories that do not exist yet are created automatically.",
                    ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmOverwrite) {
        ConfirmDialog(
            "Overwrite existing questions?",
            "${s.plan?.existingQuestions} question(s) in your database have the same ID as questions in the file. Their text, options, answer and explanation will be replaced by the file's version. Study progress for those questions is kept.",
            "Overwrite", destructive = true, onConfirm = { vm.import(overwriteExisting = true) }, onDismiss = { confirmOverwrite = false },
        )
    }
}

@Composable
private fun Figure(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IssueList(title: String, issues: List<ImportIssue>, icon: ImageVector, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text("$title (${issues.size})", style = MaterialTheme.typography.titleSmall, color = color)
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.heightIn(max = 260.dp)) {
        SelectionContainer {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                issues.forEach {
                    Text("${it.source} • ${it.location}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun GuideText(title: String, code: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    SelectionContainer {
        Text(
            code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
                .padding(top = 2.dp)
                .then(Modifier),
        )
    }
}
