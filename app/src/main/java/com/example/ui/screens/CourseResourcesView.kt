package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseResourceEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.StatCard
import com.example.ui.dialogs.ResourceReaderDialog
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import com.example.util.SlideDocumentParser

@Composable
fun CourseResourcesView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val resources by viewModel.courseResources.collectAsState()
    val selectedCourseFilter by viewModel.selectedResourceCourseFilter.collectAsState()
    val isImporting by viewModel.isImportingResource.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var sortMode by remember { mutableStateOf("RECENT") } // RECENT, PROGRESS, TITLE, SIZE
    var searchQuery by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Pending URI chosen from Android system picker -> opens Import Organizer dialog
    var pendingPickedUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateTextDeckDialog by remember { mutableStateOf(false) }
    var resourceBeingEdited by remember { mutableStateOf<CourseResourceEntity?>(null) }
    var resourcePendingDelete by remember { mutableStateOf<CourseResourceEntity?>(null) }
    var activeReaderResource by remember { mutableStateOf<CourseResourceEntity?>(null) }

    val fallbackGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingPickedUri = uri
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingPickedUri = uri
        }
    }

    val launchSafeSlidePicker: () -> Unit = {
        try {
            openDocumentLauncher.launch(arrayOf("*/*"))
        } catch (_: Throwable) {
            try {
                fallbackGetContentLauncher.launch("*/*")
            } catch (_: Throwable) {
                statusMessage = "No system file picker available on this device. Use 'Write Slides' to create a deck."
            }
        }
    }

    val filteredResources = remember(resources, selectedCourseFilter, selectedTypeFilter, searchQuery, sortMode) {
        val base = resources.filter { res ->
            val matchesCourse = selectedCourseFilter == "ALL" ||
                res.courseCode.equals(selectedCourseFilter, ignoreCase = true)
            val matchesType = when (selectedTypeFilter) {
                "ALL" -> true
                "PDF" -> res.fileType == "PDF"
                "PPTX" -> res.fileType == "PPTX" || res.fileType == "PPT"
                "DOCX" -> res.fileType == "DOCX" || res.fileType == "DOC"
                "NOTES" -> res.fileType == "TXT" || res.fileType == "IMAGE" || res.fileType == "OTHER"
                "BOOKMARKED" -> res.bookmarkedPagesCsv.isNotBlank() || res.isPinned
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                res.title.contains(searchQuery, ignoreCase = true) ||
                    res.courseCode.contains(searchQuery, ignoreCase = true) ||
                    res.moduleFolder.contains(searchQuery, ignoreCase = true) ||
                    res.extractedText.contains(searchQuery, ignoreCase = true) ||
                    res.personalNotes.contains(searchQuery, ignoreCase = true)
            }
            matchesCourse && matchesType && matchesSearch
        }
        when (sortMode) {
            "PROGRESS" -> base.sortedByDescending {
                if (it.isCompleted) 1f else (it.lastReadPage + 1).toFloat() / it.totalPages.coerceAtLeast(1).toFloat()
            }
            "TITLE" -> base.sortedBy { it.title.lowercase() }
            "SIZE" -> base.sortedByDescending { it.fileSizeBytes }
            else -> base.sortedWith(compareByDescending<CourseResourceEntity> { it.isPinned }.thenByDescending { it.lastOpenedEpochMillis })
        }
    }

    val recentDecks = remember(resources) {
        resources
            .filter { it.lastReadPage > 0 && !it.isCompleted }
            .sortedByDescending { it.lastOpenedEpochMillis }
            .take(4)
    }

    val groupedByFolder = remember(filteredResources) {
        filteredResources.groupBy { it.moduleFolder.ifBlank { "Lecture Slides" } }
    }

    val masteredCount = remember(resources) { resources.count { it.isCompleted } }
    val totalPagesAcrossDecks = remember(resources) { resources.sumOf { it.totalPages } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("course_resources_hub_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Action Card (Upload PDF / Word / PowerPoint / Others + Create Slide Deck)
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                accentGlow = LocalAuraColors.current.secondaryAccent
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.22f)
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.26f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.secondaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Course Slides & Resource Hub",
                                    fontFamily = OutfitFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "PDF, PowerPoint (.pptx), Word (.docx), Text & Slide Images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.secondaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Import lecture slides and handouts, organize them by course and week, study with Read-Aloud & Bookmarks, or synthesize 6 SM-2 Flashcards + 10-Question Quizzes in 1 tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            lineHeight = 18.sp
                        )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AuraGlassButton(
                            text = if (isImporting) "Importing..." else "Import Slide / File",
                            icon = Icons.Default.UploadFile,
                            isPrimary = true,
                            enabled = !isImporting,
                            onClick = launchSafeSlidePicker,
                            modifier = Modifier.weight(1f),
                            testTag = "upload_course_slide_button"
                        )

                        AuraGlassButton(
                            text = "Write Slides",
                            icon = Icons.Default.Add,
                            enabled = !isImporting,
                            onClick = { showCreateTextDeckDialog = true },
                            testTag = "create_text_slides_button"
                        )
                    }

                    if (isImporting) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.20f))
                                .border(1.dp, LocalAuraColors.current.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("importing_resource_banner"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LocalAuraColors.current.secondaryAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Copying & indexing slide deck safely offline...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAuraColors.current.textPrimary
                            )
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalAuraColors.current.success.copy(alpha = 0.16f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LocalAuraColors.current.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "OK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.clickable { statusMessage = null }
                            )
                        }
                    }
                }
                }
            }
        }

        // 2. Mastery & Library Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Slide Decks",
                    value = "${resources.size}",
                    subtitle = "$totalPagesAcrossDecks total slides/pages",
                    icon = Icons.Default.Slideshow,
                    accentColor = LocalAuraColors.current.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Mastered",
                    value = "$masteredCount/${resources.size}",
                    subtitle = if (resources.isEmpty()) "Add your first deck" else "${((masteredCount * 100f) / resources.size).toInt()}% completed",
                    icon = Icons.Default.CheckCircle,
                    accentColor = LocalAuraColors.current.success,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2b. Quick Resume Carousel for Recently Studied Slide Decks
        if (recentDecks.isNotEmpty()) {
            item(key = "quick_resume_carousel") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Continue Studying",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_slides_resume_strip"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentDecks, key = { "resume_${it.id}" }) { deck ->
                            AuraCard(
                                modifier = Modifier.width(210.dp),
                                cornerRadius = 18.dp,
                                onClick = { activeReaderResource = deck }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AuraBadge(
                                            text = deck.courseCode,
                                            backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                                            textColor = LocalAuraColors.current.secondaryAccent
                                        )
                                        Text(
                                            text = deck.fileType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalAuraColors.current.textMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = deck.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (deck.isCompleted) "Mastered ✓" else "Resume Slide ${deck.lastReadPage + 1}/${deck.totalPages}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (deck.isCompleted) LocalAuraColors.current.success else LocalAuraColors.current.secondaryAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Full-Text Deep Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent
                    )
                },
                placeholder = { Text("Search slide titles, weeks, or inside PDF/PPTX/DOCX text...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_course_resources_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                    unfocusedBorderColor = Color(0x35FFFFFF),
                    focusedTextColor = LocalAuraColors.current.textPrimary,
                    unfocusedTextColor = LocalAuraColors.current.textPrimary
                )
            )
        }

        // 4. Course & Format Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipPill(
                            label = "All Courses",
                            selected = selectedCourseFilter == "ALL",
                            onClick = { viewModel.setResourceCourseFilter("ALL") },
                            testTag = "resource_course_filter_all"
                        )
                    }
                    items(courses, key = { it.id }) { course ->
                        FilterChipPill(
                            label = course.code,
                            selected = selectedCourseFilter.equals(course.code, ignoreCase = true),
                            onClick = { viewModel.setResourceCourseFilter(course.code) },
                            testTag = "resource_course_filter_${course.code}"
                        )
                    }
                }

                val typeFilters = listOf(
                    "ALL" to "All Formats",
                    "PDF" to "PDF",
                    "PPTX" to "PowerPoint",
                    "DOCX" to "Word",
                    "NOTES" to "Text & Images",
                    "BOOKMARKED" to "Pinned / Starred"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(typeFilters) { (code, label) ->
                        FilterChipPill(
                            label = label,
                            selected = selectedTypeFilter == code,
                            onClick = { selectedTypeFilter = code },
                            testTag = "resource_type_filter_$code"
                        )
                    }
                }

                val sortOptions = listOf(
                    "RECENT" to "Sort: Recent",
                    "PROGRESS" to "Sort: Progress %",
                    "TITLE" to "Sort: A–Z",
                    "SIZE" to "Sort: File Size"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortOptions) { (code, label) ->
                        FilterChipPill(
                            label = label,
                            selected = sortMode == code,
                            onClick = { sortMode = code },
                            testTag = "resource_sort_$code"
                        )
                    }
                }
            }
        }

        // 5. Organized Folder Sections & Resource Cards
        if (filteredResources.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (resources.isEmpty()) "No Course Slides Added Yet" else "No Matching Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (resources.isEmpty()) {
                                "Tap 'Import Slide / File' above to add PDFs, PowerPoint (.pptx), Word (.docx), or study handouts, or tap 'Write Slides' to create a deck directly."
                            } else {
                                "Try clearing your search query or switching back to 'All Courses' / 'All Formats'."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedByFolder.forEach { (folderName, folderItems) ->
                item(key = "folder_header_$folderName") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AuraBadge(
                            text = "${folderItems.size}",
                            backgroundColor = Color(0x22FFFFFF),
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                }

                items(folderItems, key = { it.id }) { res ->
                    CourseResourceItemCard(
                        resource = res,
                        activeSearchQuery = searchQuery,
                        onOpen = { activeReaderResource = res },
                        onSynthesizePack = {
                            viewModel.synthesizeStudyPackFromResource(res) { summary ->
                                statusMessage = summary
                            }
                        },
                        onTogglePin = { viewModel.togglePinCourseResource(res) },
                        onToggleMastered = { viewModel.toggleMasteredCourseResource(res) },
                        onEdit = { resourceBeingEdited = res },
                        onDelete = { resourcePendingDelete = res }
                    )
                }
            }
        }
    }

    // Dialog 1: Import Organizer after selecting a file from device storage
    val pickedUri = pendingPickedUri
    if (pickedUri != null) {
        ResourceMetadataOrganizerDialog(
            dialogTitle = "Organize Imported Slide / Document",
            courses = courses,
            initialTitle = "",
            initialFolder = "Week 1",
            initialCourseCode = if (selectedCourseFilter != "ALL") selectedCourseFilter else (courses.firstOrNull()?.code ?: "GENERAL"),
            confirmButtonLabel = "Save & Import",
            onDismiss = { pendingPickedUri = null },
            onConfirm = { cId, cCode, customTitle, folder ->
                val uriToImport = pickedUri
                pendingPickedUri = null
                viewModel.importCourseResourceFromUri(
                    uri = uriToImport,
                    courseId = cId,
                    courseCode = cCode,
                    customTitle = customTitle,
                    moduleFolder = folder
                ) { _, message ->
                    statusMessage = message
                }
            }
        )
    }

    // Dialog 2: Author / Paste Custom Multi-Slide Study Deck
    if (showCreateTextDeckDialog) {
        CreateTextSlideDeckDialog(
            courses = courses,
            defaultCourseCode = if (selectedCourseFilter != "ALL") selectedCourseFilter else (courses.firstOrNull()?.code ?: "GENERAL"),
            onDismiss = { showCreateTextDeckDialog = false },
            onCreate = { cId, cCode, title, folder, body ->
                viewModel.createCustomTextSlideDeck(
                    courseId = cId,
                    courseCode = cCode,
                    title = title,
                    moduleFolder = folder,
                    rawSlideContent = body
                ) {
                    statusMessage = "Created slide deck '$title' under $folder!"
                }
                showCreateTextDeckDialog = false
            }
        )
    }

    // Dialog 3: Edit Resource Title / Course / Module Folder
    val editingRes = resourceBeingEdited
    if (editingRes != null) {
        ResourceMetadataOrganizerDialog(
            dialogTitle = "Edit Slide / Resource Details",
            courses = courses,
            initialTitle = editingRes.title,
            initialFolder = editingRes.moduleFolder,
            initialCourseCode = editingRes.courseCode,
            confirmButtonLabel = "Save Changes",
            onDismiss = { resourceBeingEdited = null },
            onConfirm = { cId, cCode, newTitle, newFolder ->
                viewModel.updateCourseResourceMetadata(
                    resource = editingRes,
                    newTitle = newTitle,
                    newModuleFolder = newFolder,
                    newCourseId = cId,
                    newCourseCode = cCode
                )
                resourceBeingEdited = null
            }
        )
    }

    // Dialog 4: Confirm Removing / Deleting Added Resource
    val deletingRes = resourcePendingDelete
    if (deletingRes != null) {
        AlertDialog(
            onDismissRequest = { resourcePendingDelete = null },
            title = {
                Text(
                    text = "Remove Resource?",
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
            },
            text = {
                Text(
                    text = "Remove '${deletingRes.title}' (${deletingRes.fileType}) from ${deletingRes.courseCode}? Its offline copy in AURA will be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAuraColors.current.textSecondary
                )
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Remove Resource",
                    isPrimary = true,
                    containerColor = LocalAuraColors.current.danger,
                    onClick = {
                        viewModel.deleteCourseResource(deletingRes)
                        resourcePendingDelete = null
                        statusMessage = "Removed '${deletingRes.title}'."
                    },
                    testTag = "confirm_delete_resource_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { resourcePendingDelete = null },
                    testTag = "cancel_delete_resource_button"
                )
            },
            containerColor = Color(0xF2101B30),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x33FF453A))),
                    RoundedCornerShape(24.dp)
                )
                .testTag("confirm_delete_resource_dialog")
        )
    }

    // Full-Screen Slide & Document Learning Reader
    val openRes = activeReaderResource
    if (openRes != null) {
        ResourceReaderDialog(
            resourceId = openRes.id,
            fallbackResource = openRes,
            viewModel = viewModel,
            onDismiss = { activeReaderResource = null }
        )
    }
}

