package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.local.entity.FlashcardEntity
import com.example.ui.AuraViewModel
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.ExamReadinessHeatmapCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.performAuraHaptic
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.dialogs.OralVivaDialog
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import java.util.Locale

@Composable
fun FlashcardsView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val flashcards by viewModel.flashcards.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val recentHabits by viewModel.recentAcademicHabits.collectAsState()
    val focusRecords by viewModel.focusRecords.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDueOnly by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var vivaCard by remember { mutableStateOf<FlashcardEntity?>(null) }

    val nowMillis = System.currentTimeMillis()
    val dueCards = remember(flashcards, nowMillis) {
        flashcards.filter { it.nextReviewEpochMillis <= nowMillis }
    }
    val displayedDeck = remember(flashcards, dueCards, showDueOnly) {
        if (showDueOnly && dueCards.isNotEmpty()) dueCards else flashcards
    }

    val activeCard = displayedDeck.getOrNull(currentCardIndex.coerceIn(0, (displayedDeck.size - 1).coerceAtLeast(0)))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("flashcards_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Iridescent Prism Hero Header
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
                            .alpha(if (LocalAuraColors.current.isDark) 0.30f else 0.12f)
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SM-2 Spaced Repetition",
                                    fontFamily = OutfitFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Adaptive Ease Factor & interval memory engine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.secondaryAccent
                                )
                            }

                            AuraGlassButton(
                                text = "New Card",
                                icon = Icons.Default.Add,
                                isPrimary = true,
                                onClick = { showAddDialog = true },
                                testTag = "add_flashcard_button"
                            )
                        }

                        if (flashcards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AuraGlassButton(
                                    text = "All (${flashcards.size})",
                                    isPrimary = !showDueOnly,
                                    cornerRadius = 14.dp,
                                    onClick = {
                                        showDueOnly = false
                                        currentCardIndex = 0
                                    },
                                    testTag = "flashcards_filter_all"
                                )
                                AuraGlassButton(
                                    text = "Due (${dueCards.size})",
                                    isPrimary = showDueOnly,
                                    cornerRadius = 14.dp,
                                    onClick = {
                                        showDueOnly = true
                                        currentCardIndex = 0
                                    },
                                    testTag = "flashcards_filter_due"
                                )
                                if (activeCard != null) {
                                    AuraGlassButton(
                                        text = "Voice Viva",
                                        icon = Icons.Default.RecordVoiceOver,
                                        cornerRadius = 14.dp,
                                        onClick = { vivaCard = activeCard },
                                        testTag = "open_oral_viva_button"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (flashcards.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Flashcards Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create question & answer flashcards to memorize formulas, definitions, and key exam concepts with spaced repetition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (activeCard != null) {
            // Interactive 3D Spring Flip + Swipe Recall Card
            item {
                var dragOffsetX by remember(activeCard.id) { mutableFloatStateOf(0f) }
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
                    label = "cardFlip"
                )
                val showingAnswer = rotation > 90f

                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(266.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            translationX = dragOffsetX * 0.35f
                            rotationZ = (dragOffsetX / 45f).coerceIn(-8f, 8f)
                            cameraDistance = 14f * density
                        }
                        .pointerInput(activeCard.id, displayedDeck.size) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragOffsetX > 150f -> {
                                            performAuraHaptic(view)
                                            viewModel.reviewFlashcard(activeCard, "GOOD")
                                            isFlipped = false
                                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                                        }
                                        dragOffsetX < -150f -> {
                                            performAuraHaptic(view)
                                            viewModel.reviewFlashcard(activeCard, "HARD")
                                            isFlipped = false
                                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                                        }
                                    }
                                    dragOffsetX = 0f
                                },
                                onDragCancel = { dragOffsetX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-320f, 320f)
                                }
                            )
                        }
                        .clickable {
                            performAuraHaptic(view)
                            isFlipped = !isFlipped
                        }
                        .testTag("active_flashcard_card"),
                    cornerRadius = 26.dp,
                    borderColor = when {
                        dragOffsetX > 80f -> AuraMintAccent
                        dragOffsetX < -80f -> LocalAuraColors.current.warning
                        showingAnswer -> AuraMintAccent.copy(alpha = 0.72f)
                        else -> LocalAuraColors.current.secondaryAccent.copy(alpha = 0.55f)
                    },
                    accentGlow = if (showingAnswer) AuraMintAccent else LocalAuraColors.current.primaryAccent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (showingAnswer) rotationY = 180f
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuraBadge(
                                    text = if (showingAnswer) "ANSWER • ${activeCard.courseCode}" else activeCard.courseCode,
                                    backgroundColor = if (showingAnswer) AuraMintAccent.copy(alpha = 0.22f) else LocalAuraColors.current.primaryAccent.copy(alpha = 0.20f),
                                    textColor = if (showingAnswer) AuraMintAccent else LocalAuraColors.current.secondaryAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AuraBadge(
                                    text = "EF ${String.format(Locale.US, "%.1f", activeCard.easeFactor)} • ${activeCard.intervalDays}d • Rep ${activeCard.repetitions}",
                                    backgroundColor = AuraMintAccent.copy(alpha = 0.16f),
                                    textColor = AuraMintAccent
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${currentCardIndex + 1} / ${displayedDeck.size}",
                                    fontFamily = AuraMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalAuraColors.current.textMuted
                                )
                            }

                            Text(
                                text = if (showingAnswer) activeCard.answer else activeCard.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Flip,
                                    null,
                                    tint = if (showingAnswer) AuraMintAccent else LocalAuraColors.current.secondaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showingAnswer) {
                                        "Tap to flip back • Swipe Left (Hard) / Swipe Right (Good)"
                                    } else {
                                        "Tap to 3D flip • Swipe Right = Good • Voice Viva ready"
                                    },
                                    fontSize = 11.sp,
                                    color = LocalAuraColors.current.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // SM-2 Spaced Repetition Rating Buttons
            item {
                Text(
                    text = "Rate your SM-2 recall quality:",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAuraColors.current.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecallButton(
                        label = "Again",
                        sublabel = "1d reset",
                        color = LocalAuraColors.current.danger,
                        onClick = {
                            performAuraHaptic(view)
                            viewModel.reviewFlashcard(activeCard, "AGAIN")
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                        },
                        modifier = Modifier.weight(1f).testTag("sm2_rate_again")
                    )
                    RecallButton(
                        label = "Hard",
                        sublabel = "1.2× Hard",
                        color = LocalAuraColors.current.warning,
                        onClick = {
                            performAuraHaptic(view)
                            viewModel.reviewFlashcard(activeCard, "HARD")
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                        },
                        modifier = Modifier.weight(1f).testTag("sm2_rate_hard")
                    )
                    RecallButton(
                        label = "Good",
                        sublabel = "SM-2 Good",
                        color = LocalAuraColors.current.primaryAccent,
                        onClick = {
                            performAuraHaptic(view)
                            viewModel.reviewFlashcard(activeCard, "GOOD")
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                        },
                        modifier = Modifier.weight(1f).testTag("sm2_rate_good")
                    )
                    RecallButton(
                        label = "Easy",
                        sublabel = "Easy Boost",
                        color = LocalAuraColors.current.success,
                        onClick = {
                            performAuraHaptic(view)
                            viewModel.reviewFlashcard(activeCard, "EASY")
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % displayedDeck.size.coerceAtLeast(1)
                        },
                        modifier = Modifier.weight(1f).testTag("sm2_rate_easy")
                    )
                }
            }

            item {
                SectionHeader(title = "All Decks & Cards (${flashcards.size})")
            }

            items(flashcards) { card ->
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = card.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary)
                            Text(text = card.answer, style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary, maxLines = 1)
                        }
                        AuraBadge(
                            text = "${card.intervalDays}d • EF ${String.format(Locale.US, "%.1f", card.easeFactor)}",
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                        IconButton(onClick = { viewModel.deleteFlashcard(card) }) {
                            Icon(Icons.Default.Delete, null, tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 28-Day Exam Readiness & Memory Heatmap
        item {
            ExamReadinessHeatmapCard(
                habits = recentHabits,
                focusRecords = focusRecords,
                flashcards = flashcards,
                testTag = "flashcards_readiness_heatmap"
            )
        }
    }

    vivaCard?.let { card ->
        OralVivaDialog(
            viewModel = viewModel,
            courseCode = card.courseCode,
            question = card.question,
            expectedAnswer = card.answer,
            onDismiss = { vivaCard = null }
        )
    }

    if (showAddDialog) {
        var courseCode by remember { mutableStateOf(courses.firstOrNull()?.code ?: "General") }
        var question by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Flashcard", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Question / Term") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Answer / Definition") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Card",
                    isPrimary = true,
                    onClick = {
                        if (question.isNotBlank() && answer.isNotBlank()) {
                            viewModel.addFlashcard(courseCode, "Core", question, answer)
                            showAddDialog = false
                        }
                    },
                    testTag = "save_flashcard_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showAddDialog = false },
                    testTag = "cancel_flashcard_button"
                )
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun RecallButton(
    label: String,
    sublabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
            Text(text = sublabel, fontSize = 9.sp, color = LocalAuraColors.current.textMuted)
        }
    }
}
