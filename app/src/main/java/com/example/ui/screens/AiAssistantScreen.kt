package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.AuraViewModel
import com.example.ui.ChatMessage
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.theme.AuraVioletAccent
import com.example.ui.theme.LocalAuraColors

@Composable
fun AiAssistantScreen(
    viewModel: AuraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val suggestedPrompts = listOf(
        "What lectures do I have tomorrow?",
        "What assignments are due this week?",
        "Which course should I prioritize?",
        "When is my next examination?",
        "How many hours did I study this week?",
        "Help me organize tomorrow.",
        "Create a revision plan for my examinations."
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    FrostedGlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sanctuary Glass Top Header Card
            AuraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                cornerRadius = 24.dp,
                accentGlow = LocalAuraColors.current.aiAccent
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.study_sanctuary_nebula_1793569866560),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(if (LocalAuraColors.current.isDark) 0.28f else 0.12f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraGlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            size = 48.dp,
                            iconSize = 20.dp,
                            tint = LocalAuraColors.current.textPrimary,
                            testTag = "ai_chat_back_button"
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(LocalAuraColors.current.secondaryAccent, AuraVioletAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AURA AI Study Advisor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "Grounded in your live academic schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.secondaryAccent,
                                fontSize = 11.sp
                            )
                        }

                        AuraGlassIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            onClick = { viewModel.clearChat() },
                            size = 48.dp,
                            iconSize = 19.dp,
                            tint = LocalAuraColors.current.textSecondary,
                            testTag = "ai_clear_chat_button"
                        )
                    }
                }
            }

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages, key = { it.id }) { message ->
                    ChatMessageItem(message = message)
                }

                if (isAiThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = LocalAuraColors.current.aiAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AURA is analyzing your academic records...",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.aiAccent
                            )
                        }
                    }
                }
            }

            // Liquid Glass Suggested Prompts Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedPrompts) { prompt ->
                    AuraGlassButton(
                        text = prompt,
                        onClick = { viewModel.sendAiMessage(prompt) },
                        cornerRadius = 14.dp
                    )
                }
            }

            // Frosted Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x28FFFFFF))
                    .border(width = 0.8.dp, color = Color(0x45FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask AURA about your studies, schedule...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_text_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.aiAccent,
                        unfocusedBorderColor = LocalAuraColors.current.border,
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                AuraGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendAiMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    size = 48.dp,
                    iconSize = 20.dp,
                    isPrimary = true,
                    testTag = "ai_send_button"
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LocalAuraColors.current.aiAccent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LocalAuraColors.current.aiAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val bubbleShape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isUser) 18.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 18.dp
        )

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(
                    if (isUser) {
                        Brush.linearGradient(listOf(Color(0xE638BDF8), Color(0xEB0A84FF)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xD81B2A47), Color(0xCC121E36)))
                    }
                )
                .border(1.dp, Color(0x55FFFFFF), bubbleShape)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
