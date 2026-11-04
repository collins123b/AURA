package com.example.ui.dialogs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ai.AuraAiService
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.LocalAuraColors
import java.util.Locale

@Composable
fun OralVivaDialog(
    viewModel: AuraViewModel,
    courseCode: String,
    question: String,
    expectedAnswer: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current

    var spokenOrTypedAnswer by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isGrading by remember { mutableStateOf(false) }
    var statusHint by remember { mutableStateOf("Tap 'Speak Answer' or type your defense below.") }
    var evaluation by remember { mutableStateOf<AuraAiService.VivaEvaluationResult?>(null) }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                engine?.speak(question, TextToSpeech.QUEUE_FLUSH, null, "viva_q")
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }
    DisposableEffect(recognizer) {
        onDispose {
            runCatching { recognizer?.destroy() }
        }
    }

    fun beginSpeechCapture() {
        val sr = recognizer
        if (sr == null) {
            statusHint = "On-device speech recognizer unavailable — type your viva answer below."
            return
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusHint = "Listening... explain the concept aloud!"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                statusHint = "Voice capture finished or unavailable — feel free to edit or type below."
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val top = matches?.firstOrNull().orEmpty()
                if (top.isNotBlank()) {
                    spokenOrTypedAnswer = if (spokenOrTypedAnswer.isBlank()) top else "$spokenOrTypedAnswer $top"
                    statusHint = "Captured spoken defense! Tap 'Grade My Viva' when ready."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Defend your viva answer...")
        }
        runCatching { sr.startListening(intent) }.onFailure {
            isListening = false
            statusHint = "Could not start microphone — type your oral defense below."
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginSpeechCapture()
        } else {
            statusHint = "Microphone permission declined — you can type your viva answer directly below."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF50E182C),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(
                1.3.dp,
                Brush.verticalGradient(listOf(auraColors.aiAccent, auraColors.secondaryAccent))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("oral_viva_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraBadge(
                        text = "AI ORAL VIVA EXAMINER • $courseCode",
                        backgroundColor = auraColors.aiAccent.copy(alpha = 0.22f),
                        textColor = auraColors.aiAccent
                    )
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Oral Viva",
                        onClick = onDismiss,
                        size = 34.dp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x1EFFFFFF),
                    border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("EXAMINER PROMPT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = auraColors.secondaryAccent)
                            AuraGlassIconButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Question Aloud",
                                onClick = {
                                    ttsEngine?.speak(question, TextToSpeech.QUEUE_FLUSH, null, "viva_repeat")
                                },
                                size = 30.dp,
                                tint = auraColors.secondaryAccent
                            )
                        }
                        Text(
                            text = question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = statusHint,
                    fontSize = 11.sp,
                    color = if (isListening) AuraMintAccent else auraColors.textSecondary
                )

                OutlinedTextField(
                    value = spokenOrTypedAnswer,
                    onValueChange = { spokenOrTypedAnswer = it },
                    label = { Text("Your Spoken / Written Viva Defense") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("oral_viva_answer_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraGlassButton(
                        text = if (isListening) "Stop Mic" else "Speak Answer",
                        icon = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        containerColor = if (isListening) auraColors.danger else null,
                        onClick = {
                            if (isListening) {
                                runCatching { recognizer?.stopListening() }
                                isListening = false
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    beginSpeechCapture()
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "oral_viva_mic_button"
                    )

                    AuraGlassButton(
                        text = if (isGrading) "Grading..." else "Grade My Viva",
                        icon = Icons.Default.CheckCircle,
                        isPrimary = true,
                        enabled = !isGrading && spokenOrTypedAnswer.isNotBlank(),
                        onClick = {
                            isGrading = true
                            viewModel.evaluateOralViva(
                                courseCode = courseCode,
                                question = question,
                                expectedAnswer = expectedAnswer,
                                studentSpokenAnswer = spokenOrTypedAnswer
                            ) { result ->
                                evaluation = result
                                isGrading = false
                                ttsEngine?.speak(
                                    "Score ${result.scoreOutOfTen} out of 10. ${result.verdict}. ${result.followUpQuestion}",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "viva_grade"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "oral_viva_grade_button"
                    )
                }

                evaluation?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AuraMintAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AuraMintAccent.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Viva Score: ${res.scoreOutOfTen}/10 • ${res.verdict}",
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraMintAccent,
                                fontSize = 14.sp
                            )
                            Text("Strengths: ${res.strengths}", fontSize = 11.5.sp, color = Color.White)
                            Text("Improve: ${res.missingPoints}", fontSize = 11.5.sp, color = auraColors.textSecondary)
                            Text(
                                text = "Examiner Follow-Up: ${res.followUpQuestion}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = auraColors.secondaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
