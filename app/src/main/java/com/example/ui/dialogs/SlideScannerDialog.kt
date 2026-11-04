package com.example.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors

@Composable
fun SlideScannerDialog(
    viewModel: AuraViewModel,
    defaultCourseCode: String = "General",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanningSlide.collectAsState()

    var courseCode by remember { mutableStateOf(defaultCourseCode) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var noteTitle by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("SUMMARY") }
    var extractedText by remember { mutableStateOf("") }
    var extractedCards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var autoCreateFlashcards by remember { mutableStateOf(true) }

    val handleCapturedBitmap: (Bitmap) -> Unit = { bmp ->
        selectedBitmap = bmp
        viewModel.scanSlideBitmap(bmp, courseCode.ifBlank { "General" }) { res ->
            noteTitle = res.title
            noteCategory = res.category
            extractedText = res.extractedNotes
            extractedCards = res.flashcards
        }
    }

    // Zero-permission System Camera Capture (Whiteboards & Handouts)
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            handleCapturedBitmap(bmp)
        }
    }

    // Zero-permission Android Photo Picker (Google Play Policy Compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = decodeScaledBitmapSafe(context, uri, 1280)
            if (bmp != null) {
                handleCapturedBitmap(bmp)
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
                        .background(LocalAuraColors.current.aiAccent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = LocalAuraColors.current.aiAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Whiteboard & Handout Scanner",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Snap whiteboard formulas or pick slide photos for AI notes",
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
                    .verticalScroll(rememberScrollState())
                    .testTag("whiteboard_scanner_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g. PHYS 201)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraGlassButton(
                        text = "Snap Camera",
                        icon = Icons.Default.CameraAlt,
                        isPrimary = true,
                        onClick = {
                            runCatching { cameraCaptureLauncher.launch() }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "capture_whiteboard_camera_button"
                    )
                    AuraGlassButton(
                        text = if (selectedBitmap == null) "Pick Photo" else "Pick Another",
                        icon = Icons.Default.PhotoLibrary,
                        isPrimary = false,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pick_slide_image_button"
                    )
                }

                val bmp = selectedBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured Whiteboard or Lecture Slide",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                    )
                }

                if (isScanning) {
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalAuraColors.current.aiAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Extracting whiteboard formulas, bullet takeaways & flashcards...",
                                fontSize = 12.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                    }
                }

                if (selectedBitmap != null || extractedText.isNotBlank()) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Extracted Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FORMULA", "SUMMARY", "CONCEPT", "CHEAT_SHEET").forEach { cat ->
                            AuraBadge(
                                text = cat.replace("_", " "),
                                backgroundColor = if (noteCategory == cat) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                                textColor = if (noteCategory == cat) Color.White else LocalAuraColors.current.textSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { noteCategory = cat }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        label = { Text("Extracted Formulas & Bullet Summary") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (extractedCards.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LocalAuraColors.current.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Also add ${extractedCards.size} Flashcards to Study Deck",
                                fontSize = 12.sp,
                                color = LocalAuraColors.current.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoCreateFlashcards,
                                onCheckedChange = { autoCreateFlashcards = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save to Vault",
                isPrimary = true,
                enabled = extractedText.isNotBlank() && !isScanning,
                onClick = {
                    viewModel.saveScannedSlideMaterial(
                        courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                        title = noteTitle,
                        category = noteCategory,
                        extractedNotes = extractedText,
                        flashcardsToCreate = if (autoCreateFlashcards) extractedCards else emptyList()
                    )
                    onDismiss()
                },
                testTag = "save_scanned_slide_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_slide_scanner_button"
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

private fun decodeScaledBitmapSafe(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        var sample = 1
        while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}
