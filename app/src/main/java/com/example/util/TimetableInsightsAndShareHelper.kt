package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.local.entity.LectureEntity
import com.example.ui.components.formatTime12Hour
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.util.Locale

data class StudyGapWindow(
    val dayOfWeek: Int,
    val startTime24: String,
    val endTime24: String,
    val durationMinutes: Int,
    val afterCourseCode: String,
    val beforeCourseCode: String,
    val recommendation: String
)

data class LectureConflictPair(
    val dayOfWeek: Int,
    val firstCourseCode: String,
    val secondCourseCode: String,
    val overlapWindowText: String
)

object TimetableInsightsAndShareHelper {

    fun parseMinutesSinceMidnight(time24: String): Int {
        val parts = time24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return h * 60 + m
    }

    /**
     * Automatically identifies productive Free Study Windows (>= 30 mins and <= 360 mins)
     * between consecutive lectures on a given day.
     */
    fun findFreeStudyWindowsForDay(dayLectures: List<LectureEntity>, dayOfWeek: Int): List<StudyGapWindow> {
        if (dayLectures.size < 2) return emptyList()
        val sorted = dayLectures.sortedBy { parseMinutesSinceMidnight(it.startTime) }
        val gaps = mutableListOf<StudyGapWindow>()

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val endMins = parseMinutesSinceMidnight(current.endTime)
            val nextStartMins = parseMinutesSinceMidnight(next.startTime)
            val gap = nextStartMins - endMins
            if (gap in 30..360) {
                val suggestion = when {
                    gap >= 120 -> "Deep Library Focus Block + Slide Deck Review"
                    gap >= 60 -> "50m Pomodoro Focus + Assignment Milestone"
                    else -> "Quick SM-2 Flashcard Recall & Lecture Prep"
                }
                gaps.add(
                    StudyGapWindow(
                        dayOfWeek = dayOfWeek,
                        startTime24 = current.endTime,
                        endTime24 = next.startTime,
                        durationMinutes = gap,
                        afterCourseCode = current.courseCode,
                        beforeCourseCode = next.courseCode,
                        recommendation = suggestion
                    )
                )
            }
        }
        return gaps
    }

    /**
     * Detects any overlapping lecture time clashes on the same day.
     */
    fun detectScheduleClashes(lectures: List<LectureEntity>): List<LectureConflictPair> {
        val clashes = mutableListOf<LectureConflictPair>()
        val byDay = lectures.groupBy { it.dayOfWeek }
        byDay.forEach { (day, list) ->
            val sorted = list.sortedBy { parseMinutesSinceMidnight(it.startTime) }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                val aEnd = parseMinutesSinceMidnight(a.endTime)
                val bStart = parseMinutesSinceMidnight(b.startTime)
                if (bStart < aEnd) {
                    clashes.add(
                        LectureConflictPair(
                            dayOfWeek = day,
                            firstCourseCode = a.courseCode,
                            secondCourseCode = b.courseCode,
                            overlapWindowText = "${formatTime12Hour(b.startTime)} – ${formatTime12Hour(a.endTime)}"
                        )
                    )
                }
            }
        }
        return clashes
    }

    fun formatDurationLabel(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs}h"
            else -> "${mins}m"
        }
    }

    /**
     * Renders a high-resolution, iridescent dark-glass Weekly Timetable Poster Bitmap
     * suitable for sharing with classmates on WhatsApp / Telegram or saving to device.
     */
    fun renderWeeklyTimetablePosterBitmap(
        studentName: String,
        institution: String,
        semesterLabel: String,
        lectures: List<LectureEntity>
    ): Bitmap {
        val width = 1080
        val grouped = (1..6).associateWith { day ->
            lectures.filter { it.dayOfWeek == day }.sortedBy { parseMinutesSinceMidnight(it.startTime) }
        }.filterValues { it.isNotEmpty() }

        val activeDays = if (grouped.isEmpty()) listOf(1, 2, 3, 4, 5) else grouped.keys.toList()
        val totalRows = activeDays.sumOf { (grouped[it]?.size ?: 1) }
        val height = (310 + activeDays.size * 68 + totalRows * 86).coerceIn(780, 2600)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.rgb(7, 13, 28),
                    Color.rgb(12, 24, 48),
                    Color.rgb(8, 17, 36)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header glass card
        val headerRect = RectF(44f, 40f, width - 44f, 206f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 20, 36, 68) }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(140, 92, 225, 255)
        }
        canvas.drawRoundRect(headerRect, 34f, 34f, cardFill)
        canvas.drawRoundRect(headerRect, 34f, 34f, cardBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 218, 255)
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 205, 232)
            textSize = 21f
        }

        canvas.drawText("AURA Weekly Lecture Timetable", 78f, 104f, titlePaint)
        canvas.drawText("$studentName • $semesterLabel", 78f, 144f, subPaint)
        canvas.drawText("$institution • ${lectures.size} Scheduled Weekly Classes", 78f, 178f, metaPaint)

        var y = 248f
        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 225, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val courseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val courseDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(188, 208, 235)
            textSize = 20f
        }
        val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 17, 30, 56)
        }
        val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 132, 255)
        }

        activeDays.forEach { day ->
            val dayName = DayOfWeek.of(day.coerceIn(1, 7)).name.lowercase(Locale.US)
                .replaceFirstChar { it.titlecase(Locale.US) }
            canvas.drawText(dayName.uppercase(Locale.US), 54f, y + 28f, dayHeaderPaint)
            y += 44f

            val dayList = grouped[day].orEmpty()
            if (dayList.isEmpty()) {
                val emptyRect = RectF(50f, y, width - 50f, y + 66f)
                canvas.drawRoundRect(emptyRect, 20f, 20f, rowBgPaint)
                canvas.drawText("Free Day — Independent Study & Revision", 82f, y + 41f, courseDetailPaint)
                y += 84f
            } else {
                dayList.forEach { lec ->
                    val rect = RectF(50f, y, width - 50f, y + 74f)
                    canvas.drawRoundRect(rect, 20f, 20f, rowBgPaint)

                    val parsedColor = runCatching { Color.parseColor(lec.colorHex) }.getOrDefault(Color.rgb(10, 132, 255))
                    accentStripePaint.color = parsedColor
                    canvas.drawRoundRect(RectF(50f, y, 64f, y + 74f), 20f, 20f, accentStripePaint)

                    val timeStr = "${formatTime12Hour(lec.startTime)} – ${formatTime12Hour(lec.endTime)}"
                    canvas.drawText("${lec.courseCode}: ${lec.courseName.take(28)}", 84f, y + 32f, courseTitlePaint)
                    canvas.drawText("$timeStr  •  ${lec.venue.ifBlank { "Campus Hall" }}", 84f, y + 60f, courseDetailPaint)
                    y += 86f
                }
                y += 10f
            }
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 160, 198)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated with AURA Academic Companion • V 1.1.2", width / 2f, height - 22f, footerPaint)

        return bitmap
    }

    fun shareWeeklyTimetableImage(
        context: Context,
        studentName: String,
        institution: String,
        semesterLabel: String,
        lectures: List<LectureEntity>
    ): Boolean {
        return runCatching {
            val bitmap = renderWeeklyTimetablePosterBitmap(
                studentName = studentName,
                institution = institution,
                semesterLabel = semesterLabel,
                lectures = lectures
            )
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val imageFile = File(exportDir, "AURA_Weekly_Timetable.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My Weekly Lecture Timetable ($studentName)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share Weekly Timetable Image")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
