package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.OfficeHourEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.ui.components.formatTime12Hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object AlarmScheduler {

    fun scheduleLectureAlarm(context: Context, lecture: LectureEntity) {
        val requestCode = 100000 + lecture.id.toInt()
        // If user unselected/muted alarm for this lecture, cancel any existing PendingIntent immediately!
        if (!lecture.reminderEnabled) {
            if (lecture.id != 0L) {
                cancelAlarm(context, requestCode)
            }
            return
        }

        // Always cancel any prior PendingIntent for this lecture first so duplicate triggers cannot coexist
        cancelAlarm(context, requestCode)

        val triggerMillis = computeNextLectureTriggerMillis(lecture)
        val displayStart = formatTime12Hour(lecture.startTime)

        val messageText = if (lecture.reminderMinutesBefore == 0) {
            "${lecture.courseName} starts now ($displayStart) at ${lecture.venue}."
        } else {
            "${lecture.courseName} begins in ${lecture.reminderMinutesBefore} mins ($displayStart) at ${lecture.venue}."
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Lecture Alarm: ${lecture.courseCode}")
            putExtra(NotificationHelper.EXTRA_MESSAGE, messageText)
            putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, lecture.courseCode)
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, displayStart)
            putExtra(NotificationHelper.EXTRA_EVENT_ID, lecture.id)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationHelper.EXTRA_IS_RECURRING_LECTURE, lecture.isRecurring)
            putExtra(NotificationHelper.EXTRA_ALARM_TONE_ID, lecture.alarmToneId)
            putExtra(NotificationHelper.EXTRA_CUSTOM_ALARM_PATH, lecture.customAlarmPath)
        }

        setExactClockAlarm(context, triggerMillis, requestCode, intent)
    }

    fun scheduleAssessmentAlarm(context: Context, assessment: AssessmentEntity) {
        val requestCode = 200000 + assessment.id.toInt()
        if (!assessment.reminderEnabled || assessment.status == "COMPLETED" || assessment.status == "SUBMITTED") {
            if (assessment.id != 0L) cancelAlarm(context, requestCode)
            return
        }

        val reminderOffsetMillis = assessment.reminderMinutesBefore * 60 * 1000L
        var triggerMillis = assessment.dueEpochMillis - reminderOffsetMillis
        val now = System.currentTimeMillis()
        if (triggerMillis <= now && assessment.dueEpochMillis > now) {
            triggerMillis = assessment.dueEpochMillis
        }
        if (triggerMillis <= now) return

        cancelAlarm(context, requestCode)
        val category = if (assessment.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST")) "EXAMINATION" else "ASSIGNMENT"
        val prefix = if (category == "EXAMINATION") "Examination Alarm" else "Deadline Reminder"

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "$prefix: ${assessment.courseCode}")
            putExtra(
                NotificationHelper.EXTRA_MESSAGE,
                "${assessment.title} is approaching. Check your AURA Tasks & War Room."
            )
            putExtra(NotificationHelper.EXTRA_CATEGORY, category)
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, assessment.courseCode)
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, "Upcoming Deadline")
            putExtra(NotificationHelper.EXTRA_EVENT_ID, assessment.id)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
        }

        setExactClockAlarm(context, triggerMillis, requestCode, intent)
    }

    fun scheduleStudySessionAlarm(context: Context, session: StudySessionEntity) {
        val requestCode = 300000 + session.id.toInt()
        if (session.isCompleted) {
            if (session.id != 0L) cancelAlarm(context, requestCode)
            return
        }

        var triggerMillis = session.scheduledStartEpochMillis - (5 * 60 * 1000L)
        val now = System.currentTimeMillis()
        if (triggerMillis <= now && session.scheduledStartEpochMillis > now) {
            triggerMillis = session.scheduledStartEpochMillis
        }
        if (triggerMillis <= now) return

        cancelAlarm(context, requestCode)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Study Session Starting")
            putExtra(
                NotificationHelper.EXTRA_MESSAGE,
                "Ready for ${session.title}? (${session.durationMinutes} mins deep focus)."
            )
            putExtra(NotificationHelper.EXTRA_CATEGORY, "STUDY")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, session.courseCode)
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, "${session.durationMinutes}m Session")
            putExtra(NotificationHelper.EXTRA_EVENT_ID, session.id)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
        }

        setExactClockAlarm(context, triggerMillis, requestCode, intent)
    }

    fun scheduleOfficeHourAlarm(context: Context, officeHour: OfficeHourEntity) {
        val requestCode = 400000 + officeHour.id.toInt()
        if (!officeHour.reminderEnabled) {
            if (officeHour.id != 0L) cancelAlarm(context, requestCode)
            return
        }

        cancelAlarm(context, requestCode)
        val virtualLecture = LectureEntity(
            id = officeHour.id,
            courseId = 0L,
            courseCode = officeHour.courseCode,
            courseName = "Office Hours (${officeHour.instructorName})",
            dayOfWeek = officeHour.dayOfWeek,
            startTime = officeHour.startTime,
            endTime = officeHour.endTime,
            venue = officeHour.officeLocation,
            lecturer = officeHour.instructorName,
            reminderMinutesBefore = 15,
            reminderEnabled = true,
            isRecurring = true
        )
        val triggerMillis = computeNextLectureTriggerMillis(virtualLecture)
        val displayStart = formatTime12Hour(officeHour.startTime)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Office Hours: ${officeHour.courseCode}")
            putExtra(
                NotificationHelper.EXTRA_MESSAGE,
                "${officeHour.instructorName} (${officeHour.role}) is available at $displayStart in ${officeHour.officeLocation}."
            )
            putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, officeHour.courseCode)
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, displayStart)
            putExtra(NotificationHelper.EXTRA_EVENT_ID, 0L)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
        }

        setExactClockAlarm(context, triggerMillis, requestCode, intent)
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        category: String,
        eventId: Long,
        courseCode: String = "AURA",
        snoozeMinutes: Int = 10
    ) {
        val safeMins = snoozeMinutes.coerceIn(1, 60)
        cancelAlarm(context, notificationId)
        val triggerMillis = System.currentTimeMillis() + (safeMins * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, title)
            putExtra(NotificationHelper.EXTRA_MESSAGE, "$message (Snoozed ${safeMins}m)")
            putExtra(NotificationHelper.EXTRA_CATEGORY, category)
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, courseCode)
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, "Snoozed Alert")
            putExtra(NotificationHelper.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
        }
        setExactClockAlarm(context, triggerMillis, notificationId, intent)
    }

    /**
     * Triggers an immediate single-stream live test alarm using the student's chosen alarm tone.
     */
    fun triggerTestLectureAlarmNow(
        context: Context,
        studentName: String = "Scholar",
        toneId: String = "",
        customAlarmPath: String = ""
    ) {
        val requestCode = 990101
        cancelAlarm(context, requestCode)
        val triggerMillis = System.currentTimeMillis() + 800L
        val nowFormatted = LocalTime.now().let {
            formatTime12Hour(String.format("%02d:%02d", it.hour, it.minute))
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Lecture Alarm Test • AURA")
            putExtra(
                NotificationHelper.EXTRA_MESSAGE,
                "Hey $studentName! Your selected lecture alarm sound is active and playing cleanly."
            )
            putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, "AURA TEST")
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, nowFormatted)
            putExtra(NotificationHelper.EXTRA_EVENT_ID, 0L)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationHelper.EXTRA_ALARM_TONE_ID, toneId)
            putExtra(NotificationHelper.EXTRA_CUSTOM_ALARM_PATH, customAlarmPath)
        }
        setExactClockAlarm(context, triggerMillis, requestCode, intent)
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun setExactClockAlarm(
        context: Context,
        triggerMillis: Long,
        requestCode: Int,
        intent: Intent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    suspend fun rescheduleAll(context: Context) = withContext(Dispatchers.IO) {
        val db = AuraDatabase.getInstance(context)

        val lectures = db.lectureDao().getAllLecturesList()
        for (lecture in lectures) {
            if (lecture.reminderEnabled) {
                scheduleLectureAlarm(context, lecture)
            } else {
                cancelAlarm(context, 100000 + lecture.id.toInt())
            }
        }

        val assessments = db.assessmentDao().getAllAssessmentsList()
        for (assessment in assessments) {
            scheduleAssessmentAlarm(context, assessment)
        }

        val sessions = db.studySessionDao().getAllSessionsList()
        for (session in sessions) {
            scheduleStudySessionAlarm(context, session)
        }

        val officeHours = db.officeHourDao().getAllOfficeHoursList()
        for (oh in officeHours) {
            scheduleOfficeHourAlarm(context, oh)
        }
    }

    fun computeNextLectureTriggerMillis(
        lecture: LectureEntity,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val parts = lecture.startTime.trim().split(":")
        val hour = (parts.getOrNull(0)?.toIntOrNull() ?: 8).coerceIn(0, 23)
        val minute = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
        val targetDayOfWeek = DayOfWeek.of(lecture.dayOfWeek.coerceIn(1, 7))

        val lectureStartTime = LocalTime.of(hour, minute)
        var candidateLectureStart = now
            .with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
            .withHour(lectureStartTime.hour)
            .withMinute(lectureStartTime.minute)
            .withSecond(0)
            .withNano(0)

        var candidateTrigger = candidateLectureStart.minusMinutes(lecture.reminderMinutesBefore.toLong())

        if (!candidateTrigger.isAfter(now)) {
            if (candidateLectureStart.isAfter(now)) {
                val fiveSecondsFromNow = now.plusSeconds(5)
                candidateTrigger = if (candidateLectureStart.isAfter(fiveSecondsFromNow)) {
                    candidateLectureStart
                } else {
                    fiveSecondsFromNow
                }
            } else {
                candidateLectureStart = candidateLectureStart.plusWeeks(1)
                candidateTrigger = candidateLectureStart.minusMinutes(lecture.reminderMinutesBefore.toLong())
            }
        }

        return candidateTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
