package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.QuizQuestionEntity
import com.example.data.local.entity.StudyNoteEntity
import com.example.data.local.entity.SyllabusTopicEntity
import com.example.util.CitationFormatter
import com.example.util.ParsedCitationDraft
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class SlideExtractionResult(
    val title: String,
    val category: String,
    val extractedNotes: String,
    val flashcards: List<Pair<String, String>>,
    val usedCloudAi: Boolean
)

data class ResourceStudySynthesisResult(
    val summaryTitle: String,
    val summaryContent: String,
    val flashcards: List<Pair<String, String>>,
    val quizQuestions: List<QuizQuestionEntity>,
    val usedCloudAi: Boolean
)

data class ScannedCourseDraft(
    val code: String,
    val name: String,
    val creditHours: Int = 3,
    val lecturer: String = "",
    val venue: String = ""
)

data class ScannedLectureDraft(
    val courseCode: String,
    val courseName: String,
    val dayOfWeek: Int, // 1=Mon .. 7=Sun
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val venue: String = "Main Lecture Hall",
    val walkingMinutes: Int = 10
)

data class ScannedExamDraft(
    val courseCode: String,
    val courseName: String,
    val title: String,
    val examVenue: String = "",
    val daysFromNow: Int = 14
)

data class TimetableScanResult(
    val courses: List<ScannedCourseDraft>,
    val lectures: List<ScannedLectureDraft>,
    val exams: List<ScannedExamDraft>,
    val usedCloudAi: Boolean
)

class AuraAiService(private val context: Context) {

    private val TAG = "AuraAiService"

