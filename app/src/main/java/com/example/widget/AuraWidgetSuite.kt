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
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseResourceEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FocusRecordEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.preferences.AuraPreferences
import com.example.ui.components.formatTime12Hour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

data class AuraWidgetThemePalette(
    val presetId: String,
    val primaryColorInt: Int,
    val secondaryColorInt: Int,
    val badgeHex: String
) {
    companion object {
        fun fromPresetId(presetId: String?): AuraWidgetThemePalette {
            return when (presetId?.uppercase(Locale.US)) {
                "EMERALD_MINT" -> AuraWidgetThemePalette(
                    presetId = "EMERALD_MINT",
                    primaryColorInt = Color.parseColor("#10B981"),
                    secondaryColorInt = Color.parseColor("#34D399"),
                    badgeHex = "#6EE7B7"
                )
                "ROYAL_VIOLET" -> AuraWidgetThemePalette(
                    presetId = "ROYAL_VIOLET",
                    primaryColorInt = Color.parseColor("#8B5CF6"),
                    secondaryColorInt = Color.parseColor("#C084FC"),
                    badgeHex = "#D8B4FE"
                )
                "SUNSET_GOLD" -> AuraWidgetThemePalette(
                    presetId = "SUNSET_GOLD",
                    primaryColorInt = Color.parseColor("#F59E0B"),
                    secondaryColorInt = Color.parseColor("#FBBF24"),
                    badgeHex = "#FDE68A"
                )
                else -> AuraWidgetThemePalette(
                    presetId = "GLACIAL_CYAN",
                    primaryColorInt = Color.parseColor("#0A84FF"),
                    secondaryColorInt = Color.parseColor("#5CE1FF"),
                    badgeHex = "#72E8FF"
                )
            }
        }
    }
}

object AuraWidgetSuiteManager {

    private val DAY_FULL_NAMES = mapOf(
        1 to "MONDAY",
        2 to "TUESDAY",
        3 to "WEDNESDAY",
        4 to "THURSDAY",
        5 to "FRIDAY",
        6 to "SATURDAY",
        7 to "SUNDAY"
    )

