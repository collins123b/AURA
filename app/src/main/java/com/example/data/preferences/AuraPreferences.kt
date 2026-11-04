package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_user_prefs")

data class UserProfile(
    val name: String = "Collins",
    val email: String = "collins@university.edu",
    val institution: String = "University of Ghana",
    val programme: String = "BSc. Computer Science",
    val academicLevel: String = "Level 300",
    val semester: String = "Semester 1",
    val timeZone: String = "Africa/Accra",
    val isDarkTheme: Boolean = true,
    val accentPreset: String = "GLACIAL_CYAN",
    val attendanceTargetPercent: Int = 75,
    val studyGoalHoursPerWeek: Int = 18,
    val notificationsEnabled: Boolean = true,
    val alarmsEnabled: Boolean = true,
    val alarmSoundEnabled: Boolean = true,
    val alarmGentleFadeIn: Boolean = true,
    val alarmWakeUpBrainCheck: Boolean = false,
    val defaultSnoozeMinutes: Int = 10,
    val vibrateEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15,
    val aiEnabled: Boolean = true,
    val aiProvider: String = "Gemini AI",
    val isOnboardingComplete: Boolean = true,
    val semesterBudgetLimit: Double = 1500.0,
    val currencySymbol: String = "GH₵",
    val profilePicturePath: String = "",
    val timetableImagePath: String = "",
    val timetableImageUpdatedAt: Long = 0L,
    val defaultAlarmToneId: String = "AURORA_CHIME",
    val customAlarmFilePath: String = "",
    val customAlarmFileName: String = "",
    val widgetGlassStyle: String = "FROSTED_OBSIDIAN",
    val scheduleWidgetDayOffset: Int = 0,
    val webPortalUrl: String = "https://aura-study-companion.vercel.app"
)

