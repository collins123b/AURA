package com.example.util

import com.example.data.local.entity.CitationEntity
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CitationStyle(val code: String, val label: String) {
    APA7("APA7", "APA 7th"),
    IEEE("IEEE", "IEEE"),
    MLA("MLA", "MLA 9th"),
    HARVARD("HARVARD", "Harvard");

    companion object {
        fun fromCode(code: String): CitationStyle =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: APA7
    }
}

enum class CitationSourceType(val code: String, val label: String) {
    JOURNAL("JOURNAL", "Journal Article"),
    BOOK("BOOK", "Book / Textbook"),
    WEBSITE("WEBSITE", "Website / Online"),
    LECTURE("LECTURE", "Lecture / Handout");

    companion object {
        fun fromCode(code: String): CitationSourceType =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: JOURNAL
    }
}

data class ParsedCitationDraft(
    val sourceType: String,
    val authors: String,
    val year: String,
    val title: String,
    val publicationOrPublisher: String,
    val volumeOrPages: String,
    val urlOrDoi: String
)

object CitationFormatter {

    /**
     * Formats an academic reference accurately into APA 7th, IEEE, MLA 9th, or Harvard style,
     * plus generates an inline in-text citation snippet.
     */
    fun formatCitation(
        styleCode: String,
        sourceTypeCode: String = "JOURNAL",
        authors: String,
        year: String,
        title: String,
        publicationOrPublisher: String = "",
        volumeOrPages: String = "",
        urlOrDoi: String = "",
        ieeeIndex: Int = 1
    ): String {
        val cleanAuthors = authors.trim().ifBlank { "Anonymous" }
        val cleanYear = year.trim().ifBlank { "n.d." }
        val cleanTitle = title.trim().removeSuffix(".")
        val cleanPub = publicationOrPublisher.trim().removeSuffix(".")
        val cleanVol = volumeOrPages.trim().removeSuffix(".")
        val cleanLink = normalizeLinkOrDoi(urlOrDoi.trim())

        return when (CitationStyle.fromCode(styleCode)) {
            CitationStyle.APA7 -> buildString {
                append("$cleanAuthors ($cleanYear). ")
                append("$cleanTitle.")
                if (cleanPub.isNotBlank()) append(" $cleanPub")
                if (cleanVol.isNotBlank()) append(", $cleanVol")
                if (cleanPub.isNotBlank() || cleanVol.isNotBlank()) append(".")
                if (cleanLink.isNotBlank()) append(" $cleanLink")
            }.replace("..", ".")

            CitationStyle.IEEE -> buildString {
                append("[$ieeeIndex] $cleanAuthors, \"$cleanTitle,\"")
                if (cleanPub.isNotBlank()) append(" $cleanPub,")
                if (cleanVol.isNotBlank()) append(" $cleanVol,")
                append(" $cleanYear.")
                if (cleanLink.isNotBlank()) append(" [Online]. Available: $cleanLink")
            }

            CitationStyle.MLA -> buildString {
                append("$cleanAuthors. ")
                if (sourceTypeCode == "BOOK") {
                    append("$cleanTitle. ")
                } else {
                    append("\"$cleanTitle.\" ")
                }
                if (cleanPub.isNotBlank()) append("$cleanPub, ")
                if (cleanVol.isNotBlank()) append("$cleanVol, ")
                append("$cleanYear.")
                if (cleanLink.isNotBlank()) append(" $cleanLink.")
            }.replace("..", ".")

            CitationStyle.HARVARD -> buildString {
                val accessed = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US))
                append("$cleanAuthors ($cleanYear) '$cleanTitle'")
                if (cleanPub.isNotBlank()) append(", $cleanPub")
                if (cleanVol.isNotBlank()) append(", $cleanVol")
                append(".")
                if (cleanLink.isNotBlank()) {
                    append(" Available at: $cleanLink (Accessed: $accessed).")
                }
            }
        }
    }

    fun formatInTextCitation(styleCode: String, authors: String, year: String, ieeeIndex: Int = 1): String {
        val firstSurname = authors
            .split(",", "&", "and", ";")
            .firstOrNull()
            ?.trim()
            ?.split(" ")
            ?.firstOrNull { it.length > 1 }
            ?: "Author"
        val hasMultiple = authors.contains("&") || authors.contains(";") || authors.contains(" et al", ignoreCase = true) ||
            authors.split(",").size > 2
        val authorTag = if (hasMultiple) "$firstSurname et al." else firstSurname
        val cleanYear = year.trim().ifBlank { "n.d." }

        return when (CitationStyle.fromCode(styleCode)) {
            CitationStyle.IEEE -> "[$ieeeIndex]"
            CitationStyle.MLA -> "($authorTag)"
            CitationStyle.APA7, CitationStyle.HARVARD -> "($authorTag, $cleanYear)"
        }
    }

    /**
     * Compiles a complete alphabetical (or IEEE numbered) Reference List / Bibliography
     * ready to paste into a university paper or assignment.
     */
    fun compileBibliography(citations: List<CitationEntity>, overrideStyleCode: String? = null): String {
        if (citations.isEmpty()) return ""
        val effectiveStyle = overrideStyleCode ?: citations.first().style
        val sorted = if (effectiveStyle.equals("IEEE", ignoreCase = true)) {
            citations
        } else {
            citations.sortedBy { it.authors.lowercase(Locale.US) }
        }
        return sorted.mapIndexed { index, item ->
            formatCitation(
                styleCode = effectiveStyle,
                sourceTypeCode = item.sourceType,
                authors = item.authors,
                year = item.year,
                title = item.title,
                publicationOrPublisher = item.publicationOrPublisher,
                volumeOrPages = item.volumeOrPages,
                urlOrDoi = item.urlOrDoi,
                ieeeIndex = index + 1
            )
        }.joinToString("\n\n")
    }

    /**
     * Deterministic smart parser that extracts Author, Year, Title, Journal/Publisher, and URL/DOI
     * from a pasted raw reference line, DOI, or URL even when offline.
     */
    fun parseRawSourceDeterministically(rawInput: String): ParsedCitationDraft {
        val text = rawInput.trim()
        val urlRegex = Regex("""(https?://\S+|10\.\d{4,9}/\S+)""", RegexOption.IGNORE_CASE)
        val foundUrl = urlRegex.find(text)?.value?.trimEnd('.', ',', ')').orEmpty()
        val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
        val foundYear = yearRegex.find(text)?.value ?: LocalDate.now().year.toString()

        val cleanedWithoutUrl = text.replace(foundUrl, "").trim()
        val isWebsite = foundUrl.startsWith("http", ignoreCase = true) && !foundUrl.contains("doi.org", ignoreCase = true)

        // Try splitting "Author (Year). Title. Publisher."
        val yearMatch = Regex("""\((19\d{2}|20\d{2})\)""").find(cleanedWithoutUrl)
        if (yearMatch != null) {
            val authorsPart = cleanedWithoutUrl.substring(0, yearMatch.range.first).trim().trimEnd(',', '.')
            val afterYear = cleanedWithoutUrl.substring(yearMatch.range.last + 1).trim().trimStart('.', ',', ':', '-', ' ')
            val segments = afterYear.split(". ").map { it.trim() }.filter { it.isNotBlank() }
            val titlePart = segments.getOrNull(0) ?: afterYear.ifBlank { "Untitled Academic Source" }
            val pubPart = segments.getOrNull(1).orEmpty()
            val volPart = segments.drop(2).joinToString(", ")
            return ParsedCitationDraft(
                sourceType = if (isWebsite) "WEBSITE" else "JOURNAL",
                authors = authorsPart.ifBlank { "Unknown Author" },
                year = yearMatch.groupValues[1],
                title = titlePart,
                publicationOrPublisher = pubPart,
                volumeOrPages = volPart,
                urlOrDoi = foundUrl
            )
        }

        // URL-only or freeform title fallback
        if (cleanedWithoutUrl.isBlank() && foundUrl.isNotBlank()) {
            val host = runCatching {
                java.net.URI(if (foundUrl.startsWith("10.")) "https://doi.org/$foundUrl" else foundUrl).host.orEmpty()
            }.getOrDefault("")
                .removePrefix("www.")
            val slug = foundUrl.substringAfterLast('/').substringBefore('?').replace('-', ' ').replace('_', ' ')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            return ParsedCitationDraft(
                sourceType = if (foundUrl.contains("10.")) "JOURNAL" else "WEBSITE",
                authors = host.substringBefore('.').replaceFirstChar { it.uppercase() }.ifBlank { "Editorial Board" },
                year = foundYear,
                title = slug.ifBlank { "Online Academic Reference" },
                publicationOrPublisher = host.ifBlank { "Academic Portal" },
                volumeOrPages = "",
                urlOrDoi = foundUrl
            )
        }

        val parts = cleanedWithoutUrl.split(" - ", " | ", ". ").map { it.trim() }.filter { it.isNotBlank() }
        return ParsedCitationDraft(
            sourceType = if (isWebsite) "WEBSITE" else "JOURNAL",
            authors = if (parts.size >= 2) parts[0] else "Academic Author",
            year = foundYear,
            title = if (parts.size >= 2) parts[1] else cleanedWithoutUrl.ifBlank { "Academic Reference" },
            publicationOrPublisher = parts.getOrNull(2).orEmpty(),
            volumeOrPages = parts.getOrNull(3).orEmpty(),
            urlOrDoi = foundUrl
        )
    }

    private fun normalizeLinkOrDoi(raw: String): String {
        if (raw.isBlank()) return ""
        return if (raw.startsWith("10.")) "https://doi.org/$raw" else raw
    }
}