    fun buildDeepLinkPendingIntent(
        context: Context,
        requestCode: Int,
        destination: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AuraUpNextWidgetProvider.EXTRA_WIDGET_DESTINATION, destination)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun renderAccentBarBitmap(accentColorInt: Int): Bitmap {
        val w = 14
        val h = 112
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.WHITE, accentColorInt),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 7f, 7f, paint)
        return bmp
    }

    fun renderProgressBarBitmap(
        widthPx: Int = 520,
        heightPx: Int = 24,
        progressFraction: Float,
        primaryColorInt: Int,
        secondaryColorInt: Int
    ): Bitmap {
        val w = widthPx.coerceIn(160, 1000)
        val h = heightPx.coerceIn(12, 64)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val radius = h / 2f
        val clamped = progressFraction.coerceIn(0f, 1f)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2AFFFFFF")
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, trackPaint)

        if (clamped > 0f) {
            val fillW = (w * clamped).coerceAtLeast(h.toFloat())
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, fillW, 0f,
                    intArrayOf(primaryColorInt, secondaryColorInt, Color.parseColor("#FF34D399")),
                    floatArrayOf(0f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(RectF(0f, 0f, fillW, h.toFloat()), radius, radius, fillPaint)
        }
        return bmp
    }

    fun renderExamUrgencyRingBitmap(
        sizePx: Int = 156,
        daysRemaining: Int?,
        fallbackAccentInt: Int
    ): Bitmap {
        val s = sizePx.coerceIn(80, 320)
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = s / 2f
        val cy = s / 2f
        val strokeW = s * 0.095f
        val radius = (s / 2f) - strokeW * 1.15f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val urgencyColor = when {
            daysRemaining == null -> fallbackAccentInt
            daysRemaining <= 2 -> Color.parseColor("#FFEF4444")
            daysRemaining <= 6 -> Color.parseColor("#FFF59E0B")
            else -> Color.parseColor("#FF34D399")
        }

        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#440B162C")
        }
        canvas.drawCircle(cx, cy, radius, discPaint)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = Color.parseColor("#28FFFFFF")
        }
        canvas.drawOval(arcRect, trackPaint)

        val sweep = when {
            daysRemaining == null -> 360f
            daysRemaining <= 0 -> 345f
            else -> ((14f - daysRemaining.coerceIn(0, 13)) / 14f * 320f).coerceIn(48f, 345f)
        }
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                0f, 0f, s.toFloat(), s.toFloat(),
                intArrayOf(urgencyColor, fallbackAccentInt),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawArc(arcRect, -90f, sweep, false, arcPaint)

        val mainLabel = when {
            daysRemaining == null -> "CLEAR"
            daysRemaining <= 0 -> "TODAY"
            else -> "${daysRemaining}d"
        }
        val subLabel = when {
            daysRemaining == null -> "ALL DONE"
            daysRemaining <= 0 -> "DUE NOW"
            else -> "LEFT"
        }

        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = if (mainLabel.length >= 5) s * 0.17f else s * 0.24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(mainLabel, cx, cy + s * 0.03f, mainPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = urgencyColor
            textAlign = Paint.Align.CENTER
            textSize = s * 0.125f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(subLabel, cx, cy + s * 0.21f, subPaint)

        return bmp
    }

    fun computeStudyStreakDays(focusRecords: List<FocusRecordEntity>, today: LocalDate = LocalDate.now()): Int {
        if (focusRecords.isEmpty()) return 0
        val completedDates = focusRecords
            .filter { it.completed && it.durationMinutes > 0 }
            .map {
                Instant.ofEpochMilli(it.timestampEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .toSet()
        if (completedDates.isEmpty()) return 0

        var cursor = if (completedDates.contains(today)) today else today.minusDays(1)
        if (!completedDates.contains(cursor)) return 0

        var streak = 0
        while (completedDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, providerClass)
        return runCatching {
            manager.requestPinAppWidget(provider, null, null)
        }.getOrDefault(false)
    }

    fun updateAllExtraWidgets(context: Context) {
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val manager = AppWidgetManager.getInstance(appCtx)

                val scheduleIds = manager.getAppWidgetIds(ComponentName(appCtx, AuraTodayScheduleWidgetProvider::class.java))
                if (scheduleIds.isNotEmpty()) {
                    val views = AuraTodayScheduleWidgetProvider.buildRemoteViewsForContext(appCtx)
                    scheduleIds.forEach { manager.updateAppWidget(it, views) }
                }

                val examIds = manager.getAppWidgetIds(ComponentName(appCtx, AuraExamCountdownWidgetProvider::class.java))
                if (examIds.isNotEmpty()) {
                    val views = AuraExamCountdownWidgetProvider.buildRemoteViewsForContext(appCtx)
                    examIds.forEach { manager.updateAppWidget(it, views) }
                }

                val focusIds = manager.getAppWidgetIds(ComponentName(appCtx, AuraFocusStreakWidgetProvider::class.java))
                if (focusIds.isNotEmpty()) {
                    val views = AuraFocusStreakWidgetProvider.buildRemoteViewsForContext(appCtx)
                    focusIds.forEach { manager.updateAppWidget(it, views) }
                }

                val deckIds = manager.getAppWidgetIds(ComponentName(appCtx, AuraStudyDeckWidgetProvider::class.java))
                if (deckIds.isNotEmpty()) {
                    val views = AuraStudyDeckWidgetProvider.buildRemoteViewsForContext(appCtx)
                    deckIds.forEach { manager.updateAppWidget(it, views) }
                }
            }
        }
    }

    fun resolveScheduleDayLectures(
        allLectures: List<LectureEntity>,
        todayDayOfWeek: Int,
        dayOffset: Int = 0
    ): Pair<String, List<LectureEntity>> {
        if (dayOffset != 0) {
            val steppedDay = (((todayDayOfWeek - 1 + dayOffset) % 7) + 7) % 7 + 1
            val steppedList = allLectures.filter { it.dayOfWeek == steppedDay }.sortedBy { it.startTime }
            val prefix = when (dayOffset) {
                1 -> "TOMORROW"
                -1 -> "YESTERDAY"
                else -> "VIEWING"
            }
            return "$prefix • ${DAY_FULL_NAMES[steppedDay] ?: "DAY $steppedDay"}" to steppedList
        }
        val todayList = allLectures.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.startTime }
        if (todayList.isNotEmpty()) {
            return "TODAY • ${DAY_FULL_NAMES[todayDayOfWeek] ?: "SCHEDULE"}" to todayList
        }
        for (offset in 1..6) {
            val candidateDay = ((todayDayOfWeek - 1 + offset) % 7) + 1
            val candidateList = allLectures.filter { it.dayOfWeek == candidateDay }.sortedBy { it.startTime }
            if (candidateList.isNotEmpty()) {
                return "NEXT • ${DAY_FULL_NAMES[candidateDay] ?: "UPCOMING"}" to candidateList
            }
        }
        return "TODAY'S TIMETABLE" to emptyList()
    }
}