    private val generativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.ai.generativeModel("gemini-3.8-flash")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI Logic model init error: ${e.message}")
            null
        }
    }

    private val quizSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "University-level exam question"),
                            "optionA" to Schema.string(description = "Choice A"),
                            "optionB" to Schema.string(description = "Choice B"),
                            "optionC" to Schema.string(description = "Choice C"),
                            "optionD" to Schema.string(description = "Choice D"),
                            "correctIndex" to Schema.integer(description = "Index 0..3 for A..D"),
                            "explanation" to Schema.string(description = "Brief academic explanation")
                        )
                    )
                )
            )
        )
    }

    private val quizGenerativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = quizSchema
                    }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Quiz model init error: ${e.message}")
            null
        }
    }

    private val slideScanSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Concise topic or slide title"),
                "category" to Schema.enumeration(
                    values = listOf("FORMULA", "SUMMARY", "CONCEPT", "CHEAT_SHEET"),
                    description = "Best matching study note category"
                ),
                "extractedNotes" to Schema.string(
                    description = "Clean structured bullet points and exact equations/formulas extracted from the image"
                ),
                "flashcards" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "Active recall question"),
                            "answer" to Schema.string(description = "Concise accurate answer")
                        )
                    )
                )
            )
        )
    }

    private val slideGenerativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = slideScanSchema
                    }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Slide scanner model init error: ${e.message}")
            null
        }
    }

    private val citationSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "sourceType" to Schema.enumeration(
                    values = listOf("JOURNAL", "BOOK", "WEBSITE", "LECTURE"),
                    description = "Source classification"
                ),
                "authors" to Schema.string(description = "Formatted author names, e.g. 'Lamport, L.' or 'Tanenbaum, A. S., & Bos, H.'"),
                "year" to Schema.string(description = "4-digit publication year or n.d."),
                "title" to Schema.string(description = "Full title of the article, book, or webpage"),
                "publicationOrPublisher" to Schema.string(description = "Journal name, publisher, or website name"),
                "volumeOrPages" to Schema.string(description = "Volume, issue, and page numbers if available"),
                "urlOrDoi" to Schema.string(description = "URL or DOI if available")
            )
        )
    }

    private val citationGenerativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = citationSchema
                    }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Citation model init error: ${e.message}")
            null
        }
    }

    suspend fun parseCitationSource(rawInput: String): ParsedCitationDraft = withContext(Dispatchers.IO) {
        val model = citationGenerativeModel
        if (model != null && rawInput.isNotBlank()) {
            try {
                return@withContext withTimeout(35_000L) {
                    val prompt = """
                        Extract accurate university bibliographic metadata from the following raw academic reference, topic, URL, or DOI:
                        "$rawInput"
                    """.trimIndent()
                    val response = model.generateContent(prompt)
                    val jsonText = response.text
                    if (!jsonText.isNullOrBlank()) {
                        val root = JSONObject(jsonText)
                        ParsedCitationDraft(
                            sourceType = root.optString("sourceType", "JOURNAL"),
                            authors = root.optString("authors", "").ifBlank { "Academic Author" },
                            year = root.optString("year", "").ifBlank { "2026" },
                            title = root.optString("title", "").ifBlank { rawInput.take(80) },
                            publicationOrPublisher = root.optString("publicationOrPublisher", ""),
                            volumeOrPages = root.optString("volumeOrPages", ""),
                            urlOrDoi = root.optString("urlOrDoi", "")
                        )
                    } else {
                        CitationFormatter.parseRawSourceDeterministically(rawInput)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI citation parse failed, using deterministic parser: ${e.message}", e)
            }
        }
        CitationFormatter.parseRawSourceDeterministically(rawInput)
    }

    suspend fun getAiResponse(
        prompt: String,
        studentName: String,
        courses: List<CourseEntity>,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        attendance: List<AttendanceRecordEntity>,
        weeklyStudyHours: Float
    ): String = withContext(Dispatchers.IO) {
        val contextSummary = buildAcademicContext(
            studentName = studentName,
            courses = courses,
            lectures = lectures,
            assessments = assessments,
            attendance = attendance,
            weeklyStudyHours = weeklyStudyHours
        )

        val model = generativeModel
        if (model != null) {
            try {
                return@withContext withTimeout(45_000L) {
                    val fullPrompt = """
                        You are AURA AI, an intelligent university academic companion and study advisor for $studentName.
                        Below is the student's verified academic context:
                        $contextSummary
                        
                        Student's query: "$prompt"
                        
                        Provide a concise, highly practical, motivating, and directly actionable answer.
                        Ground your answer in their actual courses, lecture times, assignments, and exam dates.
                    """.trimIndent()

                    val response = model.generateContent(fullPrompt)
                    response.text ?: AuraAcademicAdvisor.answerAcademicQuery(
                        query = prompt,
                        studentName = studentName,
                        courses = courses,
                        lectures = lectures,
                        assessments = assessments,
                        studyHoursThisWeek = weeklyStudyHours,
                        attendanceList = attendance
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini call failed or timed out: ${e.message}. Using deterministic academic advisor.", e)
            }
        }

        AuraAcademicAdvisor.answerAcademicQuery(
            query = prompt,
            studentName = studentName,
            courses = courses,
            lectures = lectures,
            assessments = assessments,
            studyHoursThisWeek = weeklyStudyHours,
            attendanceList = attendance
        )
    }

    /**
     * Generates 4-5 multiple-choice practice exam questions grounded in the student's
     * course notes, flashcards, and syllabus topics via Gemini 3.8 Flash structured JSON,
     * with deterministic synthesis from saved study materials if offline.
     */
    suspend fun generatePracticeQuiz(
        courseCode: String,
        courseName: String,
        notes: List<StudyNoteEntity>,
        flashcards: List<FlashcardEntity>,
        topics: List<SyllabusTopicEntity>
    ): List<QuizQuestionEntity> = withContext(Dispatchers.IO) {
        val courseNotes = notes.filter { it.courseCode.equals(courseCode, ignoreCase = true) || courseCode == "General" }
        val courseCards = flashcards.filter { it.courseCode.equals(courseCode, ignoreCase = true) || courseCode == "General" }
        val courseTopics = topics.filter { it.courseCode.equals(courseCode, ignoreCase = true) }

        val contextMaterial = buildString {
            appendLine("Course: $courseCode ($courseName)")
            if (courseTopics.isNotEmpty()) {
                appendLine("Syllabus Topics: ${courseTopics.joinToString(", ") { it.topicName }}")
            }
            if (courseNotes.isNotEmpty()) {
                appendLine("Study Notes & Formulas: ${courseNotes.take(6).joinToString(" | ") { "${it.title}: ${it.content}" }}")
            }
            if (courseCards.isNotEmpty()) {
                appendLine("Flashcards: ${courseCards.take(6).joinToString(" | ") { "Q: ${it.question} A: ${it.answer}" }}")
            }
        }

        val qModel = quizGenerativeModel
        if (qModel != null) {
            try {
                val jsonText = withTimeout(45_000L) {
                    val prompt = """
                        Generate 5 challenging university-level multiple-choice practice exam questions for $courseCode ($courseName).
                        Ground questions in this student's study context when available:
                        $contextMaterial
                    """.trimIndent()
                    qModel.generateContent(prompt).text
                }
                if (!jsonText.isNullOrBlank()) {
                    val root = JSONObject(jsonText)
                    val arr = root.getJSONArray("questions")
                    val parsed = mutableListOf<QuizQuestionEntity>()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        parsed.add(
                            QuizQuestionEntity(
                                courseCode = courseCode,
                                question = item.getString("question"),
                                optionA = item.getString("optionA"),
                                optionB = item.getString("optionB"),
                                optionC = item.getString("optionC"),
                                optionD = item.getString("optionD"),
                                correctOptionIndex = item.optInt("correctIndex", 0).coerceIn(0, 3),
                                explanation = item.optString("explanation", ""),
                                sourceTag = "AI"
                            )
                        )
                    }
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Structured quiz generation fallback triggered: ${e.message}", e)
            }
        }

        synthesizeSmartQuizFromVault(courseCode, courseName, courseNotes, courseCards, courseTopics)
    }

    /**
     * Multimodal Slide / Whiteboard Photo -> Study Notes + Active Recall Flashcards
     */
    suspend fun extractNotesFromSlideBitmap(
        bitmap: Bitmap,
        courseCode: String
    ): SlideExtractionResult = withContext(Dispatchers.IO) {
        val sModel = slideGenerativeModel
        if (sModel != null) {
            try {
                val jsonText = withTimeout(60_000L) {
                    sModel.generateContent(
                        content {
                            image(bitmap)
                            text(
                                "Extract all visible formulas, definitions, equations, and key academic bullet points " +
                                    "from this university lecture slide or textbook photo for course $courseCode. " +
                                    "Also create 2 to 4 active-recall flashcards."
                            )
                        }
                    ).text
                }
                if (!jsonText.isNullOrBlank()) {
                    val root = JSONObject(jsonText)
                    val cardsArr = root.optJSONArray("flashcards")
                    val cards = mutableListOf<Pair<String, String>>()
                    if (cardsArr != null) {
                        for (i in 0 until cardsArr.length()) {
                            val c = cardsArr.getJSONObject(i)
                            val q = c.optString("question")
                            val a = c.optString("answer")
                            if (q.isNotBlank() && a.isNotBlank()) cards.add(q to a)
                        }
                    }
                    return@withContext SlideExtractionResult(
                        title = root.optString("title", "$courseCode Slide Summary"),
                        category = root.optString("category", "SUMMARY"),
                        extractedNotes = root.optString("extractedNotes", ""),
                        flashcards = cards,
                        usedCloudAi = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Slide multimodal extraction failed: ${e.message}", e)
            }
        }

        // Offline / unprovisioned editable template with image metadata
        SlideExtractionResult(
            title = "$courseCode Lecture Slide (${bitmap.width}×${bitmap.height})",
            category = "SUMMARY",
            extractedNotes = "• Slide captured (${bitmap.width}×${bitmap.height}px) for $courseCode\n• Key Formula / Theorem: \n• Exam Takeaway: ",
            flashcards = listOf(
                "Core concept from $courseCode slide" to "Review key definition and formula captured from lecture slide."
            ),
            usedCloudAi = false
        )
    }

    /**
     * Synthesizes a complete study pack (Summary Note + Active-Recall Flashcards + Practice Quiz MCQs)
     * directly from an uploaded PDF, PowerPoint (.pptx), Word (.docx), or Text slide deck.
     */
    suspend fun synthesizeStudyPackFromSlides(
        courseCode: String,
        resourceTitle: String,
        plainSlideText: String,
        optionalBitmap: Bitmap? = null
    ): ResourceStudySynthesisResult = withContext(Dispatchers.IO) {
        val trimmedContext = plainSlideText.take(8_000)

        val packSchema = Schema.obj(
            properties = mapOf(
                "summaryTitle" to Schema.string(description = "Concise study summary title"),
                "summaryContent" to Schema.string(description = "Bullet-point summary of key concepts, definitions, and formulas"),
                "flashcards" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "Active recall question"),
                            "answer" to Schema.string(description = "Concise accurate answer")
                        )
                    )
                ),
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "Exam MCQ question based on the slides"),
                            "optionA" to Schema.string(description = "Choice A"),
                            "optionB" to Schema.string(description = "Choice B"),
                            "optionC" to Schema.string(description = "Choice C"),
                            "optionD" to Schema.string(description = "Choice D"),
                            "correctIndex" to Schema.integer(description = "0..3"),
                            "explanation" to Schema.string(description = "Explanation grounded in slide content")
                        )
                    )
                )
            )
        )

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            try {
                val packModel = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = packSchema
                    }
                )
                val jsonText = withTimeout(50_000L) {
                    val promptText = """
                        Analyze this university course resource '$resourceTitle' for course $courseCode.
                        Create:
                        1. A crisp exam-focused bullet summary with key definitions & formulas.
                        2. 6 active-recall flashcards.
                        3. 10 multiple-choice exam questions (4 options each).
                        
                        Slide / Document Content:
                        $trimmedContext
                    """.trimIndent()

                    if (optionalBitmap != null) {
                        packModel.generateContent(
                            content {
                                image(optionalBitmap)
                                text(promptText)
                            }
                        ).text
                    } else {
                        packModel.generateContent(promptText).text
                    }
                }

                if (!jsonText.isNullOrBlank()) {
                    val root = JSONObject(jsonText)
                    val cards = mutableListOf<Pair<String, String>>()
                    val cardsArr = root.optJSONArray("flashcards")
                    if (cardsArr != null) {
                        for (i in 0 until cardsArr.length()) {
                            val item = cardsArr.getJSONObject(i)
                            val q = item.optString("question")
                            val a = item.optString("answer")
                            if (q.isNotBlank() && a.isNotBlank()) cards.add(q to a)
                        }
                    }

                    val questions = mutableListOf<QuizQuestionEntity>()
                    val qArr = root.optJSONArray("questions")
                    if (qArr != null) {
                        for (i in 0 until qArr.length()) {
                            val item = qArr.getJSONObject(i)
                            questions.add(
                                QuizQuestionEntity(
                                    courseCode = courseCode,
                                    question = item.getString("question"),
                                    optionA = item.getString("optionA"),
                                    optionB = item.getString("optionB"),
                                    optionC = item.getString("optionC"),
                                    optionD = item.getString("optionD"),
                                    correctOptionIndex = item.optInt("correctIndex", 0).coerceIn(0, 3),
                                    explanation = item.optString("explanation", ""),
                                    sourceTag = "SLIDES"
                                )
                            )
                        }
                    }

                    val sumContent = root.optString("summaryContent", "")
                    if (sumContent.isNotBlank() && questions.isNotEmpty()) {
                        return@withContext ResourceStudySynthesisResult(
                            summaryTitle = root.optString("summaryTitle", "$resourceTitle — Key Takeaways"),
                            summaryContent = sumContent,
                            flashcards = cards,
                            quizQuestions = questions,
                            usedCloudAi = true
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Slide study pack AI fallback triggered: ${e.message}")
            }
        }

        // Deterministic offline synthesis directly from extracted slide lines (6 Flashcards + 10 Exam MCQs)
        val meaningfulLines = plainSlideText
            .lines()
            .map { it.removePrefix("•").removePrefix("-").trim() }
            .filter { it.length in 10..240 && !it.startsWith("PDF Page ") }
            .distinct()

        val topBullets = meaningfulLines.take(10)
        val summaryBody = if (topBullets.isNotEmpty()) {
            topBullets.joinToString("\n") { "• $it" }
        } else {
            "• Core study deck: $resourceTitle ($courseCode)\n• Review bookmarked slides and personal annotations before exam day."
        }

        val generatedCards = mutableListOf<Pair<String, String>>()
        topBullets.take(6).forEachIndexed { idx, line ->
            val colonSplit = line.split(":", limit = 2)
            if (colonSplit.size == 2 && colonSplit[0].length in 3..60 && colonSplit[1].length >= 4) {
                generatedCards.add("Explain '${colonSplit[0].trim()}' from $resourceTitle ($courseCode)" to colonSplit[1].trim())
            } else {
                generatedCards.add("Key concept #${idx + 1} from '$resourceTitle' ($courseCode)" to line)
            }
        }
        val fallbackConcepts = listOf(
            "Core theorem & assumptions in '$resourceTitle' ($courseCode)" to "Validate domain constraints, boundary conditions, and definitions presented in $resourceTitle.",
            "Primary problem-solving workflow for '$resourceTitle'" to "Identify known variables, apply core formula from $courseCode slides, and verify units/complexity.",
            "Common exam pitfall in '$resourceTitle' ($courseCode)" to "Skipping edge-case boundary checks or misapplying assumptions outside valid intervals.",
            "Practical application of '$resourceTitle'" to "Connects theoretical principles in $courseCode to real-world system analysis and exam case studies.",
            "High-yield summary check for '$resourceTitle'" to "Synthesize slide headings, lecturer notes, and worked examples using spaced repetition.",
            "Verification step on '$courseCode' exam questions" to "Cross-check dimensional consistency, asymptotic behavior, or base-case invariants."
        )
        var padIdx = 0
        while (generatedCards.size < 6) {
            generatedCards.add(fallbackConcepts[padIdx % fallbackConcepts.size])
            padIdx++
        }

        val seedPairs = (generatedCards + fallbackConcepts).take(10)
        val generatedQuestions = (0 until 10).map { idx ->
            val (q, ans) = seedPairs[idx % seedPairs.size]
            val correctIdx = idx % 4
            val cleanAns = ans.take(115)
            val opts = MutableList(4) { slot ->
                when (slot) {
                    correctIdx -> cleanAns
                    0 -> "Unrelated assumption outside the scope of $resourceTitle"
                    1 -> "Static boundary case contradicting $courseCode lecture slides"
                    2 -> "Deprecated approximation not supported by the slide deck"
                    else -> "Inverse corollary independent of primary variables"
                }
            }
            QuizQuestionEntity(
                courseCode = courseCode,
                question = "Q${idx + 1} [$courseCode • $resourceTitle]: $q?",
                optionA = opts[0],
                optionB = opts[1],
                optionC = opts[2],
                optionD = opts[3],
                correctOptionIndex = correctIdx,
                explanation = "Grounded in '$resourceTitle': $cleanAns",
                sourceTag = "SLIDES"
            )
        }

        ResourceStudySynthesisResult(
            summaryTitle = "$resourceTitle — Slide Summary",
            summaryContent = summaryBody,
            flashcards = generatedCards.take(6),
            quizQuestions = generatedQuestions,
            usedCloudAi = false
        )
    }

    /**
     * Multimodal Timetable & Syllabus Auto-Scanner (Photo, PDF Text, or Portal Schedule Text)
     * Uses Gemini 3.8 Flash structured JSON with a deterministic regex/line parser fallback.
     */
    suspend fun scanTimetableOrSyllabus(
        bitmap: Bitmap?,
        rawText: String
    ): TimetableScanResult = withContext(Dispatchers.IO) {
        val scheduleSchema = Schema.obj(
            properties = mapOf(
                "courses" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "code" to Schema.string(description = "Course code e.g. CSCD 301"),
                            "name" to Schema.string(description = "Course title"),
                            "creditHours" to Schema.integer(description = "Credit hours 1..6"),
                            "lecturer" to Schema.string(description = "Instructor name"),
                            "venue" to Schema.string(description = "Default room or hall")
                        )
                    )
                ),
                "lectures" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "courseCode" to Schema.string(description = "Course code"),
                            "courseName" to Schema.string(description = "Course name"),
                            "dayOfWeek" to Schema.integer(description = "1=Monday .. 7=Sunday"),
                            "startTime" to Schema.string(description = "24-hour HH:mm e.g. 08:30"),
                            "endTime" to Schema.string(description = "24-hour HH:mm e.g. 10:30"),
                            "venue" to Schema.string(description = "Room or lecture theatre")
                        )
                    )
                ),
                "exams" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "courseCode" to Schema.string(description = "Course code"),
                            "courseName" to Schema.string(description = "Course title"),
                            "title" to Schema.string(description = "Midterm or Final Exam title"),
                            "examVenue" to Schema.string(description = "Exam hall or room"),
                            "daysFromNow" to Schema.integer(description = "Approximate days until exam")
                        )
                    )
                )
            )
        )

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            try {
                val scannerModel = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = scheduleSchema
                    }
                )
                val prompt = """
                    Extract all university courses, weekly lecture time slots (convert any AM/PM times to 24-hour HH:mm, dayOfWeek 1=Mon..7=Sun), and exam dates/venues from this student timetable or syllabus.
                    Additional text context:
                    ${rawText.take(6_000)}
                """.trimIndent()

                val jsonText = withTimeout(55_000L) {
                    if (bitmap != null) {
                        scannerModel.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        ).text
                    } else {
                        scannerModel.generateContent(prompt).text
                    }
                }

                if (!jsonText.isNullOrBlank()) {
                    val root = JSONObject(jsonText)
                    val coursesOut = mutableListOf<ScannedCourseDraft>()
                    val cArr = root.optJSONArray("courses")
                    if (cArr != null) {
                        for (i in 0 until cArr.length()) {
                            val obj = cArr.getJSONObject(i)
                            val code = obj.optString("code").trim().uppercase()
                            if (code.isNotBlank()) {
                                coursesOut.add(
                                    ScannedCourseDraft(
                                        code = code,
                                        name = obj.optString("name", code).ifBlank { code },
                                        creditHours = obj.optInt("creditHours", 3).coerceIn(1, 6),
                                        lecturer = obj.optString("lecturer", ""),
                                        venue = obj.optString("venue", "")
                                    )
                                )
                            }
                        }
                    }

                    val lecturesOut = mutableListOf<ScannedLectureDraft>()
                    val lArr = root.optJSONArray("lectures")
                    if (lArr != null) {
                        for (i in 0 until lArr.length()) {
                            val obj = lArr.getJSONObject(i)
                            val code = obj.optString("courseCode").trim().uppercase()
                            if (code.isNotBlank()) {
                                lecturesOut.add(
                                    ScannedLectureDraft(
                                        courseCode = code,
                                        courseName = obj.optString("courseName", code).ifBlank { code },
                                        dayOfWeek = obj.optInt("dayOfWeek", 1).coerceIn(1, 7),
                                        startTime = normalize24Hour(obj.optString("startTime", "08:30"), "08:30"),
                                        endTime = normalize24Hour(obj.optString("endTime", "10:30"), "10:30"),
                                        venue = obj.optString("venue", "Lecture Hall")
                                    )
                                )
                            }
                        }
                    }

                    val examsOut = mutableListOf<ScannedExamDraft>()
                    val eArr = root.optJSONArray("exams")
                    if (eArr != null) {
                        for (i in 0 until eArr.length()) {
                            val obj = eArr.getJSONObject(i)
                            val code = obj.optString("courseCode").trim().uppercase()
                            if (code.isNotBlank()) {
                                examsOut.add(
                                    ScannedExamDraft(
                                        courseCode = code,
                                        courseName = obj.optString("courseName", code).ifBlank { code },
                                        title = obj.optString("title", "$code Examination"),
                                        examVenue = obj.optString("examVenue", ""),
                                        daysFromNow = obj.optInt("daysFromNow", 14).coerceIn(1, 180)
                                    )
                                )
                            }
                        }
                    }

                    if (coursesOut.isNotEmpty() || lecturesOut.isNotEmpty() || examsOut.isNotEmpty()) {
                        return@withContext TimetableScanResult(
                            courses = coursesOut,
                            lectures = lecturesOut,
                            exams = examsOut,
                            usedCloudAi = true
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Timetable AI scan fallback triggered: ${e.message}")
            }
        }

        parseScheduleTextDeterministically(rawText)
    }

    companion object {
        private val CODE_REGEX = Regex("\\b([A-Z]{2,5}\\s*\\d{3}[A-Z]?)\\b", RegexOption.IGNORE_CASE)
        private val TIME_RANGE_REGEX = Regex(
            "(\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)\\s*(?:-|–|to)\\s*(\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)",
            RegexOption.IGNORE_CASE
        )

        fun normalize24Hour(raw: String, fallback: String): String {
            val cleaned = raw.trim().uppercase()
            val m = Regex("(\\d{1,2}):(\\d{2})\\s*(AM|PM)?").find(cleaned) ?: return fallback
            var hour = m.groupValues[1].toIntOrNull() ?: return fallback
            val min = m.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val ampm = m.groupValues[3]
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            return String.format(java.util.Locale.US, "%02d:%02d", hour.coerceIn(0, 23), min)
        }

        fun parseScheduleTextDeterministically(rawText: String): TimetableScanResult {
            val coursesMap = linkedMapOf<String, ScannedCourseDraft>()
            val lectures = mutableListOf<ScannedLectureDraft>()
            val exams = mutableListOf<ScannedExamDraft>()

            val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
            lines.forEachIndexed { idx, line ->
                val codeMatch = CODE_REGEX.find(line)
                val code = codeMatch?.groupValues?.get(1)
                    ?.uppercase()
                    ?.replace(Regex("\\s+"), " ")
                    ?: return@forEachIndexed

                val segments = line.split("|").map { it.trim() }
                val firstPart = segments.firstOrNull().orEmpty()
                val inferredName = firstPart
                    .replace(Regex("(?i)^EXAM:\\s*"), "")
                    .replace(code, "", ignoreCase = true)
                    .removePrefix("-")
                    .removePrefix(":")
                    .trim()
                    .takeIf { it.length >= 3 }
                    ?: "$code Course"

                val venueCandidate = segments.lastOrNull { seg ->
                    seg.contains("Room", ignoreCase = true) ||
                        seg.contains("Hall", ignoreCase = true) ||
                        seg.contains("Block", ignoreCase = true) ||
                        seg.contains("Lab", ignoreCase = true) ||
                        seg.contains("Theatre", ignoreCase = true)
                } ?: "Room B${idx + 1}"

                if (!coursesMap.containsKey(code)) {
                    coursesMap[code] = ScannedCourseDraft(
                        code = code,
                        name = inferredName,
                        venue = venueCandidate
                    )
                }

                if (line.startsWith("EXAM", ignoreCase = true) || line.contains("Midterm", ignoreCase = true) || line.contains("Final Exam", ignoreCase = true)) {
                    exams.add(
                        ScannedExamDraft(
                            courseCode = code,
                            courseName = coursesMap[code]?.name ?: inferredName,
                            title = inferredName.ifBlank { "$code Examination" },
                            examVenue = venueCandidate,
                            daysFromNow = 14
                        )
                    )
                } else {
                    val day = when {
                        Regex("\\b(mon|monday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 1
                        Regex("\\b(tue|tues|tuesday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 2
                        Regex("\\b(wed|wednesday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 3
                        Regex("\\b(thu|thur|thurs|thursday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 4
                        Regex("\\b(fri|friday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 5
                        Regex("\\b(sat|saturday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 6
                        Regex("\\b(sun|sunday)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) -> 7
                        else -> (idx % 5) + 1
                    }
                    val timeMatch = TIME_RANGE_REGEX.find(line)
                    val start24 = timeMatch?.let { normalize24Hour(it.groupValues[1], "08:30") } ?: "08:30"
                    val end24 = timeMatch?.let { normalize24Hour(it.groupValues[2], "10:30") } ?: "10:30"

                    lectures.add(
                        ScannedLectureDraft(
                            courseCode = code,
                            courseName = coursesMap[code]?.name ?: inferredName,
                            dayOfWeek = day,
                            startTime = start24,
                            endTime = end24,
                            venue = venueCandidate
                        )
                    )
                }
            }

            return TimetableScanResult(
                courses = coursesMap.values.toList(),
                lectures = lectures,
                exams = exams,
                usedCloudAi = false
            )
        }
    }

    private fun synthesizeSmartQuizFromVault(
        courseCode: String,
        courseName: String,
        notes: List<StudyNoteEntity>,
        cards: List<FlashcardEntity>,
        topics: List<SyllabusTopicEntity>
    ): List<QuizQuestionEntity> {
        val generated = mutableListOf<QuizQuestionEntity>()

        // 1. Convert saved Flashcards into MCQs
        cards.take(3).forEachIndexed { idx, card ->
            val distractors = listOf(
                "Inverse assumption unrelated to ${card.courseCode}",
                "Deprecated heuristic not applicable under boundary conditions",
                "Constant-only approximation ignoring primary variables"
            )
            val options = mutableListOf(card.answer, distractors[0], distractors[1], distractors[2])
            val targetIdx = idx % 4
            val swapped = options[0]
            options[0] = options[targetIdx]
            options[targetIdx] = swapped

            generated.add(
                QuizQuestionEntity(
                    courseCode = courseCode,
                    question = card.question,
                    optionA = options[0],
                    optionB = options[1],
                    optionC = options[2],
                    optionD = options[3],
                    correctOptionIndex = targetIdx,
                    explanation = "From your ${card.deckTitle} flashcard deck: ${card.answer}",
                    sourceTag = "NOTES"
                )
            )
        }

        // 2. Convert saved Study Notes / Formulas into MCQs
        notes.take(2).forEachIndexed { idx, note ->
            val cleanAnswer = note.content.lines().firstOrNull { it.isNotBlank() }?.take(100) ?: note.content.take(100)
            val correctSlot = (idx + 1) % 4
            val opts = MutableList(4) { slot ->
                when (slot) {
                    correctSlot -> cleanAnswer
                    0 -> "Undefined relation independent of ${note.title}"
                    1 -> "Zero-variance static assumption for $courseCode"
                    2 -> "Unbounded linear divergence without constraints"
                    else -> "Secondary corollary outside ${note.category.lowercase()} scope"
                }
            }
            generated.add(
                QuizQuestionEntity(
                    courseCode = courseCode,
                    question = "Which statement or formula accurately represents '${note.title}' in $courseCode?",
                    optionA = opts[0],
                    optionB = opts[1],
                    optionC = opts[2],
                    optionD = opts[3],
                    correctOptionIndex = correctSlot,
                    explanation = "Verified from your Notes Vault (${note.category}): ${note.content}",
                    sourceTag = "NOTES"
                )
            )
        }

        // 3. Ensure at least 4 high-yield questions even if vault is empty
        if (generated.size < 4) {
            val topicLabel = topics.firstOrNull()?.topicName ?: courseName.ifBlank { courseCode }
            generated.add(
                QuizQuestionEntity(
                    courseCode = courseCode,
                    question = "When preparing for an examination on '$topicLabel' ($courseCode), which study strategy maximizes long-term retention?",
                    optionA = "Passive re-reading of highlighted textbook pages",
                    optionB = "Active recall testing combined with spaced-repetition intervals",
                    optionC = "Single-night massed cramming immediately prior to the paper",
                    optionD = "Memorizing isolated definitions without worked problem sets",
                    correctOptionIndex = 1,
                    explanation = "Active recall + spaced repetition strengthens retrieval pathways far better than passive review.",
                    sourceTag = "NOTES"
                )
            )
            generated.add(
                QuizQuestionEntity(
                    courseCode = courseCode,
                    question = "In university problem-solving for $courseCode, what is the first step before applying a formula or theorem?",
                    optionA = "Substitute numerical constants immediately",
                    optionB = "Verify problem assumptions, boundary conditions, and units/types",
                    optionC = "Round intermediate values to single digits",
                    optionD = "Skip edge-case verification",
                    correctOptionIndex = 1,
                    explanation = "Validating domain assumptions, units, and boundary conditions prevents invalid theorem application.",
                    sourceTag = "NOTES"
                )
            )
        }

        return generated
    }

    private fun buildAcademicContext(
        studentName: String,
        courses: List<CourseEntity>,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        attendance: List<AttendanceRecordEntity>,
        weeklyStudyHours: Float
    ): String {
        val courseList = courses.joinToString("; ") { "${it.code} (${it.name}, ${it.creditHours} credits)" }
        val lectureList = lectures.joinToString("; ") { "Day ${it.dayOfWeek}: ${it.courseCode} ${it.startTime}-${it.endTime} (${it.venue})" }
        val pendingAssessments = assessments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
            .joinToString("; ") { "${it.courseCode} [${it.type}]: ${it.title} (Priority: ${it.priority})" }

        return """
            Student: $studentName
            Courses: $courseList
            Lectures: $lectureList
            Pending Assessments: $pendingAssessments
            Study hours logged this week: ${String.format("%.1f", weeklyStudyHours)} hours
        """.trimIndent()
    }

    data class VivaEvaluationResult(
        val scoreOutOfTen: Int,
        val verdict: String,
        val strengths: String,
        val missingPoints: String,
        val followUpQuestion: String,
        val usedCloudAi: Boolean
    )

    data class RubricCritiqueResult(
        val clarityScore: Int,
        val estimatedGradeBand: String,
        val strengths: String,
        val improvements: String,
        val polishedExcerpt: String,
        val usedCloudAi: Boolean
    )

    private val vivaGenerativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val vivaSchema = Schema.obj(
                    properties = mapOf(
                        "scoreOutOfTen" to Schema.integer(description = "Viva defense score 1..10"),
                        "verdict" to Schema.string(description = "Short examiner verdict"),
                        "strengths" to Schema.string(description = "Key oral defense strengths"),
                        "missingPoints" to Schema.string(description = "Key concepts to strengthen"),
                        "followUpQuestion" to Schema.string(description = "One Socratic follow-up question")
                    )
                )
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        temperature = 0.25f
                        responseMimeType = "application/json"
                        responseSchema = vivaSchema
                    }
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Viva model init error: ${e.message}")
            null
        }
    }

    private val rubricGenerativeModel by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val rubricSchema = Schema.obj(
                    properties = mapOf(
                        "clarityScore" to Schema.integer(description = "Academic clarity score 1..100"),
                        "estimatedGradeBand" to Schema.string(description = "Estimated university grade band"),
                        "strengths" to Schema.string(description = "Rubric strengths"),
                        "improvements" to Schema.string(description = "Actionable rubric improvements"),
                        "polishedExcerpt" to Schema.string(description = "Polished opening excerpt")
                    )
                )
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        temperature = 0.3f
                        responseMimeType = "application/json"
                        responseSchema = rubricSchema
                    }
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Rubric model init error: ${e.message}")
            null
        }
    }

    suspend fun evaluateVivaAnswer(
        courseCode: String,
        question: String,
        expectedAnswer: String,
        studentSpokenAnswer: String
    ): VivaEvaluationResult = withContext(Dispatchers.IO) {
        val trimmedAnswer = studentSpokenAnswer.trim()
        if (trimmedAnswer.isBlank()) {
            return@withContext VivaEvaluationResult(
                scoreOutOfTen = 0,
                verdict = "No Spoken Answer Captured",
                strengths = "Ready for your oral defense whenever you tap Speak or type your response.",
                missingPoints = expectedAnswer.take(180),
                followUpQuestion = "Can you summarize the core concept of '$question' in your own words?",
                usedCloudAi = false
            )
        }

        val model = vivaGenerativeModel
        if (model != null) {
            try {
                val prompt = """
                    You are a supportive university oral viva examiner for course $courseCode.
                    Viva Question: $question
                    Reference Answer / Marking Key: $expectedAnswer
                    Student's Oral Answer: $trimmedAnswer

                    Grade the student's oral explanation out of 10 based on conceptual accuracy, clarity, and completeness.
                    Provide concise strengths, key missing points (if any), and one sharp Socratic follow-up question.
                """.trimIndent()

                val rawText = withTimeout(60_000L) {
                    model.generateContent(prompt).text?.trim().orEmpty()
                }
                if (rawText.isNotEmpty()) {
                    val obj = JSONObject(rawText)
                    return@withContext VivaEvaluationResult(
                        scoreOutOfTen = obj.optInt("scoreOutOfTen", 7).coerceIn(1, 10),
                        verdict = obj.optString("verdict", "Solid Conceptual Grasp"),
                        strengths = obj.optString("strengths", "Clear articulation of the primary mechanism."),
                        missingPoints = obj.optString("missingPoints", "Review edge cases and formal definitions."),
                        followUpQuestion = obj.optString("followUpQuestion", "How would this principle behave under boundary constraints?"),
                        usedCloudAi = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Viva evaluation fallback: ${e.message}")
            }
        }

        val stopWords = setOf("the", "and", "for", "with", "that", "this", "from", "into", "are", "was", "were", "your", "have")
        val keyTerms = expectedAnswer
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()
        val spokenLower = trimmedAnswer.lowercase()
        val matched = keyTerms.filter { spokenLower.contains(it) }
        val missed = keyTerms.filterNot { spokenLower.contains(it) }
        val ratio = if (keyTerms.isEmpty()) 0.75 else matched.size.toDouble() / keyTerms.size.toDouble()
        val score = (ratio * 10.0).toInt().coerceIn(if (trimmedAnswer.length > 15) 4 else 2, 10)
        val verdict = when {
            score >= 8 -> "Distinction Viva Defense"
            score >= 6 -> "Good Oral Understanding"
            else -> "Partial Recall — Needs Precision"
        }
        VivaEvaluationResult(
            scoreOutOfTen = score,
            verdict = verdict,
            strengths = if (matched.isNotEmpty()) {
                "Accurately covered key concepts: ${matched.take(5).joinToString(", ")}."
            } else {
                "Attempted an intuitive explanation in your own words."
            },
            missingPoints = if (missed.isNotEmpty()) {
                "Strengthen your answer by mentioning: ${missed.take(5).joinToString(", ")}. Key reference: ${expectedAnswer.take(140)}"
            } else {
                "All primary marking-scheme keywords were addressed."
            },
            followUpQuestion = "Can you give one practical application or worked example for $courseCode?",
            usedCloudAi = false
        )
    }

    suspend fun critiqueAcademicDraft(
        courseCode: String,
        title: String,
        draftContent: String,
        rubricFocus: String = "Academic Clarity, Argument Structure & Exam Readiness"
    ): RubricCritiqueResult = withContext(Dispatchers.IO) {
        val cleaned = draftContent.trim()
        if (cleaned.isBlank()) {
            return@withContext RubricCritiqueResult(
                clarityScore = 0,
                estimatedGradeBand = "Draft Empty",
                strengths = "Add your essay paragraph, lab discussion, or study summary first.",
                improvements = "Include a clear thesis/definition, supporting evidence or formula, and a concise takeaway.",
                polishedExcerpt = "",
                usedCloudAi = false
            )
        }

        val model = rubricGenerativeModel
        if (model != null) {
            try {
                val prompt = """
                    You are a university academic writing & rubric coach for $courseCode.
                    Title: $title
                    Rubric Focus: $rubricFocus
                    Student Draft:
                    ${cleaned.take(3500)}

                    Evaluate clarity out of 100, estimate university grade band (e.g. "A (First Class)", "B+ (Upper Second)"),
                    highlight strengths, give 2-3 actionable rubric improvements, and rewrite the opening 2-3 sentences to publication/first-class standard.
                """.trimIndent()

                val rawText = withTimeout(60_000L) {
                    model.generateContent(prompt).text?.trim().orEmpty()
                }
                if (rawText.isNotEmpty()) {
                    val obj = JSONObject(rawText)
                    return@withContext RubricCritiqueResult(
                        clarityScore = obj.optInt("clarityScore", 82).coerceIn(1, 100),
                        estimatedGradeBand = obj.optString("estimatedGradeBand", "B+ / A- Academic Standard"),
                        strengths = obj.optString("strengths", "Well-focused academic topic and terminology."),
                        improvements = obj.optString("improvements", "Add explicit quantitative evidence or worked examples."),
                        polishedExcerpt = obj.optString("polishedExcerpt", cleaned.take(220)),
                        usedCloudAi = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Rubric critique fallback: ${e.message}")
            }
        }

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = cleaned.split(Regex("[.!?]+")).filter { it.isNotBlank() }
        val avgSentenceLen = if (sentences.isEmpty()) words.size else words.size / sentences.size
        val hasStructureMarkers = listOf("therefore", "because", "however", "example", "=", "1.", "•").any {
            cleaned.lowercase().contains(it)
        }
        val score = (62 + (words.size.coerceAtMost(120) / 5) + (if (hasStructureMarkers) 10 else 0) -
            (if (avgSentenceLen > 32) 8 else 0)).coerceIn(45, 95)
        val band = when {
            score >= 85 -> "A (First-Class Readiness)"
            score >= 75 -> "B+ (Strong Upper Second)"
            score >= 65 -> "B (Solid Core Draft)"
            else -> "C+ (Needs Structural Expansion)"
        }
        RubricCritiqueResult(
            clarityScore = score,
            estimatedGradeBand = band,
            strengths = "Contains ${words.size} words across ${sentences.size.coerceAtLeast(1)} sentence(s)" +
                if (hasStructureMarkers) " with logical connectors/formulas." else ".",
            improvements = buildString {
                if (words.size < 45) append("• Expand depth with a concrete example, proof step, or citation. ")
                if (avgSentenceLen > 28) append("• Split long sentences (>28 words) for sharper exam readability. ")
                if (!hasStructureMarkers) append("• Add explicit causal signposts ('therefore', 'for instance') or numbered points.")
                if (isEmpty()) append("• Add one boundary-case limitation or practical implication to lock in First-Class marks.")
            },
            polishedExcerpt = cleaned.lines().firstOrNull { it.isNotBlank() }?.take(220).orEmpty(),
            usedCloudAi = false
        )
    }
}
