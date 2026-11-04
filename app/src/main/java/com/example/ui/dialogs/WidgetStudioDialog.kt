package com.example.ui.dialogs

import android.appwidget.AppWidgetProvider
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.AuraThemePreset
import com.example.ui.theme.LocalAuraColors
import com.example.widget.AuraExamCountdownWidgetProvider
import com.example.widget.AuraFocusStreakWidgetProvider
import com.example.widget.AuraStudyDeckWidgetProvider
import com.example.widget.AuraTodayScheduleWidgetProvider
import com.example.widget.AuraUpNextWidgetProvider
import com.example.widget.AuraWidgetSuiteManager

private data class WidgetCatalogEntry(
    val id: String,
    val title: String,
    val gridSizeBadge: String,
    val subtitle: String,
    val highlights: List<String>,
    val previewHeadline: String,
    val previewSubline: String,
    val icon: ImageVector,
    val providerClass: Class<out AppWidgetProvider>
)

@Composable
fun WidgetStudioDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAuraColors.current
    val userProfile by viewModel.userProfile.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val focusRecords by viewModel.focusRecords.collectAsState()
    val resources by viewModel.courseResources.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()

    var statusBanner by remember { mutableStateOf<String?>(null) }

    val pendingCount = remember(assessments) {
        assessments.count { it.status != "COMPLETED" && it.status != "SUBMITTED" }
    }
    val streakDays = remember(focusRecords) {
        AuraWidgetSuiteManager.computeStudyStreakDays(focusRecords)
    }
    val dueFlashcards = remember(flashcards) {
        AuraStudyDeckWidgetProvider.computeDueFlashcardsCount(flashcards)
    }

    val catalog = remember(lectures.size, pendingCount, streakDays, resources.size, dueFlashcards) {
        listOf(
            WidgetCatalogEntry(
                id = "up_next",
                title = "AURA Up-Next & Focus",
                gridSizeBadge = "4×2 •Resizable",
                subtitle = "Live class/up-next spotlight, Leave-By transit pill, Focus Ring & 4-shortcut dock.",
                highlights = listOf("Live Class Glow", "Focus Ring", "4-Tap Dock"),
                previewHeadline = lectures.firstOrNull()?.let { "${it.courseCode} • ${it.courseName}" }
                    ?: "CSCD 311 • Compiler Design",
                previewSubline = "$pendingCount Tasks Due • Leave-By Alert & 1-Tap AI/Slides Dock",
                icon = Icons.Default.AutoAwesome,
                providerClass = AuraUpNextWidgetProvider::class.java
            ),
            WidgetCatalogEntry(
                id = "today_schedule",
                title = "AURA Today's Schedule",
                gridSizeBadge = "4×3 • Timeline",
                subtitle = "Multi-slot campus agenda showing up to 3 classes with course color bars & alarm count.",
                highlights = listOf("3 Class Slots", "Course Color Bars", "+ Add Lecture"),
                previewHeadline = "${lectures.size} Scheduled Classes Synced",
                previewSubline = "Auto-rolls over to your next teaching day when today is clear",
                icon = Icons.Default.CalendarMonth,
                providerClass = AuraTodayScheduleWidgetProvider::class.java
            ),
            WidgetCatalogEntry(
                id = "exam_radar",
                title = "AURA Exam & Deadlines",
                gridSizeBadge = "4×2 • Countdown",
                subtitle = "Urgency-tinted days-left ring (Rose/Amber/Cyan), exam venue & seat pass, and War Room launch.",
                highlights = listOf("Days-Left Ring", "Hall & Seat Info", "Exam War Room"),
                previewHeadline = if (pendingCount > 0) "$pendingCount Upcoming Exam / Task Deadlines" else "All Exams & Assignments Clear",
                previewSubline = "Auto-shifts ring color from Cyan → Amber → Coral as finals approach",
                icon = Icons.Default.EventBusy,
                providerClass = AuraExamCountdownWidgetProvider::class.java
            ),
            WidgetCatalogEntry(
                id = "focus_streak",
                title = "AURA Focus & Streak",
                gridSizeBadge = "2×2 • Compact Bento",
                subtitle = "Minimalist square tile with high-DPI Pomodoro goal ring, consecutive day streak & 1-tap timer.",
                highlights = listOf("2×2 Square Fit", "Day Streak Pill", "1-Tap Pomodoro"),
                previewHeadline = "Streak • $streakDays ${if (streakDays == 1) "Day" else "Days"}",
                previewSubline = "Weekly Goal: ${userProfile.studyGoalHoursPerWeek}h • Launches Focus Timer directly",
                icon = Icons.Default.Timer,
                providerClass = AuraFocusStreakWidgetProvider::class.java
            ),
            WidgetCatalogEntry(
                id = "study_deck",
                title = "AURA Slides & Flashcards",
                gridSizeBadge = "4×2 • Study Deck",
                subtitle = "Continue reading active PDF/PPTX slides with a mastery progress bar, SM-2 due count & 10Q Quiz.",
                highlights = listOf("Slide Progress Bar", "$dueFlashcards Cards Due", "10Q AI Quiz"),
                previewHeadline = resources.firstOrNull()?.let { "${it.courseCode} • ${it.title}" }
                    ?: "Course Slides & SM-2 Flashcard Deck",
                previewSubline = "${resources.size} Slide Decks • $dueFlashcards Flashcards Due Now",
                icon = Icons.Default.Description,
                providerClass = AuraStudyDeckWidgetProvider::class.java
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("widget_studio_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = colors.bgSecondary,
            border = BorderStroke(1.dp, colors.glassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = colors.secondaryAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aurora Widget Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "5 Liquid Glass Home Screen Widgets • Live Theme Sync",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Widget Studio",
                        onClick = onDismiss,
                        size = 34.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Sync Selector Strip + Sync All Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WIDGET ACCENT SYNC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = colors.secondaryAccent
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryAccent.copy(alpha = 0.16f))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(50))
                            .clickable {
                                viewModel.refreshAllHomeWidgets()
                                statusBanner = "Synced all 5 Home Screen Widgets with latest schedule, tasks & theme!"
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("widget_studio_sync_all_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh all widgets",
                            tint = colors.secondaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sync All Now",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(AuraThemePreset.entries) { preset ->
                        val selected = userProfile.accentPreset.equals(preset.id, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) preset.primaryDark.copy(alpha = 0.24f)
                                    else colors.glassCard
                                )
                                .border(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) preset.secondaryDark else colors.glassBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    viewModel.setAccentPreset(preset.id)
                                    statusBanner = "Applied ${preset.title} glow across AURA & all 5 widgets."
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(preset.primaryDark, preset.secondaryDark))
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "WIDGET GLASS FROST MATERIAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = colors.secondaryAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "FROSTED_OBSIDIAN" to "Obsidian",
                        "CRYSTAL_CLEAR" to "Crystal Clear",
                        "DAYLIGHT_PEARL" to "Daylight Pearl"
                    ).forEach { (styleId, label) ->
                        val active = userProfile.widgetGlassStyle.equals(styleId, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) colors.primaryAccent.copy(alpha = 0.26f)
                                    else colors.glassCard
                                )
                                .border(
                                    width = if (active) 1.3.dp else 1.dp,
                                    color = if (active) colors.secondaryAccent else colors.glassBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.setWidgetGlassStyle(styleId)
                                    statusBanner = "Applied '$label' glass material to all 5 Home Screen Widgets."
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp)
                                .testTag("widget_glass_style_$styleId"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (active) colors.textPrimary else colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (statusBanner != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.success.copy(alpha = 0.14f))
                            .border(1.dp, colors.success.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusBanner.orEmpty(),
                            fontSize = 11.sp,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable 5-Widget Gallery
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("widget_studio_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(catalog, key = { _, item -> item.id }) { index, widgetItem ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0A1629),
                                            Color(0xFF0F213B)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            colors.secondaryAccent.copy(alpha = 0.45f),
                                            colors.primaryAccent.copy(alpha = 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(14.dp)
                                .testTag("widget_studio_card_${widgetItem.id}")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(colors.primaryAccent.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = widgetItem.icon,
                                            contentDescription = null,
                                            tint = colors.secondaryAccent,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${widgetItem.title}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = widgetItem.subtitle,
                                            fontSize = 11.sp,
                                            color = Color(0xFFB5C7E3),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AuraBadge(
                                        text = widgetItem.gridSizeBadge,
                                        backgroundColor = colors.secondaryAccent.copy(alpha = 0.16f),
                                        textColor = colors.secondaryAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Mini Glass Mockup Surface
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x22FFFFFF))
                                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = widgetItem.previewHeadline,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = widgetItem.previewSubline,
                                        fontSize = 11.sp,
                                        color = Color(0xFFC9DBFA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        widgetItem.highlights.take(2).forEach { chip ->
                                            Text(
                                                text = chip,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.secondaryAccent,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(colors.secondaryAccent.copy(alpha = 0.13f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    AuraGlassButton(
                                        text = "Pin Widget",
                                        icon = Icons.AutoMirrored.Filled.AddToHomeScreen,
                                        isPrimary = true,
                                        modifier = Modifier.testTag("pin_widget_btn_$index"),
                                        onClick = {
                                            viewModel.refreshAllHomeWidgets()
                                            val pinned = AuraWidgetSuiteManager.requestPinWidget(
                                                context = context,
                                                providerClass = widgetItem.providerClass
                                            )
                                            val msg = if (pinned) {
                                                "Confirm '${widgetItem.title}' in your launcher prompt!"
                                            } else {
                                                "Long-press your Home Screen → Widgets → AURA to place '${widgetItem.title}'."
                                            }
                                            statusBanner = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
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