/**
 * Widget #2: AURA Today's Schedule (4x3 Campus Timeline Board with Interactive Day Flipper)
 */
class AuraTodayScheduleWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_PREV_DAY || action == ACTION_NEXT_DAY || action == ACTION_RESET_TODAY) {
            val pendingResult = goAsync()
            val appCtx = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = AuraPreferences(appCtx)
                    when (action) {
                        ACTION_PREV_DAY -> prefs.stepScheduleWidgetDayOffset(-1)
                        ACTION_NEXT_DAY -> prefs.stepScheduleWidgetDayOffset(1)
                        ACTION_RESET_TODAY -> prefs.setScheduleWidgetDayOffset(0)
                    }
                    val manager = AppWidgetManager.getInstance(appCtx)
                    val ids = manager.getAppWidgetIds(ComponentName(appCtx, AuraTodayScheduleWidgetProvider::class.java))
                    val views = buildRemoteViewsForContext(appCtx)
                    ids.forEach { manager.updateAppWidget(it, views) }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PREV_DAY = "com.example.widget.ACTION_SCHEDULE_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.widget.ACTION_SCHEDULE_NEXT_DAY"
        const val ACTION_RESET_TODAY = "com.example.widget.ACTION_SCHEDULE_RESET_TODAY"

        private fun buildActionPendingIntent(context: Context, reqCode: Int, action: String): PendingIntent {
            val intent = Intent(context, AuraTodayScheduleWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        suspend fun buildRemoteViewsForContext(context: Context): RemoteViews {
            val db = AuraDatabase.getInstance(context)
            val prefs = AuraPreferences(context)
            val profile = runCatching { prefs.userProfileFlow.first() }.getOrNull()
            val palette = AuraWidgetThemePalette.fromPresetId(profile?.accentPreset)
            val glassStyle = profile?.widgetGlassStyle ?: "FROSTED_OBSIDIAN"
            val dayOffset = profile?.scheduleWidgetDayOffset ?: 0

            val allLectures = db.lectureDao().getAllLecturesList()
            val nowDay = LocalDate.now().dayOfWeek.value
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val (headerLabel, displayLectures) = AuraWidgetSuiteManager.resolveScheduleDayLectures(
                allLectures = allLectures,
                todayDayOfWeek = nowDay,
                dayOffset = dayOffset
            )
            val activeAlarmsCount = displayLectures.count { it.reminderEnabled }

            val backdropBmp = AuraUpNextWidgetProvider.renderWidgetBackdropBitmap(
                widthPx = 720,
                heightPx = 460,
                accentColorInt = palette.secondaryColorInt,
                isLiveNow = displayLectures.any { it.dayOfWeek == nowDay && nowTime >= it.startTime && nowTime < it.endTime },
                glassStyle = glassStyle
            )

            val slotIds = listOf(R.id.schedule_slot_1, R.id.schedule_slot_2, R.id.schedule_slot_3)
            val barIds = listOf(R.id.schedule_slot_1_bar, R.id.schedule_slot_2_bar, R.id.schedule_slot_3_bar)
            val titleIds = listOf(R.id.schedule_slot_1_title, R.id.schedule_slot_2_title, R.id.schedule_slot_3_title)
            val metaIds = listOf(R.id.schedule_slot_1_meta, R.id.schedule_slot_2_meta, R.id.schedule_slot_3_meta)
            val tagIds = listOf(R.id.schedule_slot_1_tag, R.id.schedule_slot_2_tag, R.id.schedule_slot_3_tag)

            val openDaily = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7101, AuraUpNextWidgetProvider.DEST_TIMETABLE)
            val openWeekly = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7102, AuraUpNextWidgetProvider.DEST_TIMETABLE_WEEK)
            val openAddLecture = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7103, AuraUpNextWidgetProvider.DEST_ADD_LECTURE)
            val prevDayPi = buildActionPendingIntent(context, 7104, ACTION_PREV_DAY)
            val nextDayPi = buildActionPendingIntent(context, 7105, ACTION_NEXT_DAY)
            val resetTodayPi = buildActionPendingIntent(context, 7106, ACTION_RESET_TODAY)

