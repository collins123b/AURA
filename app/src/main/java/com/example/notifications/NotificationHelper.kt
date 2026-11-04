package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    // v3 SILENT visual heads-up channels for active ringing alarms!
    // CRITICAL FIX: LectureAlarmRinger is the SOLE audio/vibration source during wake-up alarms.
    // Making the alarm notification channels silent prevents Android NotificationManager from
    // playing a second overlapping alarm sound milliseconds apart!
    const val CHANNEL_LECTURES = "aura_lectures_alarm_silent_v3"
    const val CHANNEL_EXAMINATIONS = "aura_examinations_alarm_silent_v3"
    const val CHANNEL_ASSESSMENTS = "aura_assessments_silent_v3"
    const val CHANNEL_STUDY_FOCUS = "aura_study_focus_v3"

    private val LEGACY_NOISY_CHANNELS = listOf(
        "aura_lectures_alarm",
        "aura_lectures_alarm_v2",
        "aura_assessments_alarm",
        "aura_assessments_alarm_v2",
        "aura_examinations_alarm",
        "aura_examinations_alarm_v2",
        "aura_study_focus_v2"
    )

    const val ACTION_ALARM_TRIGGER = "com.example.aura.ACTION_ALARM_TRIGGER"
    const val ACTION_DISMISS_ALARM = "com.example.aura.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.aura.ACTION_SNOOZE_ALARM"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_COURSE_CODE = "extra_course_code"
    const val EXTRA_TIME_TEXT = "extra_time_text"
    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_IS_RECURRING_LECTURE = "extra_is_recurring_lecture"
    const val EXTRA_ALARM_TONE_ID = "extra_alarm_tone_id"
    const val EXTRA_CUSTOM_ALARM_PATH = "extra_custom_alarm_path"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Purge legacy noisy channels so existing installs never double-play notification + ringer audio
            LEGACY_NOISY_CHANNELS.forEach { oldId ->
                try {
                    notificationManager.deleteNotificationChannel(oldId)
                } catch (_: Exception) {
                }
            }

            val politeSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Lecture Alarm Visual Banner Channel (Silent: LectureAlarmRinger plays the single chosen alarm sound)
            val lectureChannel = NotificationChannel(
                CHANNEL_LECTURES,
                "University Lecture Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Visual heads-up and lockscreen banners for ringing lecture alarms"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Assessment Channel (Silent when LectureAlarmRinger rings)
            val assessmentChannel = NotificationChannel(
                CHANNEL_ASSESSMENTS,
                "Assignments & Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Deadlines and reminders for course assignments, quizzes and projects"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Examination Channel (Silent: LectureAlarmRinger plays the single chosen alarm sound)
            val examChannel = NotificationChannel(
                CHANNEL_EXAMINATIONS,
                "Examinations & Tests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical visual banners for mid-semester and final examination alarms"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. Study & Focus Completion Channel (Single polite notification chime since ringer is not used here)
            val studyChannel = NotificationChannel(
                CHANNEL_STUDY_FOCUS,
                "Study & Focus Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for completed Pomodoro focus blocks and scheduled study sessions"
                enableVibration(true)
                setSound(politeSoundUri, notificationAudioAttributes)
            }

            notificationManager.createNotificationChannels(
                listOf(lectureChannel, assessmentChannel, examChannel, studyChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        category: String,
        courseCode: String = "AURA",
        timeText: String = "Now",
        eventId: Long = 0L,
        isAlarmWakeup: Boolean = false
    ) {
        createNotificationChannels(context)

        val channelId = when {
            isAlarmWakeup && category == "EXAMINATION" -> CHANNEL_EXAMINATIONS
            isAlarmWakeup && category == "ASSIGNMENT" -> CHANNEL_ASSESSMENTS
            isAlarmWakeup -> CHANNEL_LECTURES
            category == "EXAMINATION" -> CHANNEL_EXAMINATIONS
            category == "ASSIGNMENT" -> CHANNEL_ASSESSMENTS
            category == "STUDY" || category == "FOCUS" -> CHANNEL_STUDY_FOCUS
            else -> CHANNEL_LECTURES
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_COURSE_CODE, courseCode)
            putExtra(EXTRA_TIME_TEXT, timeText)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action (10 mins)
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_COURSE_CODE, courseCode)
            putExtra(EXTRA_TIME_TEXT, timeText)
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(openAppPendingIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop / Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePendingIntent)

        // Guarantee 0% notification sound overlap whenever LectureAlarmRinger handles audio
        if (isAlarmWakeup || channelId != CHANNEL_STUDY_FOCUS) {
            builder.setSilent(true)
        }

        if (isAlarmWakeup || category == "LECTURE" || category == "EXAMINATION") {
            builder.setFullScreenIntent(openAppPendingIntent, true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS might not be granted yet; LectureAlarmRinger still rings in-app
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
