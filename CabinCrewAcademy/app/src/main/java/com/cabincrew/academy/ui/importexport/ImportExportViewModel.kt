package com.cabincrew.academy.ui.importexport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabincrew.academy.data.importexport.Exporter
import com.cabincrew.academy.data.importexport.ParseResult
import com.cabincrew.academy.data.importexport.QuestionParser
import com.cabincrew.academy.data.repository.ImportOutcome
import com.cabincrew.academy.data.repository.ImportPlan
import com.cabincrew.academy.data.repository.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class ImportUiState(
    val busy: Boolean = false,
    val files: List<String> = emptyList(),
    val parse: ParseResult? = null,
    val plan: ImportPlan? = null,
    val outcome: ImportOutcome? = null,
    val message: String? = null,
)

enum class ExportKind(val mime: String, val ext: String) { JSON("application/json", "json"), CSV("text/csv", "csv") }

class ImportExportViewModel(private val repo: StudyRepository, private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun reset() { _state.value = ImportUiState() }

    fun parseFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.value = ImportUiState(busy = true)
        viewModelScope.launch {
            val (names, combined) = withContext(Dispatchers.IO) {
                val names = mutableListOf<String>()
                var result = ParseResult()
                uris.forEach { uri ->
                    val name = displayName(uri)
                    names += name
                    result += try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Could not open file")
                        QuestionParser.parse(name, decode(bytes))
                    } catch (e: Exception) {
                        ParseResult(errors = listOf(com.cabincrew.academy.data.importexport.ImportIssue(name, "file", "Could not read file: ${e.message}")))
                    }
                }
                names to QuestionParser.checkDuplicateIds(result)
            }
            val plan = repo.planImport(combined)
            _state.value = ImportUiState(busy = false, files = names, parse = combined, plan = plan)
        }
    }

    fun import(overwriteExisting: Boolean) {
        val parsed = _state.value.parse ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repo.applyImport(parsed, overwriteExisting) }
                .onSuccess { out -> _state.update { ImportUiState(outcome = out, files = it.files) } }
                .onFailure { e -> _state.update { it.copy(busy = false, message = "Import failed: ${e.message}") } }
        }
    }

    fun export(uri: Uri, kind: ExportKind) = viewModelScope.launch {
        runCatching {
            val text = if (kind == ExportKind.JSON) repo.exportJson() else repo.exportCsv()
            withContext(Dispatchers.IO) { write(uri, text, bom = kind == ExportKind.CSV) }
        }.onSuccess { _state.update { it.copy(message = "Question database exported successfully") } }
            .onFailure { e -> _state.update { it.copy(message = "Export failed: ${e.message}") } }
    }

    fun saveTemplate(uri: Uri, kind: ExportKind) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) { write(uri, if (kind == ExportKind.JSON) Exporter.JSON_TEMPLATE else Exporter.CSV_TEMPLATE, bom = kind == ExportKind.CSV) }
        }.onSuccess { _state.update { it.copy(message = "Template saved") } }
            .onFailure { e -> _state.update { it.copy(message = "Could not save template: ${e.message}") } }
    }

    private fun write(uri: Uri, text: String, bom: Boolean) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            // A UTF-8 BOM makes Excel open CSV files with accents correctly.
            if (bom) out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Could not open destination")
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) c.getString(idx)?.let { return it }
            }
        }
        return uri.lastPathSegment ?: "file"
    }

    /** UTF-8 first; files saved by older Excel versions fall back to Windows-1252. */
    private fun decode(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (e: CharacterCodingException) {
        String(bytes, Charset.forName("windows-1252"))
    }
}