            return RemoteViews(context.packageName, R.layout.aura_schedule_widget).apply {
                setImageViewBitmap(R.id.schedule_widget_backdrop, backdropBmp)
                setTextViewText(R.id.schedule_widget_day_badge, headerLabel)
                setTextViewText(
                    R.id.schedule_widget_count_pill,
                    if (displayLectures.isEmpty()) "Free Day" else "${displayLectures.size} Classes • $activeAlarmsCount Alarms"
                )

                if (displayLectures.isEmpty()) {
                    slotIds.forEach { setViewVisibility(it, View.GONE) }
                    setViewVisibility(R.id.schedule_empty_message, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.schedule_empty_message, View.GONE)
                    for (i in slotIds.indices) {
                        val lec = displayLectures.getOrNull(i)
                        if (lec == null) {
                            setViewVisibility(slotIds[i], View.GONE)
                        } else {
                            setViewVisibility(slotIds[i], View.VISIBLE)
                            val courseColor = runCatching { Color.parseColor(lec.colorHex) }
                                .getOrDefault(palette.secondaryColorInt)
                            setImageViewBitmap(barIds[i], AuraWidgetSuiteManager.renderAccentBarBitmap(courseColor))
                            setTextViewText(titleIds[i], "${lec.courseCode} • ${lec.courseName}")
                            val venue = lec.venue.ifBlank { "Campus Hall" }
                            setTextViewText(
                                metaIds[i],
                                "${formatTime12Hour(lec.startTime)} – ${formatTime12Hour(lec.endTime)} • $venue"
                            )
                            val statusTag = when {
                                lec.dayOfWeek == nowDay && nowTime >= lec.startTime && nowTime < lec.endTime -> "LIVE"
                                lec.dayOfWeek == nowDay && lec.startTime >= nowTime && i == 0 && dayOffset == 0 -> "NEXT"
                                else -> formatTime12Hour(lec.startTime)
                            }
                            setTextViewText(tagIds[i], statusTag)
                        }
                    }
                }

                setOnClickPendingIntent(R.id.schedule_widget_root, openDaily)
                setOnClickPendingIntent(R.id.schedule_btn_prev_day, prevDayPi)
                setOnClickPendingIntent(R.id.schedule_btn_next_day, nextDayPi)
                setOnClickPendingIntent(R.id.schedule_widget_day_badge, resetTodayPi)
                setOnClickPendingIntent(R.id.schedule_btn_daily, openWeekly)
                setOnClickPendingIntent(R.id.schedule_btn_add_lecture, openAddLecture)
            }
        }
    }
}

/**
 * Widget #3: AURA Exam & Deadlines Radar (4x2 Urgency Countdown)
 */
class AuraExamCountdownWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MARK_TOP_DONE) {
            val pendingResult = goAsync()
            val appCtx = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AuraDatabase.getInstance(appCtx)
                    val upcoming = selectUpcomingAssessments(db.assessmentDao().getAllAssessmentsList())
                    val top = upcoming.firstOrNull()
                    if (top != null) {
                        db.assessmentDao().updateAssessment(top.copy(status = "COMPLETED"))
                    }
                    AuraUpNextWidgetProvider.updateAllWidgets(appCtx)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_TOP_DONE = "com.example.widget.ACTION_MARK_TOP_ASSESSMENT_DONE"

