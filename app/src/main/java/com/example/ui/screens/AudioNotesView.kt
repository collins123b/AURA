package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.AudioNoteEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalAuraColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AudioNotesView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val audioNotes by viewModel.audioNotes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val isRecording by viewModel.audioRecorder.isRecording.collectAsState()
    val isPlaying by viewModel.audioRecorder.isPlaying.collectAsState()
    val currentPlayingPath by viewModel.audioRecorder.currentPlayingPath.collectAsState()
    val currentPlaybackSpeed by viewModel.audioRecorder.playbackSpeed.collectAsState()

    val context = LocalContext.current
    var recordSeconds by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingRecordedData by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.audioRecorder.startRecording("Lecture Note")
            if (!started) {
                Toast.makeText(context, "Could not initialize audio capture", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordSeconds++
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("audio_notes_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Record Controller Card
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth().testTag("recorder_control_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecording) "Recording Lecture Audio..." else "Voice Dictation & Lecture Recorder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) LocalAuraColors.current.danger else LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = if (isRecording) String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60) else "Record in-class explanations, tutorials and voice memos",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x60000000))
                            .clip(CircleShape)
                            .background(
                                if (isRecording) LocalAuraColors.current.danger else LocalAuraColors.current.primaryAccent
                            )
                            .border(1.5.dp, Color(0x55FFFFFF), CircleShape)
                            .clickable {
                                if (isRecording) {
                                    val result = viewModel.audioRecorder.stopRecording()
                                    if (result != null) {
                                        pendingRecordedData = result
                                        showSaveDialog = true
                                    }
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        val started = viewModel.audioRecorder.startRecording("Lecture Note")
                                        if (!started) {
                                            Toast.makeText(context, "Could not initialize audio capture", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                            .testTag("record_audio_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRecording) "Tap to Finish & Save" else "Tap Mic to Start Recording",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAuraColors.current.textMuted
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Recorded Lecture Memos",
                subtitle = "${audioNotes.size} audio recordings saved"
            )
        }

        if (audioNotes.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No audio lecture notes yet.\nTap the microphone to record key lecturer explanations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(audioNotes) { note ->
                val isThisPlaying = isPlaying && currentPlayingPath == note.filePath
                val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(note.recordedEpochMillis), ZoneId.systemDefault())

                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isThisPlaying) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f))
                                    .clickable {
                                        if (isThisPlaying) {
                                            viewModel.audioRecorder.stopPlaying()
                                        } else {
                                            viewModel.audioRecorder.playAudio(note.filePath)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isThisPlaying) Color.Black else LocalAuraColors.current.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(text = note.courseCode)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${note.durationSeconds}s",
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                if (note.notes.isNotBlank()) {
                                    Text(text = note.notes, style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary, maxLines = 1)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteAudioNote(note) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Playback Speed Chips (when playing)
                        if (isThisPlaying) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Speed:", fontSize = 11.sp, color = LocalAuraColors.current.textMuted)
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    val isCur = (currentPlaybackSpeed == spd)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCur) LocalAuraColors.current.primaryAccent else Color(0x18FFFFFF))
                                            .clickable { viewModel.audioRecorder.setPlaybackSpeed(spd) }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${spd}x",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCur) Color.Black else LocalAuraColors.current.textSecondary
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

    if (showSaveDialog && pendingRecordedData != null) {
        val (path, dur) = pendingRecordedData!!
        var noteTitle by remember { mutableStateOf("Lecture Highlight") }
        var courseCode by remember { mutableStateOf(courses.firstOrNull()?.code ?: "General") }
        var summaryNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                pendingRecordedData = null
            },
            title = { Text("Save Lecture Recording", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Duration: ${dur}s", color = LocalAuraColors.current.secondaryAccent, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Recording Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = summaryNotes,
                        onValueChange = { summaryNotes = it },
                        label = { Text("Key Takeaways / Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Recording",
                    onClick = {
                        val finalTitle = noteTitle.ifBlank { "Lecture Highlight" }
                        val finalCode = courseCode.ifBlank { "CSCD 301" }
                        viewModel.saveAudioNote(finalTitle, finalCode, path, dur, summaryNotes)
                        showSaveDialog = false
                        pendingRecordedData = null
                    },
                    containerColor = Color(0x330A84FF),
                    borderColor = Color(0x660A84FF),
                    textColor = Color(0xFF64D2FF)
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Discard",
                    onClick = {
                        showSaveDialog = false
                        pendingRecordedData = null
                    },
                    containerColor = Color(0x14FFFFFF),
                    borderColor = Color(0x22FFFFFF),
                    textColor = Color(0xFFA0AAC0)
                )
            },
            containerColor = Color(0xFF141923),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(22.dp))
        )
    }
}
