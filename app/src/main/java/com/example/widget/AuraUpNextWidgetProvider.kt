package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.LectureEntity
import com.example.data.preferences.AuraPreferences
import com.example.ui.components.computeLeaveByTime12Hour
import com.example.ui.components.formatTime12Hour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class WidgetDisplayData(
    val badgeText: String,
    val statsText: String,
    val lectureTitle: String,
    val lectureMeta: String,
    val leaveByText: String,
    val accentColorHex: String = "#5CE1FF",
    val focusProgressFraction: Float = 0f,
    val isLiveNow: Boolean = false
)

class AuraUpNextWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshWidgetsInternal(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshWidgetsInternal(context.applicationContext, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_WIDGET_DESTINATION = "aura_widget_destination"
        const val DEST_HOME = "HOME"
        const val DEST_TIMETABLE = "TIMETABLE"
        const val DEST_TIMETABLE_WEEK = "TIMETABLE_WEEK"
        const val DEST_ADD_LECTURE = "ADD_LECTURE"
        const val DEST_FOCUS = "FOCUS"
        const val DEST_SLIDES = "SLIDES"
        const val DEST_FLASHCARDS = "FLASHCARDS"
        const val DEST_PRACTICE_QUIZ = "PRACTICE_QUIZ"
        const val DEST_TASKS = "TASKS"
        const val DEST_WAR_ROOM = "WAR_ROOM"
        const val DEST_AI_ADVISOR = "AI_ADVISOR"

        private val DAY_LABELS = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )

        fun buildWidgetSummary(
            lectures: List<LectureEntity>,
            pendingTasksCount: Int,
            weeklyFocusMinutes: Int,
            nowDayOfWeek: Int,
            nowTime24: String,
            weeklyGoalHours: Int = 20
        ): WidgetDisplayData {
            val weeklyHours = weeklyFocusMinutes / 60f
            val hoursStr = String.format(Locale.US, "%.1fh Focus", weeklyHours)
            val stats = "$pendingTasksCount Pending • $hoursStr"
            val safeGoal = weeklyGoalHours.coerceAtLeast(1)
            val focusFraction = (weeklyHours / safeGoal.toFloat()).coerceIn(0f, 1f)

            if (lectures.isEmpty()) {
                return WidgetDisplayData(
                    badgeText = "AURA • LIVE ACADEMIC",
                    statsText = stats,
                    lectureTitle = "No Lectures Scheduled Yet",
                    lectureMeta = "Add or AI-scan your semester timetable",
                    leaveByText = "Tap Schedule or Slides below to start",
                    accentColorHex = "#5CE1FF",
                    focusProgressFraction = focusFraction,
                    isLiveNow = false
                )
            }

            // 1. Check if a lecture is LIVE right now today
            val liveLecture = lectures
                .filter { it.dayOfWeek == nowDayOfWeek && nowTime24 >= it.startTime && nowTime24 < it.endTime }
                .minByOrNull { it.startTime }

            if (liveLecture != null) {
                val start12 = formatTime12Hour(liveLecture.startTime)
                val end12 = formatTime12Hour(liveLecture.endTime)
                val venue = liveLecture.venue.ifBlank { "Campus Hall" }
                return WidgetDisplayData(
                    badgeText = "AURA • LIVE CLASS NOW",
                    statsText = stats,
                    lectureTitle = "${liveLecture.courseCode} • ${liveLecture.courseName}",
                    lectureMeta = "Today $start12 – $end12 • $venue",
                    leaveByText = "In Session until $end12 • Open Slides",
                    accentColorHex = liveLecture.colorHex.ifBlank { "#34D399" },
                    focusProgressFraction = focusFraction,
                    isLiveNow = true
                )
            }

            // 2. Otherwise pick the next upcoming lecture today or later in the week
            val todayUpcoming = lectures
                .filter { it.dayOfWeek == nowDayOfWeek && it.startTime >= nowTime24 }
                .minByOrNull { it.startTime }

            val nextLecture = todayUpcoming
                ?: lectures.filter { it.dayOfWeek > nowDayOfWeek }
                    .minWithOrNull(compareBy({ it.dayOfWeek }, { it.startTime }))
                ?: lectures.minWithOrNull(compareBy({ it.dayOfWeek }, { it.startTime }))!!

            val dayLabel = if (nextLecture.dayOfWeek == nowDayOfWeek && todayUpcoming != null) {
                "Today"
            } else {
                DAY_LABELS[nextLecture.dayOfWeek] ?: "Week"
            }

            val start12 = formatTime12Hour(nextLecture.startTime)
            val end12 = formatTime12Hour(nextLecture.endTime)
            val venue = nextLecture.venue.ifBlank { "Campus Hall" }
            val walkMins = nextLecture.walkingMinutes.coerceIn(1, 90)
            val leaveBy12 = computeLeaveByTime12Hour(nextLecture.startTime, walkMins)

            return WidgetDisplayData(
                badgeText = "AURA • UP NEXT ($dayLabel)",
                statsText = stats,
                lectureTitle = "${nextLecture.courseCode} • ${nextLecture.courseName}",
                lectureMeta = "$dayLabel $start12 – $end12 • $venue",
                leaveByText = "Leave by $leaveBy12 (${walkMins}m walk)",
                accentColorHex = nextLecture.colorHex.ifBlank { "#5CE1FF" },
                focusProgressFraction = focusFraction,
                isLiveNow = false
            )
        }

        fun renderWidgetBackdropBitmap(
            widthPx: Int = 720,
            heightPx: Int = 340,
            accentColorInt: Int = Color.parseColor("#5CE1FF"),
            isLiveNow: Boolean = false,
            glassStyle: String = "FROSTED_OBSIDIAN"
        ): Bitmap {
            val w = widthPx.coerceIn(240, 1200)
            val h = heightPx.coerceIn(140, 800)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val cornerRadius = h * 0.16f
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)

            // 1. Customizable Liquid Glass Frost Base (Frosted Obsidian, Crystal Clear, or Daylight Pearl)
            val baseColors = when (glassStyle.uppercase()) {
                "CRYSTAL_CLEAR" -> intArrayOf(
                    Color.parseColor("#9E132B59"),
                    Color.parseColor("#8C0B1B3B"),
                    Color.parseColor("#9E071022")
                )
                "DAYLIGHT_PEARL" -> intArrayOf(
                    Color.parseColor("#EB1E3A6E"),
                    Color.parseColor("#EB152952"),
                    Color.parseColor("#F20E1D3B")
                )
                else -> intArrayOf(
                    Color.parseColor("#FF0E2146"),
                    Color.parseColor("#FF09142B"),
                    Color.parseColor("#FF060B18")
                )
            }
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    baseColors,
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, basePaint)

            // 2. Top-Left Electric Cyan Aurora Bloom
            val cyanBloom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val primaryGlow = if (isLiveNow) Color.parseColor("#5534D399") else Color.parseColor("#520A84FF")
                shader = RadialGradient(
                    w * 0.16f,
                    h * 0.12f,
                    w * 0.58f,
                    intArrayOf(primaryGlow, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, cyanBloom)

            // 3. Bottom-Right Course-Accent / Violet Prism Bloom
            val r = Color.red(accentColorInt)
            val g = Color.green(accentColorInt)
            val b = Color.blue(accentColorInt)
            val accentBloom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w * 0.86f,
                    h * 0.85f,
                    w * 0.52f,
                    intArrayOf(Color.argb(86, r, g, b), Color.parseColor("#22818CF8"), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, accentBloom)

            // 4. Diagonal Iridescent Prism Light Beam + Orbital Glass Arc
            val prismPath = Path().apply {
                moveTo(w * 0.24f, 0f)
                lineTo(w * 0.66f, 0f)
                lineTo(w * 0.34f, h.toFloat())
                lineTo(-w * 0.04f, h.toFloat())
                close()
            }
            val prismPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    w * 0.20f, 0f, w * 0.55f, h.toFloat(),
                    intArrayOf(
                        Color.parseColor("#1EFFFFFF"),
                        Color.parseColor("#125CE1FF"),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(prismPath, prismPaint)

            val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
                color = Color.parseColor("#2272E8FF")
            }
            canvas.drawCircle(w * 0.84f, h * 0.18f, h * 0.62f, orbitPaint)

            // 5. Delicate Top Liquid Glass Specular Sheen
            val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h * 0.38f,
                    intArrayOf(Color.parseColor("#28FFFFFF"), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h * 0.38f, sheenPaint)

            canvas.restore()

            // 5. Crisp Luminous Glass Rim Stroke
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.6f
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        Color.parseColor("#AA9BE8FF"),
                        Color.argb(145, r, g, b),
                        Color.parseColor("#66818CF8")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val inset = 1.4f
            canvas.drawRoundRect(
                RectF(inset, inset, w - inset, h - inset),
                cornerRadius,
                cornerRadius,
                borderPaint
            )

            return bmp
        }

        fun renderWidgetFocusRingBitmap(
            sizePx: Int = 156,
            progressFraction: Float,
            accentColorInt: Int = Color.parseColor("#5CE1FF")
        ): Bitmap {
            val s = sizePx.coerceIn(72, 320)
            val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val cx = s / 2f
            val cy = s / 2f
            val clamped = progressFraction.coerceIn(0f, 1f)
            val strokeW = s * 0.095f
            val radius = (s / 2f) - strokeW * 1.15f

            // Inner frosted glass disc
            val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy - radius * 0.25f, radius,
                    intArrayOf(Color.parseColor("#381B3564"), Color.parseColor("#55091224")),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, discPaint)

            // Subtle track circle
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = Color.parseColor("#2EFFFFFF")
            }
            val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawOval(arcRect, trackPaint)

            // Active progress arc (Cyan -> Mint -> Course Accent)
            val sweepAngle = (clamped * 360f).coerceAtLeast(if (clamped > 0f) 14f else 0f)
            if (sweepAngle > 0f) {
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeCap = Paint.Cap.ROUND
                    shader = LinearGradient(
                        0f, 0f, s.toFloat(), s.toFloat(),
                        intArrayOf(
                            Color.parseColor("#FF5CE1FF"),
                            Color.parseColor("#FF34D399"),
                            accentColorInt
                        ),
                        floatArrayOf(0f, 0.6f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawArc(arcRect, -90f, sweepAngle, false, arcPaint)

                // Luminous end-cap orb
                val endRad = Math.toRadians((-90f + sweepAngle).toDouble())
                val tipX = cx + (radius * cos(endRad)).toFloat()
                val tipY = cy + (radius * sin(endRad)).toFloat()
                val orbGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#665CE1FF")
                }
                canvas.drawCircle(tipX, tipY, strokeW * 0.85f, orbGlow)
                val orbCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                }
                canvas.drawCircle(tipX, tipY, strokeW * 0.36f, orbCore)
            }

            // Percentage label
            val pctText = "${(clamped * 100f).roundToInt()}%"
            val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = s * 0.24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(pctText, cx, cy + s * 0.03f, pctPaint)

            // Micro caption "GOAL"
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF72E8FF")
                textAlign = Paint.Align.CENTER
                textSize = s * 0.13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText("GOAL", cx, cy + s * 0.21f, subPaint)

            return bmp
        }

        private fun renderCourseAccentBarBitmap(accentColorInt: Int): Bitmap {
            val w = 16
            val h = 140
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(Color.parseColor("#FF72E8FF"), accentColorInt),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 8f, 8f, paint)
            return bmp
        }

        private fun buildDestinationPendingIntent(
            context: Context,
            requestCode: Int,
            destination: String
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_WIDGET_DESTINATION, destination)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val manager = AppWidgetManager.getInstance(appCtx)
                    val ids = manager.getAppWidgetIds(ComponentName(appCtx, AuraUpNextWidgetProvider::class.java))
                    if (ids.isNotEmpty()) {
                        refreshWidgetsInternal(appCtx, manager, ids)
                    }
                }
            }
            AuraWidgetSuiteManager.updateAllExtraWidgets(appCtx)
        }

        suspend fun buildRemoteViewsForContext(context: Context): RemoteViews {
            val db = AuraDatabase.getInstance(context)
            val prefs = AuraPreferences(context)
            val profile = runCatching { prefs.userProfileFlow.first() }.getOrNull()
            val palette = AuraWidgetThemePalette.fromPresetId(profile?.accentPreset)
            val lectures = db.lectureDao().getAllLecturesList()
            val pendingCount = db.assessmentDao().getAllAssessmentsList()
                .count { it.status != "COMPLETED" && it.status != "SUBMITTED" }
            val weekAgo = System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
            val weeklyFocusMins = db.focusRecordDao().getAllFocusRecordsList()
                .filter { it.completed && it.timestampEpochMillis >= weekAgo }
                .sumOf { it.durationMinutes }

            val nowDay = LocalDate.now().dayOfWeek.value
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val summary = buildWidgetSummary(
                lectures = lectures,
                pendingTasksCount = pendingCount,
                weeklyFocusMinutes = weeklyFocusMins,
                nowDayOfWeek = nowDay,
                nowTime24 = nowTime,
                weeklyGoalHours = profile?.studyGoalHoursPerWeek ?: 20
            )

            val parsedAccent = if (lectures.isEmpty()) {
                palette.secondaryColorInt
            } else {
                runCatching { Color.parseColor(summary.accentColorHex) }
                    .getOrDefault(palette.secondaryColorInt)
            }

            val backdropBmp = renderWidgetBackdropBitmap(
                widthPx = 720,
                heightPx = 340,
                accentColorInt = parsedAccent,
                isLiveNow = summary.isLiveNow,
                glassStyle = profile?.widgetGlassStyle ?: "FROSTED_OBSIDIAN"
            )
            val ringBmp = renderWidgetFocusRingBitmap(
                sizePx = 156,
                progressFraction = summary.focusProgressFraction,
                accentColorInt = parsedAccent
            )
            val accentBarBmp = renderCourseAccentBarBitmap(parsedAccent)

            val homeIntent = buildDestinationPendingIntent(context, 7010, DEST_HOME)
            val timetableIntent = buildDestinationPendingIntent(context, 7011, DEST_TIMETABLE)
            val focusIntent = buildDestinationPendingIntent(context, 7012, DEST_FOCUS)
            val slidesIntent = buildDestinationPendingIntent(context, 7013, DEST_SLIDES)
            val aiIntent = buildDestinationPendingIntent(context, 7014, DEST_AI_ADVISOR)

            return RemoteViews(context.packageName, R.layout.aura_home_widget).apply {
                setImageViewBitmap(R.id.widget_aurora_backdrop, backdropBmp)
                setImageViewBitmap(R.id.widget_focus_ring_image, ringBmp)
                setImageViewBitmap(R.id.widget_course_accent_bar, accentBarBmp)

                setTextViewText(R.id.widget_badge_text, summary.badgeText)
                setTextViewText(R.id.widget_stats_text, summary.statsText)
                setTextViewText(R.id.widget_lecture_title, summary.lectureTitle)
                setTextViewText(R.id.widget_lecture_meta, summary.lectureMeta)
                setTextViewText(R.id.widget_leave_by_text, summary.leaveByText)

                setOnClickPendingIntent(R.id.widget_root_container, homeIntent)
                setOnClickPendingIntent(R.id.widget_spotlight_card, homeIntent)
                setOnClickPendingIntent(R.id.widget_btn_timetable, timetableIntent)
                setOnClickPendingIntent(R.id.widget_btn_focus, focusIntent)
                setOnClickPendingIntent(R.id.widget_btn_slides, slidesIntent)
                setOnClickPendingIntent(R.id.widget_btn_ai, aiIntent)
            }
        }

        private suspend fun refreshWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return
            val views = buildRemoteViewsForContext(context)
            for (widgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