        fun selectUpcomingAssessments(
            allAssessments: List<AssessmentEntity>,
            nowMillis: Long = System.currentTimeMillis()
        ): List<AssessmentEntity> {
            return allAssessments
                .filter { it.status != "COMPLETED" && it.status != "SUBMITTED" && it.dueEpochMillis >= nowMillis - 86_400_000L }
                .sortedBy { it.dueEpochMillis }
        }

        suspend fun buildRemoteViewsForContext(context: Context): RemoteViews {
            val db = AuraDatabase.getInstance(context)
            val prefs = AuraPreferences(context)
            val profile = runCatching { prefs.userProfileFlow.first() }.getOrNull()
            val palette = AuraWidgetThemePalette.fromPresetId(profile?.accentPreset)
            val glassStyle = profile?.widgetGlassStyle ?: "FROSTED_OBSIDIAN"

            val nowMillis = System.currentTimeMillis()
            val upcoming = selectUpcomingAssessments(db.assessmentDao().getAllAssessmentsList(), nowMillis)
            val primary = upcoming.firstOrNull()
            val secondary = upcoming.getOrNull(1)

            val daysRemaining: Int? = primary?.let {
                val diffHours = (it.dueEpochMillis - nowMillis) / (1000.0 * 3600.0 * 24.0)
                max(0, ceil(diffHours).toInt())
            }

            val backdropBmp = AuraUpNextWidgetProvider.renderWidgetBackdropBitmap(
                widthPx = 720,
                heightPx = 340,
                accentColorInt = if (daysRemaining != null && daysRemaining <= 3) Color.parseColor("#F59E0B") else palette.secondaryColorInt,
                isLiveNow = false,
                glassStyle = glassStyle
            )
            val ringBmp = AuraWidgetSuiteManager.renderExamUrgencyRingBitmap(
                sizePx = 156,
                daysRemaining = daysRemaining,
                fallbackAccentInt = palette.secondaryColorInt
            )

            val openTasks = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7201, AuraUpNextWidgetProvider.DEST_TASKS)
            val openWarRoom = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7202, AuraUpNextWidgetProvider.DEST_WAR_ROOM)
            val markDonePi = PendingIntent.getBroadcast(
                context,
                7203,
                Intent(context, AuraExamCountdownWidgetProvider::class.java).apply { action = ACTION_MARK_TOP_DONE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(context.packageName, R.layout.aura_exam_widget).apply {
                setImageViewBitmap(R.id.exam_widget_backdrop, backdropBmp)
                setImageViewBitmap(R.id.exam_widget_ring_image, ringBmp)

                setTextViewText(
                    R.id.exam_widget_summary_pill,
                    if (upcoming.isEmpty()) "0 Pending" else "${upcoming.size} Upcoming"
                )

                if (primary == null) {
                    setTextViewText(R.id.exam_widget_badge, "AURA • ALL CLEAR")
                    setTextViewText(R.id.exam_widget_primary_title, "No Upcoming Exams or Deadlines")
                    setTextViewText(R.id.exam_widget_primary_meta, "Tap Tasks below to log exams or assignments")
                    setTextViewText(R.id.exam_widget_secondary_line, "Exam War Room ready whenever finals approach")
                } else {
                    val isExam = primary.type.contains("EXAM", ignoreCase = true) ||
                        primary.type.contains("MID_SEM", ignoreCase = true) ||
                        primary.type.contains("TEST", ignoreCase = true)
                    setTextViewText(
                        R.id.exam_widget_badge,
                        if (isExam) "AURA • EXAM RADAR" else "AURA • NEXT DEADLINE"
                    )
                    setTextViewText(R.id.exam_widget_primary_title, "${primary.courseCode} • ${primary.title}")

                    val venueLine = buildString {
                        if (primary.examVenue.isNotBlank()) append(primary.examVenue)
                        if (primary.seatNumber.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(primary.seatNumber)
                        }
                        if (isEmpty()) append("${primary.type.replace('_', ' ')} • Priority: ${primary.priority}")
                    }
                    setTextViewText(R.id.exam_widget_primary_meta, venueLine)

                    val secondText = if (secondary != null) {
                        "Next up: ${secondary.courseCode} • ${secondary.title}"
                    } else {
                        "Checklist: ${primary.materialsChecklist.ifBlank { "Review slides & flashcards" }}"
                    }
                    setTextViewText(R.id.exam_widget_secondary_line, secondText)
                }

                setOnClickPendingIntent(R.id.exam_widget_root, openTasks)
                setOnClickPendingIntent(R.id.exam_widget_spotlight, openTasks)
                setOnClickPendingIntent(R.id.exam_btn_tasks, openTasks)
                setOnClickPendingIntent(R.id.exam_btn_mark_done, markDonePi)
                setOnClickPendingIntent(R.id.exam_btn_war_room, openWarRoom)
            }
        }
    }
}

