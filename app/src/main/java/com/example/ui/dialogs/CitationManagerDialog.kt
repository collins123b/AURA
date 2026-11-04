package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CitationEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.performAuraHaptic
import com.example.ui.theme.LocalAuraColors
import com.example.util.CitationFormatter
import com.example.util.CitationSourceType
import com.example.util.CitationStyle
import java.time.LocalDate

@Composable
fun CitationManagerDialog(
    viewModel: AuraViewModel,
    defaultCourseCode: String = "General",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val savedCitations by viewModel.citations.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val isParsing by viewModel.isParsingCitation.collectAsState()

    var selectedStyle by remember { mutableStateOf(CitationStyle.APA7) }
    var selectedSourceType by remember { mutableStateOf(CitationSourceType.JOURNAL) }
    var courseCode by remember { mutableStateOf(defaultCourseCode.ifBlank { courses.firstOrNull()?.code ?: "General" }) }

    var smartParseInput by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var title by remember { mutableStateOf("") }
    var publicationOrPublisher by remember { mutableStateOf("") }
    var volumeOrPages by remember { mutableStateOf("") }
    var urlOrDoi by remember { mutableStateOf("") }

    val livePreview = remember(
        selectedStyle,
        selectedSourceType,
        authors,
        year,
        title,
        publicationOrPublisher,
        volumeOrPages,
        urlOrDoi,
        savedCitations.size
    ) {
        if (title.isBlank() && authors.isBlank()) {
            ""
        } else {
            CitationFormatter.formatCitation(
                styleCode = selectedStyle.code,
                sourceTypeCode = selectedSourceType.code,
                authors = authors,
                year = year,
                title = title,
                publicationOrPublisher = publicationOrPublisher,
                volumeOrPages = volumeOrPages,
                urlOrDoi = urlOrDoi,
                ieeeIndex = savedCitations.size + 1
            )
        }
    }

    val liveInText = remember(selectedStyle, authors, year, savedCitations.size) {
        if (authors.isBlank()) ""
        else CitationFormatter.formatInTextCitation(selectedStyle.code, authors, year, savedCitations.size + 1)
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
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academic Citation & Bibliography",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "APA 7th • IEEE • MLA 9th • Harvard Reference Builder",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Citation Formatter", tint = LocalAuraColors.current.textMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("citation_manager_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Style Selector Chips
                Text(
                    text = "Citation Style",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CitationStyle.entries.forEach { style ->
                        val active = selectedStyle == style
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) LocalAuraColors.current.primaryAccent else Color(0x1EFFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (active) LocalAuraColors.current.secondaryAccent else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    performAuraHaptic(view)
                                    selectedStyle = style
                                }
                                .testTag("citation_style_${style.code}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else LocalAuraColors.current.textSecondary
                                )
                            }
                        }
                    }
                }

                // 2. AI Smart Parse Card (URL / DOI / Raw Citation Text)
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LocalAuraColors.current.aiAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Parse URL, DOI, or Paper Reference",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                        }
                        OutlinedTextField(
                            value = smartParseInput,
                            onValueChange = { smartParseInput = it },
                            placeholder = {
                                Text(
                                    "e.g. Cormen, T. H. (2022). Introduction to Algorithms. MIT Press.",
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("citation_smart_parse_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalAuraColors.current.aiAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            AuraGlassButton(
                                text = if (isParsing) "Parsing..." else "Smart Auto-Fill",
                                icon = Icons.Default.AutoAwesome,
                                isPrimary = true,
                                enabled = smartParseInput.isNotBlank() && !isParsing,
                                onClick = {
                                    viewModel.parseRawCitationSource(smartParseInput) { parsed ->
                                        selectedSourceType = CitationSourceType.fromCode(parsed.sourceType)
                                        authors = parsed.authors
                                        year = parsed.year
                                        title = parsed.title
                                        publicationOrPublisher = parsed.publicationOrPublisher
                                        volumeOrPages = parsed.volumeOrPages
                                        urlOrDoi = parsed.urlOrDoi
                                    }
                                },
                                testTag = "citation_smart_parse_button"
                            )
                        }
                    }
                }

                // 3. Source Type Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CitationSourceType.entries.toList()) { srcType ->
                        val active = selectedSourceType == srcType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) LocalAuraColors.current.secondaryAccent.copy(alpha = 0.22f) else Color(0x18FFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (active) LocalAuraColors.current.secondaryAccent else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier.clickable { selectedSourceType = srcType }
                        ) {
                            Text(
                                text = srcType.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 4. Structured Source Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authors,
                        onValueChange = { authors = it },
                        label = { Text("Author(s) (e.g. Lamport, L.)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.7f)
                            .testTag("citation_authors_input")
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("citation_year_input")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Article, Book, or Webpage Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("citation_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = publicationOrPublisher,
                        onValueChange = { publicationOrPublisher = it },
                        label = {
                            Text(
                                when (selectedSourceType) {
                                    CitationSourceType.BOOK -> "Publisher"
                                    CitationSourceType.WEBSITE -> "Site Name"
                                    else -> "Journal / Course"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = volumeOrPages,
                        onValueChange = { volumeOrPages = it },
                        label = { Text("Vol / Pages") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = urlOrDoi,
                        onValueChange = { urlOrDoi = it },
                        label = { Text("DOI or URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // 5. Live Formatted Preview + Save Reference Button
                if (livePreview.isNotBlank()) {
                    AuraCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = LocalAuraColors.current.secondaryAccent.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuraBadge(
                                    text = "${selectedStyle.label} PREVIEW",
                                    backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                                    textColor = LocalAuraColors.current.secondaryAccent
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (liveInText.isNotBlank()) {
                                    AuraBadge(
                                        text = "In-text: $liveInText",
                                        backgroundColor = Color(0x22FFFFFF),
                                        textColor = LocalAuraColors.current.warning
                                    )
                                }
                            }
                            Text(
                                text = livePreview,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = LocalAuraColors.current.textPrimary,
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AuraGlassButton(
                                    text = "Copy",
                                    icon = Icons.Default.ContentCopy,
                                    onClick = {
                                        copyTextToClipboard(context, "Citation", livePreview)
                                        Toast.makeText(context, "Copied ${selectedStyle.label} citation!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AuraGlassButton(
                                    text = "Save to Bibliography",
                                    icon = Icons.Default.Add,
                                    isPrimary = true,
                                    onClick = {
                                        performAuraHaptic(view)
                                        viewModel.saveCitation(
                                            CitationEntity(
                                                courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                                                sourceType = selectedSourceType.code,
                                                authors = authors.trim().ifBlank { "Anonymous" },
                                                year = year.trim().ifBlank { "n.d." },
                                                title = title.trim().ifBlank { "Untitled Reference" },
                                                publicationOrPublisher = publicationOrPublisher.trim(),
                                                volumeOrPages = volumeOrPages.trim(),
                                                urlOrDoi = urlOrDoi.trim(),
                                                style = selectedStyle.code,
                                                formattedCitation = livePreview
                                            )
                                        )
                                        title = ""
                                        authors = ""
                                        publicationOrPublisher = ""
                                        volumeOrPages = ""
                                        urlOrDoi = ""
                                        smartParseInput = ""
                                        Toast.makeText(context, "Saved reference to Bibliography!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.4f),
                                    testTag = "save_citation_button"
                                )
                            }
                        }
                    }
                }

                // 6. Saved Reference List & 1-Tap Copy Full Alphabetical Bibliography
                if (savedCitations.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saved Bibliography (${savedCitations.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = LocalAuraColors.current.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AuraGlassButton(
                            text = "Copy All (${selectedStyle.label})",
                            icon = Icons.Default.ContentCopy,
                            isPrimary = true,
                            onClick = {
                                val fullBib = CitationFormatter.compileBibliography(savedCitations, selectedStyle.code)
                                copyTextToClipboard(context, "AURA Bibliography", fullBib)
                                Toast.makeText(
                                    context,
                                    "Copied ${savedCitations.size} formatted ${selectedStyle.label} references!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            testTag = "copy_bibliography_button"
                        )
                    }

                    savedCitations.forEachIndexed { idx, item ->
                        val restyled = CitationFormatter.formatCitation(
                            styleCode = selectedStyle.code,
                            sourceTypeCode = item.sourceType,
                            authors = item.authors,
                            year = item.year,
                            title = item.title,
                            publicationOrPublisher = item.publicationOrPublisher,
                            volumeOrPages = item.volumeOrPages,
                            urlOrDoi = item.urlOrDoi,
                            ieeeIndex = idx + 1
                        )
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(text = item.courseCode)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AuraBadge(
                                        text = CitationFormatter.formatInTextCitation(
                                            selectedStyle.code,
                                            item.authors,
                                            item.year,
                                            idx + 1
                                        ),
                                        backgroundColor = Color(0x1EFFFFFF),
                                        textColor = LocalAuraColors.current.secondaryAccent
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            copyTextToClipboard(context, "Academic Citation", restyled)
                                            Toast.makeText(context, "Reference copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Citation",
                                            tint = LocalAuraColors.current.secondaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCitation(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Citation",
                                            tint = LocalAuraColors.current.danger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = restyled,
                                    fontSize = 11.sp,
                                    color = LocalAuraColors.current.textPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Done",
                onClick = onDismiss,
                testTag = "close_citation_dialog_button"
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

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
