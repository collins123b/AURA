package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CourseResourceEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.theme.LocalAuraColors
import com.example.util.MediaStorageHelper
import com.example.util.SlideDeckPage
import com.example.util.SlideDocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ResourceReaderDialog(
    resourceId: Long,
    fallbackResource: CourseResourceEntity,
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allResources by viewModel.courseResources.collectAsState()
    val isSynthesizing by viewModel.isSynthesizingResource.collectAsState()

    val resource = remember(allResources, resourceId, fallbackResource) {
        allResources.find { it.id == resourceId } ?: fallbackResource
    }

    val pages: List<SlideDeckPage> by produceState(
        initialValue = remember(resource.extractedText, resource.title) {
            SlideDocumentParser.deserializePages(resource.extractedText, resource.title)
        },
        key1 = resource.localFilePath,
        key2 = resource.extractedText
    ) {
        value = withContext(Dispatchers.IO) {
            SlideDocumentParser.loadPagesForResource(resource)
        }
    }

    val effectiveTotalPages = maxOf(resource.totalPages, pages.size, 1)
    var currentPageIndex by remember(resource.id) {
        mutableIntStateOf(resource.lastReadPage.coerceIn(0, effectiveTotalPages - 1))
    }

    var isOutlineMode by remember { mutableStateOf(false) }
    var showPdfExtractedText by remember { mutableStateOf(false) }
    var readerCanvasTheme by remember { mutableIntStateOf(0) } // 0 = Aurora Glass, 1 = Warm Sepia, 2 = Crisp Paper
    var isZoomedIn by remember { mutableStateOf(false) }
    var pinchScale by remember(currentPageIndex) { mutableFloatStateOf(1f) }
    var panOffsetX by remember(currentPageIndex) { mutableFloatStateOf(0f) }
    var panOffsetY by remember(currentPageIndex) { mutableFloatStateOf(0f) }
    var fontSizeSp by remember { mutableFloatStateOf(15f) }
    var inDocQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var showNotesPanel by remember { mutableStateOf(false) }
    var personalNotesDraft by remember(resource.id, resource.personalNotes) {
        mutableStateOf(resource.personalNotes)
    }
    var alsoPinNotesToVault by remember { mutableStateOf(true) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Text-to-Speech (Read Aloud) support
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        runCatching {
            instance = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching { instance?.language = Locale.US }
                    isTtsReady = true
                }
            }
            ttsEngine = instance
        }
        onDispose {
            runCatching {
                instance?.stop()
                instance?.shutdown()
            }
        }
    }

    // Persist reading position whenever user flips slides
    LaunchedEffect(currentPageIndex) {
        viewModel.updateResourceReadingProgress(resource, currentPageIndex)
    }

    // Load visual bitmap for PDF or IMAGE slides on IO thread with downsampled OOM-safe decoding
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(resource.localFilePath, resource.fileType, currentPageIndex, isZoomedIn) {
        renderedBitmap = withContext(Dispatchers.IO) {
            val file = File(resource.localFilePath)
            when {
                resource.fileType == "PDF" && file.exists() -> {
                    SlideDocumentParser.renderPdfPageBitmap(
                        file = file,
                        pageIndex = currentPageIndex,
                        targetWidthPx = if (isZoomedIn) 1680 else 1080
                    )
                }
                resource.fileType == "IMAGE" && file.exists() -> {
                    MediaStorageHelper.loadBitmapSafe(
                        path = file.absolutePath,
                        maxDimension = if (isZoomedIn) 1800 else 1280
                    )
                }
                else -> null
            }
        }
    }

    val bookmarkedPages = remember(resource.bookmarkedPagesCsv) {
        resource.bookmarkedPagesCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sorted()
    }
    val isCurrentBookmarked = bookmarkedPages.contains(currentPageIndex)
    val currentSlide = pages.getOrNull(currentPageIndex) ?: SlideDeckPage(
        pageNumber = currentPageIndex + 1,
        title = "${resource.title} • Page ${currentPageIndex + 1}",
        body = ""
    )

    Dialog(
        onDismissRequest = {
            runCatching { ttsEngine?.stop() }
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        FrostedGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .testTag("resource_reader_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Top Reader Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Slide Reader",
                        onClick = {
                            runCatching { ttsEngine?.stop() }
                            onDismiss()
                        },
                        size = 48.dp,
                        tint = LocalAuraColors.current.textPrimary,
                        testTag = "reader_close_button"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AuraBadge(
                                text = resource.courseCode,
                                backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.25f),
                                textColor = LocalAuraColors.current.secondaryAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AuraBadge(
                                text = "${resource.fileType} • ${resource.moduleFolder}",
                                backgroundColor = Color(0x1FFFFFFF),
                                textColor = LocalAuraColors.current.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bookmark Current Slide
                    AuraGlassIconButton(
                        icon = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Slide",
                        onClick = { viewModel.toggleResourceBookmark(resource, currentPageIndex) },
                        size = 48.dp,
                        tint = if (isCurrentBookmarked) LocalAuraColors.current.warning else LocalAuraColors.current.textPrimary,
                        testTag = "reader_bookmark_button"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Read Aloud (TTS)
                    AuraGlassIconButton(
                        icon = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) "Stop Reading Aloud" else "Read Slide Aloud",
                        onClick = {
                            if (isSpeaking) {
                                runCatching { ttsEngine?.stop() }
                                isSpeaking = false
                            } else {
                                val textToSpeak = "${currentSlide.title}. ${currentSlide.body}"
                                if (isTtsReady && ttsEngine != null) {
                                    runCatching {
                                        ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "slide_tts")
                                        isSpeaking = true
                                    }
                                } else {
                                    statusBanner = "Voice engine warming up or unavailable on this device."
                                }
                            }
                        },
                        size = 48.dp,
                        tint = if (isSpeaking) LocalAuraColors.current.warning else LocalAuraColors.current.secondaryAccent,
                        testTag = "reader_tts_button"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Open in External Viewer
                    AuraGlassIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in External Viewer",
                        onClick = {
                            val opened = SlideDocumentParser.openResourceExternally(context, resource)
                            if (!opened) {
                                statusBanner = "No external app found for ${resource.fileType}; viewing directly inside AURA."
                            }
                        },
                        size = 48.dp,
                        tint = LocalAuraColors.current.textPrimary,
                        testTag = "reader_open_external_button"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mode & Study Controls Strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AuraGlassButton(
                            text = if (isOutlineMode) "Slide View" else "Outline (${pages.size})",
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            isPrimary = isOutlineMode,
                            onClick = { isOutlineMode = !isOutlineMode },
                            cornerRadius = 14.dp,
                            testTag = "reader_toggle_outline_button"
                        )
                    }

                    if (resource.fileType == "PDF" || resource.fileType == "IMAGE") {
                        item {
                            AuraGlassButton(
                                text = if (isZoomedIn || pinchScale > 1.05f) "1.0× Reset" else "1.8× Zoom",
                                icon = Icons.Default.ZoomIn,
                                isPrimary = isZoomedIn || pinchScale > 1.05f,
                                onClick = {
                                    if (isZoomedIn || pinchScale > 1.05f) {
                                        isZoomedIn = false
                                        pinchScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    } else {
                                        isZoomedIn = true
                                        pinchScale = 1.8f
                                    }
                                },
                                cornerRadius = 14.dp,
                                testTag = "reader_zoom_button"
                            )
                        }
                    }

                    if (resource.fileType == "PDF") {
                        item {
                            AuraGlassButton(
                                text = if (showPdfExtractedText) "Visual PDF" else "Extracted Text",
                                isPrimary = showPdfExtractedText,
                                onClick = { showPdfExtractedText = !showPdfExtractedText },
                                cornerRadius = 14.dp,
                                testTag = "reader_pdf_text_mode_button"
                            )
                        }
                    }

                    item {
                        AuraGlassButton(
                            text = if (showNotesPanel) "Hide Notes" else "My Takeaways",
                            icon = Icons.Default.EditNote,
                            isPrimary = showNotesPanel,
                            onClick = { showNotesPanel = !showNotesPanel },
                            cornerRadius = 14.dp,
                            testTag = "reader_notes_toggle"
                        )
                    }

                    item {
                        AuraGlassButton(
                            text = if (isSynthesizing) "Synthesizing..." else "AI 10-Q Pack",
                            icon = Icons.Default.AutoAwesome,
                            isPrimary = true,
                            onClick = {
                                if (!isSynthesizing) {
                                    viewModel.synthesizeStudyPackFromResource(resource) { msg ->
                                        statusBanner = msg
                                    }
                                }
                            },
                            cornerRadius = 14.dp,
                            testTag = "reader_ai_synthesize_button"
                        )
                    }

                    item {
                        AuraGlassButton(
                            text = "A+",
                            onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(24f) },
                            cornerRadius = 14.dp,
                            testTag = "reader_font_increase"
                        )
                    }

                    item {
                        AuraGlassButton(
                            text = "A-",
                            onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(12f) },
                            cornerRadius = 14.dp,
                            testTag = "reader_font_decrease"
                        )
                    }

                    item {
                        AuraGlassButton(
                            text = "+ Flashcard",
                            icon = Icons.Default.Style,
                            onClick = {
                                viewModel.createFlashcardFromSlidePage(
                                    courseCode = resource.courseCode,
                                    deckTitle = resource.title,
                                    slideTitle = currentSlide.title,
                                    slideBody = currentSlide.body
                                ) {
                                    statusBanner = "Saved Slide ${currentPageIndex + 1} to ${resource.courseCode} SM-2 Flashcards!"
                                }
                            },
                            cornerRadius = 14.dp,
                            testTag = "reader_quick_flashcard_button"
                        )
                    }

                    item {
                        val modeLabel = when (readerCanvasTheme) {
                            1 -> "Sepia Tint"
                            2 -> "Crisp Paper"
                            else -> "Aurora Glass"
                        }
                        AuraGlassButton(
                            text = modeLabel,
                            icon = Icons.Default.Palette,
                            isPrimary = readerCanvasTheme != 0,
                            onClick = { readerCanvasTheme = (readerCanvasTheme + 1) % 3 },
                            cornerRadius = 14.dp,
                            testTag = "reader_canvas_theme_button"
                        )
                    }

                    item {
                        AuraGlassButton(
                            text = if (showSearchField) "Close Find" else "Find in Deck",
                            isPrimary = showSearchField,
                            onClick = { showSearchField = !showSearchField },
                            cornerRadius = 14.dp,
                            testTag = "reader_find_toggle"
                        )
                    }
                }

                // Status / AI Synthesis Banner
                if (statusBanner != null || isSynthesizing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSynthesizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalAuraColors.current.aiAccent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Building Summary Note, Flashcards & Practice Quiz from '${resource.title}'...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Dismiss",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.secondaryAccent,
                                    modifier = Modifier
                                        .clickable { statusBanner = null }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // In-Deck Find Input
                if (showSearchField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inDocQuery,
                        onValueChange = { q ->
                            inDocQuery = q
                            if (q.length >= 2) {
                                val matchIdx = pages.indexOfFirst {
                                    it.title.contains(q, ignoreCase = true) ||
                                        it.body.contains(q, ignoreCase = true) ||
                                        it.speakerNotes.contains(q, ignoreCase = true)
                                }
                                if (matchIdx >= 0) currentPageIndex = matchIdx
                            }
                        },
                        placeholder = { Text("Type keyword or formula to jump & highlight...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_find_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                }

                // Bookmarked Slides Quick-Jump Bar
                if (bookmarkedPages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "Bookmarks:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAuraColors.current.warning
                            )
                        }
                        items(bookmarkedPages) { bIdx ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (bIdx == currentPageIndex) LocalAuraColors.current.warning.copy(alpha = 0.28f) else Color(0x1EFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    LocalAuraColors.current.warning.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.clickable {
                                    isOutlineMode = false
                                    currentPageIndex = bIdx.coerceIn(0, effectiveTotalPages - 1)
                                }
                            ) {
                                Text(
                                    text = "Slide ${bIdx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.warning,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Main Reader Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isOutlineMode) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_outline_list"),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pages, key = { it.pageNumber }) { page ->
                                val idx = (page.pageNumber - 1).coerceAtLeast(0)
                                val isActive = idx == currentPageIndex
                                AuraCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 18.dp,
                                    onClick = {
                                        currentPageIndex = idx
                                        isOutlineMode = false
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AuraBadge(
                                                text = "Slide ${page.pageNumber}",
                                                backgroundColor = if (isActive) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                                                textColor = if (isActive) Color.White else LocalAuraColors.current.secondaryAccent
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = page.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = LocalAuraColors.current.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = page.body,
                                            fontSize = (fontSizeSp - 1f).sp,
                                            color = LocalAuraColors.current.textSecondary,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val customCanvasBg = when (readerCanvasTheme) {
                            1 -> Color(0xF5F4ECD8) // Warm Sepia
                            2 -> Color(0xF8F8FAFC) // Crisp Paper
                            else -> null
                        }
                        val slideTitleColor = if (readerCanvasTheme == 0) LocalAuraColors.current.textPrimary else Color(0xFF1E293B)
                        val slideBodyColor = if (readerCanvasTheme == 0) LocalAuraColors.current.textPrimary else Color(0xFF334155)

                        AuraCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_active_slide_card"),
                            cornerRadius = 22.dp,
                            backgroundColor = customCanvasBg
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AuraBadge(
                                            text = "SLIDE ${currentPageIndex + 1} OF $effectiveTotalPages",
                                            backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                                            textColor = LocalAuraColors.current.secondaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val progressPct = (((currentPageIndex + 1).toFloat() / effectiveTotalPages.toFloat()) * 100f).roundToInt()
                                        AuraBadge(
                                            text = "$progressPct% Read",
                                            backgroundColor = Color(0x1EFFFFFF),
                                            textColor = LocalAuraColors.current.textSecondary
                                        )
                                    }
                                    Text(
                                        text = SlideDocumentParser.formatFileSize(resource.fileSizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalAuraColors.current.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val bmp = renderedBitmap
                                if (bmp != null && !showPdfExtractedText) {
                                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                        val newScale = (pinchScale * zoomChange).coerceIn(1f, 3.2f)
                                        pinchScale = newScale
                                        isZoomedIn = newScale > 1.1f
                                        if (newScale > 1.02f) {
                                            panOffsetX = (panOffsetX + offsetChange.x).coerceIn(-650f, 650f)
                                            panOffsetY = (panOffsetY + offsetChange.y).coerceIn(-650f, 650f)
                                        } else {
                                            panOffsetX = 0f
                                            panOffsetY = 0f
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .pointerInput(currentPageIndex) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        if (pinchScale > 1.1f) {
                                                            pinchScale = 1f
                                                            panOffsetX = 0f
                                                            panOffsetY = 0f
                                                            isZoomedIn = false
                                                        } else {
                                                            pinchScale = 2.0f
                                                            isZoomedIn = true
                                                        }
                                                    }
                                                )
                                            }
                                            .transformable(state = transformState)
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "${resource.title} page ${currentPageIndex + 1}",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer(
                                                    scaleX = pinchScale,
                                                    scaleY = pinchScale,
                                                    translationX = panOffsetX,
                                                    translationY = panOffsetY
                                                )
                                        )
                                        if (pinchScale > 1.05f) {
                                            AuraBadge(
                                                text = "${String.format(Locale.US, "%.1f", pinchScale)}× Pinch/Double-Tap",
                                                backgroundColor = Color(0xCC0B1528),
                                                textColor = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Slide Heading
                                Text(
                                    text = currentSlide.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = slideTitleColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Highlighted Slide Body
                                HighlightedSlideText(
                                    text = currentSlide.body,
                                    highlightQuery = inDocQuery,
                                    fontSizeSp = fontSizeSp,
                                    overrideTextColor = slideBodyColor
                                )

                                if (currentSlide.speakerNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x1EFFFFFF))
                                            .border(1.dp, Color(0x3564D2FF), RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "LECTURER SPEAKER NOTES",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalAuraColors.current.secondaryAccent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentSlide.speakerNotes,
                                            fontSize = (fontSizeSp - 1f).sp,
                                            color = LocalAuraColors.current.textSecondary,
                                            lineHeight = (fontSizeSp + 5f).sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Personal Takeaways Scratchpad (bounded height so bottom dock never clips)
                if (showNotesPanel) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AuraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 152.dp),
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Personal Takeaways & Exam Notes (${resource.courseCode})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.secondaryAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = personalNotesDraft,
                                onValueChange = { personalNotesDraft = it },
                                placeholder = { Text("Jot down key formulas, exam hints, or questions...") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reader_notes_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                                    unfocusedBorderColor = Color(0x35FFFFFF),
                                    focusedTextColor = LocalAuraColors.current.textPrimary,
                                    unfocusedTextColor = LocalAuraColors.current.textPrimary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = alsoPinNotesToVault,
                                        onCheckedChange = { alsoPinNotesToVault = it },
                                        colors = CheckboxDefaults.colors(checkedColor = LocalAuraColors.current.primaryAccent)
                                    )
                                    Text(
                                        text = "Pin to Vault",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LocalAuraColors.current.textSecondary
                                    )
                                }
                                AuraGlassButton(
                                    text = "Save Notes",
                                    isPrimary = true,
                                    onClick = {
                                        viewModel.saveResourcePersonalNotes(
                                            resource = resource,
                                            notesText = personalNotesDraft,
                                            alsoSaveToVault = alsoPinNotesToVault
                                        )
                                        showNotesPanel = false
                                        statusBanner = if (alsoPinNotesToVault) {
                                            "Saved takeaways & pinned to your Notes Vault!"
                                        } else {
                                            "Saved takeaways to this slide deck."
                                        }
                                    },
                                    cornerRadius = 12.dp,
                                    testTag = "reader_save_notes_button"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Slide Scrubber & Navigation Controls (Always pinned inside safe area)
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (effectiveTotalPages > 1) {
                            Slider(
                                value = currentPageIndex.toFloat(),
                                onValueChange = { currentPageIndex = it.roundToInt().coerceIn(0, effectiveTotalPages - 1) },
                                valueRange = 0f..(effectiveTotalPages - 1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = LocalAuraColors.current.secondaryAccent,
                                    activeTrackColor = LocalAuraColors.current.primaryAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .testTag("reader_page_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AuraGlassIconButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Slide",
                                onClick = {
                                    if (currentPageIndex > 0) currentPageIndex -= 1
                                },
                                size = 48.dp,
                                tint = if (currentPageIndex > 0) LocalAuraColors.current.textPrimary else LocalAuraColors.current.textMuted,
                                testTag = "reader_prev_page_button"
                            )

                            AuraGlassButton(
                                text = if (resource.isCompleted) "Mastered ✓" else "Mark Mastered",
                                icon = Icons.Default.CheckCircle,
                                isPrimary = resource.isCompleted,
                                containerColor = if (resource.isCompleted) LocalAuraColors.current.success else null,
                                onClick = { viewModel.toggleMasteredCourseResource(resource) },
                                cornerRadius = 16.dp,
                                testTag = "reader_mark_mastered_button"
                            )

                            AuraGlassIconButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Slide",
                                onClick = {
                                    if (currentPageIndex < effectiveTotalPages - 1) currentPageIndex += 1
                                },
                                size = 48.dp,
                                tint = if (currentPageIndex < effectiveTotalPages - 1) LocalAuraColors.current.textPrimary else LocalAuraColors.current.textMuted,
                                testTag = "reader_next_page_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedSlideText(
    text: String,
    highlightQuery: String,
    fontSizeSp: Float,
    overrideTextColor: Color? = null
) {
    val primaryTextColor = overrideTextColor ?: LocalAuraColors.current.textPrimary
    val highlightBg = LocalAuraColors.current.warning.copy(alpha = 0.38f)

    val annotated = remember(text, highlightQuery, primaryTextColor, highlightBg) {
        if (highlightQuery.isBlank() || highlightQuery.length < 2) {
            buildAnnotatedString { append(text) }
        } else {
            buildAnnotatedString {
                var idx = 0
                val lowerText = text.lowercase(Locale.US)
                val lowerQ = highlightQuery.lowercase(Locale.US)
                while (idx < text.length) {
                    val found = lowerText.indexOf(lowerQ, idx)
                    if (found < 0) {
                        append(text.substring(idx))
                        break
                    }
                    if (found > idx) {
                        append(text.substring(idx, found))
                    }
                    withStyle(SpanStyle(background = highlightBg, fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(text.substring(found, found + highlightQuery.length))
                    }
                    idx = found + highlightQuery.length
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp + 7f).sp,
        color = primaryTextColor
    )
}
