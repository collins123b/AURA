package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.AcademicExpenseEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseResourceEntity
import android.net.Uri
import com.example.data.local.entity.FocusDistractionEntity
import com.example.data.local.entity.FocusTrackEntity
import com.example.data.local.entity.GroupProjectEntity
import com.example.data.local.entity.GroupTaskEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.OfficeHourEntity
import com.example.data.local.entity.QuizQuestionEntity
import com.example.data.local.entity.CitationEntity
import com.example.data.local.entity.StudyNoteEntity
import com.example.data.preferences.AuraPreferences
import com.example.notifications.ActiveAlarmPayload
import com.example.notifications.AlarmReceiver
import com.example.notifications.AlarmScheduler
import com.example.notifications.AlarmToneOption
import com.example.notifications.LectureAlarmRinger
import com.example.notifications.NotificationHelper
import com.example.ui.components.formatTime12Hour
import com.example.util.AcademicExportHelper
import com.example.util.AttendanceCalculator
import com.example.util.CitationFormatter
import com.example.util.MediaStorageHelper
import com.example.util.SlideDocumentParser
import com.example.util.SubtaskChecklistHelper
import com.example.util.TimetableInsightsAndShareHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LectureAlarmRinger.stopRinging(context)
        LectureAlarmRinger.stopPreview(context)
        AlarmReceiver.resetDeduplicationForTests()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AURA", appName)
    }

    @Test
    fun `formatTime12Hour formats 24h strings into AM and PM accurately`() {
        assertEquals("8:00 AM", formatTime12Hour("08:00"))
        assertEquals("2:30 PM", formatTime12Hour("14:30"))
        assertEquals("12:00 AM", formatTime12Hour("00:00"))
        assertEquals("12:45 PM", formatTime12Hour("12:45"))
    }

    @Test
    fun `Skip-Safe Attendance Calculator and Final Exam Predictor compute exact academic targets`() {
        // 1. 9 Present, 1 Absent => 90% (>= 75% target) => can miss 2 upcoming classes (9 / 12 = 75%)
        val recordsAbove = (1..9).map {
            AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = it.toLong(), status = "PRESENT")
        } + AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = 10L, status = "ABSENT")

        val safeSummary = AttendanceCalculator.computeCourseAttendance(recordsAbove, targetPercent = 75)
        assertEquals(90, safeSummary.percentage)
        assertFalse(safeSummary.isBelowTarget)
        assertEquals(2, safeSummary.safeMissAllowance)

        // 2. 2 Present, 2 Absent => 50% (< 75% target) => needs 4 consecutive classes ((2+4)/(4+4) = 6/8 = 75%)
        val recordsBelow = listOf(
            AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = 1L, status = "PRESENT"),
            AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = 2L, status = "PRESENT"),
            AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = 3L, status = "ABSENT"),
            AttendanceRecordEntity(courseId = 1L, courseCode = "CSCD 301", dateEpochDay = 4L, status = "ABSENT")
        )
        val recoverySummary = AttendanceCalculator.computeCourseAttendance(recordsBelow, targetPercent = 75)
        assertEquals(50, recoverySummary.percentage)
        assertTrue(recoverySummary.isBelowTarget)
        assertEquals(4, recoverySummary.classesNeededToRecover)

        // 3. Final Exam Score Predictor: 30% CA weight, 24/30 earned, target 80% (Grade A) => needs 56/70 (80.0%)
        val examPrediction = AttendanceCalculator.predictFinalExamScore(
            caWeightPercent = 30.0,
            caScoreEarnedOutOfWeight = 24.0,
            targetTotalScore = 80.0
        )
        assertTrue(examPrediction.isAchievable)
        assertEquals(56.0, examPrediction.requiredExamMarks, 0.01)
        assertEquals(80.0, examPrediction.requiredExamPercentage, 0.01)
    }

    @Test
    fun `AcademicExportHelper generates valid RFC-5545 ICS calendar and printable PDF schedule`() {
        val sampleLecture = LectureEntity(
            id = 1L,
            courseId = 1L,
            courseCode = "CSCD 301",
            courseName = "Software Engineering",
            dayOfWeek = DayOfWeek.MONDAY.value,
            startTime = "08:30",
            endTime = "10:30",
            venue = "Room B2",
            lecturer = "Dr. Mensah",
            reminderMinutesBefore = 15,
            reminderEnabled = true
        )
        val sampleExam = AssessmentEntity(
            id = 5L,
            courseId = 1L,
            courseCode = "CSCD 301",
            courseName = "Software Engineering",
            title = "Mid-Semester Examination",
            type = "MID_SEM",
            dueEpochMillis = System.currentTimeMillis() + 86_400_000L
        )
        val sampleNote = StudyNoteEntity(
            id = 2L,
            courseCode = "CSCD 301",
            title = "Amdahl's Law",
            content = "S = 1 / ((1 - p) + p/s)",
            category = "FORMULA",
            isPinned = true
        )

        val ics = AcademicExportHelper.generateIcsCalendar(
            studentName = "Kwame",
            institution = "University of Ghana",
            lectures = listOf(sampleLecture),
            officeHours = emptyList(),
            assessments = listOf(sampleExam)
        )
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=MO"))
        assertTrue(ics.contains("BEGIN:VALARM"))
        assertTrue(ics.contains("END:VCALENDAR"))

        val pdfBuffer = ByteArrayOutputStream()
        AcademicExportHelper.writeSemesterPdf(
            outputStream = pdfBuffer,
            studentName = "Kwame",
            institution = "University of Ghana",
            programme = "BSc Computer Science",
            lectures = listOf(sampleLecture),
            assessments = listOf(sampleExam),
            notes = listOf(sampleNote)
        )
        val pdfBytes = pdfBuffer.toByteArray()
        assertTrue("Expected non-empty PDF payload", pdfBytes.size > 200)
        assertEquals("%PDF", String(pdfBytes.take(4).toByteArray(), Charsets.US_ASCII))
    }

    @Test
    fun `computeNextLectureTriggerMillis handles imminent lectures and midnight wrap-around`() {
        val monday10Am = LocalDateTime.of(2026, 11, 2, 10, 0, 0) // 2026-11-02 is Monday

        val imminentLecture = LectureEntity(
            id = 10L,
            courseId = 1L,
            courseCode = "CSCD 301",
            courseName = "Software Engineering",
            dayOfWeek = DayOfWeek.MONDAY.value,
            startTime = "10:08",
            endTime = "12:00",
            venue = "Room B2",
            lecturer = "Dr. Mensah",
            reminderMinutesBefore = 15,
            reminderEnabled = true
        )
        val imminentTrigger = AlarmScheduler.computeNextLectureTriggerMillis(imminentLecture, monday10Am)
        val expectedTodayStart = monday10Am.withMinute(8).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedTodayStart, imminentTrigger)

        val pastLecture = imminentLecture.copy(startTime = "08:00", endTime = "10:00")
        val nextWeekTrigger = AlarmScheduler.computeNextLectureTriggerMillis(pastLecture, monday10Am)
        val expectedNextWeek = monday10Am.plusWeeks(1).withHour(7).withMinute(45)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedNextWeek, nextWeekTrigger)

        val midnightLecture = imminentLecture.copy(
            dayOfWeek = DayOfWeek.TUESDAY.value,
            startTime = "00:05",
            endTime = "01:05",
            reminderMinutesBefore = 15
        )
        val midnightTrigger = AlarmScheduler.computeNextLectureTriggerMillis(midnightLecture, monday10Am)
        val expectedMondayNight = monday10Am.withHour(23).withMinute(50)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedMondayNight, midnightTrigger)
    }

    @Test
    fun `AlarmScheduler schedules exact alarm and AlarmReceiver rings and posts notification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannels(context)

        val lecture = LectureEntity(
            id = 42L,
            courseId = 1L,
            courseCode = "MATH 204",
            courseName = "Linear Algebra",
            dayOfWeek = LocalDateTime.now().dayOfWeek.value,
            startTime = "23:55",
            endTime = "23:59",
            venue = "Auditorium 1",
            lecturer = "Prof. Owusu",
            reminderMinutesBefore = 5,
            reminderEnabled = true
        )

        AlarmScheduler.scheduleLectureAlarm(context, lecture)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        val receiver = AlarmReceiver()
        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Lecture Alarm: MATH 204")
            putExtra(NotificationHelper.EXTRA_MESSAGE, "Linear Algebra begins in 5 mins (11:55 PM)")
            putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, "MATH 204")
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, "11:55 PM")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 100042)
        }
        receiver.onReceive(context, triggerIntent)

        var attempts = 0
        while (LectureAlarmRinger.activeAlarmFlow.value == null && attempts < 30) {
            Thread.sleep(50)
            attempts++
        }

        assertNotNull("Expected LectureAlarmRinger to start active alarm", LectureAlarmRinger.activeAlarmFlow.value)
        assertEquals("MATH 204", LectureAlarmRinger.activeAlarmFlow.value?.courseCode)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotifManager = shadowOf(notificationManager)
        assertTrue("Expected high-priority lecture notification to be posted", shadowNotifManager.allNotifications.isNotEmpty())

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_DISMISS_ALARM
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 100042)
        }
        receiver.onReceive(context, dismissIntent)
        assertNull("Expected active alarm to clear on dismiss", LectureAlarmRinger.activeAlarmFlow.value)
    }

    @Test
    fun `Room persists Notes, Office Hours, Quiz Questions, Group Projects and Academic Expenses`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // 1. Study Notes & Formula Vault
            val noteId = db.studyNoteDao().insertStudyNote(
                StudyNoteEntity(
                    courseCode = "PHYS 201",
                    title = "Euler's Identity",
                    content = "e^(i*pi) + 1 = 0",
                    category = "FORMULA",
                    isPinned = true
                )
            )
            assertTrue(noteId > 0L)
            assertEquals("Euler's Identity", db.studyNoteDao().getAllStudyNotes().first().first().title)

            // 2. Lecturer Office Hours
            val ohId = db.officeHourDao().insertOfficeHour(
                OfficeHourEntity(
                    courseCode = "CSCD 301",
                    instructorName = "Dr. Boateng",
                    role = "Lecturer",
                    dayOfWeek = 3,
                    startTime = "14:00",
                    endTime = "16:00",
                    officeLocation = "CS Block Room 204",
                    email = "boateng@ug.edu.gh",
                    agendaNotes = "Project architecture review",
                    reminderEnabled = true
                )
            )
            assertTrue(ohId > 0L)
            assertEquals("Dr. Boateng", db.officeHourDao().getAllOfficeHoursList().first().instructorName)

            // 3. Practice Quiz Bank
            val qId = db.quizDao().insertQuestion(
                QuizQuestionEntity(
                    courseCode = "CSCD 301",
                    question = "Which pattern decouples UI from business state in Compose?",
                    optionA = "MVVM with StateFlow",
                    optionB = "Singleton Global Var",
                    optionC = "Static Activity Reference",
                    optionD = "Thread.sleep Polling",
                    correctOptionIndex = 0,
                    explanation = "MVVM with StateFlow provides lifecycle-aware reactive UI state."
                )
            )
            assertTrue(qId > 0L)
            assertEquals(1, db.quizDao().getAllQuestions().first().size)

            // 4. Group Project & Teammate Tasks
            val projectId = db.groupProjectDao().insertGroupProject(
                GroupProjectEntity(
                    courseCode = "CSCD 301",
                    projectTitle = "Distributed Timetable Compiler",
                    teamName = "Team Alpha",
                    dueEpochMillis = System.currentTimeMillis() + 172_800_000L
                )
            )
            db.groupProjectDao().insertGroupTask(
                GroupTaskEntity(
                    projectId = projectId,
                    memberName = "Ama",
                    memberContact = "ama@ug.edu.gh",
                    taskTitle = "Build Room DAO layer"
                )
            )
            assertEquals(1, db.groupProjectDao().getAllGroupProjects().first().size)
            assertEquals("Ama", db.groupProjectDao().getAllGroupTasks().first().first().memberName)

            // 5. Campus Budget & Academic Expenses
            db.academicExpenseDao().insertExpense(
                AcademicExpenseEntity(
                    title = "Linear Algebra Lab Manual",
                    category = "BOOKS",
                    amount = 65.0
                )
            )
            val expenses = db.academicExpenseDao().getAllExpenses().first()
            assertEquals(1, expenses.size)
            assertEquals(65.0, expenses.first().amount, 0.01)
        } finally {
            db.close()
        }
    }

    @Test
    fun `MainActivity renders all features including AM-PM picker, Practice Quiz, Slide Scanner and Group Projects`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Verify Home Dashboard & matched Assignments / Examinations overview cards
            composeTestRule.onNodeWithTag("dashboard_header").assertIsDisplayed()
            composeTestRule.onNodeWithTag("stat_pending_assignments").assertIsDisplayed()
            composeTestRule.onNodeWithTag("stat_upcoming_exams").assertIsDisplayed()

            // 2. Navigate to Timetable -> verify Export button + AM/PM Lecture Time Picker + Office Hours
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.onNodeWithTag("timetable_export_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("fab_add_lecture").performClick()
            composeTestRule.onNodeWithTag("input_lecture_start_time").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("input_lecture_start_time").performClick()
            composeTestRule.onNodeWithTag("input_lecture_start_time_confirm").assertIsDisplayed()
            composeTestRule.onNodeWithTag("input_lecture_start_time_confirm").performClick()
            composeTestRule.onNodeWithTag("cancel_lecture_button").performClick()

            composeTestRule.onNodeWithTag("tab_office_hours").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("office_hours_hub_view").assertIsDisplayed()

            // 3. Navigate to Study Hub -> verify Focus Timer, Notes Vault + Slide Scanner, and Practice Quiz
            composeTestRule.onNodeWithTag("nav_item_study").performClick()
            composeTestRule.onNodeWithTag("focus_timer_list")
                .performScrollToNode(hasTestTag("timer_toggle_button"))
            composeTestRule.onNodeWithTag("timer_toggle_button").assertIsDisplayed()

            composeTestRule.onNodeWithTag("study_tab_notes_vault").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("study_notes_vault_view").assertIsDisplayed()
            composeTestRule.onNodeWithTag("scan_slide_photo_button").assertIsDisplayed()

            composeTestRule.onNodeWithTag("study_tab_quiz").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("practice_quiz_view").assertIsDisplayed()

            // 4. Navigate to Tasks Hub -> switch to Group Projects Splitter
            composeTestRule.onNodeWithTag("nav_item_tasks").performClick()
            composeTestRule.onNodeWithTag("tasks_mode_group_projects").performClick()
            composeTestRule.onNodeWithTag("group_projects_view").assertIsDisplayed()
        }
    }

    @Test
    fun `SlideDocumentParser parses PPTX slides, DOCX sections, PDF text streams and cleans up local files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Construct a valid OOXML PowerPoint (.pptx) ZIP in memory with 2 slides + speaker notes
        val pptxBytes = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                      <a:p><a:r><a:t>Deadlock Prevention &amp; Banker's Algorithm</a:t></a:r></a:p>
                      <a:p><a:r><a:t>Mutual exclusion, Hold and Wait, No Preemption, Circular Wait</a:t></a:r></a:p>
                    </p:sld>""".trimIndent().toByteArray()
                )
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("ppt/notesSlides/notesSlide1.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <p:notes xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                      <a:p><a:r><a:t>Emphasize Banker's safety check complexity O(m * n^2) for mid-sem.</a:t></a:r></a:p>
                    </p:notes>""".trimIndent().toByteArray()
                )
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("ppt/slides/slide2.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                      <a:p><a:r><a:t>Resource Allocation Graphs</a:t></a:r></a:p>
                      <a:p><a:r><a:t>Cycle with single-instance resources implies deadlock</a:t></a:r></a:p>
                    </p:sld>""".trimIndent().toByteArray()
                )
                zos.closeEntry()
            }
        }.toByteArray()

        val pptxPages = SlideDocumentParser.parsePptxBytes(pptxBytes)
        assertEquals(2, pptxPages.size)
        assertTrue(pptxPages[0].title.contains("Deadlock Prevention"))
        assertTrue(pptxPages[0].body.contains("Circular Wait"))
        assertTrue(pptxPages[0].speakerNotes.contains("Banker's safety check"))
        assertTrue(pptxPages[1].title.contains("Resource Allocation Graphs"))

        // 2. Construct a valid OOXML Word (.docx) ZIP in memory with heading + paragraphs
        val docxBytes = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body>
                        <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Normalized Relational Schemas</w:t></w:r></w:p>
                        <w:p><w:r><w:t>Third Normal Form (3NF) eliminates transitive functional dependencies.</w:t></w:r></w:p>
                      </w:body>
                    </w:document>""".trimIndent().toByteArray()
                )
                zos.closeEntry()
            }
        }.toByteArray()

        val docxPages = SlideDocumentParser.parseDocxBytes(docxBytes)
        assertEquals(1, docxPages.size)
        assertEquals("Normalized Relational Schemas", docxPages[0].title)
        assertTrue(docxPages[0].body.contains("Third Normal Form"))

        // 3. Parse PDF text stream
        val samplePdf = """
            %PDF-1.4
            1 0 obj << /Type /Page >> endobj
            2 0 obj << /Length 68 >>
            stream
            BT /F1 14 Tf 50 700 Td (Fourier Transform Properties) Tj T* (Linearity and Time Shifting) Tj ET
            endstream
            endobj
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)
        val pdfPages = SlideDocumentParser.extractPdfTextPages(samplePdf)
        assertEquals(1, pdfPages.size)
        assertTrue(pdfPages[0].title.contains("Fourier Transform"))
        assertTrue(pdfPages[0].body.contains("Time Shifting"))

        // 4. Create local deck file and verify physical deletion when removing resource
        val createdDeck = SlideDocumentParser.buildTextSlideDeckPayload(
            context = context,
            title = "Week 4 Concurrency",
            rawContent = "# Semaphores\n• wait() and signal()\n---\n# Monitors\n• High-level synchronization"
        )
        assertEquals(2, createdDeck.totalPages)
        val storedFile = File(createdDeck.localFilePath)
        assertTrue(storedFile.exists())
        assertTrue(SlideDocumentParser.deleteLocalResourceFile(createdDeck.localFilePath))
        assertFalse(storedFile.exists())
    }

    @Test
    fun `Course edit propagates code and title to linked entities and course delete cleans up lectures and resources`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val courseId = db.courseDao().insertCourse(
                CourseEntity(
                    code = "CSCD 301",
                    name = "Operating Systems",
                    lecturer = "Dr. Mensah",
                    creditHours = 3
                )
            )
            db.lectureDao().insertLecture(
                LectureEntity(
                    courseId = courseId,
                    courseCode = "CSCD 301",
                    courseName = "Operating Systems",
                    dayOfWeek = 2,
                    startTime = "10:00",
                    endTime = "12:00",
                    venue = "Room A1",
                    lecturer = "Dr. Mensah"
                )
            )
            val resId = db.courseResourceDao().insertResource(
                CourseResourceEntity(
                    courseId = courseId,
                    courseCode = "CSCD 301",
                    title = "Week 1 Kernel Architecture.pptx",
                    moduleFolder = "Week 1",
                    fileType = "PPTX",
                    totalPages = 14
                )
            )
            assertTrue(resId > 0L)

            // Edit course code & name and propagate
            val edited = CourseEntity(
                id = courseId,
                code = "CSCD 311",
                name = "Advanced Operating Systems",
                lecturer = "Prof. Mensah",
                creditHours = 4,
                colorHex = "#6366F1"
            )
            db.courseDao().updateCourse(edited)
            db.lectureDao().updateCourseInfoForLectures(courseId, edited.code, edited.name, edited.colorHex)
            db.courseResourceDao().updateCourseCodeForResources(courseId, edited.code)

            assertEquals("CSCD 311", db.courseDao().getCourseById(courseId)?.code)
            assertEquals("Advanced Operating Systems", db.lectureDao().getAllLecturesList().first().courseName)
            assertEquals("CSCD 311", db.courseResourceDao().getResourceById(resId)?.courseCode)

            // Remove single resource
            db.courseResourceDao().deleteResourceById(resId)
            assertTrue(db.courseResourceDao().getAllResources().first().isEmpty())

            // Delete course & linked lectures
            db.lectureDao().deleteLecturesByCourse(courseId)
            db.courseDao().deleteCourse(edited)
            assertTrue(db.courseDao().getAllCoursesList().isEmpty())
            assertTrue(db.lectureDao().getAllLecturesList().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `Course Manager UI adds edits and deletes course and Course Slides Hub creates opens and removes slides`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Open Course Manager from Timetable -> Add Course -> Edit Course -> Delete Course
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.onNodeWithTag("manage_courses_button").performClick()
            composeTestRule.onNodeWithTag("course_manager_dialog").assertIsDisplayed()

            composeTestRule.onNodeWithTag("course_manager_add_button").performClick()
            composeTestRule.onNodeWithTag("input_course_code").performTextInput("CSCD 415")
            composeTestRule.onNodeWithTag("input_course_name").performTextInput("Compilers")
            composeTestRule.onNodeWithTag("save_course_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                composeTestRule.onAllNodesWithTag("course_manager_list").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("CSCD 415").assertIsDisplayed()
            composeTestRule.onNodeWithText("Compilers").assertIsDisplayed()

            // Edit the added course
            val editNodes = composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Edit CSCD 415")
            )
            editNodes[0].performClick()
            composeTestRule.onNodeWithTag("input_course_name").performTextClearance()
            composeTestRule.onNodeWithTag("input_course_name").performTextInput("Compiler Design & Optimization")
            composeTestRule.onNodeWithTag("save_course_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                runCatching {
                    composeTestRule.onNodeWithText("Compiler Design & Optimization").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            // Delete the added course
            val deleteNodes = composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Delete CSCD 415")
            )
            deleteNodes[0].performClick()
            composeTestRule.onNodeWithTag("confirm_delete_course_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                composeTestRule.onAllNodes(
                    androidx.compose.ui.test.hasContentDescription("Delete CSCD 415")
                ).fetchSemanticsNodes().isEmpty()
            }
            composeTestRule.onNodeWithTag("close_course_manager_button").performClick()

            // 2. Navigate to Study Hub -> Course Slides -> Create Deck -> Open Reader -> Bookmark -> Remove Resource
            composeTestRule.onNodeWithTag("nav_item_study").performClick()
            composeTestRule.onNodeWithTag("study_tab_slides").performClick()
            composeTestRule.onNodeWithTag("course_resources_hub_view").assertIsDisplayed()
            composeTestRule.onNodeWithTag("upload_course_slide_button").assertIsDisplayed()

            composeTestRule.onNodeWithTag("create_text_slides_button").performClick()
            composeTestRule.onNodeWithTag("text_deck_title_input").performTextInput("Week 2 Lexical Analysis")
            composeTestRule.onNodeWithTag("save_text_deck_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                runCatching {
                    composeTestRule.onNodeWithTag("course_resources_hub_view")
                        .performScrollToNode(androidx.compose.ui.test.hasText("Week 2 Lexical Analysis"))
                    composeTestRule.onNodeWithText("Week 2 Lexical Analysis").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            // Open Slide Reader
            composeTestRule.onNodeWithTag("course_resources_hub_view")
                .performScrollToNode(androidx.compose.ui.test.hasText("Open & Learn"))
            composeTestRule.onNodeWithText("Open & Learn").performClick()
            composeTestRule.onNodeWithTag("resource_reader_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("reader_next_page_button").performClick()
            composeTestRule.onNodeWithTag("reader_bookmark_button").performClick()
            composeTestRule.onNodeWithTag("reader_close_button").performClick()

            // Remove the added resource
            composeTestRule.onNodeWithTag("course_resources_hub_view")
                .performScrollToNode(androidx.compose.ui.test.hasContentDescription("Remove Resource"))
            val removeResNodes = composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Remove Resource")
            )
            removeResNodes[0].performClick()
            composeTestRule.onNodeWithTag("confirm_delete_resource_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                composeTestRule.onAllNodes(
                    androidx.compose.ui.test.hasContentDescription("Remove Resource")
                ).fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun `Single-Alarm Guarantee prevents overlapping double alarm streams and uses silent notification channel`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val lectureChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_LECTURES)
        val examChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_EXAMINATIONS)
        assertNotNull(lectureChannel)
        assertNotNull(examChannel)
        assertNull("Lecture visual channel must be silent so NotificationManager never plays a 2nd alarm", lectureChannel.sound)
        assertNull("Examination visual channel must be silent so NotificationManager never plays a 2nd alarm", examChannel.sound)

        AlarmReceiver.resetDeduplicationForTests()
        val receiver = AlarmReceiver()
        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = NotificationHelper.ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_TITLE, "Lecture Alarm: CSCD 301")
            putExtra(NotificationHelper.EXTRA_MESSAGE, "Operating Systems begins in 15 mins")
            putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
            putExtra(NotificationHelper.EXTRA_COURSE_CODE, "CSCD 301")
            putExtra(NotificationHelper.EXTRA_TIME_TEXT, "8:30 AM")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 100991)
            putExtra(NotificationHelper.EXTRA_ALARM_TONE_ID, AlarmToneOption.ZEN_BOWL.id)
        }

        // Fire two identical broadcast intents <50ms apart to simulate rapid double delivery
        receiver.onReceive(context, triggerIntent)
        receiver.onReceive(context, triggerIntent)

        var attempts = 0
        while (LectureAlarmRinger.activeAlarmFlow.value == null && attempts < 30) {
            Thread.sleep(50)
            attempts++
        }

        val activePayload = LectureAlarmRinger.activeAlarmFlow.value
        assertNotNull("Expected single active alarm payload", activePayload)
        assertEquals(100991, activePayload?.notificationId)
        assertEquals(AlarmToneOption.ZEN_BOWL.id, activePayload?.alarmToneId)
        assertTrue(LectureAlarmRinger.isRinging)

        // Also verify direct back-to-back startRinging calls never spawn duplicate streams
        LectureAlarmRinger.startRinging(
            context = context,
            payload = ActiveAlarmPayload(
                notificationId = 100991,
                title = "Lecture Alarm: CSCD 301",
                message = "Duplicate check",
                category = "LECTURE",
                courseCode = "CSCD 301",
                timeText = "8:30 AM",
                eventId = 0L,
                alarmToneId = AlarmToneOption.ZEN_BOWL.id
            )
        )
        assertTrue(LectureAlarmRinger.isRinging)

        LectureAlarmRinger.stopRinging(context)
        assertFalse(LectureAlarmRinger.isRinging)
        assertNull(LectureAlarmRinger.activeAlarmFlow.value)
    }

    @Test
    fun `Per-Lecture Alarm Selection mutes unselected lectures and cancels scheduled PendingIntent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val activeLecture = LectureEntity(
            id = 777L,
            courseId = 1L,
            courseCode = "STAT 223",
            courseName = "Probability Theory",
            dayOfWeek = LocalDateTime.now().dayOfWeek.value,
            startTime = "23:50",
            endTime = "23:59",
            venue = "Room C4",
            lecturer = "Dr. Addo",
            reminderMinutesBefore = 15,
            reminderEnabled = true,
            alarmToneId = AlarmToneOption.SUNRISE_HARP.id
        )

        AlarmScheduler.scheduleLectureAlarm(context, activeLecture)
        assertTrue("Expected enabled lecture alarm to be scheduled", shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // User deselects alarm for this lecture -> PendingIntent must be cancelled immediately
        val mutedLecture = activeLecture.copy(reminderEnabled = false)
        AlarmScheduler.scheduleLectureAlarm(context, mutedLecture)
        assertTrue("Expected muted lecture alarm to cancel its scheduled PendingIntent", shadowAlarmManager.scheduledAlarms.isEmpty())

        // Persist muted lecture in Room and verify AlarmReceiver refuses to ring it even if an old broadcast arrives
        val db = AuraDatabase.getInstance(context)
        val insertedId = db.lectureDao().insertLecture(mutedLecture.copy(id = 0L))
        try {
            AlarmReceiver.resetDeduplicationForTests()
            LectureAlarmRinger.stopRinging(context)

            val staleIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = NotificationHelper.ACTION_ALARM_TRIGGER
                putExtra(NotificationHelper.EXTRA_TITLE, "Lecture Alarm: STAT 223")
                putExtra(NotificationHelper.EXTRA_CATEGORY, "LECTURE")
                putExtra(NotificationHelper.EXTRA_COURSE_CODE, "STAT 223")
                putExtra(NotificationHelper.EXTRA_EVENT_ID, insertedId)
                putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 100777)
            }
            AlarmReceiver().onReceive(context, staleIntent)
            Thread.sleep(250)
            assertNull("Muted lecture must NOT ring when reminderEnabled is false", LectureAlarmRinger.activeAlarmFlow.value)
        } finally {
            db.lectureDao().getLectureById(insertedId)?.let { db.lectureDao().deleteLecture(it) }
        }
    }

    @Test
    fun `Profile Picture, Timetable Picture, Custom Alarm Sound and Focus Music & Distraction Pad persist cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AuraPreferences(context)

        // 1. Profile Picture & Timetable Picture local import + scaling + safe decode
        val sampleJpgBytes = MediaStorageHelper.createSyntheticSampleBitmapBytes(width = 120, height = 80)
        val tempSourceImg = File(context.cacheDir, "sample_upload.jpg").apply { writeBytes(sampleJpgBytes) }
        val imgUri = Uri.fromFile(tempSourceImg)

        val savedProfilePath = MediaStorageHelper.importProfilePicture(context, imgUri)
        val savedTimetablePath = MediaStorageHelper.importTimetablePicture(context, imgUri)
        assertNotNull(savedProfilePath)
        assertNotNull(savedTimetablePath)
        assertNotNull(MediaStorageHelper.loadBitmapSafe(savedProfilePath!!))
        assertNotNull(MediaStorageHelper.loadBitmapSafe(savedTimetablePath!!))

        prefs.setProfilePicturePath(savedProfilePath)
        prefs.setTimetableImagePath(savedTimetablePath)
        val updatedProfile = prefs.userProfileFlow.first()
        assertEquals(savedProfilePath, updatedProfile.profilePicturePath)
        assertEquals(savedTimetablePath, updatedProfile.timetableImagePath)
        assertTrue(updatedProfile.timetableImageUpdatedAt > 0L)

        // 2. Custom Audio Import for Alarm Sound & Study Music
        val tempAudio = File(context.cacheDir, "Deep_Focus_Piano.mp3").apply {
            writeBytes(ByteArray(512) { (it % 127).toByte() })
        }
        val importedAudio = MediaStorageHelper.importAudioFile(
            context = context,
            uri = Uri.fromFile(tempAudio),
            subfolder = "focus_music",
            fallbackPrefix = "Study Track"
        )
        assertNotNull(importedAudio)
        assertEquals("Deep Focus Piano", importedAudio!!.title)
        assertTrue(File(importedAudio.localFilePath).exists())

        prefs.setCustomAlarmSound(importedAudio.localFilePath, importedAudio.title)
        val alarmProfile = prefs.userProfileFlow.first()
        assertEquals(AlarmToneOption.CUSTOM_AUDIO.id, alarmProfile.defaultAlarmToneId)
        assertEquals("Deep Focus Piano", alarmProfile.customAlarmFileName)

        // Clean up test files & reset prefs
        assertTrue(MediaStorageHelper.deleteLocalMediaFile(context, savedProfilePath))
        assertTrue(MediaStorageHelper.deleteLocalMediaFile(context, savedTimetablePath))
        assertTrue(MediaStorageHelper.deleteLocalMediaFile(context, importedAudio.localFilePath))
        prefs.setProfilePicturePath("")
        prefs.setTimetableImagePath("")
        prefs.setCustomAlarmSound("", "")

        // 3. Room persistence for FocusTrackEntity & FocusDistractionEntity
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val trackId = db.focusTrackDao().insertTrack(
                FocusTrackEntity(
                    title = "Midnight Lo-Fi Study Beats",
                    artistOrCategory = "Personal Deck",
                    localFilePath = "/data/user/0/com.example/files/focus_music/lofi.mp3",
                    mimeType = "audio/mpeg",
                    durationSeconds = 185,
                    isFavorite = true
                )
            )
            assertTrue(trackId > 0L)
            assertEquals("Midnight Lo-Fi Study Beats", db.focusTrackDao().getAllTracksList().first().title)

            val distractionId = db.focusDistractionDao().insertDistraction(
                FocusDistractionEntity(
                    courseCode = "CSCD 301",
                    thoughtText = "Email lab partner about mutex benchmark"
                )
            )
            assertTrue(distractionId > 0L)
            assertEquals(1, db.focusDistractionDao().getAllDistractions().first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `UI renders Lecture Alarm Studio, Timetable Photo Card, Focus Music Deck, Distraction Pad and Profile Photo Upload`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Timetable Screen -> Timetable Photo Card + Lecture Alarm & Sound Studio Dialog
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.onNodeWithTag("timetable_photo_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("timetable_alarm_studio_button").performClick()
            composeTestRule.onNodeWithTag("lecture_alarm_manager_dialog").assertIsDisplayed()

            composeTestRule.onNodeWithTag("alarm_studio_tab_sounds").performClick()
            composeTestRule.onNodeWithTag("alarm_sounds_lazy_list")
                .performScrollToNode(hasTestTag("select_alarm_tone_ZEN_BOWL"))
            composeTestRule.onNodeWithTag("select_alarm_tone_ZEN_BOWL").performClick()
            composeTestRule.onNodeWithTag("close_alarm_studio_button").performClick()

            // 2. Study Focus Section -> Custom Music Deck, Box Breathing & Distraction Parking Lot
            composeTestRule.onNodeWithTag("nav_item_study").performClick()
            composeTestRule.onNodeWithTag("focus_timer_list")
                .performScrollToNode(hasTestTag("focus_custom_music_card"))
            composeTestRule.onNodeWithTag("focus_custom_music_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("add_focus_music_button").assertIsDisplayed()

            composeTestRule.onNodeWithTag("focus_timer_list")
                .performScrollToNode(hasTestTag("focus_breathing_card"))
            composeTestRule.onNodeWithTag("focus_breathing_card").assertIsDisplayed()

            composeTestRule.onNodeWithTag("focus_timer_list")
                .performScrollToNode(hasTestTag("focus_distraction_pad_card"))
            composeTestRule.onNodeWithTag("distraction_thought_input")
                .performTextInput("Verify TLB miss latency formula")
            composeTestRule.onNodeWithTag("add_distraction_thought_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                runCatching {
                    composeTestRule.onNodeWithTag("focus_timer_list")
                        .performScrollToNode(androidx.compose.ui.test.hasText("Verify TLB miss latency formula"))
                    composeTestRule.onNodeWithText("Verify TLB miss latency formula").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            // 3. Profile Screen -> Upload Profile Picture & Open Alarm Studio
            composeTestRule.onNodeWithTag("nav_item_profile").performClick()
            composeTestRule.onNodeWithTag("upload_profile_photo_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("profile_open_alarm_studio_button"))
            composeTestRule.onNodeWithTag("profile_open_alarm_studio_button").assertIsDisplayed()
        }
    }

    @Test
    fun `PDF Slide Reader and Export PDF dialogs keep all action buttons visible inside screen bounds`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // 1. Open Timetable -> Export PDF & .ICS Hub -> verify Share PDF & Save PDF buttons fit and scroll cleanly
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_export_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("share_pdf_schedule_button")
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("save_pdf_schedule_button")
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("Done").performClick()
            composeTestRule.waitForIdle()

            // 2. Navigate to Study -> Course Slides -> Create Deck -> Open Slide/PDF Reader
            composeTestRule.onNodeWithTag("nav_item_study").performClick()
            composeTestRule.onNodeWithTag("study_tab_slides").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("create_text_slides_button").performClick()
            composeTestRule.onNodeWithTag("text_deck_title_input").performTextInput("Week 4 Virtual Memory Paging")
            composeTestRule.onNodeWithTag("save_text_deck_button").performClick()

            composeTestRule.waitUntil(5_000L) {
                runCatching {
                    composeTestRule.onNodeWithTag("course_resources_hub_view")
                        .performScrollToNode(androidx.compose.ui.test.hasText("Week 4 Virtual Memory Paging"))
                    composeTestRule.onNodeWithText("Week 4 Virtual Memory Paging").assertIsDisplayed()
                    true
                }.getOrDefault(false)
            }

            composeTestRule.onNodeWithTag("course_resources_hub_view")
                .performScrollToNode(androidx.compose.ui.test.hasText("Open & Learn"))
            composeTestRule.onNodeWithText("Open & Learn").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("resource_reader_dialog").assertIsDisplayed()

            // Toggle Personal Takeaways notes scratchpad OPEN to verify bottom dock buttons still fit on screen!
            composeTestRule.onNodeWithTag("reader_notes_toggle").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("reader_notes_input").assertIsDisplayed()
            composeTestRule.onNodeWithTag("reader_prev_page_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("reader_mark_mastered_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("reader_next_page_button").assertIsDisplayed()

            composeTestRule.onNodeWithTag("reader_close_button").performClick()
            composeTestRule.waitForIdle()

            // Clean up created slide deck so test is self-contained
            composeTestRule.onNodeWithTag("course_resources_hub_view")
                .performScrollToNode(androidx.compose.ui.test.hasContentDescription("Remove Resource"))
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Remove Resource"))[0].performClick()
            composeTestRule.onNodeWithTag("confirm_delete_resource_button").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `AI Slide-to-10Q Exam Pack synthesizer and SM-2 Flashcard intervals work end-to-end`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aiService = com.example.ai.AuraAiService(context)

        val pack = aiService.synthesizeStudyPackFromSlides(
            courseCode = "CSCD 301",
            resourceTitle = "Process Scheduling & Deadlocks",
            plainSlideText = """
                Slide 1: Preemptive Scheduling
                Round Robin assigns fixed time quanta to ready processes to guarantee fairness and responsiveness.
                Slide 2: Coffman Deadlock Conditions
                Mutual exclusion, hold and wait, no preemption, and circular wait must hold simultaneously for deadlock.
                Slide 3: Banker's Algorithm
                Evaluates safe state allocation sequences before granting resource requests.
            """.trimIndent()
        )

        assertTrue("Summary note should be non-blank", pack.summaryContent.isNotBlank())
        assertEquals("Should synthesize 6 SM-2 flashcards", 6, pack.flashcards.size)
        assertEquals("Should synthesize 10 MCQ exam questions", 10, pack.quizQuestions.size)

        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val (q, a) = pack.flashcards.first()
            val insertedId = db.flashcardDao().insertFlashcard(
                com.example.data.local.entity.FlashcardEntity(
                    courseCode = "CSCD 301",
                    deckTitle = pack.summaryTitle,
                    question = q,
                    answer = a
                )
            )
            val initial = db.flashcardDao().getAllFlashcardsList().first { it.id == insertedId }
            assertEquals(2.5, initial.easeFactor, 0.01)
            assertEquals(1, initial.intervalDays)
            assertEquals(0, initial.repetitions)

            // Simulate SM-2 EASY recall update (quality = 5): rep 0 -> 1, interval 1d -> 4d, EF increases
            val updatedEf = (initial.easeFactor + 0.1).coerceIn(1.3, 3.2)
            val updatedCard = initial.copy(
                easeFactor = updatedEf,
                intervalDays = 4,
                repetitions = 1,
                boxLevel = 3
            )
            db.flashcardDao().updateFlashcard(updatedCard)
            val reloaded = db.flashcardDao().getAllFlashcardsList().first { it.id == insertedId }
            assertEquals(4, reloaded.intervalDays)
            assertEquals(1, reloaded.repetitions)
            assertEquals(2.6, reloaded.easeFactor, 0.01)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Campus Walk Leave-By buffer, Live Home Widget summary and Timetable Scanner parser work accurately`() {
        // 1. Pre-lecture walking departure math (including midnight wrap)
        assertEquals("8:20 AM", com.example.ui.components.computeLeaveByTime12Hour("08:30", 10))
        assertEquals("1:45 PM", com.example.ui.components.computeLeaveByTime12Hour("14:00", 15))
        assertEquals("11:50 PM", com.example.ui.components.computeLeaveByTime12Hour("00:05", 15))

        // 2. Home Screen Widget summary builder
        val mondayLecture = LectureEntity(
            id = 12L,
            courseId = 2L,
            courseCode = "CSCD 301",
            courseName = "Operating Systems",
            dayOfWeek = 1,
            startTime = "08:30",
            endTime = "10:30",
            venue = "Room B12",
            lecturer = "Prof. Mensah",
            walkingMinutes = 10
        )
        val widgetSummary = com.example.widget.AuraUpNextWidgetProvider.buildWidgetSummary(
            lectures = listOf(mondayLecture),
            pendingTasksCount = 3,
            weeklyFocusMinutes = 125,
            nowDayOfWeek = 1,
            nowTime24 = "07:45"
        )
        assertEquals("AURA • UP NEXT (Today)", widgetSummary.badgeText)
        assertTrue(widgetSummary.lectureTitle.contains("CSCD 301"))
        assertTrue(widgetSummary.leaveByText.contains("Leave by 8:20 AM"))
        assertTrue(widgetSummary.statsText.contains("3 Pending"))

        // 3. Deterministic Timetable & Syllabus portal text scanner
        val scanned = com.example.ai.AuraAiService.parseScheduleTextDeterministically(
            """
                CSCD 311 - Compiler Design | Mon 08:30 - 10:30 | Room N1
                MATH 304 - Numerical Analysis | Wed 2:00 PM - 4:00 PM | Block B4
                EXAM: CSCD 311 Midterm Exam | Hall C2
            """.trimIndent()
        )
        assertEquals(2, scanned.courses.size)
        assertEquals(2, scanned.lectures.size)
        assertEquals(1, scanned.exams.size)
        assertEquals("14:00", scanned.lectures[1].startTime)
        assertEquals("Hall C2", scanned.exams.first().examVenue)
    }

    @Test
    fun `Official Exam Hall Pass persists in Room and Profile renders Readiness Heatmap and Cloud Sync Auth Gate`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val db = Room.inMemoryDatabaseBuilder(app, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val examId = db.assessmentDao().insertAssessment(
                AssessmentEntity(
                    courseId = 1L,
                    courseCode = "CSCD 301",
                    courseName = "Operating Systems",
                    title = "End of Semester Examination",
                    type = "FINAL_EXAM",
                    dueEpochMillis = System.currentTimeMillis() + 86_400_000L * 5,
                    examVenue = "Great Hall Wing B",
                    seatNumber = "Row 4 • Seat 18",
                    indexNumber = "10984521",
                    materialsChecklist = "Student ID, HB Pencils, Non-Programmable Calculator"
                )
            )
            val savedExam = db.assessmentDao().getAllAssessmentsList().first { it.id == examId }
            assertEquals("Great Hall Wing B", savedExam.examVenue)
            assertEquals("Row 4 • Seat 18", savedExam.seatNumber)
            assertEquals("10984521", savedExam.indexNumber)
        } finally {
            db.close()
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // Verify Timetable AI Scanner dialog opens cleanly
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("ai_scan_timetable_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_scanner_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("load_sample_portal_schedule").performClick()
            composeTestRule.onNodeWithText("Close").performClick()
            composeTestRule.waitForIdle()

            // Verify Profile Screen renders 28-Day Exam Readiness Heatmap & opens Cloud Sync Google Sign-In Gate
            composeTestRule.onNodeWithTag("nav_item_profile").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("profile_readiness_heatmap"))
            composeTestRule.onNodeWithTag("profile_readiness_heatmap").assertIsDisplayed()

            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("open_cloud_sync_card"))
            composeTestRule.onNodeWithTag("open_cloud_sync_card").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("cloud_sign_in_gate_view").assertIsDisplayed()
            composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("close_cloud_sync_dialog_button").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun academicCitationFormatterAndRoomVaultPersistCorrectly() = runBlocking {
        // 1. Verify APA 7th, IEEE, MLA 9th, and Harvard formatting
        val apa = CitationFormatter.formatCitation(
            styleCode = "APA7",
            sourceTypeCode = "JOURNAL",
            authors = "Tanenbaum, A. S., & Bos, H.",
            year = "2023",
            title = "Modern Operating Systems",
            publicationOrPublisher = "Pearson Education",
            volumeOrPages = "5th ed., pp. 42-88",
            urlOrDoi = "10.1145/3581783"
        )
        assertTrue(apa.contains("Tanenbaum, A. S., & Bos, H. (2023). Modern Operating Systems."))
        assertTrue(apa.contains("https://doi.org/10.1145/3581783"))

        val ieee = CitationFormatter.formatCitation(
            styleCode = "IEEE",
            sourceTypeCode = "JOURNAL",
            authors = "A. Vaswani et al.",
            year = "2017",
            title = "Attention Is All You Need",
            publicationOrPublisher = "NeurIPS",
            ieeeIndex = 2
        )
        assertTrue(ieee.startsWith("[2] A. Vaswani et al., \"Attention Is All You Need,\""))
        assertEquals("[2]", CitationFormatter.formatInTextCitation("IEEE", "A. Vaswani et al.", "2017", 2))
        assertEquals("(Tanenbaum et al., 2023)", CitationFormatter.formatInTextCitation("APA7", "Tanenbaum, A. S., & Bos, H.", "2023"))

        // 2. Verify deterministic raw source / DOI parser
        val parsed = CitationFormatter.parseRawSourceDeterministically(
            "Cormen, T. H. (2022). Introduction to Algorithms. MIT Press. https://doi.org/10.5555/algorithms"
        )
        assertEquals("Cormen, T. H", parsed.authors)
        assertEquals("2022", parsed.year)
        assertTrue(parsed.title.contains("Introduction to Algorithms"))
        assertEquals("https://doi.org/10.5555/algorithms", parsed.urlOrDoi)

        // 3. Verify Room v9 CitationDao persistence & alphabetical bibliography compilation
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val idZ = db.citationDao().insertCitation(
                CitationEntity(
                    courseCode = "CSCD 301",
                    sourceType = "BOOK",
                    style = "APA7",
                    authors = "Zobel, J.",
                    year = "2021",
                    title = "Writing for Computer Science",
                    publicationOrPublisher = "Springer",
                    formattedCitation = "Zobel, J. (2021). Writing for Computer Science. Springer."
                )
            )
            db.citationDao().insertCitation(
                CitationEntity(
                    courseCode = "CSCD 301",
                    sourceType = "JOURNAL",
                    style = "APA7",
                    authors = "Aho, A. V.",
                    year = "2020",
                    title = "Compilers: Principles and Tools",
                    publicationOrPublisher = "ACM",
                    formattedCitation = "Aho, A. V. (2020). Compilers: Principles and Tools. ACM."
                )
            )
            val stored = db.citationDao().getAllCitationsList()
            assertEquals(2, stored.size)

            val bibliography = CitationFormatter.compileBibliography(stored, "APA7")
            assertTrue(bibliography.indexOf("Aho, A. V.") < bibliography.indexOf("Zobel, J."))

            val zobelEntity = stored.first { it.id == idZ }
            db.citationDao().deleteCitation(zobelEntity)
            assertEquals(1, db.citationDao().getAllCitationsList().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun subtaskChecklistsAndTimetableInsightsAndPosterRenderCorrectly() {
        // 1. SubtaskChecklistHelper default milestone generation & JSON round-trip
        val examSteps = SubtaskChecklistHelper.generateDefaultSubtasksForType("FINAL_EXAM")
        assertEquals(4, examSteps.size)
        val toggled = examSteps.mapIndexed { idx, step ->
            if (idx == 0) step.copy(isDone = true) else step
        }
        val serialized = SubtaskChecklistHelper.serializeSubtasks(toggled)
        val restored = SubtaskChecklistHelper.parseSubtasks(serialized)
        assertEquals(4, restored.size)
        assertTrue(restored[0].isDone)
        assertFalse(restored[1].isDone)

        // 2. TimetableInsightsAndShareHelper Free Study Windows & Clash Detection
        val mondayLectures = listOf(
            LectureEntity(
                id = 1L,
                courseId = 1L,
                courseCode = "CSCD 301",
                courseName = "Operating Systems",
                dayOfWeek = 1,
                startTime = "08:00",
                endTime = "10:00",
                venue = "Room A1"
            ),
            LectureEntity(
                id = 2L,
                courseId = 2L,
                courseCode = "MATH 304",
                courseName = "Numerical Analysis",
                dayOfWeek = 1,
                startTime = "12:00",
                endTime = "14:00",
                venue = "Block C"
            ),
            LectureEntity(
                id = 3L,
                courseId = 3L,
                courseCode = "STAT 302",
                courseName = "Stochastic Processes",
                dayOfWeek = 1,
                startTime = "13:30",
                endTime = "15:00",
                venue = "Lab 2"
            )
        )
        val gaps = TimetableInsightsAndShareHelper.findFreeStudyWindowsForDay(mondayLectures, 1)
        assertEquals(1, gaps.size)
        assertEquals(120, gaps.first().durationMinutes)
        assertEquals("CSCD 301", gaps.first().afterCourseCode)
        assertEquals("MATH 304", gaps.first().beforeCourseCode)

        val clashes = TimetableInsightsAndShareHelper.detectScheduleClashes(mondayLectures)
        assertEquals(1, clashes.size)
        assertEquals("MATH 304", clashes.first().firstCourseCode)
        assertEquals("STAT 302", clashes.first().secondCourseCode)

        // 3. Weekly Timetable Poster Bitmap generation
        val poster = TimetableInsightsAndShareHelper.renderWeeklyTimetablePosterBitmap(
            studentName = "Collins Bawa",
            institution = "University of Ghana",
            semesterLabel = "Level 300 • Semester 2",
            lectures = mondayLectures
        )
        assertEquals(1080, poster.width)
        assertTrue(poster.height >= 780)
    }

    @Test
    fun newAuroraThemeStudioBentoStudyCitationsExamMatrixAndDeveloperInfoWorkEndToEnd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AuraPreferences(context)

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // 1. Home Screen Quick Actions: Citations dialog opens & closes cleanly
            composeTestRule.onNodeWithTag("home_lazy_list")
                .performScrollToNode(hasTestTag("quick_actions_section"))
            composeTestRule.onNodeWithTag("quick_action_whiteboard_scanner").assertIsDisplayed()
            composeTestRule.onNodeWithTag("quick_action_citations").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("citation_manager_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("citation_style_IEEE").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Done").performClick()
            composeTestRule.waitForIdle()

            // 2. Study Screen 4-Category Bento Switcher
            composeTestRule.onNodeWithTag("nav_item_study").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("study_bento_category_switcher").assertIsDisplayed()

            // 3. Profile Screen: Aurora Accent Theme Studio, Final Exam Multi-Grade Matrix & About Developer
            composeTestRule.onNodeWithTag("nav_item_profile").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("profile_theme_preset_card"))
            composeTestRule.onNodeWithTag("profile_theme_preset_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("theme_preset_EMERALD_MINT").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(5_000L) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                runBlocking { prefs.userProfileFlow.first().accentPreset == "EMERALD_MINT" }
            }

            // Open GPA & Exam Predictor -> switch to Exam Predictor -> verify Multi-Grade Matrix
            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("open_gpa_calculator_card"))
            composeTestRule.onNodeWithTag("open_gpa_calculator_card").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("gpa_tab_exam_predictor").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("exam_grade_matrix_card").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithText("Close").performClick()
            composeTestRule.waitForIdle()

            // Verify About Developer card remains intact at bottom of Profile
            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("about_developer_card"))
            composeTestRule.onNodeWithTag("about_developer_card").assertIsDisplayed()
            composeTestRule.onNodeWithText("Collins Bawa").assertIsDisplayed()
            composeTestRule.onNodeWithText("V 1.1.2").assertIsDisplayed()
        }
    }

    @Test
    fun timetableHorizontalHeaderScrollabilityQuickDaysAndSearchWorkCleanly() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // Navigate to Timetable section
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.waitForIdle()

            // 1. Verify Timetable Header Card is displayed horizontally and compact (< 150.dp height, never wrapped vertically!)
            val headerNode = composeTestRule.onNodeWithTag("timetable_header_card")
            headerNode.assertIsDisplayed()
            val headerHeight = headerNode.getUnclippedBoundsInRoot().height
            assertTrue(
                "Expected compact horizontal header (< 150.dp), actual was $headerHeight",
                headerHeight < 150.dp
            )

            // 2. Verify Collapsible Search & Course Filter Bar toggles open and collapses to 0 space cleanly
            assertTrue(
                composeTestRule.onAllNodesWithTag("timetable_search_input")
                    .fetchSemanticsNodes().isEmpty()
            )
            composeTestRule.onNodeWithTag("timetable_search_toggle_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_search_input").assertIsDisplayed()
            composeTestRule.onNodeWithTag("timetable_search_input").performTextInput("CSCD")
            composeTestRule.waitForIdle()
            // Close search bar -> resets query and collapses cleanly
            composeTestRule.onNodeWithTag("timetable_search_toggle_button").performClick()
            composeTestRule.waitForIdle()
            assertTrue(
                composeTestRule.onAllNodesWithTag("timetable_search_input")
                    .fetchSemanticsNodes().isEmpty()
            )

            // 3. Verify Unified Vertical LazyColumn scrolls up smoothly so Quick Day Selector & Monday lectures are visible
            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToNode(hasTestTag("timetable_day_quick_strip"))
            composeTestRule.onNodeWithTag("day_chip_1").performClick()
            composeTestRule.waitForIdle()

            // Scroll down into the day's schedule items inside timetable_main_lazy_list (header scrolls off-screen!)
            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToIndex(5)
            composeTestRule.waitForIdle()

            // 4. Verify Weekly, Monthly, and Agenda views also scroll inside the unified Timetable LazyColumn
            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToIndex(0)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("tab_weekly").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToNode(hasTestTag("weekly_grid_view"))
            composeTestRule.onNodeWithTag("weekly_grid_view").assertIsDisplayed()

            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToIndex(0)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("tab_monthly").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_main_lazy_list")
                .performScrollToNode(hasTestTag("monthly_calendar_view"))
            composeTestRule.onNodeWithTag("monthly_calendar_view").assertIsDisplayed()

            // 5. Verify Tasks & Assessments unified LazyColumn also scrolls smoothly
            composeTestRule.onNodeWithTag("nav_item_tasks").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("tasks_assessments_lazy_list").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tasks_assessments_lazy_list")
                .performScrollToIndex(3)
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun slidePdfLargeAndBinaryStreamImportNeverCrashesAndUsesSidecarSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Build a real-world stress PDF containing:
        //    - Valid PDF text stream ("Deadlock Prevention & Banker's Algorithm")
        //    - A massive 30,000-byte '(' unclosed/escaped stream that previously caused StackOverflowError in regex!
        val tempPdf = File(context.cacheDir, "stress_lecture_slides.pdf")
        val giantParenPayload = "(" + ("A\\(B\\)C_BINARY_STREAM_DATA_".repeat(1200))
        val pdfContent = buildString {
            appendLine("%PDF-1.7")
            appendLine("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj")
            appendLine("2 0 obj << /Type /Pages /Count 2 /Kids [3 0 R 4 0 R] >> endobj")
            appendLine("3 0 obj << /Type /Page /Parent 2 0 R /Contents 5 0 R >> endobj")
            appendLine("4 0 obj << /Type /Page /Parent 2 0 R /Contents 6 0 R >> endobj")
            appendLine("5 0 obj << /Length 160 >>")
            appendLine("stream")
            appendLine("BT /F1 18 Tf 72 720 Td (Slide 1: Deadlock Prevention) Tj T* (Banker Algorithm Safety State Check) Tj ET")
            appendLine("endstream")
            appendLine("endobj")
            appendLine("6 0 obj << /Length ${giantParenPayload.length + 120} >>")
            appendLine("stream")
            appendLine(giantParenPayload)
            appendLine("BT /F1 16 Tf 72 700 Td [(Slide 2: Virtual Memory) -120 ( & Page Replacement)] TJ ET")
            appendLine("endstream")
            appendLine("endobj")
            appendLine("%%EOF")
        }
        tempPdf.writeBytes(pdfContent.toByteArray(Charsets.ISO_8859_1))

        val importedPdf = SlideDocumentParser.importDocumentFromUri(
            context = context,
            uri = Uri.fromFile(tempPdf),
            customTitle = "CSCD 301 Stress PDF"
        )
        assertNotNull("Expected non-null ImportedResourcePayload without StackOverflowError", importedPdf)
        val pdfPayload = importedPdf!!
        assertEquals("PDF", pdfPayload.fileType)
        assertTrue(pdfPayload.totalPages >= 1)
        val extractedPlain = SlideDocumentParser.plainSearchableText(pdfPayload.serializedText)
        assertTrue(extractedPlain.contains("Deadlock Prevention"))

        // 2. Build a huge multi-hundred-slide text/markdown deck (> 240,000 chars) to verify SQLite CursorWindow protection
        //    + 100% lossless restoration via local .aura_pages.txt sidecar!
        val tempHugeDeck = File(context.cacheDir, "massive_textbook_slides.md")
        val hugeMarkdown = buildString {
            for (i in 1..220) {
                appendLine("# Lecture Slide $i: Kernel Synchronization Topic $i")
                appendLine("Detailed university lecture notes for slide $i covering semaphores, monitors, mutexes, and memory barriers. ".repeat(12))
                appendLine("---")
            }
        }
        assertTrue("Precondition: deck exceeds SQLite safe threshold", hugeMarkdown.length > SlideDocumentParser.MAX_SQLITE_SERIALIZED_CHARS)
        tempHugeDeck.writeText(hugeMarkdown, Charsets.UTF_8)

        val importedHuge = SlideDocumentParser.importDocumentFromUri(
            context = context,
            uri = Uri.fromFile(tempHugeDeck),
            customTitle = "Massive OS Textbook"
        )
        assertNotNull(importedHuge)
        val hugePayload = importedHuge!!
        assertTrue(
            "SQLite serializedText must be safely bounded <= ${SlideDocumentParser.MAX_SQLITE_SERIALIZED_CHARS}",
            hugePayload.serializedText.length <= SlideDocumentParser.MAX_SQLITE_SERIALIZED_CHARS
        )
        assertEquals(220, hugePayload.totalPages)

        val entity = CourseResourceEntity(
            id = 99L,
            courseCode = "CSCD 301",
            title = hugePayload.title,
            fileType = hugePayload.fileType,
            localFilePath = hugePayload.localFilePath,
            totalPages = hugePayload.totalPages,
            extractedText = hugePayload.serializedText
        )
        val restoredPages = SlideDocumentParser.loadPagesForResource(entity)
        assertEquals("Sidecar should restore all 220 slides losslessly", 220, restoredPages.size)
        assertTrue(restoredPages.last().title.contains("220") || restoredPages.last().body.contains("220"))

        // Clean up local resource + sidecar
        SlideDocumentParser.deleteLocalResourceFile(pdfPayload.localFilePath)
        SlideDocumentParser.deleteLocalResourceFile(hugePayload.localFilePath)
        tempPdf.delete()
        tempHugeDeck.delete()
    }

    @Test
    fun timetableSafeDeleteConfirmationUndoLockAndGlobalSearchWorkEndToEnd() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // 1. Verify Home above-the-fold stats remain visible AND Global Campus Search opens cleanly
            composeTestRule.onNodeWithTag("stat_upcoming_exams").assertIsDisplayed()
            composeTestRule.onNodeWithTag("home_global_search_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("global_search_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("global_search_input").performTextInput("CSCD")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("close_global_search_button").performClick()
            composeTestRule.waitForIdle()

            // 2. Navigate to Timetable and test Schedule Lock Shield toggle
            composeTestRule.onNodeWithTag("nav_item_timetable").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("timetable_lock_toggle_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_locked_status_banner").assertIsDisplayed()
            // FAB is hidden while locked to prevent accidental modifications
            assertTrue(composeTestRule.onAllNodesWithTag("fab_add_lecture").fetchSemanticsNodes().isEmpty())

            // Unlock schedule again
            composeTestRule.onNodeWithTag("timetable_lock_toggle_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("fab_add_lecture").assertIsDisplayed()

            // 3. Add a test lecture on today's schedule so we can verify Safe Delete Confirmation + Undo Restore
            composeTestRule.onNodeWithTag("fab_add_lecture").performClick()
            composeTestRule.waitForIdle()
            if (composeTestRule.onAllNodesWithTag("lecture_dialog_new_course_chip").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithTag("lecture_dialog_new_course_chip").performClick()
                composeTestRule.waitForIdle()
            }
            composeTestRule.onNodeWithTag("input_lecture_course_code").performTextInput("SAFE 499")
            composeTestRule.onNodeWithTag("input_lecture_course_name").performTextInput("Safe Schedule Engineering")
            composeTestRule.onNodeWithTag("save_lecture_button").performClick()
            composeTestRule.waitForIdle()

            // Wait briefly for coroutine conflict check / DB insert, and confirm Save Anyway if time slot overlaps
            composeTestRule.waitUntil(5_000L) {
                val hasConflictBtn = composeTestRule.onAllNodesWithTag("conflict_save_anyway_button").fetchSemanticsNodes().isNotEmpty()
                if (hasConflictBtn) {
                    composeTestRule.onNodeWithTag("conflict_save_anyway_button").performClick()
                    composeTestRule.waitForIdle()
                }
                val savedInDb = runBlocking {
                    AuraDatabase.getInstance(ApplicationProvider.getApplicationContext())
                        .lectureDao()
                        .getAllLecturesList()
                        .any { it.courseCode == "SAFE 499" }
                }
                savedInDb
            }

            // Switch to Agenda view & filter to SAFE 499
            composeTestRule.onNodeWithTag("tab_agenda").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_search_toggle_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("timetable_search_input").performTextInput("SAFE 499")
            composeTestRule.waitForIdle()

            val safeLecture = runBlocking {
                AuraDatabase.getInstance(ApplicationProvider.getApplicationContext()).lectureDao().getAllLecturesList()
            }.first { it.courseCode == "SAFE 499" }

            composeTestRule.onNodeWithTag("agenda_list_view")
                .performScrollToNode(hasTestTag("agenda_request_delete_${safeLecture.id}"))
            composeTestRule.onNodeWithText("SAFE 499 • Safe Schedule Engineering").assertIsDisplayed()

            // Click delete button on Agenda card
            composeTestRule.onNodeWithTag("agenda_request_delete_${safeLecture.id}").performClick()
            composeTestRule.waitForIdle()

            // Verify lecture was NOT immediately deleted — confirmation dialog is displayed!
            composeTestRule.onNodeWithTag("confirm_delete_lecture_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("confirm_delete_course_from_timetable_button").assertIsDisplayed()

            // Cancel first -> lecture stays in timetable
            composeTestRule.onNodeWithTag("cancel_delete_lecture_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("agenda_list_view")
                .performScrollToNode(hasTestTag("agenda_request_delete_${safeLecture.id}"))
            composeTestRule.onNodeWithText("SAFE 499 • Safe Schedule Engineering").assertIsDisplayed()

            // Now remove the slot and verify 1-Tap Undo Banner restores it!
            composeTestRule.onNodeWithTag("agenda_request_delete_${safeLecture.id}").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("confirm_remove_lecture_slot_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("agenda_list_view")
                .performScrollToNode(hasTestTag("timetable_undo_delete_banner"))
            composeTestRule.onNodeWithTag("timetable_undo_delete_banner").assertIsDisplayed()
            composeTestRule.onNodeWithTag("timetable_undo_delete_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(5_000L) {
                runBlocking {
                    AuraDatabase.getInstance(ApplicationProvider.getApplicationContext())
                        .lectureDao()
                        .getAllLecturesList()
                        .any { it.courseCode == "SAFE 499" }
                }
            }

            // Finally test "Delete Entire Course (SAFE 499)" right from the Timetable confirmation dialog!
            val restoredLecture = runBlocking {
                AuraDatabase.getInstance(ApplicationProvider.getApplicationContext()).lectureDao().getAllLecturesList()
            }.first { it.courseCode == "SAFE 499" }

            composeTestRule.onNodeWithTag("agenda_list_view")
                .performScrollToNode(hasTestTag("agenda_request_delete_${restoredLecture.id}"))
            composeTestRule.onNodeWithTag("agenda_request_delete_${restoredLecture.id}").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("confirm_delete_course_from_timetable_button")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(5_000L) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                runBlocking {
                    AuraDatabase.getInstance(ApplicationProvider.getApplicationContext())
                        .courseDao()
                        .getAllCoursesList()
                        .none { it.code == "SAFE 499" }
                }
            }
        }
    }

    @Test
    fun auroraHomeScreenWidgetRendersLiquidGlassViewsAndDeepLinksCleanly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // 1. Verify Live-Now vs Up-Next summary & custom high-DPI Aurora Bitmap renderers
            val liveSummary = com.example.widget.AuraUpNextWidgetProvider.buildWidgetSummary(
                lectures = listOf(
                    LectureEntity(
                        id = 55L,
                        courseId = 3L,
                        courseCode = "CSCD 311",
                        courseName = "Compiler Design",
                        dayOfWeek = 2,
                        startTime = "10:00",
                        endTime = "12:00",
                        venue = "Hall C2",
                        colorHex = "#34D399"
                    )
                ),
                pendingTasksCount = 2,
                weeklyFocusMinutes = 480,
                nowDayOfWeek = 2,
                nowTime24 = "10:45",
                weeklyGoalHours = 16
            )
            assertTrue(liveSummary.isLiveNow)
            assertEquals("AURA • LIVE CLASS NOW", liveSummary.badgeText)
            assertEquals(0.5f, liveSummary.focusProgressFraction, 0.01f)

            val backdropBmp = com.example.widget.AuraUpNextWidgetProvider.renderWidgetBackdropBitmap(
                widthPx = 720,
                heightPx = 340,
                isLiveNow = true
            )
            assertEquals(720, backdropBmp.width)
            assertEquals(340, backdropBmp.height)

            val ringBmp = com.example.widget.AuraUpNextWidgetProvider.renderWidgetFocusRingBitmap(
                sizePx = 156,
                progressFraction = 0.65f
            )
            assertEquals(156, ringBmp.width)

            // 2. Inflate real RemoteViews into a parent FrameLayout to guarantee 100% RemoteViews XML & drawable compatibility
            val remoteViews = com.example.widget.AuraUpNextWidgetProvider.buildRemoteViewsForContext(context)
            val hostParent = android.widget.FrameLayout(context)
            val inflatedWidget = remoteViews.apply(context, hostParent)
            assertNotNull(inflatedWidget)
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_aurora_backdrop))
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_focus_ring_image))
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_btn_timetable))
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_btn_focus))
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_btn_slides))
            assertNotNull(inflatedWidget.findViewById<android.view.View>(R.id.widget_btn_ai))
        }

        // 3. Verify launching MainActivity via Widget "Slides" dock shortcut routes directly to Study -> Course Slides
        val context = ApplicationProvider.getApplicationContext<Context>()
        val slidesWidgetIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(
                com.example.widget.AuraUpNextWidgetProvider.EXTRA_WIDGET_DESTINATION,
                com.example.widget.AuraUpNextWidgetProvider.DEST_SLIDES
            )
        }
        ActivityScenario.launch<MainActivity>(slidesWidgetIntent).use {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("upload_course_slide_button").assertIsDisplayed()
        }
    }

    @Test
    fun fiveAuroraHomeScreenWidgetsAndWidgetStudioInflateAndRouteCleanly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val hostParent = android.widget.FrameLayout(context)

            // 1. Verify AuraWidgetThemePalette & custom Canvas renderers
            val violetPalette = com.example.widget.AuraWidgetThemePalette.fromPresetId("ROYAL_VIOLET")
            assertEquals("ROYAL_VIOLET", violetPalette.presetId)

            val urgencyRing = com.example.widget.AuraWidgetSuiteManager.renderExamUrgencyRingBitmap(
                sizePx = 156,
                daysRemaining = 2,
                fallbackAccentInt = violetPalette.secondaryColorInt
            )
            assertEquals(156, urgencyRing.width)

            val progressBmp = com.example.widget.AuraWidgetSuiteManager.renderProgressBarBitmap(
                widthPx = 560,
                heightPx = 22,
                progressFraction = 0.72f,
                primaryColorInt = violetPalette.primaryColorInt,
                secondaryColorInt = violetPalette.secondaryColorInt
            )
            assertEquals(560, progressBmp.width)

            // 2. Inflate all 4 newly added specialized Home Screen Widgets into real Android Views
            val scheduleViews = com.example.widget.AuraTodayScheduleWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedSchedule = scheduleViews.apply(context, hostParent)
            assertNotNull(inflatedSchedule.findViewById<android.view.View>(R.id.schedule_widget_backdrop))
            assertNotNull(inflatedSchedule.findViewById<android.view.View>(R.id.schedule_btn_daily))
            assertNotNull(inflatedSchedule.findViewById<android.view.View>(R.id.schedule_btn_add_lecture))

            val examViews = com.example.widget.AuraExamCountdownWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedExam = examViews.apply(context, hostParent)
            assertNotNull(inflatedExam.findViewById<android.view.View>(R.id.exam_widget_backdrop))
            assertNotNull(inflatedExam.findViewById<android.view.View>(R.id.exam_widget_ring_image))
            assertNotNull(inflatedExam.findViewById<android.view.View>(R.id.exam_btn_tasks))
            assertNotNull(inflatedExam.findViewById<android.view.View>(R.id.exam_btn_war_room))

            val focusViews = com.example.widget.AuraFocusStreakWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedFocus = focusViews.apply(context, hostParent)
            assertNotNull(inflatedFocus.findViewById<android.view.View>(R.id.focus_widget_backdrop))
            assertNotNull(inflatedFocus.findViewById<android.view.View>(R.id.focus_widget_ring_image))
            assertNotNull(inflatedFocus.findViewById<android.view.View>(R.id.focus_widget_start_btn))

            val deckViews = com.example.widget.AuraStudyDeckWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedDeck = deckViews.apply(context, hostParent)
            assertNotNull(inflatedDeck.findViewById<android.view.View>(R.id.study_deck_widget_backdrop))
            assertNotNull(inflatedDeck.findViewById<android.view.View>(R.id.study_deck_progress_bar))
            assertNotNull(inflatedDeck.findViewById<android.view.View>(R.id.study_deck_btn_slides))
            assertNotNull(inflatedDeck.findViewById<android.view.View>(R.id.study_deck_btn_flashcards))
            assertNotNull(inflatedDeck.findViewById<android.view.View>(R.id.study_deck_btn_quiz))
        }

        // 3. Verify new deep-link route (DEST_ADD_LECTURE opens Add Lecture Dialog on Timetable) & Profile -> Widget Studio
        val context = ApplicationProvider.getApplicationContext<Context>()
        val addLectureIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(
                com.example.widget.AuraUpNextWidgetProvider.EXTRA_WIDGET_DESTINATION,
                com.example.widget.AuraUpNextWidgetProvider.DEST_ADD_LECTURE
            )
        }
        ActivityScenario.launch<MainActivity>(addLectureIntent).use {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("add_lecture_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("cancel_lecture_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("nav_item_profile").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("profile_lazy_list")
                .performScrollToNode(hasTestTag("open_widget_studio_card"))
            composeTestRule.onNodeWithTag("open_widget_studio_card").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("widget_studio_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithTag("widget_studio_sync_all_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("widget_studio_card_up_next").assertIsDisplayed()
        }
    }

    @Test
    fun cloudProfileSettingsAndAcademicVaultRoundTripAndCustomFocusAnyDurationWork() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AuraDatabase.getInstance(app)
        val prefs = AuraPreferences(app)

        runBlocking {
            // 1. Configure custom student profile & settings
            prefs.updateProfile(
                name = "Ama Mensah",
                institution = "KNUST Kumasi",
                programme = "BSc Computer Engineering",
                level = "Level 300",
                semester = "Semester 2",
                timeZone = "Africa/Accra",
                attendanceTarget = 82,
                studyGoalHours = 28
            )
            prefs.setAccentPreset("ROYAL_VIOLET")
            prefs.setWidgetGlassStyle("DAYLIGHT_PEARL")
            prefs.setDefaultSnoozeMinutes(15)

            val exportedSettings = prefs.exportProfileSettingsList("ama.mensah@knust.edu.gh")
            assertTrue(exportedSettings.any { it.startsWith("name=Ama Mensah") })
            assertTrue(exportedSettings.any { it.startsWith("institution=KNUST Kumasi") })
            assertTrue(exportedSettings.any { it.startsWith("email=ama.mensah@knust.edu.gh") })
            assertTrue(exportedSettings.any { it.startsWith("widgetGlassStyle=DAYLIGHT_PEARL") })

            // Wipe/overwrite local profile, then restore from cloud settings list
            prefs.updateProfile(
                name = "Temp User",
                institution = "Other Campus",
                programme = "General",
                level = "Level 100",
                semester = "Semester 1",
                timeZone = "UTC",
                attendanceTarget = 75,
                studyGoalHours = 10
            )
            prefs.setAccentPreset("GLACIAL_CYAN")
            prefs.setWidgetGlassStyle("FROSTED_OBSIDIAN")

            prefs.restoreFromSettingsList(
                entries = exportedSettings,
                studentName = "Ama Mensah",
                institution = "KNUST Kumasi",
                programme = "BSc Computer Engineering"
            )
            val restoredProfile = prefs.userProfileFlow.first()
            assertEquals("Ama Mensah", restoredProfile.name)
            assertEquals("KNUST Kumasi", restoredProfile.institution)
            assertEquals("BSc Computer Engineering", restoredProfile.programme)
            assertEquals("ama.mensah@knust.edu.gh", restoredProfile.email)
            assertEquals(82, restoredProfile.attendanceTargetPercent)
            assertEquals(28, restoredProfile.studyGoalHoursPerWeek)
            assertEquals("ROYAL_VIOLET", restoredProfile.accentPreset)
            assertEquals("DAYLIGHT_PEARL", restoredProfile.widgetGlassStyle)
            assertEquals(15, restoredProfile.defaultSnoozeMinutes)

            // 2. Verify AuraViewModel unrestricted Custom Focus Timer (any hours/mins/secs) & Cloud Backup builder
            val vm = com.example.ui.AuraViewModel(app)
            vm.setCustomFocusDurationSeconds(95) // 1 min 35 secs
            assertEquals(95, vm.focusRemainingSeconds.value)

            vm.setCustomFocusDurationSeconds(3 * 3600 + 15 * 60 + 20) // 3h 15m 20s
            assertEquals(11720, vm.focusRemainingSeconds.value)

            vm.adjustFocusDurationMinutes(10) // +10m => 3h 25m 00s or +600s
            assertTrue(vm.focusRemainingSeconds.value >= 12000)

            assertTrue(vm.shouldAutoRestoreForUid("uid_cross_town_scholar_1"))
            prefs.markUidAutoRestored("uid_cross_town_scholar_1")
            assertFalse(vm.shouldAutoRestoreForUid("uid_cross_town_scholar_1"))

            // Insert sample course + lecture + note and verify buildCloudBackupPayload captures all
            val cId = db.courseDao().insertCourse(
                CourseEntity(code = "COE 354", name = "Operating Systems", creditHours = 4, lecturer = "Dr. Boateng", venue = "PB 201")
            )
            db.lectureDao().insertLecture(
                LectureEntity(
                    courseId = cId,
                    courseCode = "COE 354",
                    courseName = "Operating Systems",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "10:00",
                    venue = "PB 201",
                    walkingMinutes = 15
                )
            )
            db.studyNoteDao().insertStudyNote(
                StudyNoteEntity(
                    courseCode = "COE 354",
                    title = "Deadlock Coffman Conditions",
                    content = "Mutual exclusion, hold and wait, no preemption, and circular wait.",
                    category = "CONCEPT",
                    isPinned = true
                )
            )

            val payload = vm.buildCloudBackupPayload("uid_ama_123", "ama.mensah@knust.edu.gh")
            assertEquals("Ama Mensah", payload.studentName)
            assertEquals("KNUST Kumasi", payload.institution)
            assertTrue(payload.profileSettings.isNotEmpty())
            assertTrue(payload.courseRecords.any { it.contains("COE 354") })
            assertTrue(payload.lectureRecords.any { it.contains("COE 354") })
            assertTrue(payload.noteRecords.any { it.contains("Deadlock Coffman Conditions") })
        }
    }

    @Test
    fun widgetGlassFrostStylesDayFlipperAndCompletionToolsComputeAccurately() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AuraPreferences(context)

        runBlocking {
            // 1. Verify Widget Glass Frost Styles & interactive Schedule/Exam buttons inflate cleanly
            prefs.setWidgetGlassStyle("CRYSTAL_CLEAR")
            val hostParent = android.widget.FrameLayout(context)

            val scheduleViews = com.example.widget.AuraTodayScheduleWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedSchedule = scheduleViews.apply(context, hostParent)
            assertNotNull(inflatedSchedule.findViewById<android.view.View>(R.id.schedule_btn_prev_day))
            assertNotNull(inflatedSchedule.findViewById<android.view.View>(R.id.schedule_btn_next_day))

            val examViews = com.example.widget.AuraExamCountdownWidgetProvider.buildRemoteViewsForContext(context)
            val inflatedExam = examViews.apply(context, hostParent)
            assertNotNull(inflatedExam.findViewById<android.view.View>(R.id.exam_btn_mark_done))

            // Step day offset forward and reset
            prefs.setScheduleWidgetDayOffset(0)
            val nextOffset = prefs.stepScheduleWidgetDayOffset(1)
            assertEquals(1, nextOffset)
            prefs.setScheduleWidgetDayOffset(0)
            assertEquals(0, prefs.userProfileFlow.first().scheduleWidgetDayOffset)

            // 2. Completion Feature 1: 90-Min REM Sleep Cycle & First-Lecture Wake-Up Planner
            val sampleLectures = listOf(
                LectureEntity(
                    courseId = 1L,
                    courseCode = "CSCD 311",
                    courseName = "Compiler Design",
                    dayOfWeek = java.time.LocalDate.now().plusDays(1).dayOfWeek.value,
                    startTime = "08:00",
                    endTime = "10:00",
                    venue = "JQB 14",
                    walkingMinutes = 15
                ),
                LectureEntity(
                    courseId = 2L,
                    courseCode = "MATH 351",
                    courseName = "Numerical Analysis",
                    dayOfWeek = 2,
                    startTime = "10:00",
                    endTime = "13:00",
                    venue = "Math Block"
                ),
                LectureEntity(
                    courseId = 3L,
                    courseCode = "STAT 332",
                    courseName = "Stochastic Processes",
                    dayOfWeek = 2,
                    startTime = "14:00",
                    endTime = "16:30",
                    venue = "Stats Lab"
                )
            )
            val sleepPlan = com.example.util.AcademicCompletionTools.computeWakeUpAndSleepPlan(
                lectures = sampleLectures,
                morningPrepMinutes = 35
            )
            assertEquals("8:00 AM", sleepPlan.firstLectureStart12Hour)
            assertEquals("7:45 AM", sleepPlan.leaveHostel12Hour)
            assertEquals("7:10 AM", sleepPlan.recommendedWakeUp12Hour)
            assertEquals(3, sleepPlan.bedtimeOptions.size)

            // 3. Completion Feature 2: Semester Workload, Marathon Day & Exam Cluster Advisor
            val nowMs = System.currentTimeMillis()
            val workload = com.example.util.AcademicCompletionTools.analyzeSemesterWorkload(
                courses = listOf(
                    CourseEntity(code = "CSCD 311", name = "Compiler Design", creditHours = 4),
                    CourseEntity(code = "MATH 351", name = "Numerical Analysis", creditHours = 4)
                ),
                lectures = sampleLectures,
                assessments = listOf(
                    AssessmentEntity(
                        courseId = 1L,
                        courseCode = "CSCD 311",
                        courseName = "Compiler Design",
                        title = "Midsem Paper",
                        type = "EXAM",
                        dueEpochMillis = nowMs + 3 * 86_400_000L
                    ),
                    AssessmentEntity(
                        courseId = 2L,
                        courseCode = "MATH 351",
                        courseName = "Numerical Analysis",
                        title = "Written Exam",
                        type = "EXAM",
                        dueEpochMillis = nowMs + 4 * 86_400_000L
                    )
                )
            )
            assertEquals(8, workload.totalCreditHours)
            assertEquals(16, workload.recommendedSelfStudyHours)
            assertTrue(workload.marathonDays.isNotEmpty())
            assertTrue(workload.examClusterAlerts.isNotEmpty())

            // 4. Completion Feature 3 & Oral Viva deterministic offline grading
            val aiService = com.example.ai.AuraAiService(context)
            val vivaRes = aiService.evaluateVivaAnswer(
                courseCode = "CSCD 311",
                question = "What is the role of lexical analysis in a compiler?",
                expectedAnswer = "Lexical analysis scans source characters into tokens and removes whitespace and comments.",
                studentSpokenAnswer = "Lexical analysis scans characters to produce tokens and strips comments and whitespace."
            )
            assertTrue(vivaRes.scoreOutOfTen >= 6)

            val rubricRes = aiService.critiqueAcademicDraft(
                courseCode = "CSCD 311",
                title = "LL(1) vs LR(1) Parsing",
                draftContent = "LL(1) parsers construct a leftmost derivation top-down because they inspect one lookahead token; however, LR(1) parsers handle a broader class of context-free grammars bottom-up. For example, left-recursive rules work naturally in LR tables."
            )
            assertTrue(rubricRes.clarityScore >= 65)
            assertTrue(rubricRes.estimatedGradeBand.isNotBlank())
        }
    }
}
