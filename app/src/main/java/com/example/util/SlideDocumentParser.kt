package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.local.entity.CourseResourceEntity
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class SlideDeckPage(
    val pageNumber: Int, // 1-indexed
    val title: String,
    val body: String,
    val speakerNotes: String = ""
)

data class ImportedResourcePayload(
    val title: String,
    val originalFileName: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val fileType: String, // "PDF", "PPTX", "DOCX", "PPT", "DOC", "TXT", "IMAGE", "OTHER"
    val mimeType: String,
    val totalPages: Int,
    val serializedText: String
)

object SlideDocumentParser {

    private const val PAGE_DELIM = "\n===AURA_SLIDE_PAGE===\n"
    private const val FIELD_DELIM = "\n---AURA_SLIDE_FIELD---\n"

    // Safe cap well below Android SQLite CursorWindow's 2 MB row limit; full decks live in sidecar file
    const val MAX_SQLITE_SERIALIZED_CHARS = 180_000
    private const val MAX_PDF_SCAN_BYTES = 4 * 1024 * 1024 // 4 MB bounded text-stream scan
    private const val MAX_BINARY_SAMPLE_BYTES = 2 * 1024 * 1024 // 2 MB bounded sample for legacy/generic files
    private const val MAX_XML_ENTRY_BYTES = 1_500_000 // 1.5 MB per OOXML slide/document entry
    private const val MAX_SLIDES_PER_DECK = 400

    private fun sidecarPagesFile(localFilePath: String): File =
        File("$localFilePath.aura_pages.txt")

    fun truncateForSqlite(serialized: String): String {
        if (serialized.length <= MAX_SQLITE_SERIALIZED_CHARS) return serialized
        // Truncate cleanly at a page boundary if possible so deserializePages stays valid even without sidecar
        val cutRegion = serialized.substring(0, MAX_SQLITE_SERIALIZED_CHARS)
        val lastBoundary = cutRegion.lastIndexOf(PAGE_DELIM)
        return if (lastBoundary > 2048) {
            cutRegion.substring(0, lastBoundary)
        } else {
            cutRegion
        }
    }

    fun loadPagesForResource(resource: CourseResourceEntity): List<SlideDeckPage> {
        val rawSerialized = try {
            if (resource.localFilePath.isNotBlank()) {
                val sidecar = sidecarPagesFile(resource.localFilePath)
                if (sidecar.exists() && sidecar.length() in 1..(16L * 1024L * 1024L)) {
                    sidecar.readText(Charsets.UTF_8)
                } else {
                    resource.extractedText
                }
            } else {
                resource.extractedText
            }
        } catch (_: Throwable) {
            resource.extractedText
        }
        return deserializePages(rawSerialized, resource.title)
    }

    fun loadPlainTextForResource(resource: CourseResourceEntity): String {
        return try {
            loadPagesForResource(resource).joinToString("\n\n") { page ->
                buildString {
                    appendLine(page.title)
                    appendLine(page.body)
                    if (page.speakerNotes.isNotBlank()) appendLine(page.speakerNotes)
                }
            }
        } catch (_: Throwable) {
            plainSearchableText(resource.extractedText)
        }
    }

