package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.formatTime12Hour
import com.example.ui.screens.StudyTabs
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraVioletAccent
import com.example.ui.theme.LocalAuraColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class GlobalSearchFilter(val label: String) {
    ALL("All"),
    SLIDES("Slides & PDFs"),
    TIMETABLE("Timetable"),
    TASKS("Exams & Tasks"),
    NOTES("Notes & Cards")
}

private data class GlobalSearchResultItem(
    val key: String,
    val category: GlobalSearchFilter,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun GlobalSearchDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit,
    onOpenTimetableDay: (Int) -> Unit,
    onOpenStudyTab: (Int, String?) -> Unit,
    onOpenTasks: () -> Unit,
    onOpenManageCourses: () -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    val courses by viewModel.courses.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val resources by viewModel.courseResources.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val studyNotes by viewModel.studyNotes.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()

    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(GlobalSearchFilter.ALL) }

    val trimmedQuery = query.trim()

    val searchResults = remember(
        trimmedQuery,
        activeFilter,
        courses,
        lectures,
        resources,
        assessments,
        studyNotes,
        flashcards
    ) {
        val list = mutableListOf<GlobalSearchResultItem>()
        val q = trimmedQuery

        fun matches(vararg fields: String): Boolean {
            if (q.isEmpty()) return true
            return fields.any { it.contains(q, ignoreCase = true) }
        }

        // 1. Uploaded Course Slides & PDFs
        if (activeFilter == GlobalSearchFilter.ALL || activeFilter == GlobalSearchFilter.SLIDES) {
            resources.filter { res ->
                matches(
                    res.title,
                    res.courseCode,
                    res.moduleFolder,
                    res.originalFileName,
                    res.personalNotes,
                    res.extractedText.take(16_000)
                )
            }.take(if (q.isEmpty()) 4 else 20).forEach { res ->
                val progressPct = if (res.totalPages > 0) {
                    (((res.lastReadPage + 1).coerceAtMost(res.totalPages) * 100) / res.totalPages)
                } else 100
                list.add(
                    GlobalSearchResultItem(
                        key = "res_${res.id}",
                        category = GlobalSearchFilter.SLIDES,
                        badgeText = "${res.courseCode} • ${res.fileType}",
                        title = res.title,
                        subtitle = "${res.moduleFolder} • ${res.totalPages} pages • $progressPct% read",
                        icon = if (res.fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Slideshow,
                        accentColor = auraColors.secondaryAccent,
                        onClick = {
                            onOpenStudyTab(StudyTabs.COURSE_SLIDES, res.courseCode)
                            onDismiss()
                        }
                    )
                )
            }
        }

        // 2. Timetable Lectures & Enrolled Courses
        if (activeFilter == GlobalSearchFilter.ALL || activeFilter == GlobalSearchFilter.TIMETABLE) {
            lectures.filter { lec ->
                matches(lec.courseCode, lec.courseName, lec.venue, lec.lecturer)
            }.take(if (q.isEmpty()) 4 else 20).forEach { lec ->
                val dayLabel = DayOfWeek.of(lec.dayOfWeek.coerceIn(1, 7))
                    .name.lowercase().replaceFirstChar { it.uppercase() }
                list.add(
                    GlobalSearchResultItem(
                        key = "lec_${lec.id}",
                        category = GlobalSearchFilter.TIMETABLE,
                        badgeText = "$dayLabel • ${lec.courseCode}",
                        title = lec.courseName,
                        subtitle = "${formatTime12Hour(lec.startTime)} – ${formatTime12Hour(lec.endTime)} • ${lec.venue}",
                        icon = Icons.Default.CalendarToday,
                        accentColor = AuraMintAccent,
                        onClick = {
                            onOpenTimetableDay(lec.dayOfWeek)
                            onDismiss()
                        }
                    )
                )
            }

            if (q.isNotEmpty()) {
                courses.filter { c ->
                    matches(c.code, c.name, c.lecturer, c.venue)
                }.take(10).forEach { course ->
                    list.add(
                        GlobalSearchResultItem(
                            key = "course_${course.id}",
                            category = GlobalSearchFilter.TIMETABLE,
                            badgeText = "COURSE • ${course.creditHours} Cr",
                            title = "${course.code} — ${course.name}",
                            subtitle = "${course.venue.ifBlank { "Campus" }} • ${course.lecturer.ifBlank { "Manage or edit course" }}",
                            icon = Icons.Default.School,
                            accentColor = auraColors.primaryAccent,
                            onClick = {
                                onOpenManageCourses()
                                onDismiss()
                            }
                        )
                    )
                }
            }
        }

        // 3. Assessments & Upcoming Exams
        if (activeFilter == GlobalSearchFilter.ALL || activeFilter == GlobalSearchFilter.TASKS) {
            assessments.filter { a ->
                matches(a.title, a.courseCode, a.courseName, a.type, a.examVenue, a.description)
            }.take(if (q.isEmpty()) 3 else 15).forEach { item ->
                val dueStr = runCatching {
                    Instant.ofEpochMilli(item.dueEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                }.getOrDefault("Upcoming")
                val isExam = item.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST")
                list.add(
                    GlobalSearchResultItem(
                        key = "task_${item.id}",
                        category = GlobalSearchFilter.TASKS,
                        badgeText = "${item.courseCode} • ${item.type.replace('_', ' ')}",
                        title = item.title,
                        subtitle = "Due $dueStr • Status: ${item.status}",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        accentColor = if (isExam) auraColors.warning else AuraVioletAccent,
                        onClick = {
                            if (isExam) {
                                onOpenStudyTab(StudyTabs.WAR_ROOM, item.courseCode)
                            } else {
                                onOpenTasks()
                            }
                            onDismiss()
                        }
                    )
                )
            }
        }

        // 4. Study Notes & SM-2 Flashcards
        if (activeFilter == GlobalSearchFilter.ALL || activeFilter == GlobalSearchFilter.NOTES) {
            studyNotes.filter { n ->
                matches(n.title, n.content, n.courseCode, n.category)
            }.take(if (q.isEmpty()) 2 else 15).forEach { note ->
                list.add(
                    GlobalSearchResultItem(
                        key = "note_${note.id}",
                        category = GlobalSearchFilter.NOTES,
                        badgeText = "${note.courseCode} • NOTE",
                        title = note.title,
                        subtitle = note.content.replace('\n', ' ').take(90),
                        icon = Icons.Default.Description,
                        accentColor = auraColors.secondaryAccent,
                        onClick = {
                            onOpenStudyTab(StudyTabs.NOTES_VAULT, note.courseCode)
                            onDismiss()
                        }
                    )
                )
            }

            flashcards.filter { f ->
                matches(f.question, f.answer, f.courseCode, f.deckTitle)
            }.take(if (q.isEmpty()) 2 else 15).forEach { card ->
                list.add(
                    GlobalSearchResultItem(
                        key = "card_${card.id}",
                        category = GlobalSearchFilter.NOTES,
                        badgeText = "${card.courseCode} • FLASHCARD",
                        title = card.question,
                        subtitle = card.answer.replace('\n', ' ').take(90),
                        icon = Icons.Default.Style,
                        accentColor = AuraVioletAccent,
                        onClick = {
                            onOpenStudyTab(StudyTabs.FLASHCARDS, card.courseCode)
                            onDismiss()
                        }
                    )
                )
            }
        }

        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x455CE1FF))),
                    RoundedCornerShape(28.dp)
                )
                .testTag("global_search_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF50B1528)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(auraColors.primaryAccent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = auraColors.secondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AURA Campus Spotlight",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "${resources.size} slides • ${lectures.size} classes • ${assessments.size} tasks • ${flashcards.size} cards",
                            fontSize = 11.sp,
                            color = auraColors.secondaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Search",
                        onClick = onDismiss,
                        size = 44.dp,
                        tint = auraColors.textPrimary,
                        testTag = "close_global_search_button"
                    )
                }

                // Instant Search Input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Search slides, PDFs, courses, venues, exams, notes...",
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = auraColors.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Query",
                                    tint = auraColors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = auraColors.secondaryAccent,
                        unfocusedBorderColor = Color(0x38FFFFFF),
                        focusedContainerColor = Color(0x22FFFFFF),
                        unfocusedContainerColor = Color(0x14FFFFFF),
                        focusedTextColor = auraColors.textPrimary,
                        unfocusedTextColor = auraColors.textPrimary
                    )
                )

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(GlobalSearchFilter.entries) { filter ->
                        val selected = activeFilter == filter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) auraColors.primaryAccent else Color(0x1AFFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (selected) auraColors.secondaryAccent else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeFilter = filter }
                                .testTag("global_search_filter_${filter.name}")
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) Color.White else auraColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Quick Command Launchpad Strip when query is empty
                if (trimmedQuery.isEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickCommands = listOf(
                            Triple("Slide Hub", Icons.Default.Slideshow, auraColors.secondaryAccent) to {
                                onOpenStudyTab(StudyTabs.COURSE_SLIDES, null)
                                onDismiss()
                            },
                            Triple("Timetable", Icons.Default.CalendarToday, AuraMintAccent) to {
                                onOpenTimetableDay(LocalDate.now().dayOfWeek.value)
                                onDismiss()
                            },
                            Triple("Focus Timer", Icons.Default.Timer, AuraVioletAccent) to {
                                onOpenStudyTab(StudyTabs.FOCUS, null)
                                onDismiss()
                            },
                            Triple("Courses", Icons.Default.School, auraColors.primaryAccent) to {
                                onOpenManageCourses()
                                onDismiss()
                            },
                            Triple("Ask AURA AI", Icons.Default.AutoAwesome, auraColors.aiAccent) to {
                                onOpenAiAssistant()
                                onDismiss()
                            }
                        )
                        items(quickCommands) { (triple, action) ->
                            val (label, icon, tint) = triple
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = tint.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = action)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = auraColors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Results List
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = auraColors.secondaryAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (trimmedQuery.isEmpty()) "Start typing to search everything" else "No matches for \"$trimmedQuery\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = auraColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Search across uploaded PDFs & slides, scheduled lectures, enrolled courses, assignments, notes, and flashcards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = auraColors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("global_search_results_list"),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults, key = { it.key }) { item ->
                            AuraCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = item.onClick)
                                    .testTag("search_result_${item.key}"),
                                cornerRadius = 18.dp,
                                borderColor = item.accentColor.copy(alpha = 0.40f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(item.accentColor.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        AuraBadge(
                                            text = item.badgeText,
                                            backgroundColor = item.accentColor.copy(alpha = 0.18f),
                                            textColor = item.accentColor
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = auraColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.subtitle.isNotBlank()) {
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = auraColors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = auraColors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