@Composable
private fun CourseResourceItemCard(
    resource: CourseResourceEntity,
    activeSearchQuery: String,
    onOpen: () -> Unit,
    onSynthesizePack: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMastered: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (typeIcon, typeAccent) = remember(resource.fileType) {
        when (resource.fileType) {
            "PDF" -> Icons.Default.PictureAsPdf to Color(0xFF38BDF8)
            "PPTX", "PPT" -> Icons.Default.Slideshow to Color(0xFFF59E0B)
            "DOCX", "DOC" -> Icons.Default.Description to Color(0xFF60A5FA)
            "IMAGE" -> Icons.Default.Image to Color(0xFF2DD4BF)
            else -> Icons.AutoMirrored.Filled.MenuBook to Color(0xFFA78BFA)
        }
    }

    val readFraction = remember(resource.lastReadPage, resource.totalPages, resource.isCompleted) {
        if (resource.isCompleted) 1f
        else if (resource.totalPages <= 1) 0f
        else ((resource.lastReadPage + 1).toFloat() / resource.totalPages.toFloat()).coerceIn(0f, 1f)
    }

    val bookmarkCount = remember(resource.bookmarkedPagesCsv) {
        resource.bookmarkedPagesCsv.split(",").count { it.trim().toIntOrNull() != null }
    }

    val matchingSnippet = remember(activeSearchQuery, resource.extractedText) {
        if (activeSearchQuery.length < 2) null
        else {
            val plain = SlideDocumentParser.plainSearchableText(resource.extractedText)
            val idx = plain.indexOf(activeSearchQuery, ignoreCase = true)
            if (idx >= 0) {
                val start = (idx - 35).coerceAtLeast(0)
                val end = (idx + activeSearchQuery.length + 55).coerceAtMost(plain.length)
                "…" + plain.substring(start, end).replace("\n", " ").trim() + "…"
            } else null
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_resource_card_${resource.id}"),
        cornerRadius = 20.dp,
        onClick = onOpen
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeAccent.copy(alpha = 0.2f))
                        .border(1.dp, typeAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = resource.fileType,
                        tint = typeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuraBadge(
                            text = resource.courseCode,
                            backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AuraBadge(
                            text = resource.fileType,
                            backgroundColor = typeAccent.copy(alpha = 0.2f),
                            textColor = typeAccent
                        )
                        if (bookmarkCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarked slides",
                                tint = LocalAuraColors.current.warning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AuraGlassIconButton(
                    icon = Icons.Default.PushPin,
                    contentDescription = if (resource.isPinned) "Unpin Slide Deck" else "Pin Slide Deck",
                    onClick = onTogglePin,
                    size = 44.dp,
                    iconSize = 17.dp,
                    tint = if (resource.isPinned) LocalAuraColors.current.warning else LocalAuraColors.current.textMuted,
                    testTag = "pin_resource_${resource.id}"
                )
            }

            if (matchingSnippet != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Match in slides: $matchingSnippet",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.warning,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reading Progress Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (resource.isCompleted) {
                        "Mastered • ${resource.totalPages} slide/page(s)"
                    } else {
                        "Slide ${resource.lastReadPage + 1} of ${resource.totalPages} • ${SlideDocumentParser.formatFileSize(resource.fileSizeBytes)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (resource.isCompleted) LocalAuraColors.current.success else LocalAuraColors.current.textSecondary
                )
                Text(
                    text = "${(readFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.secondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { readFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (resource.isCompleted) LocalAuraColors.current.success else LocalAuraColors.current.primaryAccent,
                trackColor = Color(0x22FFFFFF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Open & Learn | AI 10-Q | Mastered | Edit | Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AuraGlassButton(
                    text = "Open & Learn",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isPrimary = true,
                    onClick = onOpen,
                    cornerRadius = 14.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "open_resource_${resource.id}"
                )

                AuraGlassIconButton(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "Generate AI 10-Question Study Pack",
                    onClick = onSynthesizePack,
                    size = 48.dp,
                    iconSize = 18.dp,
                    tint = LocalAuraColors.current.aiAccent,
                    testTag = "ai_pack_resource_${resource.id}"
                )

                AuraGlassIconButton(
                    icon = Icons.Default.CheckCircle,
                    contentDescription = "Toggle Mastered",
                    onClick = onToggleMastered,
                    size = 48.dp,
                    iconSize = 18.dp,
                    tint = if (resource.isCompleted) LocalAuraColors.current.success else LocalAuraColors.current.textMuted,
                    testTag = "master_resource_${resource.id}"
                )

                AuraGlassIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit Resource Folder or Title",
                    onClick = onEdit,
                    size = 48.dp,
                    iconSize = 18.dp,
                    tint = LocalAuraColors.current.secondaryAccent,
                    testTag = "edit_resource_${resource.id}"
                )

                AuraGlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Remove Resource",
                    onClick = onDelete,
                    size = 48.dp,
                    iconSize = 18.dp,
                    tint = LocalAuraColors.current.danger,
                    testTag = "delete_resource_${resource.id}"
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) LocalAuraColors.current.primaryAccent.copy(alpha = 0.34f) else Color(0x1EFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) LocalAuraColors.current.secondaryAccent else Color(0x38FFFFFF)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else LocalAuraColors.current.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ResourceMetadataOrganizerDialog(
    dialogTitle: String,
    courses: List<CourseEntity>,
    initialTitle: String,
    initialFolder: String,
    initialCourseCode: String,
    confirmButtonLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Long, courseCode: String, title: String, folder: String) -> Unit
) {
    val matchedCourse = courses.find { it.code.equals(initialCourseCode, ignoreCase = true) } ?: courses.firstOrNull()
    var selectedCourseId by remember { mutableLongStateOf(matchedCourse?.id ?: 0L) }
    var courseCodeText by remember { mutableStateOf(matchedCourse?.code ?: initialCourseCode.ifBlank { "CSCD 301" }) }
    var titleText by remember { mutableStateOf(initialTitle) }
    var folderText by remember { mutableStateOf(initialFolder) }

    val quickFolders = listOf(
        "Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6",
        "Lecture Slides", "Handouts", "Past Papers", "Lab Manuals"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (courses.isNotEmpty()) {
                    Text(
                        text = "Assign to Course",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAuraColors.current.textSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(courses, key = { it.id }) { c ->
                            val isSel = courseCodeText.equals(c.code, ignoreCase = true)
                            FilterChipPill(
                                label = c.code,
                                selected = isSel,
                                onClick = {
                                    selectedCourseId = c.id
                                    courseCodeText = c.code
                                },
                                testTag = "organizer_course_${c.code}"
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = courseCodeText,
                        onValueChange = { courseCodeText = it },
                        label = { Text("Course Code (e.g. CSCD 301)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Slide / Resource Title (leave blank for file name)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("organizer_title_input")
                )

                Text(
                    text = "Module / Week Folder",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAuraColors.current.textSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickFolders) { f ->
                        FilterChipPill(
                            label = f,
                            selected = folderText.equals(f, ignoreCase = true),
                            onClick = { folderText = f },
                            testTag = "organizer_folder_$f"
                        )
                    }
                }

                OutlinedTextField(
                    value = folderText,
                    onValueChange = { folderText = it },
                    label = { Text("Custom Folder / Week Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("organizer_folder_input")
                )
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = confirmButtonLabel,
                isPrimary = true,
                onClick = {
                    onConfirm(selectedCourseId, courseCodeText, titleText, folderText)
                },
                testTag = "organizer_confirm_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "organizer_cancel_button"
            )
        },
        containerColor = Color(0xF2101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
            RoundedCornerShape(24.dp)
        )
    )
}

@Composable
private fun CreateTextSlideDeckDialog(
    courses: List<CourseEntity>,
    defaultCourseCode: String,
    onDismiss: () -> Unit,
    onCreate: (courseId: Long, courseCode: String, title: String, folder: String, rawContent: String) -> Unit
) {
    val initialCourse = courses.find { it.code.equals(defaultCourseCode, ignoreCase = true) } ?: courses.firstOrNull()
    var selectedCourseId by remember { mutableLongStateOf(initialCourse?.id ?: 0L) }
    var courseCode by remember { mutableStateOf(initialCourse?.code ?: defaultCourseCode.ifBlank { "CSCD 301" }) }
    var title by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("Week 1") }
    var content by remember {
        mutableStateOf(
            "# Slide 1: Core Definitions\n• Key theorem or concept\n• Important assumptions\n---\n# Slide 2: Worked Example & Formulas\n• Step 1: Identify variables\n• Exam takeaway: Verify units"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Study Slide Deck",
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (courses.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(courses, key = { it.id }) { c ->
                            FilterChipPill(
                                label = c.code,
                                selected = courseCode.equals(c.code, ignoreCase = true),
                                onClick = {
                                    selectedCourseId = c.id
                                    courseCode = c.code
                                },
                                testTag = "text_deck_course_${c.code}"
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("text_deck_course_input")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title (e.g. Week 3 Process Scheduling)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("text_deck_title_input")
                )

                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Folder / Week (e.g. Week 3, Lecture Slides)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("text_deck_folder_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Slides (separate slides with --- or # Heading)") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("text_deck_body_input")
                )
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save Slide Deck",
                isPrimary = true,
                onClick = {
                    if (content.isNotBlank()) {
                        onCreate(
                            selectedCourseId,
                            courseCode,
                            title.ifBlank { "$courseCode Study Slides" },
                            folder,
                            content
                        )
                    }
                },
                testTag = "save_text_deck_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_text_deck_button"
            )
        },
        containerColor = Color(0xF2101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
            RoundedCornerShape(24.dp)
        )
    )
}
