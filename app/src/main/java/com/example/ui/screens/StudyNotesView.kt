package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.CircularProgressIndicator
import com.example.ai.AuraAiService
import com.example.data.local.entity.StudyNoteEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.dialogs.CitationManagerDialog
import com.example.ui.dialogs.SlideScannerDialog
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.LocalAuraColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StudyNotesView(
    viewModel: AuraViewModel
) {
    val context = LocalContext.current
    val notes by viewModel.studyNotes.collectAsState()
    val courses by viewModel.courses.collectAsState()

    val citations by viewModel.citations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var editingNote by remember { mutableStateOf<StudyNoteEntity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showSlideScanner by remember { mutableStateOf(false) }
    var showCitationDialog by remember { mutableStateOf(false) }
    var isCritiquingDraft by remember { mutableStateOf(false) }
    var rubricResult by remember { mutableStateOf<AuraAiService.RubricCritiqueResult?>(null) }
    var rubricTargetNote by remember { mutableStateOf<StudyNoteEntity?>(null) }

    val categories = listOf(
        "ALL" to "All Notes",
        "PINNED" to "Pinned",
        "FORMULA" to "Formulas",
        "CONCEPT" to "Concepts",
        "SUMMARY" to "Summaries",
        "CHEAT_SHEET" to "Cheat-Sheets"
    )

    val filteredNotes = remember(notes, searchQuery, selectedCategory) {
        notes.filter { note ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "PINNED" -> note.isPinned
                else -> note.category == selectedCategory
            }
            val matchesQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.courseCode.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_notes_vault_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuraCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f))
                                .border(1.dp, Color(0x66FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notes & Formula Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${notes.size} saved notes • ${notes.count { it.isPinned }} pinned",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.secondaryAccent,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraGlassButton(
                            text = "New Note",
                            icon = Icons.Default.Add,
                            isPrimary = true,
                            cornerRadius = 14.dp,
                            onClick = {
                                editingNote = null
                                showEditorDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "add_study_note_button"
                        )
                        AuraGlassButton(
                            text = "Scan",
                            icon = Icons.Default.CameraAlt,
                            cornerRadius = 14.dp,
                            onClick = { showSlideScanner = true },
                            modifier = Modifier.weight(1f),
                            testTag = "scan_slide_photo_button"
                        )
                        AuraGlassButton(
                            text = "Cite (${citations.size})",
                            icon = Icons.Default.FormatQuote,
                            cornerRadius = 14.dp,
                            onClick = { showCitationDialog = true },
                            modifier = Modifier.weight(1f),
                            testTag = "open_citations_from_notes_button"
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search formulas, theorems, course codes...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Notes",
                        tint = LocalAuraColors.current.secondaryAccent
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_study_notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                    unfocusedBorderColor = Color(0x36FFFFFF),
                    focusedContainerColor = Color(0x1EFFFFFF),
                    unfocusedContainerColor = Color(0x16FFFFFF),
                    focusedTextColor = LocalAuraColors.current.textPrimary,
                    unfocusedTextColor = LocalAuraColors.current.textPrimary
                )
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { (key, label) ->
                    val isSelected = selectedCategory == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xBBFFFFFF) else Color(0x35FFFFFF)
                        ),
                        modifier = Modifier.clickable { selectedCategory = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else LocalAuraColors.current.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (notes.isEmpty()) "Your Formula & Notes Vault is Empty" else "No Matching Notes Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Capture high-yield equations, definitions, and lecture summaries—then turn any note into a Leitner flashcard with one tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                StudyNoteGlassCard(
                    note = note,
                    onTogglePin = { viewModel.togglePinStudyNote(note) },
                    onEdit = {
                        editingNote = note
                        showEditorDialog = true
                    },
                    onDelete = { viewModel.deleteStudyNote(note) },
                    onConvertToFlashcard = {
                        viewModel.convertNoteToFlashcard(note)
                        Toast.makeText(
                            context,
                            "Added '${note.title}' to Flashcards deck!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(note.title, "${note.title}\n\n${note.content}"))
                        Toast.makeText(context, "Copied note to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onAiRubricCoach = {
                        rubricTargetNote = note
                        isCritiquingDraft = true
                        viewModel.critiqueStudyNoteDraft(note.courseCode, note.title, note.content) { res ->
                            isCritiquingDraft = false
                            rubricResult = res
                        }
                    }
                )
            }
        }
    }

    if (showSlideScanner) {
        SlideScannerDialog(
            viewModel = viewModel,
            defaultCourseCode = courses.firstOrNull()?.code ?: "General",
            onDismiss = { showSlideScanner = false }
        )
    }

    if (showCitationDialog) {
        CitationManagerDialog(
            viewModel = viewModel,
            defaultCourseCode = courses.firstOrNull()?.code ?: "General",
            onDismiss = { showCitationDialog = false }
        )
    }

    if (showEditorDialog) {
        var courseCode by remember(editingNote) {
            mutableStateOf(editingNote?.courseCode ?: (courses.firstOrNull()?.code ?: "General"))
        }
        var title by remember(editingNote) { mutableStateOf(editingNote?.title ?: "") }
        var content by remember(editingNote) { mutableStateOf(editingNote?.content ?: "") }
        var category by remember(editingNote) { mutableStateOf(editingNote?.category ?: "FORMULA") }
        var isPinned by remember(editingNote) { mutableStateOf(editingNote?.isPinned ?: false) }

        val editorCategories = listOf(
            "FORMULA" to "Formula",
            "CONCEPT" to "Concept",
            "SUMMARY" to "Summary",
            "CHEAT_SHEET" to "Cheat-Sheet"
        )

        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            title = {
                Text(
                    text = if (editingNote == null) "Save Note / Formula" else "Edit Note",
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (courses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(courses) { c ->
                                val selected = courseCode == c.code
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                                    modifier = Modifier.clickable { courseCode = c.code }
                                ) {
                                    Text(
                                        text = c.code,
                                        color = if (selected) Color.White else LocalAuraColors.current.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text("Course Code") },
                            modifier = Modifier.fillMaxWidth().testTag("input_note_course_code"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(editorCategories) { (key, label) ->
                            val selected = category == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) LocalAuraColors.current.secondaryAccent.copy(alpha = 0.3f) else Color(0x1EFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) LocalAuraColors.current.secondaryAccent else Color(0x30FFFFFF)
                                ),
                                modifier = Modifier.clickable { category = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Note / Formula Title (e.g. Bayes' Theorem)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_note_title"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Equation, Essay Draft, Definition, or Summary") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("input_note_content"),
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp)
                    )

                    AuraGlassButton(
                        text = if (isCritiquingDraft) "Grading Draft Rubric..." else "AI Assignment Rubric & Clarity Coach",
                        icon = Icons.Default.AutoAwesome,
                        enabled = !isCritiquingDraft && content.isNotBlank(),
                        onClick = {
                            isCritiquingDraft = true
                            rubricTargetNote = editingNote
                            viewModel.critiqueStudyNoteDraft(courseCode, title.ifBlank { "Academic Draft" }, content) { res ->
                                isCritiquingDraft = false
                                rubricResult = res
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "editor_rubric_coach_button"
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save to Vault",
                    isPrimary = true,
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.saveStudyNote(
                                id = editingNote?.id ?: 0L,
                                courseCode = courseCode,
                                title = title,
                                content = content,
                                category = category,
                                isPinned = isPinned
                            )
                            showEditorDialog = false
                        }
                    },
                    testTag = "save_study_note_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showEditorDialog = false },
                    testTag = "cancel_study_note_button"
                )
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
                RoundedCornerShape(24.dp)
            )
        )
    }

    rubricResult?.let { critique ->
        AlertDialog(
            onDismissRequest = { rubricResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AuraMintAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Rubric & Clarity Coach",
                        fontWeight = FontWeight.ExtraBold,
                        color = LocalAuraColors.current.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuraBadge(
                            text = "Clarity ${critique.clarityScore}/100",
                            backgroundColor = AuraMintAccent.copy(alpha = 0.22f),
                            textColor = AuraMintAccent
                        )
                        AuraBadge(
                            text = critique.estimatedGradeBand,
                            backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f),
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    Text("Strengths", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AuraMintAccent)
                    Text(critique.strengths, fontSize = 12.sp, color = LocalAuraColors.current.textPrimary)

                    Text("Rubric Improvements", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LocalAuraColors.current.warning)
                    Text(critique.improvements, fontSize = 12.sp, color = LocalAuraColors.current.textPrimary)

                    if (critique.polishedExcerpt.isNotBlank()) {
                        Text("First-Class Opening Rewrite", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LocalAuraColors.current.secondaryAccent)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1EFFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = critique.polishedExcerpt,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Done",
                    isPrimary = true,
                    onClick = { rubricResult = null },
                    testTag = "close_rubric_coach_dialog"
                )
            },
            containerColor = Color(0xF50E192D),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.testTag("ai_rubric_critique_dialog")
        )
    }
}

@Composable
private fun StudyNoteGlassCard(
    note: StudyNoteEntity,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConvertToFlashcard: () -> Unit,
    onCopy: () -> Unit,
    onAiRubricCoach: () -> Unit
) {
    val updatedStr = remember(note.updatedEpochMillis) {
        val dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(note.updatedEpochMillis),
            ZoneId.systemDefault()
        )
        dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US))
    }

    val categoryLabel = when (note.category) {
        "FORMULA" -> "Formula"
        "CHEAT_SHEET" -> "Cheat-Sheet"
        "SUMMARY" -> "Summary"
        else -> "Concept"
    }

    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (note.isPinned) LocalAuraColors.current.secondaryAccent.copy(alpha = 0.7f) else null,
        testTag = "study_note_card_${note.id}"
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(text = note.courseCode)
                Spacer(modifier = Modifier.width(6.dp))
                AuraBadge(
                    text = categoryLabel,
                    backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                    textColor = LocalAuraColors.current.secondaryAccent
                )
                Spacer(modifier = Modifier.weight(1f))
                AuraGlassIconButton(
                    icon = Icons.Default.PushPin,
                    contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                    onClick = onTogglePin,
                    size = 36.dp,
                    iconSize = 16.dp,
                    isPrimary = note.isPinned,
                    tint = if (note.isPinned) Color.White else LocalAuraColors.current.textSecondary,
                    testTag = "pin_note_${note.id}"
                )
                Spacer(modifier = Modifier.width(6.dp))
                AuraGlassIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit note",
                    onClick = onEdit,
                    size = 36.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.textPrimary,
                    testTag = "edit_note_${note.id}"
                )
                Spacer(modifier = Modifier.width(6.dp))
                AuraGlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete note",
                    onClick = onDelete,
                    size = 36.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.danger,
                    testTag = "delete_note_${note.id}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (note.category == "FORMULA") FontFamily.Monospace else FontFamily.Default,
                    color = LocalAuraColors.current.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = updatedStr,
                    fontSize = 10.sp,
                    color = LocalAuraColors.current.textMuted,
                    modifier = Modifier.weight(1f)
                )

                AuraGlassButton(
                    text = "Coach",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onAiRubricCoach,
                    cornerRadius = 14.dp,
                    testTag = "critique_note_${note.id}"
                )

                AuraGlassButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    onClick = onCopy,
                    cornerRadius = 14.dp,
                    testTag = "copy_note_${note.id}"
                )

                AuraGlassButton(
                    text = "Card",
                    icon = Icons.Default.Style,
                    isPrimary = true,
                    onClick = onConvertToFlashcard,
                    cornerRadius = 14.dp,
                    testTag = "convert_note_flashcard_${note.id}"
                )
            }
        }
    }
}