    fun importDocumentFromUri(
        context: Context,
        uri: Uri,
        customTitle: String = ""
    ): ImportedResourcePayload? {
        return try {
            val resolver = context.contentResolver

            // Best-effort persistable URI read grant (silently ignored when picker URI isn't persistable)
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {
            }

            var displayName = "Course_Slide_${System.currentTimeMillis()}"
            var reportedSize = 0L

            try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                                val candidate = cursor.getString(nameIdx)
                                if (!candidate.isNullOrBlank()) displayName = candidate
                            }
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                                reportedSize = cursor.getLong(sizeIdx).coerceAtLeast(0L)
                            }
                        }
                    }
            } catch (_: Throwable) {
                uri.lastPathSegment?.takeIf { it.isNotBlank() }?.let { seg ->
                    displayName = seg.substringAfterLast('/')
                }
            }

            val rawMime = try {
                resolver.getType(uri)
            } catch (_: Throwable) {
                null
            }
            var detectedMime = rawMime?.takeIf { it.isNotBlank() } ?: guessMimeTypeFromName(displayName)

            // Ensure extension exists on sanitized filename when displayName lacked one (e.g. cloud provider IDs)
            if (!displayName.contains('.')) {
                val ext = extensionForMime(detectedMime)
                if (ext.isNotEmpty()) {
                    displayName = "$displayName.$ext"
                }
            }

            val dir = File(context.filesDir, "course_resources").apply { mkdirs() }
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(80).ifBlank { "slide_deck.pdf" }
            val destFile = File(dir, "res_${System.currentTimeMillis()}_$sanitized")

            val copiedOk = copyUriToLocalFileSafely(resolver, uri, destFile) { fallbackMime ->
                if (fallbackMime.isNotBlank()) detectedMime = fallbackMime
            }

            if (!copiedOk || !destFile.exists()) return null

            val actualSize = if (destFile.length() > 0L) destFile.length() else reportedSize
            parseLocalFile(
                file = destFile,
                originalFileName = displayName,
                mimeType = detectedMime,
                customTitle = customTitle
            ).copy(fileSizeBytes = actualSize)
        } catch (_: Throwable) {
            null
        }
    }

    private fun copyUriToLocalFileSafely(
        resolver: ContentResolver,
        uri: Uri,
        destFile: File,
        onResolvedMime: (String) -> Unit
    ): Boolean {
        // Primary stream copy (works for local files, Downloads, MediaStore, SAF documents)
        val directCopy = try {
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().buffered(16 * 1024).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) output.write(buffer, 0, read)
                    }
                    output.flush()
                }
                true
            } ?: false
        } catch (_: Throwable) {
            false
        }
        if (directCopy && destFile.exists()) return true

        // Fallback for virtual cloud documents (e.g. Google Slides / Docs exported via SAF as PDF)
        for (preferredMime in listOf("application/pdf", "*/*")) {
            val typedCopy = try {
                resolver.openTypedAssetFileDescriptor(uri, preferredMime, null)?.use { afd ->
                    afd.createInputStream().use { input ->
                        destFile.outputStream().buffered(16 * 1024).use { output ->
                            input.copyTo(output, 16 * 1024)
                            output.flush()
                        }
                    }
                    if (preferredMime == "application/pdf") onResolvedMime("application/pdf")
                    true
                } ?: false
            } catch (_: Throwable) {
                false
            }
            if (typedCopy && destFile.exists()) return true
        }
        return false
    }

    private fun extensionForMime(mime: String): String {
        val m = mime.lowercase(Locale.US)
        return when {
            m.contains("pdf") -> "pdf"
            m.contains("presentationml") -> "pptx"
            m.contains("ms-powerpoint") -> "ppt"
            m.contains("wordprocessingml") -> "docx"
            m.contains("msword") -> "doc"
            m.startsWith("image/png") -> "png"
            m.startsWith("image/") -> "jpg"
            m.startsWith("text/") -> "txt"
            else -> ""
        }
    }

    fun parseLocalFile(
        file: File,
        originalFileName: String = file.name,
        mimeType: String = guessMimeTypeFromName(originalFileName),
        customTitle: String = ""
    ): ImportedResourcePayload {
        val fileType = detectFileTypeWithMagicHeader(file, originalFileName, mimeType)
        val cleanDefaultTitle = originalFileName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { "Course Resource" }
        val finalTitle = customTitle.trim().ifBlank { cleanDefaultTitle }

        val pages: List<SlideDeckPage> = try {
            when (fileType) {
                "PPTX" -> parsePptxFile(file)
                "DOCX" -> parseDocxFile(file)
                "PDF" -> parsePdfFile(file)
                "TXT" -> parsePlainTextOrMarkdown(readBoundedUtf8Text(file, MAX_BINARY_SAMPLE_BYTES), finalTitle)
                "PPT", "DOC" -> parseLegacyBinaryOffice(file, fileType, finalTitle)
                "IMAGE" -> parseImageSlide(file, finalTitle)
                else -> parseGenericFallback(file, originalFileName, finalTitle)
            }
        } catch (_: Throwable) {
            emptyList()
        }

        val safePages = pages.take(MAX_SLIDES_PER_DECK).ifEmpty {
            listOf(
                SlideDeckPage(
                    pageNumber = 1,
                    title = finalTitle,
                    body = "Imported $fileType resource ($originalFileName). Ready for visual reading, study notes, bookmarks, or external viewer."
                )
            )
        }

        val fullSerialized = try {
            serializePages(safePages)
        } catch (_: Throwable) {
            serializePages(
                listOf(
                    SlideDeckPage(
                        pageNumber = 1,
                        title = finalTitle,
                        body = "Imported $fileType resource ($originalFileName)."
                    )
                )
            )
        }

        // Write full serialized deck to sidecar file so even 300+ page textbooks never hit SQLite CursorWindow limits
        try {
            if (file.exists()) {
                sidecarPagesFile(file.absolutePath).writeText(fullSerialized, Charsets.UTF_8)
            }
        } catch (_: Throwable) {
        }

        val dbSafeSerialized = truncateForSqlite(fullSerialized)

        return ImportedResourcePayload(
            title = finalTitle,
            originalFileName = originalFileName,
            localFilePath = file.absolutePath,
            fileSizeBytes = file.length(),
            fileType = fileType,
            mimeType = mimeType,
            totalPages = safePages.size.coerceAtLeast(1),
            serializedText = dbSafeSerialized
        )
    }

    fun buildTextSlideDeckPayload(
        context: Context,
        title: String,
        rawContent: String
    ): ImportedResourcePayload {
        val dir = File(context.filesDir, "course_resources").apply { mkdirs() }
        val safeName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50).ifBlank { "Slide_Deck" }
        val destFile = File(dir, "deck_${System.currentTimeMillis()}_$safeName.md")
        try {
            destFile.writeText(rawContent, Charsets.UTF_8)
        } catch (_: Throwable) {
        }

        val pages = parsePlainTextOrMarkdown(rawContent, title).take(MAX_SLIDES_PER_DECK)
        val fullSerialized = serializePages(pages)
        try {
            sidecarPagesFile(destFile.absolutePath).writeText(fullSerialized, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
        return ImportedResourcePayload(
            title = title.trim().ifBlank { "Lecture Slide Deck" },
            originalFileName = "$safeName.md",
            localFilePath = destFile.absolutePath,
            fileSizeBytes = destFile.length(),
            fileType = "TXT",
            mimeType = "text/markdown",
            totalPages = pages.size.coerceAtLeast(1),
            serializedText = truncateForSqlite(fullSerialized)
        )
    }

    private fun detectFileTypeWithMagicHeader(file: File, fileName: String, mimeType: String): String {
        val byNameOrMime = detectFileType(fileName, mimeType)
        if (byNameOrMime != "OTHER") return byNameOrMime

        // Inspect magic header bytes (%PDF- or PK ZIP OOXML) when cloud provider gives generic filename/MIME
        return try {
            if (!file.exists() || file.length() < 4L) return "OTHER"
            val magic = ByteArray(8)
            val n = FileInputStream(file).use { it.read(magic) }
            if (n >= 4 && magic[0] == '%'.code.toByte() && magic[1] == 'P'.code.toByte() &&
                magic[2] == 'D'.code.toByte() && magic[3] == 'F'.code.toByte()
            ) {
                "PDF"
            } else if (n >= 4 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()) {
                inspectZipMagicType(file) ?: "OTHER"
            } else {
                "OTHER"
            }
        } catch (_: Throwable) {
            "OTHER"
        }
    }

    private fun inspectZipMagicType(file: File): String? {
        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zis ->
                var checked = 0
                var entry = zis.nextEntry
                while (entry != null && checked < 40) {
                    val name = entry.name
                    if (name.startsWith("ppt/")) return "PPTX"
                    if (name.startsWith("word/")) return "DOCX"
                    zis.closeEntry()
                    entry = zis.nextEntry
                    checked++
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    fun detectFileType(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        val mime = mimeType.lowercase(Locale.US)
        return when {
            ext == "pdf" || mime.contains("pdf") -> "PDF"
            ext == "pptx" || mime.contains("presentationml") -> "PPTX"
            ext == "ppt" || mime.contains("ms-powerpoint") -> "PPT"
            ext == "docx" || mime.contains("wordprocessingml") -> "DOCX"
            ext == "doc" || mime.contains("msword") -> "DOC"
            ext in setOf("txt", "md", "csv", "rtf") || mime.startsWith("text/") -> "TXT"
            ext in setOf("png", "jpg", "jpeg", "webp") || mime.startsWith("image/") -> "IMAGE"
            else -> "OTHER"
        }
    }

    fun guessMimeTypeFromName(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "pdf" -> "application/pdf"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "txt", "md" -> "text/plain"
            "csv" -> "text/csv"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    private fun readBoundedBytes(file: File, maxBytes: Int): ByteArray {
        if (!file.exists() || file.length() <= 0L) return ByteArray(0)
        val targetLen = minOf(file.length(), maxBytes.toLong()).toInt()
        if (targetLen <= 0) return ByteArray(0)
        return try {
            FileInputStream(file).use { fis ->
                val out = ByteArray(targetLen)
                var offset = 0
                while (offset < targetLen) {
                    val read = fis.read(out, offset, targetLen - offset)
                    if (read <= 0) break
                    offset += read
                }
                if (offset == targetLen) out else out.copyOf(offset)
            }
        } catch (_: Throwable) {
            ByteArray(0)
        }
    }

    private fun readBoundedUtf8Text(file: File, maxBytes: Int): String {
        val bytes = readBoundedBytes(file, maxBytes)
        return if (bytes.isEmpty()) "" else String(bytes, Charsets.UTF_8)
    }

    private fun readEntryBounded(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream(minOf(maxBytes, 32 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (total < maxBytes) {
            val toRead = minOf(buffer.size, maxBytes - total)
            val count = input.read(buffer, 0, toRead)
            if (count <= 0) break
            out.write(buffer, 0, count)
            total += count
        }
        return out.toByteArray()
    }

    /**
     * Parses a PowerPoint (.pptx) OOXML ZIP archive into ordered slides + speaker notes.
     */
    fun parsePptxBytes(bytes: ByteArray): List<SlideDeckPage> {
        if (bytes.isEmpty()) return emptyList()
        return try {
            parsePptxStream(ByteArrayInputStream(bytes))
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parsePptxFile(file: File): List<SlideDeckPage> {
        if (!file.exists() || file.length() <= 0L) return emptyList()
        return try {
            BufferedInputStream(FileInputStream(file), 32 * 1024).use { stream ->
                parsePptxStream(stream)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parsePptxStream(rawStream: InputStream): List<SlideDeckPage> {
        val slideXmlMap = mutableMapOf<Int, ByteArray>()
        val notesXmlMap = mutableMapOf<Int, ByteArray>()
        val slideRegex = Regex("^ppt/slides/slide(\\d+)\\.xml$")
        val notesRegex = Regex("^ppt/notesSlides/notesSlide(\\d+)\\.xml$")

        try {
            ZipInputStream(rawStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val slideMatch = slideRegex.matchEntire(entry.name)
                        val notesMatch = notesRegex.matchEntire(entry.name)
                        if (slideMatch != null && slideXmlMap.size < MAX_SLIDES_PER_DECK) {
                            val idx = slideMatch.groupValues[1].toIntOrNull() ?: 0
                            if (idx > 0) slideXmlMap[idx] = readEntryBounded(zis, MAX_XML_ENTRY_BYTES)
                        } else if (notesMatch != null && notesXmlMap.size < MAX_SLIDES_PER_DECK) {
                            val idx = notesMatch.groupValues[1].toIntOrNull() ?: 0
                            if (idx > 0) notesXmlMap[idx] = readEntryBounded(zis, MAX_XML_ENTRY_BYTES)
                        }
                    }
                    try {
                        zis.closeEntry()
                    } catch (_: Throwable) {
                    }
                    entry = try {
                        zis.nextEntry
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        } catch (_: Throwable) {
        }

        if (slideXmlMap.isEmpty()) return emptyList()

        return slideXmlMap.keys.sorted().mapIndexed { listIdx, slideNumber ->
            val paragraphs = extractXmlParagraphs(
                xmlBytes = slideXmlMap.getValue(slideNumber),
                paragraphLocalName = "p",
                textLocalName = "t"
            )
            val notesParagraphs = notesXmlMap[slideNumber]?.let {
                extractXmlParagraphs(it, paragraphLocalName = "p", textLocalName = "t")
                    .filterNot { line -> line.trim().all { ch -> ch.isDigit() } }
            }.orEmpty()

            val slideTitle = paragraphs.firstOrNull()?.take(90) ?: "Slide ${listIdx + 1}"
            val bulletLines = if (paragraphs.size > 1) {
                paragraphs.drop(1).joinToString("\n") { line ->
                    if (line.startsWith("•") || line.startsWith("-")) line else "• $line"
                }
            } else {
                paragraphs.firstOrNull().orEmpty()
            }

            SlideDeckPage(
                pageNumber = listIdx + 1,
                title = slideTitle,
                body = bulletLines.take(8_000).ifBlank { "(Visual diagram / media slide)" },
                speakerNotes = notesParagraphs.joinToString("\n").take(4_000)
            )
        }
    }

    /**
     * Parses a Word (.docx) OOXML ZIP archive (`word/document.xml`) into structured study pages.
     */
    fun parseDocxBytes(bytes: ByteArray): List<SlideDeckPage> {
        if (bytes.isEmpty()) return emptyList()
        return try {
            parseDocxStream(ByteArrayInputStream(bytes))
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseDocxFile(file: File): List<SlideDeckPage> {
        if (!file.exists() || file.length() <= 0L) return emptyList()
        return try {
            BufferedInputStream(FileInputStream(file), 32 * 1024).use { stream ->
                parseDocxStream(stream)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseDocxStream(rawStream: InputStream): List<SlideDeckPage> {
        var documentXml: ByteArray? = null
        try {
            ZipInputStream(rawStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == "word/document.xml") {
                        documentXml = readEntryBounded(zis, MAX_XML_ENTRY_BYTES * 2)
                        break
                    }
                    try {
                        zis.closeEntry()
                    } catch (_: Throwable) {
                    }
                    entry = try {
                        zis.nextEntry
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        } catch (_: Throwable) {
        }

        val xmlBytes = documentXml ?: return emptyList()
        val blocks = extractDocxBlocks(xmlBytes)
        if (blocks.isEmpty()) return emptyList()

        val pages = mutableListOf<SlideDeckPage>()
        var currentTitle = "Section 1"
        val currentLines = mutableListOf<String>()
        var currentCharCount = 0

        fun flushSection() {
            if (currentLines.isNotEmpty() && pages.size < MAX_SLIDES_PER_DECK) {
                pages.add(
                    SlideDeckPage(
                        pageNumber = pages.size + 1,
                        title = currentTitle,
                        body = currentLines.joinToString("\n\n").take(8_000)
                    )
                )
                currentLines.clear()
                currentCharCount = 0
            }
        }

        for ((isHeading, text) in blocks) {
            if (pages.size >= MAX_SLIDES_PER_DECK) break
            if (isHeading && currentLines.isNotEmpty() && currentCharCount >= 280) {
                flushSection()
                currentTitle = text.take(90)
            } else if (isHeading && currentLines.isEmpty()) {
                currentTitle = text.take(90)
            } else {
                if (currentCharCount + text.length > 1250 && currentLines.isNotEmpty()) {
                    val prevTitle = currentTitle
                    flushSection()
                    currentTitle = "$prevTitle (cont.)"
                }
                currentLines.add(text)
                currentCharCount += text.length
            }
        }
        flushSection()
        return pages
    }

    private fun extractDocxBlocks(xmlBytes: ByteArray): List<Pair<Boolean, String>> {
        val result = mutableListOf<Pair<Boolean, String>>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
            val paragraphs = doc.getElementsByTagName("*")
            val maxNodes = minOf(paragraphs.length, 25_000)
            for (i in 0 until maxNodes) {
                val node = paragraphs.item(i) ?: continue
                val tag = node.nodeName.substringAfter(':')
                if (tag == "p") {
                    var isHeading = false
                    val sb = StringBuilder()
                    collectDocxParagraphText(node, sb, depth = 0) { styleVal ->
                        if (styleVal.contains("Heading", ignoreCase = true) ||
                            styleVal.contains("Title", ignoreCase = true)
                        ) {
                            isHeading = true
                        }
                    }
                    val cleaned = sb.toString().trim()
                    if (cleaned.isNotBlank()) {
                        result.add(isHeading to cleaned)
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return result
    }

    private fun collectDocxParagraphText(
        node: Node,
        sb: StringBuilder,
        depth: Int,
        onStyleFound: (String) -> Unit
    ) {
        if (depth > 48 || sb.length > 16_000) return
        val children = node.childNodes ?: return
        for (i in 0 until children.length) {
            val child = children.item(i) ?: continue
            val local = child.nodeName.substringAfter(':')
            when (local) {
                "pStyle" -> {
                    if (child is Element) {
                        val styleVal = child.getAttribute("w:val").ifBlank { child.getAttribute("val") }
                        if (styleVal.isNotBlank()) onStyleFound(styleVal)
                    }
                }
                "t" -> sb.append(child.textContent ?: "")
                "tab" -> sb.append("    ")
                "br", "cr" -> sb.append("\n")
                else -> collectDocxParagraphText(child, sb, depth + 1, onStyleFound)
            }
        }
    }

    private fun extractXmlParagraphs(
        xmlBytes: ByteArray,
        paragraphLocalName: String,
        textLocalName: String
    ): List<String> {
        val lines = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
            val allNodes = doc.getElementsByTagName("*")
            val maxNodes = minOf(allNodes.length, 20_000)
            for (i in 0 until maxNodes) {
                val node = allNodes.item(i) ?: continue
                if (node.nodeName.substringAfter(':') == paragraphLocalName) {
                    val sb = StringBuilder()
                    collectTextRuns(node, textLocalName, sb, depth = 0)
                    val text = sb.toString().replace(Regex("\\s+"), " ").trim()
                    if (text.isNotBlank()) {
                        lines.add(text)
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return lines
    }

    private fun collectTextRuns(node: Node, textLocalName: String, sb: StringBuilder, depth: Int) {
        if (depth > 48 || sb.length > 16_000) return
        val children = node.childNodes ?: return
        for (i in 0 until children.length) {
            val child = children.item(i) ?: continue
            val local = child.nodeName.substringAfter(':')
            if (local == textLocalName) {
                sb.append(child.textContent ?: "")
            } else if (local == "br") {
                sb.append("\n")
            } else {
                collectTextRuns(child, textLocalName, sb, depth + 1)
            }
        }
    }

    /**
     * Parses a PDF file safely with bounded memory and zero regex recursion.
     * Uses PdfRenderer for exact native page count + iterative O(N) text stream extraction.
     */
    fun parsePdfFile(file: File): List<SlideDeckPage> {
        return try {
            val rendererPageCount = getPdfRendererPageCount(file)
            val boundedSample = readBoundedBytes(file, MAX_PDF_SCAN_BYTES)
            extractPdfTextPages(boundedSample, fallbackPageCount = rendererPageCount)
        } catch (_: Throwable) {
            val fallbackCount = getPdfRendererPageCount(file).coerceIn(1, MAX_SLIDES_PER_DECK)
            (0 until fallbackCount).map { idx ->
                SlideDeckPage(
                    pageNumber = idx + 1,
                    title = "PDF Slide ${idx + 1}",
                    body = "PDF Page ${idx + 1} of $fallbackCount — Visual slide rendered above."
                )
            }
        }
    }

    fun getPdfRendererPageCount(file: File): Int {
        if (!file.exists() || file.length() <= 0L) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount.coerceAtLeast(0)
                }
            }
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Renders a crisp high-DPI white-backed Bitmap for a specific 0-indexed PDF page.
     * Returns null safely if running in a non-native JVM test environment or if file is encrypted/invalid.
     */
    fun renderPdfPageBitmap(
        file: File,
        pageIndex: Int,
        targetWidthPx: Int = 1080
    ): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return null
                    val safeIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIdx).use { page ->
                        val ratio = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.35f
                        val width = targetWidthPx.coerceIn(480, 1920)
                        val height = (width * ratio).toInt().coerceIn(360, 2560)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Linear O(N) non-recursive PDF stream text extractor.
     * Completely eliminates StackOverflowError and OutOfMemoryError on image-heavy or large PDFs.
     */
    fun extractPdfTextPages(bytes: ByteArray, fallbackPageCount: Int = 0): List<SlideDeckPage> {
        if (bytes.isEmpty() && fallbackPageCount <= 0) return emptyList()
        return try {
            val scanLen = minOf(bytes.size, MAX_PDF_SCAN_BYTES)
            val latin = if (scanLen > 0) String(bytes, 0, scanLen, Charsets.ISO_8859_1) else ""
            val pageMarkerCount = countPdfPageMarkersIterative(latin)
            val totalPages = maxOf(fallbackPageCount, pageMarkerCount, 1).coerceAtMost(MAX_SLIDES_PER_DECK)

            val streamTexts = mutableListOf<String>()
            var searchPos = 0
            var streamsInspected = 0
            while (searchPos < latin.length && streamsInspected < 600 && streamTexts.size < totalPages + 24) {
                val streamPos = findStreamKeyword(latin, searchPos)
                if (streamPos < 0) break
                val lineBreakPos = latin.indexOf('\n', streamPos + 6)
                if (lineBreakPos < 0 || lineBreakPos - streamPos > 10) {
                    searchPos = streamPos + 6
                    continue
                }
                val dataStart = lineBreakPos + 1
                val endIdx = latin.indexOf("endstream", dataStart)
                if (endIdx <= dataStart) break

                streamsInspected++
                val headerWindow = latin.substring((streamPos - 260).coerceAtLeast(0), streamPos)
                val rawLen = endIdx - dataStart

                // Skip embedded images, fonts, metadata, or giant binary streams
                val isNonTextBinary = headerWindow.contains("/Subtype") &&
                    (headerWindow.contains("/Image") || headerWindow.contains("/XML")) ||
                    headerWindow.contains("/FontFile") ||
                    headerWindow.contains("/DCTDecode") ||
                    headerWindow.contains("/JPXDecode")

                if (!isNonTextBinary && rawLen in 1..(256 * 1024)) {
                    var dataEnd = endIdx
                    while (dataEnd > dataStart &&
                        (bytes[dataEnd - 1] == '\n'.code.toByte() || bytes[dataEnd - 1] == '\r'.code.toByte())
                    ) {
                        dataEnd--
                    }
                    val rawSlice = bytes.copyOfRange(dataStart, dataEnd)
                    val decoded = if (headerWindow.contains("FlateDecode")) {
                        inflateSafely(rawSlice)
                    } else {
                        String(rawSlice, Charsets.ISO_8859_1)
                    }
                    if (!decoded.isNullOrBlank()) {
                        val extracted = extractPdfTextOperatorsIterative(decoded)
                        if (extracted.isNotBlank()) {
                            streamTexts.add(extracted)
                        }
                    }
                }
                searchPos = endIdx + 9
            }

            (0 until totalPages).map { idx ->
                val textForPage = streamTexts.getOrNull(idx)?.trim().orEmpty()
                val firstLine = textForPage.lineSequence().firstOrNull { it.isNotBlank() }?.take(85)
                SlideDeckPage(
                    pageNumber = idx + 1,
                    title = firstLine ?: "PDF Slide ${idx + 1}",
                    body = textForPage.take(6_000).ifBlank {
                        "PDF Page ${idx + 1} of $totalPages — Visual slide rendered above. Add personal takeaways below or bookmark this slide for revision."
                    }
                )
            }
        } catch (_: Throwable) {
            val safeTotal = fallbackPageCount.coerceAtLeast(1).coerceAtMost(MAX_SLIDES_PER_DECK)
            (0 until safeTotal).map { idx ->
                SlideDeckPage(
                    pageNumber = idx + 1,
                    title = "PDF Slide ${idx + 1}",
                    body = "PDF Page ${idx + 1} of $safeTotal — Visual slide rendered above."
                )
            }
        }
    }

    private fun findStreamKeyword(latin: String, fromIndex: Int): Int {
        var pos = fromIndex
        while (pos < latin.length) {
            val idx = latin.indexOf("stream", pos)
            if (idx < 0) return -1
            // Ignore "endstream" matches
            val precededByEnd = idx >= 3 && latin.startsWith("end", idx - 3)
            val nextCh = latin.getOrNull(idx + 6)
            if (!precededByEnd && (nextCh == '\n' || nextCh == '\r')) {
                return idx
            }
            pos = idx + 6
        }
        return -1
    }

    private fun countPdfPageMarkersIterative(latin: String): Int {
        var count = 0
        var idx = 0
        while (idx < latin.length) {
            val found = latin.indexOf("/Type", idx)
            if (found < 0) break
            var cursor = found + 5
            while (cursor < latin.length && latin[cursor].isWhitespace()) cursor++
            if (latin.startsWith("/Page", cursor)) {
                val after = latin.getOrNull(cursor + 5)
                if (after == null || (!after.isLetterOrDigit() && after != 's')) {
                    count++
                }
            }
            idx = found + 5
        }
        return count
    }

    private fun inflateSafely(compressed: ByteArray): String? {
        for (nowrap in listOf(false, true)) {
            val result = try {
                val inflater = Inflater(nowrap)
                try {
                    inflater.setInput(compressed)
                    val out = ByteArrayOutputStream(minOf(compressed.size * 2, 32 * 1024))
                    val buf = ByteArray(4096)
                    var guard = 0
                    while (!inflater.finished() && guard < 128) {
                        val count = inflater.inflate(buf)
                        if (count <= 0) break
                        out.write(buf, 0, count)
                        guard++
                    }
                    out.toString(Charsets.ISO_8859_1.name())
                } finally {
                    runCatching { inflater.end() }
                }
            } catch (_: Throwable) {
                null
            }
            if (!result.isNullOrBlank()) return result
        }
        return null
    }

    /**
     * Pure iterative O(N) PDF text operator parser.
     * Zero regex backtracking — guaranteed never to throw StackOverflowError regardless of input!
     */
    private fun extractPdfTextOperatorsIterative(contentStream: String): String {
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        val tokenBuf = StringBuilder()
        val len = minOf(contentStream.length, 300_000)
        var i = 0

        fun flushLine() {
            if (currentLine.isNotBlank() && lines.size < 250) {
                lines.add(currentLine.toString().trim())
            }
            currentLine.clear()
        }

        while (i < len && lines.size < 250) {
            val ch = contentStream[i]
            if (ch == '(') {
                // Parse PDF literal string (...) supporting nested parens and backslash escapes iteratively
                tokenBuf.clear()
                var depth = 1
                i++
                while (i < len && depth > 0) {
                    val c = contentStream[i]
                    if (c == '\\' && i + 1 < len) {
                        val next = contentStream[i + 1]
                        when (next) {
                            'n' -> {
                                tokenBuf.append('\n')
                                i += 2
                            }
                            'r' -> i += 2
                            't' -> {
                                tokenBuf.append(' ')
                                i += 2
                            }
                            '(', ')', '\\' -> {
                                tokenBuf.append(next)
                                i += 2
                            }
                            in '0'..'7' -> {
                                // Octal escape up to 3 digits
                                var oct = next.digitToInt()
                                var consumed = 2
                                if (i + 2 < len && contentStream[i + 2] in '0'..'7') {
                                    oct = oct * 8 + contentStream[i + 2].digitToInt()
                                    consumed = 3
                                    if (i + 3 < len && contentStream[i + 3] in '0'..'7') {
                                        oct = oct * 8 + contentStream[i + 3].digitToInt()
                                        consumed = 4
                                    }
                                }
                                if (oct in 32..126) tokenBuf.append(oct.toChar())
                                i += consumed
                            }
                            else -> {
                                tokenBuf.append(next)
                                i += 2
                            }
                        }
                    } else if (c == '(') {
                        depth++
                        if (tokenBuf.length < 2048) tokenBuf.append('(')
                        i++
                    } else if (c == ')') {
                        depth--
                        if (depth > 0 && tokenBuf.length < 2048) tokenBuf.append(')')
                        i++
                    } else {
                        if (tokenBuf.length < 2048) tokenBuf.append(c)
                        i++
                    }
                }
                val cleaned = tokenBuf.toString().trim()
                val printableCount = cleaned.count { it.code in 32..126 }
                if (cleaned.isNotEmpty() && printableCount * 2 >= cleaned.length) {
                    if (currentLine.isNotEmpty() && !currentLine.endsWith(" ")) {
                        currentLine.append(' ')
                    }
                    currentLine.append(cleaned)
                }
            } else if (ch == 'T' && i + 1 < len) {
                val next = contentStream[i + 1]
                val after = contentStream.getOrNull(i + 2)
                val prev = if (i > 0) contentStream[i - 1] else ' '
                val boundaryBefore = !prev.isLetterOrDigit()
                val boundaryAfter = after == null || !after.isLetterOrDigit()
                if (boundaryBefore && next == '*') {
                    flushLine()
                    i += 2
                } else if (boundaryBefore && boundaryAfter && (next == 'd' || next == 'D')) {
                    flushLine()
                    i += 2
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        flushLine()
        return lines.joinToString("\n")
    }

    fun parsePlainTextOrMarkdown(raw: String, fallbackTitle: String): List<SlideDeckPage> {
        val normalized = raw.replace("\r\n", "\n").trim()
        if (normalized.isBlank()) {
            return listOf(SlideDeckPage(1, fallbackTitle, "Empty text document."))
        }

        val rawChunks = if (normalized.contains("\n---\n")) {
            normalized.split("\n---\n").map { it.trim() }.filter { it.isNotBlank() }
        } else if (Regex("(?m)^#{1,2}\\s+").containsMatchIn(normalized)) {
            normalized.split(Regex("(?m)(?=^#{1,2}\\s+)")).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            normalized.chunked(1100)
        }

        return rawChunks.take(MAX_SLIDES_PER_DECK).mapIndexed { idx, chunk ->
            val chunkLines = chunk.lines()
            val first = chunkLines.firstOrNull()?.removePrefix("##")?.removePrefix("#")?.trim().orEmpty()
            val title = if (first.isNotBlank() && first.length <= 85 && chunkLines.size > 1) {
                first
            } else {
                "$fallbackTitle • Slide ${idx + 1}"
            }
            val body = if (first == title && chunkLines.size > 1) {
                chunkLines.drop(1).joinToString("\n").trim().ifBlank { chunk }
            } else {
                chunk
            }
            SlideDeckPage(
                pageNumber = idx + 1,
                title = title,
                body = body
            )
        }
    }

    private fun parseLegacyBinaryOffice(file: File, fileType: String, fallbackTitle: String): List<SlideDeckPage> {
        val bytes = readBoundedBytes(file, MAX_BINARY_SAMPLE_BYTES)
        if (bytes.isEmpty()) return emptyList()
        val extracted = extractReadableRunsFromBinary(bytes)
        if (extracted.isBlank()) {
            return listOf(
                SlideDeckPage(
                    pageNumber = 1,
                    title = "$fallbackTitle ($fileType)",
                    body = "Legacy Microsoft Office ($fileType) binary file imported (${formatFileSize(file.length())}).\nTap 'Open External' in the top bar to view native animations/formatting, or add your study notes right below."
                )
            )
        }
        return parsePlainTextOrMarkdown(extracted, fallbackTitle)
    }

    private fun extractReadableRunsFromBinary(bytes: ByteArray): String {
        val runs = mutableListOf<String>()
        val u16 = StringBuilder()
        var i = 0
        while (i + 1 < bytes.size && runs.size < 400) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt() and 0xFF
            if (hi == 0 && (lo in 32..126 || lo == 10)) {
                u16.append(lo.toChar())
                i += 2
            } else {
                if (u16.length >= 6) {
                    val candidate = u16.toString().trim()
                    if (candidate.any { it.isLetter() } && !isOfficeMetadataNoise(candidate)) {
                        runs.add(candidate)
                    }
                }
                u16.clear()
                i += 1
            }
        }
        if (u16.length >= 6 && runs.size < 400) {
            val candidate = u16.toString().trim()
            if (candidate.any { it.isLetter() } && !isOfficeMetadataNoise(candidate)) {
                runs.add(candidate)
            }
        }

        if (runs.sumOf { it.length } < 40) {
            val ascii = StringBuilder()
            for (b in bytes) {
                if (runs.size >= 400) break
                val c = b.toInt() and 0xFF
                if (c in 32..126 || c == 10) {
                    ascii.append(c.toChar())
                } else {
                    if (ascii.length >= 8) {
                        val s = ascii.toString().trim()
                        if (s.count { it.isLetter() } >= 4 && !isOfficeMetadataNoise(s)) {
                            runs.add(s)
                        }
                    }
                    ascii.clear()
                }
            }
        }
        return runs.distinct().joinToString("\n• ")
    }

    private fun isOfficeMetadataNoise(s: String): Boolean {
        val noisy = listOf(
            "Root Entry", "WordDocument", "PowerPoint Document",
            "SummaryInformation", "DocumentSummary", "Times New Roman", "Calibri", "Arial"
        )
        return noisy.any { s.contains(it, ignoreCase = true) }
    }

    private fun parseImageSlide(file: File, fallbackTitle: String): List<SlideDeckPage> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }
        val dims = if (opts.outWidth > 0 && opts.outHeight > 0) "${opts.outWidth}×${opts.outHeight}px" else "Slide Image"
        return listOf(
            SlideDeckPage(
                pageNumber = 1,
                title = fallbackTitle,
                body = "Visual Lecture Slide ($dims) — Pinch or double-tap to zoom, or tap 'AI 10-Q Pack' above to extract formulas, study notes, flashcards, and quiz questions."
            )
        )
    }

    private fun parseGenericFallback(file: File, fileName: String, title: String): List<SlideDeckPage> {
        val textAttempt = try {
            val sample = readBoundedBytes(file, MAX_BINARY_SAMPLE_BYTES)
            val printable = sample.count { (it.toInt() and 0xFF) in 9..126 }
            if (sample.isNotEmpty() && printable.toDouble() / sample.size > 0.75) {
                String(sample, Charsets.UTF_8)
            } else null
        } catch (_: Throwable) {
            null
        }

        return if (!textAttempt.isNullOrBlank()) {
            parsePlainTextOrMarkdown(textAttempt, title)
        } else {
            listOf(
                SlideDeckPage(
                    pageNumber = 1,
                    title = title,
                    body = "Course file '$fileName' (${formatFileSize(file.length())}) stored offline in AURA.\nTap 'Open External' above to launch with an installed viewer, or record your study notes below."
                )
            )
        }
    }

    fun serializePages(pages: List<SlideDeckPage>): String {
        return pages.joinToString(PAGE_DELIM) { page ->
            listOf(
                page.pageNumber.toString(),
                page.title.replace(FIELD_DELIM, " "),
                page.body.replace(FIELD_DELIM, " "),
                page.speakerNotes.replace(FIELD_DELIM, " ")
            ).joinToString(FIELD_DELIM)
        }
    }

    fun deserializePages(serialized: String, fallbackTitle: String = "Slide"): List<SlideDeckPage> {
        if (serialized.isBlank()) {
            return listOf(SlideDeckPage(1, fallbackTitle, "No extracted text available for this slide."))
        }
        if (!serialized.contains(FIELD_DELIM)) {
            return parsePlainTextOrMarkdown(serialized, fallbackTitle)
        }
        return serialized.split(PAGE_DELIM).mapIndexedNotNull { index, rawPage ->
            val parts = rawPage.split(FIELD_DELIM)
            if (parts.isEmpty()) null
            else SlideDeckPage(
                pageNumber = parts.getOrNull(0)?.toIntOrNull() ?: (index + 1),
                title = parts.getOrNull(1)?.ifBlank { "$fallbackTitle ${index + 1}" } ?: "$fallbackTitle ${index + 1}",
                body = parts.getOrNull(2).orEmpty(),
                speakerNotes = parts.getOrNull(3).orEmpty()
            )
        }.ifEmpty {
            listOf(SlideDeckPage(1, fallbackTitle, serialized))
        }
    }

    fun plainSearchableText(serialized: String): String {
        return deserializePages(serialized).joinToString("\n\n") { page ->
            buildString {
                appendLine(page.title)
                appendLine(page.body)
                if (page.speakerNotes.isNotBlank()) appendLine(page.speakerNotes)
            }
        }
    }

    fun deleteLocalResourceFile(localFilePath: String): Boolean {
        if (localFilePath.isBlank()) return false
        return try {
            runCatching { sidecarPagesFile(localFilePath).delete() }
            val f = File(localFilePath)
            if (f.exists()) f.delete() else false
        } catch (_: Throwable) {
            false
        }
    }

    fun openResourceExternally(context: Context, resource: CourseResourceEntity): Boolean {
        val file = File(resource.localFilePath)
        if (!file.exists()) return false
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resource.mimeType.ifBlank { guessMimeTypeFromName(file.name) })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open ${resource.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(Locale.US, "%.1f KB", kb.coerceAtLeast(0.1))
        } else {
            String.format(Locale.US, "%.2f MB", kb / 1024.0)
        }
    }
}
