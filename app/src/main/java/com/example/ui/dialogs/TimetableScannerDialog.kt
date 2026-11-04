package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.TimetableScanResult
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.formatTime12Hour
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import com.example.util.SlideDocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimetableScannerDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanningTimetable.collectAsState()

    var rawPortalText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSourceLabel by remember { mutableStateOf<String?>(null) }
    var scanResult by remember { mutableStateOf<TimetableScanResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    try {
                        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (rawBytes == null || rawBytes.isEmpty()) {
                            null
                        } else {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
                            var sample = 1
                            while ((bounds.outWidth / sample) > 2048 || (bounds.outHeight / sample) > 2048) {
                                sample *= 2
                            }
                            BitmapFactory.decodeByteArray(
                                rawBytes,
                                0,
                                rawBytes.size,
                                BitmapFactory.Options().apply { inSampleSize = sample }
                            )
                        }
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (bmp != null) {
                    selectedBitmap = bmp
                    selectedSourceLabel = "Timetable Photo (${bmp.width}×${bmp.height}px)"
                    viewModel.scanTimetableOrSyllabus(bmp, rawPortalText) { result ->
                        scanResult = result
                    }
                } else {
                    statusBanner = "Could not decode image. Try another photo or PDF."
                }
            }
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = withContext(Dispatchers.IO) {
                    SlideDocumentParser.importDocumentFromUri(context, uri, "Scanned Syllabus")
                }
                if (imported != null) {
                    val plain = SlideDocumentParser.plainSearchableText(imported.serializedText)
                    rawPortalText = plain
                    selectedSourceLabel = "${imported.originalFileName} (${imported.fileType})"
                    SlideDocumentParser.deleteLocalResourceFile(imported.localFilePath)
                    viewModel.scanTimetableOrSyllabus(selectedBitmap, plain) { result ->
                        scanResult = result
                    }
                } else {
                    statusBanner = "Could not read selected PDF/document."
                }
            }
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
                        .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Timetable & Syllabus Scanner",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Scan photo, PDF syllabus, or portal text into lectures & exams",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
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
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("timetable_scanner_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraGlassButton(
                        text = "Scan Photo",
                        icon = Icons.Default.CameraAlt,
                        isPrimary = true,
                        onClick = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = 14.dp,
                        testTag = "scan_timetable_photo_button"
                    )

                    AuraGlassButton(
                        text = "Scan PDF/Doc",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            pickDocumentLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/*",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        cornerRadius = 14.dp,
                        testTag = "scan_timetable_pdf_button"
                    )
                }

                if (selectedSourceLabel != null) {
                    AuraBadge(
                        text = "Source: $selectedSourceLabel",
                        backgroundColor = Color(0x2438BDF8),
                        textColor = LocalAuraColors.current.secondaryAccent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Or paste portal schedule / syllabus text:",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAuraColors.current.textSecondary
                    )
                    Text(
                        text = "Paste Format Example",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier
                            .clickable {
                                rawPortalText = "CSCD 311 - Compiler Design | Mon 08:30 - 10:30 | Room N1\n" +
                                    "MATH 304 - Numerical Analysis | Wed 2:00 PM - 4:00 PM | Block B4\n" +
                                    "EXAM: CSCD 311 Midterm Exam | Hall C2"
                            }
                            .testTag("load_sample_portal_schedule")
                    )
                }

                OutlinedTextField(
                    value = rawPortalText,
                    onValueChange = { rawPortalText = it },
                    placeholder = {
                        Text(
                            text = "e.g. CSCD 301 - Operating Systems | Mon 8:30 AM - 10:30 AM | Room B12",
                            fontSize = 12.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_raw_text_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                AuraGlassButton(
                    text = if (isScanning) "Extracting Courses, Times & Venues..." else "Extract Schedule Now",
                    icon = Icons.Default.AutoAwesome,
                    isPrimary = true,
                    enabled = !isScanning && (rawPortalText.isNotBlank() || selectedBitmap != null),
                    onClick = {
                        viewModel.scanTimetableOrSyllabus(selectedBitmap, rawPortalText) { res ->
                            scanResult = res
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    testTag = "run_timetable_scan_button"
                )

                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyzing timetable slots, AM/PM times & exam dates...",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary
                        )
                    }
                }

                if (statusBanner != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LocalAuraColors.current.success.copy(alpha = 0.18f))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LocalAuraColors.current.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textPrimary
                        )
                    }
                }

                val parsed = scanResult
                if (parsed != null) {
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AuraBadge(
                                text = "${parsed.courses.size} Course(s) • ${parsed.lectures.size} Lecture(s) • ${parsed.exams.size} Exam(s)",
                                backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.25f),
                                textColor = LocalAuraColors.current.secondaryAccent
                            )
                            parsed.lectures.forEach { lec ->
                                Text(
                                    text = "• ${lec.courseCode} (${lec.courseName}) — Day ${lec.dayOfWeek} ${formatTime12Hour(lec.startTime)}–${formatTime12Hour(lec.endTime)} @ ${lec.venue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textPrimary
                                )
                            }
                            parsed.exams.forEach { ex ->
                                Text(
                                    text = "• EXAM: ${ex.courseCode} ${ex.title} @ ${ex.examVenue.ifBlank { "TBA" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.warning
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val readyResult = scanResult
            if (readyResult != null && (readyResult.courses.isNotEmpty() || readyResult.lectures.isNotEmpty() || readyResult.exams.isNotEmpty())) {
                AuraGlassButton(
                    text = "Import All (${readyResult.lectures.size + readyResult.exams.size})",
                    isPrimary = true,
                    onClick = {
                        viewModel.importScannedTimetable(readyResult) { summary ->
                            statusBanner = summary
                            onDismiss()
                        }
                    },
                    testTag = "confirm_import_scanned_timetable"
                )
            } else {
                AuraGlassButton(
                    text = "Close",
                    onClick = onDismiss,
                    testTag = "close_timetable_scanner_button"
                )
            }
        },
        dismissButton = {
            if (scanResult != null) {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        },
        containerColor = Color(0xF2101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x385CE1FF))),
            RoundedCornerShape(24.dp)
        )
    )
}
