package com.example.util

import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.LectureEntity
import org.json.JSONArray
import org.json.JSONObject

data class ExportData(
    val courses: List<CourseEntity>,
    val lectures: List<LectureEntity>,
    val assessments: List<AssessmentEntity>
)

object TimetableJsonHelper {

    fun exportToJson(data: ExportData): String {
        val root = JSONObject()
        root.put("app", "AURA")
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())

        val coursesArray = JSONArray()
        data.courses.forEach { c ->
            val obj = JSONObject().apply {
                put("code", c.code)
                put("name", c.name)
                put("lecturer", c.lecturer)
                put("venue", c.venue)
                put("colorHex", c.colorHex)
                put("creditHours", c.creditHours)
                put("attendanceTargetPercent", c.attendanceTargetPercent)
                put("notes", c.notes)
            }
            coursesArray.put(obj)
        }
        root.put("courses", coursesArray)

        val lecturesArray = JSONArray()
        data.lectures.forEach { l ->
            val obj = JSONObject().apply {
                put("courseCode", l.courseCode)
                put("courseName", l.courseName)
                put("dayOfWeek", l.dayOfWeek)
                put("startTime", l.startTime)
                put("endTime", l.endTime)
                put("venue", l.venue)
                put("lecturer", l.lecturer)
                put("reminderMinutesBefore", l.reminderMinutesBefore)
                put("reminderEnabled", l.reminderEnabled)
                put("alarmToneId", l.alarmToneId)
                put("colorHex", l.colorHex)
            }
            lecturesArray.put(obj)
        }
        root.put("lectures", lecturesArray)

        val assessmentsArray = JSONArray()
        data.assessments.forEach { a ->
            val obj = JSONObject().apply {
                put("courseCode", a.courseCode)
                put("courseName", a.courseName)
                put("title", a.title)
                put("type", a.type)
                put("description", a.description)
                put("dueEpochMillis", a.dueEpochMillis)
                put("priority", a.priority)
                put("status", a.status)
            }
            assessmentsArray.put(obj)
        }
        root.put("assessments", assessmentsArray)

        return root.toString(2)
    }

    fun parseJson(jsonString: String): ExportData? {
        return try {
            val root = JSONObject(jsonString)
            val coursesList = mutableListOf<CourseEntity>()
            val lecturesList = mutableListOf<LectureEntity>()
            val assessmentsList = mutableListOf<AssessmentEntity>()

            if (root.has("courses")) {
                val array = root.getJSONArray("courses")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    coursesList.add(
                        CourseEntity(
                            code = obj.optString("code", "COURSE"),
                            name = obj.optString("name", "Untitled Course"),
                            lecturer = obj.optString("lecturer", ""),
                            venue = obj.optString("venue", ""),
                            colorHex = obj.optString("colorHex", "#0A84FF"),
                            creditHours = obj.optInt("creditHours", 3),
                            attendanceTargetPercent = obj.optInt("attendanceTargetPercent", 75),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            if (root.has("lectures")) {
                val array = root.getJSONArray("lectures")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    lecturesList.add(
                        LectureEntity(
                            courseId = 0L,
                            courseCode = obj.optString("courseCode", "COURSE"),
                            courseName = obj.optString("courseName", "Lecture"),
                            dayOfWeek = obj.optInt("dayOfWeek", 1),
                            startTime = obj.optString("startTime", "08:30"),
                            endTime = obj.optString("endTime", "10:30"),
                            venue = obj.optString("venue", ""),
                            lecturer = obj.optString("lecturer", ""),
                            reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 15),
                            reminderEnabled = obj.optBoolean("reminderEnabled", true),
                            alarmToneId = obj.optString("alarmToneId", ""),
                            colorHex = obj.optString("colorHex", "#0A84FF")
                        )
                    )
                }
            }

            if (root.has("assessments")) {
                val array = root.getJSONArray("assessments")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    assessmentsList.add(
                        AssessmentEntity(
                            courseId = 0L,
                            courseCode = obj.optString("courseCode", "COURSE"),
                            courseName = obj.optString("courseName", ""),
                            title = obj.optString("title", "Assessment"),
                            type = obj.optString("type", "ASSIGNMENT"),
                            description = obj.optString("description", ""),
                            dueEpochMillis = obj.optLong("dueEpochMillis", System.currentTimeMillis() + 86400000L),
                            priority = obj.optString("priority", "HIGH"),
                            status = obj.optString("status", "PENDING")
                        )
                    )
                }
            }

            ExportData(courses = coursesList, lectures = lecturesList, assessments = assessmentsList)
        } catch (_: Exception) {
            null
        }
    }
}
