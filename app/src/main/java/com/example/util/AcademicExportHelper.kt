package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CitationEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.OfficeHourEntity
import com.example.data.local.entity.StudyNoteEntity
import com.example.ui.components.formatTime12Hour
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object AcademicExportHelper {

    private val icsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)

    /**
     * Generates a standards-compliant RFC-5545 .ics calendar payload containing
     * recurring weekly university lectures, lecturer office hours, and upcoming assessments/exams.
     */
    fun generateIcsCalendar(
        studentName: String,
        institution: String,
        lectures: List<LectureEntity>,
        officeHours: List<OfficeHourEntity>,
        assessments: List<AssessmentEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//AURA Academic Companion//$institution//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        sb.appendLine("X-WR-CALNAME:AURA Timetable - $studentName")

        val today = LocalDate.now()
        val stamp = LocalDateTime.now().format(icsDateTimeFormatter)

        // 1. Weekly Recurring Lectures
        lectures.forEach { lec ->
            val dow = DayOfWeek.of(lec.dayOfWeek.coerceIn(1, 7))
            val rruleDay = when (dow) {
                DayOfWeek.MONDAY -> "MO"
                DayOfWeek.TUESDAY -> "TU"
                DayOfWeek.WEDNESDAY -> "WE"
                DayOfWeek.THURSDAY -> "TH"
                DayOfWeek.FRIDAY -> "FR"
                DayOfWeek.SATURDAY -> "SA"
                DayOfWeek.SUNDAY -> "SU"
            }
            val firstDate = today.with(TemporalAdjusters.nextOrSame(dow))
            val start = parseLocalTimeSafe(lec.startTime, 8, 30)
            val end = parseLocalTimeSafe(lec.endTime, 10, 30)

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:aura-lecture-${lec.id}-${lec.courseCode.filter { it.isLetterOrDigit() }}@aura.edu")
            sb.appendLine("DTSTAMP:$stamp")
            sb.appendLine("DTSTART:${firstDate.atTime(start).format(icsDateTimeFormatter)}")
            sb.appendLine("DTEND:${firstDate.atTime(end).format(icsDateTimeFormatter)}")
            if (lec.isRecurring) {
                sb.appendLine("RRULE:FREQ=WEEKLY;BYDAY=$rruleDay;COUNT=16")
            }
            sb.appendLine("SUMMARY:${escapeIcs("${lec.courseCode}: ${lec.courseName}")}")
            sb.appendLine("LOCATION:${escapeIcs(lec.venue)}")
            sb.appendLine("DESCRIPTION:${escapeIcs("Lecturer: ${lec.lecturer}")}")
            if (lec.reminderEnabled) {
                sb.appendLine("BEGIN:VALARM")
                sb.appendLine("TRIGGER:-PT${lec.reminderMinutesBefore.coerceAtLeast(5)}M")
                sb.appendLine("ACTION:DISPLAY")
                sb.appendLine("DESCRIPTION:Upcoming Lecture - ${escapeIcs(lec.courseCode)}")
                sb.appendLine("END:VALARM")
            }
            sb.appendLine("END:VEVENT")
        }

        // 2. Weekly Lecturer Office Hours
        officeHours.forEach { oh ->
            val dow = DayOfWeek.of(oh.dayOfWeek.coerceIn(1, 7))
            val rruleDay = dow.name.take(2)
            val firstDate = today.with(TemporalAdjusters.nextOrSame(dow))
            val start = parseLocalTimeSafe(oh.startTime, 14, 0)
            val end = parseLocalTimeSafe(oh.endTime, 15, 30)

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:aura-officehour-${oh.id}@aura.edu")
            sb.appendLine("DTSTAMP:$stamp")
            sb.appendLine("DTSTART:${firstDate.atTime(start).format(icsDateTimeFormatter)}")
            sb.appendLine("DTEND:${firstDate.atTime(end).format(icsDateTimeFormatter)}")
            sb.appendLine("RRULE:FREQ=WEEKLY;BYDAY=$rruleDay;COUNT=14")
            sb.appendLine("SUMMARY:${escapeIcs("Office Hours: ${oh.instructorName} (${oh.courseCode})")}")
            sb.appendLine("LOCATION:${escapeIcs(oh.officeLocation)}")
            sb.appendLine("DESCRIPTION:${escapeIcs(oh.agendaNotes)}")
            sb.appendLine("END:VEVENT")
        }

        // 3. Upcoming Assessments & Examinations
        assessments.filter { it.status != "COMPLETED" }.forEach { item ->
            val dueDt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(item.dueEpochMillis),
                ZoneId.systemDefault()
            )
            val endDt = dueDt.plusHours(2)
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:aura-assessment-${item.id}@aura.edu")
            sb.appendLine("DTSTAMP:$stamp")
            sb.appendLine("DTSTART:${dueDt.format(icsDateTimeFormatter)}")
            sb.appendLine("DTEND:${endDt.format(icsDateTimeFormatter)}")
            sb.appendLine("SUMMARY:${escapeIcs("[${item.type.replace("_", " ")}] ${item.courseCode}: ${item.title}")}")
            sb.appendLine("DESCRIPTION:${escapeIcs(item.notes)}")
            sb.appendLine("END:VEVENT")
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    /**
     * Generates a clean, multi-section printable PDF containing the Student's Weekly Timetable,
     * Upcoming Examinations/Deadlines, and Pinned Formulas & Notes.
     */
    fun writeSemesterPdf(
        outputStream: OutputStream,
        studentName: String,
        institution: String,
        programme: String,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        notes: List<StudyNoteEntity>,
        citations: List<CitationEntity> = emptyList()
    ) {
        val doc = PdfDocument()
        try {
            val pageWidth = 595 // A4 standard points
            val pageHeight = 842
            var pageNum = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas
            var y = 48f

            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(12, 26, 52) }
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(10, 132, 255) }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(165, 216, 255)
                textSize = 11f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(10, 88, 202)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 30, 48)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(55, 65, 85)
                textSize = 10f
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 230, 245)
                strokeWidth = 1f
            }

            fun ensureSpace(requiredHeight: Float) {
                if (y + requiredHeight > pageHeight - 44f) {
                    doc.finishPage(page)
                    pageNum += 1
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 48f
                }
            }

            // Top Banner
            canvas.drawRoundRect(32f, 28f, pageWidth - 32f, 96f, 12f, 12f, headerBgPaint)
            canvas.drawText("AURA Academic Timetable & Study Sheet", 48f, 56f, titlePaint)
            canvas.drawText("$studentName • $programme • $institution", 48f, 76f, subtitlePaint)
            y = 120f

            // Section 1: Weekly Lecture Timetable
            canvas.drawText("1. Weekly Lecture Schedule", 36f, y, sectionPaint)
            y += 8f
            canvas.drawLine(36f, y, pageWidth - 36f, y, accentPaint)
            y += 18f

            if (lectures.isEmpty()) {
                canvas.drawText("No lectures currently scheduled.", 40f, y, bodyPaint)
                y += 20f
            } else {
                val sortedLecs = lectures.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
                sortedLecs.forEach { lec ->
                    ensureSpace(34f)
                    val dayLabel = DayOfWeek.of(lec.dayOfWeek.coerceIn(1, 7)).name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                    val timeRange = "${formatTime12Hour(lec.startTime)} - ${formatTime12Hour(lec.endTime)}"
                    canvas.drawText("$dayLabel ($timeRange)  •  ${lec.courseCode}: ${lec.courseName}", 40f, y, bodyBoldPaint)
                    y += 14f
                    canvas.drawText("Venue: ${lec.venue}   |   Lecturer: ${lec.lecturer.ifBlank { "TBA" }}", 48f, y, bodyPaint)
                    y += 10f
                    canvas.drawLine(40f, y, pageWidth - 40f, y, dividerPaint)
                    y += 14f
                }
            }

            y += 10f
            ensureSpace(50f)

            // Section 2: Upcoming Examinations & Assessments
            canvas.drawText("2. Upcoming Examinations & Deadlines", 36f, y, sectionPaint)
            y += 8f
            canvas.drawLine(36f, y, pageWidth - 36f, y, accentPaint)
            y += 18f

            val pending = assessments.filter { it.status != "COMPLETED" }.sortedBy { it.dueEpochMillis }
            if (pending.isEmpty()) {
                canvas.drawText("No pending assessments or exams.", 40f, y, bodyPaint)
                y += 20f
            } else {
                val fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy • h:mm a", Locale.US)
                pending.take(15).forEach { item ->
                    ensureSpace(30f)
                    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.dueEpochMillis), ZoneId.systemDefault())
                    canvas.drawText("[${item.type.replace("_", " ")}] ${item.courseCode} — ${item.title}", 40f, y, bodyBoldPaint)
                    y += 14f
                    canvas.drawText("Due: ${dt.format(fmt)}   |   Priority: ${item.priority}", 48f, y, bodyPaint)
                    y += 16f
                }
            }

            y += 10f
            ensureSpace(50f)

            // Section 3: High-Yield Formula & Study Vault Sheet
            canvas.drawText("3. Formula & Revision Vault Sheet", 36f, y, sectionPaint)
            y += 8f
            canvas.drawLine(36f, y, pageWidth - 36f, y, accentPaint)
            y += 18f

            if (notes.isEmpty()) {
                canvas.drawText("Save formulas or cheat-sheets in Notes Vault to include them here.", 40f, y, bodyPaint)
            } else {
                notes.take(12).forEach { note ->
                    ensureSpace(42f)
                    canvas.drawText("[${note.courseCode} • ${note.category}] ${note.title}", 40f, y, bodyBoldPaint)
                    y += 14f
                    val snippet = note.content.replace("\n", "  ").take(110)
                    canvas.drawText(snippet, 48f, y, bodyPaint)
                    y += 18f
                }
            }

            if (citations.isNotEmpty()) {
                y += 10f
                ensureSpace(50f)
                canvas.drawText("4. Academic References & Bibliography", 36f, y, sectionPaint)
                y += 8f
                canvas.drawLine(36f, y, pageWidth - 36f, y, accentPaint)
                y += 18f

                citations.take(10).forEach { cit ->
                    ensureSpace(34f)
                    canvas.drawText("[${cit.style} • ${cit.courseCode}] ${cit.formattedCitation.take(105)}", 40f, y, bodyPaint)
                    y += 16f
                }
            }

            doc.finishPage(page)
            doc.writeTo(outputStream)
        } catch (_: Throwable) {
            writePurePdf14(outputStream, studentName, institution, programme, lectures, assessments, notes, citations)
        } finally {
            try {
                doc.close()
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Standards-compliant pure PDF-1.4 writer used when Android's native Skia PdfDocument JNI
     * is unavailable (e.g. local JVM unit tests or headless environments).
     */
    private fun writePurePdf14(
        outputStream: OutputStream,
        studentName: String,
        institution: String,
        programme: String,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        notes: List<StudyNoteEntity>,
        citations: List<CitationEntity> = emptyList()
    ) {
        fun escPdf(text: String): String = text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .map { if (it.code in 32..126) it else '-' }
            .joinToString("")

        val lines = mutableListOf<String>()
        lines += "AURA Academic Timetable & Study Sheet"
        lines += "$studentName - $programme - $institution"
        lines += ""
        lines += "1. Weekly Lecture Schedule"
        if (lectures.isEmpty()) {
            lines += "  No lectures currently scheduled."
        } else {
            lectures.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })).take(18).forEach { lec ->
                val dayLabel = DayOfWeek.of(lec.dayOfWeek.coerceIn(1, 7)).name.take(3)
                lines += "  $dayLabel ${formatTime12Hour(lec.startTime)}-${formatTime12Hour(lec.endTime)} | ${lec.courseCode}: ${lec.courseName} (${lec.venue})"
            }
        }
        lines += ""
        lines += "2. Upcoming Examinations & Deadlines"
        val pending = assessments.filter { it.status != "COMPLETED" }.sortedBy { it.dueEpochMillis }
        if (pending.isEmpty()) {
            lines += "  No pending assessments or exams."
        } else {
            val fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy h:mm a", Locale.US)
            pending.take(12).forEach { item ->
                val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.dueEpochMillis), ZoneId.systemDefault())
                lines += "  [${item.type}] ${item.courseCode} - ${item.title} (${dt.format(fmt)})"
            }
        }
        lines += ""
        lines += "3. Formula & Revision Vault Sheet"
        if (notes.isEmpty()) {
            lines += "  Save formulas in Notes Vault to include them here."
        } else {
            notes.take(10).forEach { note ->
                lines += "  [${note.courseCode}] ${note.title}: ${note.content.replace("\n", " ").take(80)}"
            }
        }
        if (citations.isNotEmpty()) {
            lines += ""
            lines += "4. Academic Bibliography"
            citations.take(6).forEach { cit ->
                lines += "  [${cit.style}] ${cit.formattedCitation.take(90)}"
            }
        }

        val streamContent = buildString {
            appendLine("BT")
            appendLine("/F1 11 Tf")
            appendLine("42 790 Td")
            appendLine("15 TL")
            lines.take(46).forEach { line ->
                appendLine("(${escPdf(line)}) Tj T*")
            }
            appendLine("ET")
        }
        val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)

        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n$streamContent\nendstream\nendobj\n"
        )

        val header = "%PDF-1.4\n"
        val offsets = mutableListOf<Int>()
        val pdfBuilder = StringBuilder(header)
        for (obj in objects) {
            offsets += pdfBuilder.length
            pdfBuilder.append(obj)
        }
        val xrefStart = pdfBuilder.length
        pdfBuilder.append("xref\n0 ${objects.size + 1}\n")
        pdfBuilder.append("0000000000 65535 f \n")
        for (off in offsets) {
            pdfBuilder.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        pdfBuilder.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        pdfBuilder.append("startxref\n$xrefStart\n%%EOF\n")

        outputStream.write(pdfBuilder.toString().toByteArray(Charsets.US_ASCII))
    }

    fun exportAndSharePdf(
        context: Context,
        studentName: String,
        institution: String,
        programme: String,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        notes: List<StudyNoteEntity>,
        citations: List<CitationEntity> = emptyList()
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(dir, "AURA_Semester_Schedule_and_Formulas.pdf")
        FileOutputStream(pdfFile).use { out ->
            writeSemesterPdf(out, studentName, institution, programme, lectures, assessments, notes, citations)
        }
        shareFileViaProvider(context, pdfFile, "application/pdf", "Share AURA PDF Schedule")
        return pdfFile
    }

    fun exportAndShareIcs(
        context: Context,
        studentName: String,
        institution: String,
        lectures: List<LectureEntity>,
        officeHours: List<OfficeHourEntity>,
        assessments: List<AssessmentEntity>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val icsFile = File(dir, "AURA_University_Timetable.ics")
        val content = generateIcsCalendar(studentName, institution, lectures, officeHours, assessments)
        icsFile.writeText(content, Charsets.UTF_8)
        shareFileViaProvider(context, icsFile, "text/calendar", "Sync AURA .ICS Calendar")
        return icsFile
    }

    private fun shareFileViaProvider(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun parseLocalTimeSafe(time24: String, fallbackH: Int, fallbackM: Int): LocalTime {
        return try {
            val parts = time24.trim().split(":")
            LocalTime.of(
                parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: fallbackH,
                parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: fallbackM
            )
        } catch (_: Exception) {
            LocalTime.of(fallbackH, fallbackM)
        }
    }

    private fun escapeIcs(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
}
