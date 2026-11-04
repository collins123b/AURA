package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors
import com.example.util.AcademicExportHelper
import com.example.util.TimetableInsightsAndShareHelper

@Composable
fun JsonImportExportDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val officeHours by viewModel.officeHours.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val studyNotes by viewModel.studyNotes.collectAsState()
    val citations by viewModel.citations.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = PDF & .ICS, 1 = Export JSON, 2 = Import JSON
    var exportedJsonText by remember { mutableStateOf("Generating JSON...") }
    var importInputText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    AcademicExportHelper.writeSemesterPdf(
                        outputStream = out,
                        studentName = userProfile.name,
                        institution = userProfile.institution,
                        programme = userProfile.programme,
                        lectures = lectures,
                        assessments = assessments,
                        notes = studyNotes,
                        citations = citations
                    )
                }
                Toast.makeText(context, "Saved Printable PDF Timetable, Formulas & Bibliography!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        if (uri != null) {
            try {
                val ics = AcademicExportHelper.generateIcsCalendar(
                    studentName = userProfile.name,
                    institution = userProfile.institution,
                    lectures = lectures,
                    officeHours = officeHours,
                    assessments = assessments
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(ics.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Saved .ICS Calendar file!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save .ICS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            exportedJsonText = viewModel.exportTimetableJson()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LocalAuraColors.current.secondaryAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.PictureAsPdf
                            1 -> Icons.Default.Share
                            else -> Icons.Default.Download
                        },
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academic Export & Sync Hub",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "PDF Schedule, .ICS Calendar Sync & JSON Backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LocalAuraColors.current.textMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x15FFFFFF),
                    contentColor = LocalAuraColors.current.primaryAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = LocalAuraColors.current.primaryAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("PDF & .ICS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("export_tab_pdf_ics")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Export JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("export_tab_json_out")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Import JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("export_tab_json_in")
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Printable PDF Schedule & Formula Sheet Card
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(text = "PRINTABLE PDF")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${lectures.size} Lectures • ${studyNotes.size} Vault Notes",
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.secondaryAccent
                                    )
                                }
                                Text(
                                    text = "Semester Timetable, Exams & Formula Sheet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Generates a clean A4 PDF combining your weekly 12h AM/PM timetable, upcoming examinations, and saved formulas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AuraGlassButton(
                                        text = "Share PDF",
                                        icon = Icons.Default.Share,
                                        isPrimary = true,
                                        onClick = {
                                            try {
                                                AcademicExportHelper.exportAndSharePdf(
                                                    context = context,
                                                    studentName = userProfile.name,
                                                    institution = userProfile.institution,
                                                    programme = userProfile.programme,
                                                    lectures = lectures,
                                                    assessments = assessments,
                                                    notes = studyNotes,
                                                    citations = citations
                                                )
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "share_pdf_schedule_button"
                                    )
                                    AuraGlassButton(
                                        text = "Save PDF",
                                        icon = Icons.Default.Download,
                                        onClick = {
                                            savePdfLauncher.launch("AURA_Semester_Schedule.pdf")
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "save_pdf_schedule_button"
                                    )
                                }
                            }
                        }

                        // Shareable Weekly Timetable Poster Image (.PNG) Card
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(
                                        text = "POSTER IMAGE (.PNG)",
                                        textColor = LocalAuraColors.current.secondaryAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "WhatsApp, Telegram & Gallery",
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.textMuted
                                    )
                                }
                                Text(
                                    text = "Shareable Weekly Timetable Poster",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Renders a high-resolution dark-glass timetable poster card to share with classmates.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp
                                )
                                AuraGlassButton(
                                    text = "Share Weekly Timetable Image",
                                    icon = Icons.Default.Image,
                                    isPrimary = true,
                                    onClick = {
                                        val ok = TimetableInsightsAndShareHelper.shareWeeklyTimetableImage(
                                            context = context,
                                            studentName = userProfile.name,
                                            institution = userProfile.institution,
                                            semesterLabel = "${userProfile.academicLevel} • ${userProfile.semester}",
                                            lectures = lectures
                                        )
                                        if (!ok) {
                                            Toast.makeText(context, "Could not launch image share.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "share_timetable_image_button"
                                )
                            }
                        }

                        // RFC-5545 .ICS Calendar Sync Card
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(
                                        text = "RFC-5545 .ICS",
                                        textColor = LocalAuraColors.current.warning
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google, Apple & Outlook Calendar",
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.textMuted
                                    )
                                }
                                Text(
                                    text = "University Calendar Feed (.ICS)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Exports recurring weekly lectures, lecturer office hours, and exam reminders into any calendar app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AuraGlassButton(
                                        text = "Sync .ICS",
                                        icon = Icons.Default.CalendarMonth,
                                        isPrimary = true,
                                        onClick = {
                                            try {
                                                AcademicExportHelper.exportAndShareIcs(
                                                    context = context,
                                                    studentName = userProfile.name,
                                                    institution = userProfile.institution,
                                                    lectures = lectures,
                                                    officeHours = officeHours,
                                                    assessments = assessments
                                                )
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Calendar export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "share_ics_calendar_button"
                                    )
                                    AuraGlassButton(
                                        text = "Save .ICS",
                                        icon = Icons.Default.Download,
                                        onClick = {
                                            saveIcsLauncher.launch("AURA_Timetable.ics")
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "save_ics_calendar_button"
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Copy your current timetable schema to share with coursemates or keep an offline backup:",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary
                        )

                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = exportedJsonText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LocalAuraColors.current.textPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        AuraGlassButton(
                            text = "Copy JSON to Clipboard",
                            icon = Icons.Default.ContentCopy,
                            isPrimary = true,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AURA Timetable Backup", exportedJsonText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied JSON to Clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "copy_json_button"
                        )
                    }

                    else -> {
                        Text(
                            text = "Paste a valid AURA timetable JSON payload below to import courses and lecture schedules:",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary
                        )

                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = {
                                importInputText = it
                                importStatusMessage = null
                            },
                            placeholder = { Text("Paste JSON here...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 10
                        )

                        if (importStatusMessage != null) {
                            Text(
                                text = importStatusMessage!!,
                                color = if (isSuccess) LocalAuraColors.current.success else LocalAuraColors.current.danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        AuraGlassButton(
                            text = "Validate & Import Schedule",
                            icon = Icons.Default.Upload,
                            isPrimary = true,
                            onClick = {
                                if (importInputText.isNotBlank()) {
                                    viewModel.importTimetableJson(importInputText) { success, msg ->
                                        isSuccess = success
                                        importStatusMessage = msg
                                        if (success) importInputText = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "validate_import_json_button"
                        )
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Done",
                isPrimary = true,
                onClick = onDismiss,
                testTag = "json_dialog_done_button"
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