class AuraPreferences(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val INSTITUTION = stringPreferencesKey("user_institution")
        val PROGRAMME = stringPreferencesKey("user_programme")
        val LEVEL = stringPreferencesKey("user_level")
        val SEMESTER = stringPreferencesKey("user_semester")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val ATTENDANCE_TARGET = intPreferencesKey("attendance_target")
        val STUDY_GOAL_HOURS = intPreferencesKey("study_goal_hours")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ALARMS_ENABLED = booleanPreferencesKey("alarms_enabled")
        val ALARM_SOUND_ENABLED = booleanPreferencesKey("alarm_sound_enabled")
        val ALARM_GENTLE_FADE_IN = booleanPreferencesKey("alarm_gentle_fade_in")
        val ALARM_WAKE_UP_BRAIN_CHECK = booleanPreferencesKey("alarm_wake_up_brain_check")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SEMESTER_BUDGET_LIMIT = doublePreferencesKey("semester_budget_limit")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val PROFILE_PICTURE_PATH = stringPreferencesKey("profile_picture_path")
        val TIMETABLE_IMAGE_PATH = stringPreferencesKey("timetable_image_path")
        val TIMETABLE_IMAGE_UPDATED_AT = longPreferencesKey("timetable_image_updated_at")
        val DEFAULT_ALARM_TONE_ID = stringPreferencesKey("default_alarm_tone_id")
        val CUSTOM_ALARM_FILE_PATH = stringPreferencesKey("custom_alarm_file_path")
        val CUSTOM_ALARM_FILE_NAME = stringPreferencesKey("custom_alarm_file_name")
        val WIDGET_GLASS_STYLE = stringPreferencesKey("widget_glass_style")
        val SCHEDULE_WIDGET_DAY_OFFSET = intPreferencesKey("schedule_widget_day_offset")
        val LAST_AUTO_RESTORED_UID = stringPreferencesKey("last_auto_restored_uid")
        val WEB_PORTAL_URL = stringPreferencesKey("web_portal_url")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "Collins",
            email = prefs[Keys.EMAIL] ?: "collins@university.edu",
            institution = prefs[Keys.INSTITUTION] ?: "University of Ghana",
            programme = prefs[Keys.PROGRAMME] ?: "BSc. Computer Science",
            academicLevel = prefs[Keys.LEVEL] ?: "Level 300",
            semester = prefs[Keys.SEMESTER] ?: "Semester 1",
            timeZone = prefs[Keys.TIME_ZONE] ?: "Africa/Accra",
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            accentPreset = prefs[Keys.ACCENT_PRESET] ?: "GLACIAL_CYAN",
            attendanceTargetPercent = prefs[Keys.ATTENDANCE_TARGET] ?: 75,
            studyGoalHoursPerWeek = prefs[Keys.STUDY_GOAL_HOURS] ?: 18,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            alarmsEnabled = prefs[Keys.ALARMS_ENABLED] ?: true,
            alarmSoundEnabled = prefs[Keys.ALARM_SOUND_ENABLED] ?: true,
            alarmGentleFadeIn = prefs[Keys.ALARM_GENTLE_FADE_IN] ?: true,
            alarmWakeUpBrainCheck = prefs[Keys.ALARM_WAKE_UP_BRAIN_CHECK] ?: false,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 10,
            vibrateEnabled = prefs[Keys.VIBRATE_ENABLED] ?: true,
            defaultReminderMinutes = prefs[Keys.DEFAULT_REMINDER_MINUTES] ?: 15,
            aiEnabled = prefs[Keys.AI_ENABLED] ?: true,
            aiProvider = prefs[Keys.AI_PROVIDER] ?: "Gemini AI",
            isOnboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: true,
            semesterBudgetLimit = prefs[Keys.SEMESTER_BUDGET_LIMIT] ?: 1500.0,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "GH₵",
            profilePicturePath = prefs[Keys.PROFILE_PICTURE_PATH] ?: "",
            timetableImagePath = prefs[Keys.TIMETABLE_IMAGE_PATH] ?: "",
            timetableImageUpdatedAt = prefs[Keys.TIMETABLE_IMAGE_UPDATED_AT] ?: 0L,
            defaultAlarmToneId = prefs[Keys.DEFAULT_ALARM_TONE_ID] ?: "AURORA_CHIME",
            customAlarmFilePath = prefs[Keys.CUSTOM_ALARM_FILE_PATH] ?: "",
            customAlarmFileName = prefs[Keys.CUSTOM_ALARM_FILE_NAME] ?: "",
            widgetGlassStyle = prefs[Keys.WIDGET_GLASS_STYLE] ?: "FROSTED_OBSIDIAN",
            scheduleWidgetDayOffset = prefs[Keys.SCHEDULE_WIDGET_DAY_OFFSET] ?: 0,
            webPortalUrl = prefs[Keys.WEB_PORTAL_URL] ?: "https://aura-study-companion.vercel.app"
        )
    }

    suspend fun updateProfile(
        name: String? = null,
        institution: String? = null,
        programme: String? = null,
        level: String? = null,
        semester: String? = null,
        timeZone: String? = null,
        attendanceTarget: Int? = null,
        studyGoalHours: Int? = null,
        email: String? = null
    ) {
        context.dataStore.edit { prefs ->
            name?.let { prefs[Keys.NAME] = it }
            institution?.let { prefs[Keys.INSTITUTION] = it }
            programme?.let { prefs[Keys.PROGRAMME] = it }
            level?.let { prefs[Keys.LEVEL] = it }
            semester?.let { prefs[Keys.SEMESTER] = it }
            timeZone?.let { prefs[Keys.TIME_ZONE] = it }
            attendanceTarget?.let { prefs[Keys.ATTENDANCE_TARGET] = it }
            studyGoalHours?.let { prefs[Keys.STUDY_GOAL_HOURS] = it }
            email?.let { if (it.isNotBlank()) prefs[Keys.EMAIL] = it }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun setAccentPreset(preset: String) {
        context.dataStore.edit { it[Keys.ACCENT_PRESET] = preset.ifBlank { "GLACIAL_CYAN" } }
    }

    suspend fun setWidgetGlassStyle(style: String) {
        val valid = when (style.uppercase()) {
            "CRYSTAL_CLEAR", "DAYLIGHT_PEARL", "FROSTED_OBSIDIAN" -> style.uppercase()
            else -> "FROSTED_OBSIDIAN"
        }
        context.dataStore.edit { it[Keys.WIDGET_GLASS_STYLE] = valid }
    }

    suspend fun setScheduleWidgetDayOffset(offset: Int) {
        context.dataStore.edit { it[Keys.SCHEDULE_WIDGET_DAY_OFFSET] = offset.coerceIn(-6, 6) }
    }

    suspend fun stepScheduleWidgetDayOffset(delta: Int): Int {
        var next = 0
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SCHEDULE_WIDGET_DAY_OFFSET] ?: 0
            next = ((current + delta + 7) % 7).let { if (it > 6) 0 else it }
            prefs[Keys.SCHEDULE_WIDGET_DAY_OFFSET] = next
        }
        return next
    }

    suspend fun hasAutoRestoredUid(uid: String): Boolean {
        if (uid.isBlank()) return true
        val prefs = context.dataStore.data.first()
        return prefs[Keys.LAST_AUTO_RESTORED_UID] == uid
    }

    suspend fun markUidAutoRestored(uid: String) {
        if (uid.isBlank()) return
        context.dataStore.edit { it[Keys.LAST_AUTO_RESTORED_UID] = uid }
    }

    suspend fun exportProfileSettingsList(fallbackEmail: String = ""): List<String> {
        val p = userProfileFlow.first()
        val effectiveEmail = if (fallbackEmail.isNotBlank()) fallbackEmail else p.email
        val pairs = listOf(
            "name" to p.name,
            "email" to effectiveEmail,
            "institution" to p.institution,
            "programme" to p.programme,
            "academicLevel" to p.academicLevel,
            "semester" to p.semester,
            "timeZone" to p.timeZone,
            "isDarkTheme" to p.isDarkTheme.toString(),
            "accentPreset" to p.accentPreset,
            "attendanceTargetPercent" to p.attendanceTargetPercent.toString(),
            "studyGoalHoursPerWeek" to p.studyGoalHoursPerWeek.toString(),
            "notificationsEnabled" to p.notificationsEnabled.toString(),
            "alarmsEnabled" to p.alarmsEnabled.toString(),
            "alarmSoundEnabled" to p.alarmSoundEnabled.toString(),
            "alarmGentleFadeIn" to p.alarmGentleFadeIn.toString(),
            "alarmWakeUpBrainCheck" to p.alarmWakeUpBrainCheck.toString(),
            "defaultSnoozeMinutes" to p.defaultSnoozeMinutes.toString(),
            "vibrateEnabled" to p.vibrateEnabled.toString(),
            "defaultReminderMinutes" to p.defaultReminderMinutes.toString(),
            "aiEnabled" to p.aiEnabled.toString(),
            "semesterBudgetLimit" to p.semesterBudgetLimit.toString(),
            "currencySymbol" to p.currencySymbol,
            "defaultAlarmToneId" to p.defaultAlarmToneId,
            "widgetGlassStyle" to p.widgetGlassStyle,
            "webPortalUrl" to p.webPortalUrl
        )
        return pairs.map { (k, v) ->
            "$k=${v.replace("\n", " ").trim()}".take(190)
        }
    }

    suspend fun restoreFromSettingsList(
        entries: List<String>,
        studentName: String,
        institution: String,
        programme: String
    ) {
        val map = entries.mapNotNull { raw ->
            val idx = raw.indexOf('=')
            if (idx <= 0) null else raw.substring(0, idx).trim() to raw.substring(idx + 1).trim()
        }.toMap()

        context.dataStore.edit { prefs ->
            val nameVal = map["name"]?.takeIf { it.isNotBlank() } ?: studentName
            if (nameVal.isNotBlank()) prefs[Keys.NAME] = nameVal
            map["email"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.EMAIL] = it }

            val instVal = map["institution"]?.takeIf { it.isNotBlank() } ?: institution
            if (instVal.isNotBlank()) prefs[Keys.INSTITUTION] = instVal

            val progVal = map["programme"]?.takeIf { it.isNotBlank() } ?: programme
            if (progVal.isNotBlank()) prefs[Keys.PROGRAMME] = progVal

            map["academicLevel"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.LEVEL] = it }
            map["semester"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.SEMESTER] = it }
            map["timeZone"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.TIME_ZONE] = it }
            map["isDarkTheme"]?.toBooleanStrictOrNull()?.let { prefs[Keys.IS_DARK_THEME] = it }
            map["accentPreset"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.ACCENT_PRESET] = it }
            map["attendanceTargetPercent"]?.toIntOrNull()?.let { prefs[Keys.ATTENDANCE_TARGET] = it.coerceIn(50, 100) }
            map["studyGoalHoursPerWeek"]?.toIntOrNull()?.let { prefs[Keys.STUDY_GOAL_HOURS] = it.coerceIn(1, 80) }
            map["notificationsEnabled"]?.toBooleanStrictOrNull()?.let { prefs[Keys.NOTIFICATIONS_ENABLED] = it }
            map["alarmsEnabled"]?.toBooleanStrictOrNull()?.let { prefs[Keys.ALARMS_ENABLED] = it }
            map["alarmSoundEnabled"]?.toBooleanStrictOrNull()?.let { prefs[Keys.ALARM_SOUND_ENABLED] = it }
            map["alarmGentleFadeIn"]?.toBooleanStrictOrNull()?.let { prefs[Keys.ALARM_GENTLE_FADE_IN] = it }
            map["alarmWakeUpBrainCheck"]?.toBooleanStrictOrNull()?.let { prefs[Keys.ALARM_WAKE_UP_BRAIN_CHECK] = it }
            map["defaultSnoozeMinutes"]?.toIntOrNull()?.let { prefs[Keys.DEFAULT_SNOOZE_MINUTES] = it.coerceIn(5, 30) }
            map["vibrateEnabled"]?.toBooleanStrictOrNull()?.let { prefs[Keys.VIBRATE_ENABLED] = it }
            map["defaultReminderMinutes"]?.toIntOrNull()?.let { prefs[Keys.DEFAULT_REMINDER_MINUTES] = it.coerceIn(0, 120) }
            map["aiEnabled"]?.toBooleanStrictOrNull()?.let { prefs[Keys.AI_ENABLED] = it }
            map["semesterBudgetLimit"]?.toDoubleOrNull()?.let { prefs[Keys.SEMESTER_BUDGET_LIMIT] = it.coerceAtLeast(0.0) }
            map["currencySymbol"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.CURRENCY_SYMBOL] = it }
            map["defaultAlarmToneId"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.DEFAULT_ALARM_TONE_ID] = it }
            map["widgetGlassStyle"]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.WIDGET_GLASS_STYLE] = it }
            map["webPortalUrl"]?.takeIf { it.startsWith("http") }?.let { prefs[Keys.WEB_PORTAL_URL] = it }
        }
    }

    suspend fun setWebPortalUrl(url: String) {
        val cleaned = url.trim().trimEnd('/').ifBlank { "https://aura-study-companion.vercel.app" }
        context.dataStore.edit { it[Keys.WEB_PORTAL_URL] = cleaned }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAlarmsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARMS_ENABLED] = enabled }
    }

    suspend fun setAlarmSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_SOUND_ENABLED] = enabled }
    }

    suspend fun setAlarmGentleFadeIn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_GENTLE_FADE_IN] = enabled }
    }

    suspend fun setAlarmWakeUpBrainCheck(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_WAKE_UP_BRAIN_CHECK] = enabled }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE_MINUTES] = minutes.coerceIn(5, 30) }
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ENABLED] = enabled }
    }

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setBudgetConfig(limit: Double, currency: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEMESTER_BUDGET_LIMIT] = limit.coerceAtLeast(0.0)
            prefs[Keys.CURRENCY_SYMBOL] = currency.ifBlank { "GH₵" }
        }
    }

    suspend fun setProfilePicturePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_PICTURE_PATH] = path
        }
    }

    suspend fun setTimetableImagePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMETABLE_IMAGE_PATH] = path
            prefs[Keys.TIMETABLE_IMAGE_UPDATED_AT] = if (path.isBlank()) 0L else System.currentTimeMillis()
        }
    }

    suspend fun setDefaultAlarmTone(toneId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_ALARM_TONE_ID] = toneId.ifBlank { "AURORA_CHIME" }
        }
    }

    suspend fun setCustomAlarmSound(filePath: String, fileName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ALARM_FILE_PATH] = filePath
            prefs[Keys.CUSTOM_ALARM_FILE_NAME] = fileName
            if (filePath.isNotBlank()) {
                prefs[Keys.DEFAULT_ALARM_TONE_ID] = "CUSTOM_AUDIO"
            } else if (prefs[Keys.DEFAULT_ALARM_TONE_ID] == "CUSTOM_AUDIO") {
                prefs[Keys.DEFAULT_ALARM_TONE_ID] = "AURORA_CHIME"
            }
        }
    }
}
