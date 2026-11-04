package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.NotificationHistoryEntity
import com.example.data.preferences.AuraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 1)

        when (action) {
            NotificationHelper.ACTION_ALARM_TRIGGER -> {
                // Deduplicate rapid back-to-back broadcast deliveries for the exact same alarm
                val nowMs = System.currentTimeMillis()
                synchronized(dedupLock) {
                    val prevMs = recentTriggers[notificationId] ?: 0L
                    if (nowMs - prevMs < 4_000L) {
                        return
                    }
                    recentTriggers[notificationId] = nowMs
                }

                val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "AURA Academic Alert"
                val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE)
                    ?: "You have an upcoming academic commitment."
                val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: "LECTURE"
                val courseCode = intent.getStringExtra(NotificationHelper.EXTRA_COURSE_CODE) ?: category
                val timeText = intent.getStringExtra(NotificationHelper.EXTRA_TIME_TEXT) ?: "Now"
                val eventId = intent.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, 0L)
                val intentToneId = intent.getStringExtra(NotificationHelper.EXTRA_ALARM_TONE_ID).orEmpty()
                val intentCustomPath = intent.getStringExtra(NotificationHelper.EXTRA_CUSTOM_ALARM_PATH).orEmpty()

                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = pm?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AURA:LectureAlarmWakeLock"
                )
                wakeLock?.acquire(15_000L)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AuraDatabase.getInstance(context)
                        val prefs = AuraPreferences(context)
                        val profile = prefs.userProfileFlow.first()

                        // Verify per-lecture alarm selection: if user disabled alarm for this lecture, do not ring!
                        val lecture = if (category == "LECTURE" && eventId > 0L) {
                            db.lectureDao().getLectureById(eventId)
                        } else {
                            null
                        }
                        if (category == "LECTURE" && eventId > 0L && lecture != null && !lecture.reminderEnabled) {
                            return@launch
                        }

                        val resolvedToneId = lecture?.alarmToneId?.takeIf { it.isNotBlank() }
                            ?: intentToneId.takeIf { it.isNotBlank() }
                            ?: profile.defaultAlarmToneId

                        val resolvedCustomPath = lecture?.customAlarmPath?.takeIf { it.isNotBlank() }
                            ?: intentCustomPath.takeIf { it.isNotBlank() }
                            ?: profile.customAlarmFilePath

                        // 1. Show silent visual banner / full-screen wakeup intent (0% audio from NotificationManager)
                        if (profile.notificationsEnabled) {
                            NotificationHelper.showNotification(
                                context = context,
                                notificationId = notificationId,
                                title = title,
                                message = message,
                                category = category,
                                courseCode = courseCode,
                                timeText = timeText,
                                eventId = eventId,
                                isAlarmWakeup = profile.alarmsEnabled
                            )
                        }

                        // 2. Start the SINGLE exclusive audio stream via LectureAlarmRinger
                        if (profile.alarmsEnabled) {
                            LectureAlarmRinger.startRinging(
                                context = context,
                                payload = ActiveAlarmPayload(
                                    notificationId = notificationId,
                                    title = title,
                                    message = message,
                                    category = category,
                                    courseCode = courseCode,
                                    timeText = timeText,
                                    eventId = eventId,
                                    alarmToneId = resolvedToneId,
                                    customAlarmPath = resolvedCustomPath
                                ),
                                playSound = profile.alarmSoundEnabled,
                                vibrate = profile.vibrateEnabled,
                                gentleFadeIn = profile.alarmGentleFadeIn
                            )
                        }

                        // 3. Persist notification record to database
                        db.notificationHistoryDao().insertNotification(
                            NotificationHistoryEntity(
                                title = title,
                                message = message,
                                category = category,
                                triggerEpochMillis = System.currentTimeMillis(),
                                isRead = false
                            )
                        )

                        // 4. Re-arm next week's recurring lecture alarm automatically if still enabled
                        if (lecture != null && lecture.isRecurring && lecture.reminderEnabled) {
                            AlarmScheduler.scheduleLectureAlarm(context, lecture)
                        }
                    } finally {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                        pendingResult?.finish()
                    }
                }
            }

            NotificationHelper.ACTION_DISMISS_ALARM -> {
                LectureAlarmRinger.stopRinging(context)
                NotificationHelper.cancelNotification(context, notificationId)
            }

            NotificationHelper.ACTION_SNOOZE_ALARM -> {
                LectureAlarmRinger.stopRinging(context)
                NotificationHelper.cancelNotification(context, notificationId)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "AURA Academic Alert"
                val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "Upcoming academic event"
                val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: "LECTURE"
                val courseCode = intent.getStringExtra(NotificationHelper.EXTRA_COURSE_CODE) ?: "AURA"
                val eventId = intent.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, 0L)

                val pendingSnooze = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val snoozeMins = AuraPreferences(context).userProfileFlow.first().defaultSnoozeMinutes
                        AlarmScheduler.scheduleSnoozeAlarm(
                            context = context,
                            notificationId = notificationId,
                            title = title,
                            message = message,
                            category = category,
                            eventId = eventId,
                            courseCode = courseCode,
                            snoozeMinutes = snoozeMins
                        )
                    } finally {
                        pendingSnooze?.finish()
                    }
                }
            }
        }
    }

    companion object {
        private val dedupLock = Any()
        private val recentTriggers = mutableMapOf<Int, Long>()

        fun resetDeduplicationForTests() {
            synchronized(dedupLock) {
                recentTriggers.clear()
            }
        }
    }
}