/**
 * Widget #4: AURA Focus & Streak (Compact 2x2 Bento Tile)
 */
class AuraFocusStreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun buildRemoteViewsForContext(context: Context): RemoteViews {
            val db = AuraDatabase.getInstance(context)
            val prefs = AuraPreferences(context)
            val profile = runCatching { prefs.userProfileFlow.first() }.getOrNull()
            val palette = AuraWidgetThemePalette.fromPresetId(profile?.accentPreset)

            val allRecords = db.focusRecordDao().getAllFocusRecordsList()
            val weekAgo = System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
            val weeklyMins = allRecords.filter { it.completed && it.timestampEpochMillis >= weekAgo }.sumOf { it.durationMinutes }
            val weeklyHours = weeklyMins / 60f
            val goalHours = (profile?.studyGoalHoursPerWeek ?: 20).coerceAtLeast(1)
            val progress = (weeklyHours / goalHours.toFloat()).coerceIn(0f, 1f)
            val streakDays = AuraWidgetSuiteManager.computeStudyStreakDays(allRecords)

            val backdropBmp = AuraUpNextWidgetProvider.renderWidgetBackdropBitmap(
                widthPx = 380,
                heightPx = 380,
                accentColorInt = palette.secondaryColorInt,
                isLiveNow = streakDays > 0,
                glassStyle = profile?.widgetGlassStyle ?: "FROSTED_OBSIDIAN"
            )
            val ringBmp = AuraUpNextWidgetProvider.renderWidgetFocusRingBitmap(
                sizePx = 184,
                progressFraction = progress,
                accentColorInt = palette.secondaryColorInt
            )

            val openFocus = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7301, AuraUpNextWidgetProvider.DEST_FOCUS)

            return RemoteViews(context.packageName, R.layout.aura_focus_widget).apply {
                setImageViewBitmap(R.id.focus_widget_backdrop, backdropBmp)
                setImageViewBitmap(R.id.focus_widget_ring_image, ringBmp)

                setTextViewText(
                    R.id.focus_widget_streak_badge,
                    if (streakDays == 1) "STREAK • 1 DAY" else "STREAK • $streakDays DAYS"
                )
                setTextViewText(
                    R.id.focus_widget_hours_text,
                    String.format(Locale.US, "%.1fh / %dh Goal", weeklyHours, goalHours)
                )

                setOnClickPendingIntent(R.id.focus_widget_root, openFocus)
                setOnClickPendingIntent(R.id.focus_widget_start_btn, openFocus)
            }
        }
    }
}

/**
 * Widget #5: AURA Slides & Flashcards Study Deck (4x2)
 */
class AuraStudyDeckWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViewsForContext(context.applicationContext)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun computeDueFlashcardsCount(cards: List<FlashcardEntity>, nowMillis: Long = System.currentTimeMillis()): Int {
            return cards.count { it.nextReviewEpochMillis <= nowMillis }
        }

        fun pickSpotlightDeck(resources: List<CourseResourceEntity>): CourseResourceEntity? {
            return resources.firstOrNull { !it.isCompleted && it.lastReadPage > 0 }
                ?: resources.firstOrNull { !it.isCompleted }
                ?: resources.firstOrNull()
        }

        suspend fun buildRemoteViewsForContext(context: Context): RemoteViews {
            val db = AuraDatabase.getInstance(context)
            val prefs = AuraPreferences(context)
            val profile = runCatching { prefs.userProfileFlow.first() }.getOrNull()
            val palette = AuraWidgetThemePalette.fromPresetId(profile?.accentPreset)

            val resources = runCatching { db.courseResourceDao().getAllResources().first() }.getOrDefault(emptyList())
            val flashcards = db.flashcardDao().getAllFlashcardsList()
            val dueCardsCount = computeDueFlashcardsCount(flashcards)
            val activeDeck = pickSpotlightDeck(resources)

            val deckFraction = when {
                activeDeck == null -> if (flashcards.isEmpty()) 0f else 0.35f
                activeDeck.isCompleted -> 1f
                else -> ((activeDeck.lastReadPage + 1).toFloat() / activeDeck.totalPages.coerceAtLeast(1).toFloat()).coerceIn(0.06f, 1f)
            }

            val backdropBmp = AuraUpNextWidgetProvider.renderWidgetBackdropBitmap(
                widthPx = 720,
                heightPx = 340,
                accentColorInt = palette.secondaryColorInt,
                isLiveNow = false,
                glassStyle = profile?.widgetGlassStyle ?: "FROSTED_OBSIDIAN"
            )
            val progressBmp = AuraWidgetSuiteManager.renderProgressBarBitmap(
                widthPx = 560,
                heightPx = 22,
                progressFraction = deckFraction,
                primaryColorInt = palette.primaryColorInt,
                secondaryColorInt = palette.secondaryColorInt
            )

            val openSlides = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7401, AuraUpNextWidgetProvider.DEST_SLIDES)
            val openCards = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7402, AuraUpNextWidgetProvider.DEST_FLASHCARDS)
            val openQuiz = AuraWidgetSuiteManager.buildDeepLinkPendingIntent(context, 7403, AuraUpNextWidgetProvider.DEST_PRACTICE_QUIZ)

            return RemoteViews(context.packageName, R.layout.aura_study_deck_widget).apply {
                setImageViewBitmap(R.id.study_deck_widget_backdrop, backdropBmp)
                setImageViewBitmap(R.id.study_deck_progress_bar, progressBmp)

                setTextViewText(
                    R.id.study_deck_cards_due_pill,
                    if (flashcards.isEmpty()) "Add Cards" else "$dueCardsCount Cards Due"
                )

                if (activeDeck == null) {
                    setTextViewText(R.id.study_deck_title, "Upload Course Slides or PDFs")
                    setTextViewText(
                        R.id.study_deck_meta,
                        "Import PDF, PPTX, or DOCX decks for AI 10Q quizzes & flashcards"
                    )
                } else {
                    setTextViewText(R.id.study_deck_title, "${activeDeck.courseCode} • ${activeDeck.title}")
                    val progressLabel = if (activeDeck.isCompleted) {
                        "Mastered ✓ • ${resources.size} ${if (resources.size == 1) "Deck" else "Decks"} in Hub"
                    } else {
                        "Slide ${activeDeck.lastReadPage + 1} of ${activeDeck.totalPages.coerceAtLeast(1)} • ${resources.size} ${if (resources.size == 1) "Deck" else "Decks"}"
                    }
                    setTextViewText(R.id.study_deck_meta, progressLabel)
                }

                setOnClickPendingIntent(R.id.study_deck_widget_root, openSlides)
                setOnClickPendingIntent(R.id.study_deck_spotlight, openSlides)
                setOnClickPendingIntent(R.id.study_deck_btn_slides, openSlides)
                setOnClickPendingIntent(R.id.study_deck_btn_flashcards, openCards)
                setOnClickPendingIntent(R.id.study_deck_btn_quiz, openQuiz)
            }
        }
    }
}
