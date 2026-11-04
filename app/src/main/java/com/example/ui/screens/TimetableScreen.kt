package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.LectureEntity
import com.example.notifications.AlarmToneOption
import com.example.ui.AuraViewModel
import com.example.ui.TimetableViewMode
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.computeLeaveByTime12Hour
import com.example.ui.components.formatTime12Hour
import com.example.ui.dialogs.AddLectureDialog
import com.example.ui.dialogs.CourseManagerDialog
import com.example.ui.dialogs.JsonImportExportDialog
import com.example.ui.dialogs.LectureAlarmManagerDialog
import com.example.ui.dialogs.TimetablePhotoViewerDialog
import com.example.ui.dialogs.TimetableScannerDialog
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.util.MediaStorageHelper
import com.example.util.StudyGapWindow
import com.example.util.TimetableInsightsAndShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimetableScreen(
    viewModel: AuraViewModel,
    onOpenCourseSlides: ((String) -> Unit)? = null,
    onStartFocusForCourse: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val previewingToneId by viewModel.previewingAlarmToneId.collectAsState()
    val viewMode by viewModel.timetableViewMode.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val conflictWarning by viewModel.conflictWarning.collectAsState()
    val isTimetableLocked by viewModel.isTimetableLocked.collectAsState()
    val recentlyDeletedLecture by viewModel.recentlyDeletedLecture.collectAsState()

    var showAddLectureDialog by remember { mutableStateOf(false) }
    var showCourseManagerDialog by remember { mutableStateOf(false) }
    var showExportHubDialog by remember { mutableStateOf(false) }
    var showAlarmStudioDialog by remember { mutableStateOf(false) }
    var showTimetablePhotoViewer by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var selectedLectureForEdit by remember { mutableStateOf<LectureEntity?>(null) }
    var pendingSaveLecture by remember { mutableStateOf<LectureEntity?>(null) }
    var lecturePendingRemoval by remember { mutableStateOf<LectureEntity?>(null) }

    var showSearchFilterBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseFilter by remember { mutableStateOf("ALL") }

    val timetablePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importTimetablePictureFromUri(uri)
        }
    }

    val context = LocalContext.current
    val ringingAlarmsCount = remember(lectures) { lectures.count { it.reminderEnabled } }
    val scheduleClashes = remember(lectures) {
        TimetableInsightsAndShareHelper.detectScheduleClashes(lectures)
    }

    val filteredLectures = remember(lectures, searchQuery, selectedCourseFilter) {
        lectures.filter { lec ->
            val matchesCourse = selectedCourseFilter == "ALL" ||
                lec.courseCode.equals(selectedCourseFilter, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                lec.courseCode.contains(q, ignoreCase = true) ||
                lec.courseName.contains(q, ignoreCase = true) ||
                lec.venue.contains(q, ignoreCase = true) ||
                lec.lecturer.contains(q, ignoreCase = true)
            matchesCourse && matchesSearch
        }
    }

    FrostedGlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        if (viewMode == TimetableViewMode.OFFICE_HOURS) {
            // Office Hours mode uses its own dedicated LazyColumn; keep only slim horizontal header + tab bar above it
            Column(modifier = Modifier.fillMaxSize()) {
                TimetableHorizontalHeaderCard(
                    coursesCount = courses.size,
                    ringingAlarmsCount = ringingAlarmsCount,
                    totalLecturesCount = lectures.size,
                    isTimetableLocked = isTimetableLocked,
                    onToggleLock = { viewModel.toggleTimetableLocked() },
                    isSearchActive = showSearchFilterBar,
                    onToggleSearch = { showSearchFilterBar = !showSearchFilterBar },
                    onOpenCourses = { showCourseManagerDialog = true },
                    onOpenAiScanner = { showScannerDialog = true },
                    onOpenAlarmStudio = { showAlarmStudioDialog = true },
                    onOpenPhoto = {
                        if (userProfile.timetableImagePath.isNotBlank()) {
                            showTimetablePhotoViewer = true
                        } else {
                            timetablePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    onOpenExport = { showExportHubDialog = true }
                )

                TimetableModeTabsBar(
                    viewMode = viewMode,
                    onSelectMode = { viewModel.setTimetableViewMode(it) }
                )

                Box(modifier = Modifier.weight(1f)) {
                    OfficeHoursView(viewModel = viewModel)
                }
            }
        } else {
            // Unified Full-Screen Vertical LazyColumn so the header, photo strip, day selector & lectures
            // all scroll smoothly upward together and NEVER block or clip the day's schedule!
            val dayOfWeekInt = selectedDate.dayOfWeek.value
            val dayLectures = remember(filteredLectures, dayOfWeekInt) {
                filteredLectures.filter { it.dayOfWeek == dayOfWeekInt }.sortedBy { it.startTime }
            }
            val freeStudyWindows = remember(dayLectures, dayOfWeekInt) {
                TimetableInsightsAndShareHelper.findFreeStudyWindowsForDay(dayLectures, dayOfWeekInt)
            }
            val sortedAgendaLectures = remember(filteredLectures) {
                filteredLectures.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
            }
            val isViewingToday = selectedDate == LocalDate.now()
            val nowMinutes = remember(isViewingToday) {
                val now = LocalTime.now()
                now.hour * 60 + now.minute
            }
            val nextUpLectureIdToday = remember(dayLectures, isViewingToday, nowMinutes) {
                if (!isViewingToday) null
                else dayLectures.firstOrNull {
                    TimetableInsightsAndShareHelper.parseMinutesSinceMidnight(it.startTime) > nowMinutes
                }?.id
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(
                        if (viewMode == TimetableViewMode.AGENDA) "agenda_list_view"
                        else "timetable_main_lazy_list"
                    ),
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Sleek 100% Horizontal Intro Header Card (Zero Vertical Text Wrapping)
                item(key = "header_card") {
                    TimetableHorizontalHeaderCard(
                        coursesCount = courses.size,
                        ringingAlarmsCount = ringingAlarmsCount,
                        totalLecturesCount = lectures.size,
                        isTimetableLocked = isTimetableLocked,
                        onToggleLock = { viewModel.toggleTimetableLocked() },
                        isSearchActive = showSearchFilterBar,
                        onToggleSearch = {
                            showSearchFilterBar = !showSearchFilterBar
                            if (!showSearchFilterBar) {
                                searchQuery = ""
                                selectedCourseFilter = "ALL"
                            }
                        },
                        onOpenCourses = { showCourseManagerDialog = true },
                        onOpenAiScanner = { showScannerDialog = true },
                        onOpenAlarmStudio = { showAlarmStudioDialog = true },
                        onOpenPhoto = {
                            if (userProfile.timetableImagePath.isNotBlank()) {
                                showTimetablePhotoViewer = true
                            } else {
                                timetablePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        onOpenExport = { showExportHubDialog = true }
                    )
                }

                // 1b. Schedule Lock Shield Banner or 1-Tap Undo Restore Banner
                if (isTimetableLocked) {
                    item(key = "locked_shield_banner") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AuraMintAccent.copy(alpha = 0.16f))
                                .border(1.dp, AuraMintAccent.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("timetable_locked_status_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AuraMintAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Schedule Protected • Accidental delete & edit disabled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraMintAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AuraMintAccent.copy(alpha = 0.24f),
                                border = BorderStroke(1.dp, AuraMintAccent),
                                modifier = Modifier.clickable { viewModel.toggleTimetableLocked() }
                            ) {
                                Text(
                                    text = "Unlock",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AuraMintAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (recentlyDeletedLecture != null) {
                    val deletedSnap = recentlyDeletedLecture!!
                    item(key = "undo_deleted_lecture_banner") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xEB10223E))
                                .border(1.dp, LocalAuraColors.current.secondaryAccent.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("timetable_undo_delete_banner"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Removed ${deletedSnap.courseCode} (${formatTime12Hour(deletedSnap.startTime)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAuraColors.current.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AuraGlassButton(
                                text = "Undo",
                                isPrimary = true,
                                cornerRadius = 10.dp,
                                onClick = { viewModel.restoreRecentlyDeletedLecture() },
                                testTag = "timetable_undo_delete_button"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.dismissRecentlyDeletedLecture() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("timetable_dismiss_undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Undo",
                                    tint = LocalAuraColors.current.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Collapsible Search & Course Filter Strip (0dp when closed)
                if (showSearchFilterBar) {
                    item(key = "search_filter_bar") {
                        TimetableSearchAndCourseFilterStrip(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            courseCodes = remember(courses) { courses.map { it.code }.distinct() },
                            selectedCourseFilter = selectedCourseFilter,
                            onSelectCourseFilter = { selectedCourseFilter = it },
                            onManageSelectedCourse = { showCourseManagerDialog = true },
                            onClear = {
                                searchQuery = ""
                                selectedCourseFilter = "ALL"
                            }
                        )
                    }
                }

                // 3. Compact View Mode Tabs (Daily • Weekly • Monthly • Agenda • Office Hours)
                item(key = "mode_tabs") {
                    TimetableModeTabsBar(
                        viewMode = viewMode,
                        onSelectMode = { viewModel.setTimetableViewMode(it) }
                    )
                }

                // 4. Slim Horizontal Official Timetable Picture Strip
                item(key = "photo_banner") {
                    TimetablePhotoBannerCard(
                        imagePath = userProfile.timetableImagePath,
                        onUploadOrReplace = {
                            timetablePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenFullScreen = { showTimetablePhotoViewer = true },
                        onRemove = { viewModel.removeTimetablePicture() }
                    )
                }

                // 5. Smart Clash Detector & 1-Tap Share Poster Strip
                item(key = "clash_and_share_strip") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scheduleClashes.isNotEmpty()) {
                            val firstClash = scheduleClashes.first()
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LocalAuraColors.current.danger.copy(alpha = 0.18f))
                                    .border(
                                        1.dp,
                                        LocalAuraColors.current.danger.copy(alpha = 0.55f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("timetable_clash_warning_banner"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.danger,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Clash: ${firstClash.firstCourseCode} & ${firstClash.secondCourseCode} (${firstClash.overlapWindowText})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.danger,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AuraMintAccent.copy(alpha = 0.14f))
                                    .border(1.dp, AuraMintAccent.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AuraMintAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lectures.isEmpty()) "Clash-Free • Add classes" else "Zero Clashes • Study gaps active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraMintAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        AuraGlassButton(
                            text = "Share Poster",
                            icon = Icons.Default.Share,
                            cornerRadius = 14.dp,
                            onClick = {
                                val ok = TimetableInsightsAndShareHelper.shareWeeklyTimetableImage(
                                    context = context,
                                    studentName = userProfile.name,
                                    institution = userProfile.institution,
                                    semesterLabel = userProfile.semester,
                                    lectures = lectures
                                )
                                if (!ok) {
                                    Toast.makeText(context, "Unable to share timetable image", Toast.LENGTH_SHORT).show()
                                }
                            },
                            testTag = "timetable_share_poster_button"
                        )
                    }
                }

                // 6. Mode-Specific Scrollable Schedule Content
                when (viewMode) {
                    TimetableViewMode.DAILY -> {
                        // Quick Mon–Sun Day Pill Strip + "Today" Jump
                        item(key = "daily_quick_strip") {
                            QuickDaySelectorStrip(
                                selectedDate = selectedDate,
                                allLectures = filteredLectures,
                                onSelectDayOfWeek = { targetDow ->
                                    val delta = (targetDow - selectedDate.dayOfWeek.value).toLong()
                                    viewModel.setSelectedDate(selectedDate.plusDays(delta))
                                },
                                onJumpToToday = { viewModel.setSelectedDate(LocalDate.now()) }
                            )
                        }

                        // Date Navigation Bar
                        item(key = "daily_date_nav") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AuraGlassIconButton(
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Day",
                                    onClick = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                                    size = 44.dp,
                                    tint = LocalAuraColors.current.textPrimary,
                                    testTag = "prev_day_button"
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = buildString {
                                            append(if (isViewingToday) "Today" else DayOfWeek.of(dayOfWeekInt).name.lowercase().replaceFirstChar { it.uppercase() })
                                            append(" • ${dayLectures.size} ${if (dayLectures.size == 1) "lecture" else "lectures"}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LocalAuraColors.current.secondaryAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                AuraGlassIconButton(
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Day",
                                    onClick = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                                    size = 44.dp,
                                    tint = LocalAuraColors.current.textPrimary,
                                    testTag = "next_day_button"
                                )
                            }
                        }

                        if (dayLectures.isEmpty()) {
                            item(key = "daily_empty") {
                                AuraCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No lectures scheduled for ${DayOfWeek.of(dayOfWeekInt).name.lowercase().replaceFirstChar { it.uppercase() }}.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalAuraColors.current.textPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap '+' below to add a class, or tap another day chip above to view your weekly lectures.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalAuraColors.current.textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            if (freeStudyWindows.isNotEmpty()) {
                                items(
                                    items = freeStudyWindows,
                                    key = { gap -> "gap_${gap.dayOfWeek}_${gap.startTime24}_${gap.endTime24}" }
                                ) { gap ->
                                    FreeStudyWindowItemCard(
                                        gap = gap,
                                        onStartFocus = {
                                            onStartFocusForCourse?.invoke(gap.beforeCourseCode)
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            items(dayLectures, key = { "daily_lec_${it.id}" }) { lecture ->
                                val startMins = TimetableInsightsAndShareHelper.parseMinutesSinceMidnight(lecture.startTime)
                                val endMins = TimetableInsightsAndShareHelper.parseMinutesSinceMidnight(lecture.endTime)
                                val isLiveNow = isViewingToday && nowMinutes in startMins until endMins
                                val liveProgress = if (isLiveNow && endMins > startMins) {
                                    ((nowMinutes - startMins).toFloat() / (endMins - startMins).toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                val minsRemaining = if (isLiveNow) (endMins - nowMinutes).coerceAtLeast(1) else 0

                                TimetableLectureCard(
                                    lecture = lecture,
                                    defaultAlarmToneId = userProfile.defaultAlarmToneId,
                                    isScheduleLocked = isTimetableLocked,
                                    isLiveNow = isLiveNow,
                                    liveProgress = liveProgress,
                                    liveMinutesRemaining = minsRemaining,
                                    isUpNextToday = lecture.id == nextUpLectureIdToday,
                                    onToggleAlarm = { viewModel.toggleLectureAlarm(lecture) },
                                    onOpenSlides = if (onOpenCourseSlides != null) {
                                        { onOpenCourseSlides(lecture.courseCode) }
                                    } else null,
                                    onEdit = { selectedLectureForEdit = lecture },
                                    onDuplicate = {
                                        viewModel.saveLecture(
                                            lecture.copy(id = 0L, dayOfWeek = (lecture.dayOfWeek % 7) + 1)
                                        ) {}
                                    },
                                    onDelete = { lecturePendingRemoval = lecture },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    TimetableViewMode.WEEKLY -> {
                        item(key = "weekly_grid") {
                            WeeklyGridView(
                                lectures = filteredLectures,
                                onLectureClick = {
                                    if (!isTimetableLocked) selectedLectureForEdit = it
                                }
                            )
                        }
                    }

                    TimetableViewMode.MONTHLY -> {
                        item(key = "monthly_calendar") {
                            MonthlyCalendarView(
                                selectedDate = selectedDate,
                                onDateSelected = { viewModel.setSelectedDate(it) },
                                onOpenDayView = {
                                    viewModel.setSelectedDate(it)
                                    viewModel.setTimetableViewMode(TimetableViewMode.DAILY)
                                },
                                lectures = filteredLectures
                            )
                        }
                    }

                    TimetableViewMode.AGENDA -> {
                        if (sortedAgendaLectures.isEmpty()) {
                            item(key = "agenda_empty") {
                                AuraCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No lectures match your current filter.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalAuraColors.current.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap '+' to schedule a lecture or clear search filters.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalAuraColors.current.textSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(sortedAgendaLectures, key = { "agenda_${it.id}" }) { lecture ->
                                AgendaLectureCard(
                                    lecture = lecture,
                                    isScheduleLocked = isTimetableLocked,
                                    onToggleAlarm = { viewModel.toggleLectureAlarm(lecture) },
                                    onEditLecture = { selectedLectureForEdit = lecture },
                                    onDeleteLecture = { lecturePendingRemoval = lecture },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    TimetableViewMode.OFFICE_HOURS -> Unit
                }
            }
        }

        // Liquid Glass Floating Action Button to Add Lecture (hidden while Schedule Lock Shield is active)
        if (viewMode != TimetableViewMode.OFFICE_HOURS && !isTimetableLocked) {
            AuraGlassIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Lecture",
                onClick = { showAddLectureDialog = true },
                size = 56.dp,
                iconSize = 25.dp,
                isPrimary = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                testTag = "fab_add_lecture"
            )
        }
    }

    // Add / Edit Lecture Dialog
    if (showAddLectureDialog || selectedLectureForEdit != null) {
        AddLectureDialog(
            courses = courses,
            initialLecture = selectedLectureForEdit,
            onManageCourses = { showCourseManagerDialog = true },
            previewingToneId = previewingToneId,
            onPreviewTone = { toneId ->
                viewModel.previewAlarmTone(toneId, userProfile.customAlarmFilePath)
            },
            onDismiss = {
                viewModel.stopAlarmPreview()
                showAddLectureDialog = false
                selectedLectureForEdit = null
            },
            onSave = { newLecture ->
                viewModel.stopAlarmPreview()
                pendingSaveLecture = newLecture
                viewModel.saveLecture(newLecture) {
                    showAddLectureDialog = false
                    selectedLectureForEdit = null
                    pendingSaveLecture = null
                }
            }
        )
    }

    // Lecture Alarm Selector & Sound Studio Dialog
    if (showAlarmStudioDialog) {
        LectureAlarmManagerDialog(
            viewModel = viewModel,
            onDismiss = { showAlarmStudioDialog = false }
        )
    }

    // Full-Screen Zoomable Official Timetable Photo Viewer
    if (showTimetablePhotoViewer && userProfile.timetableImagePath.isNotBlank()) {
        TimetablePhotoViewerDialog(
            imagePath = userProfile.timetableImagePath,
            onReplaceUri = { uri -> viewModel.importTimetablePictureFromUri(uri) },
            onRemove = { viewModel.removeTimetablePicture() },
            onDismiss = { showTimetablePhotoViewer = false }
        )
    }

    // Enrolled Course Manager Dialog (Add, Edit, Delete Courses & Open Course Slides)
    if (showCourseManagerDialog) {
        CourseManagerDialog(
            viewModel = viewModel,
            onDismiss = { showCourseManagerDialog = false },
            onOpenCourseSlides = onOpenCourseSlides
        )
    }

    if (showExportHubDialog) {
        JsonImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showExportHubDialog = false }
        )
    }

    if (showScannerDialog) {
        TimetableScannerDialog(
            viewModel = viewModel,
            onDismiss = { showScannerDialog = false }
        )
    }

    // Timetable Conflict Warning Dialog (Frosted Liquid Glass)
    if (conflictWarning != null && pendingSaveLecture != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearConflictWarning() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LocalAuraColors.current.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Timetable Conflict Warning",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "$conflictWarning\n\nDo you want to save this lecture anyway?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAuraColors.current.textSecondary
                )
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Anyway",
                    isPrimary = true,
                    containerColor = LocalAuraColors.current.danger,
                    onClick = {
                        val lec = pendingSaveLecture
                        if (lec != null) {
                            viewModel.forceSaveLecture(lec) {
                                showAddLectureDialog = false
                                selectedLectureForEdit = null
                                pendingSaveLecture = null
                            }
                        }
                    },
                    testTag = "conflict_save_anyway_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Adjust Time",
                    onClick = { viewModel.clearConflictWarning() },
                    testTag = "conflict_adjust_time_button"
                )
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x4564D2FF))),
                RoundedCornerShape(24.dp)
            )
        )
    }

    // Safe Confirmation Dialog before removing any lecture slot or added course from the Timetable
    if (lecturePendingRemoval != null) {
        val target = lecturePendingRemoval!!
        val targetDayName = DayOfWeek.of(target.dayOfWeek).name.lowercase().replaceFirstChar { it.uppercase() }
        val totalSlotsForCourse = lectures.count { it.courseCode.equals(target.courseCode, ignoreCase = true) }

        Dialog(onDismissRequest = { lecturePendingRemoval = null }) {
            Surface(
                color = Color(0xF5101B30),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x45FF453A)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_delete_lecture_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = LocalAuraColors.current.danger,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Remove ${target.courseCode}?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                    }

                    Text(
                        text = "${target.courseName} • $targetDayName ${formatTime12Hour(target.startTime)}–${formatTime12Hour(target.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalAuraColors.current.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Remove only this slot (with 1-tap Undo) or delete the entire course (${totalSlotsForCourse} ${if (totalSlotsForCourse == 1) "slot" else "slots"}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )

                    AuraGlassButton(
                        text = "Remove This Slot Only",
                        isPrimary = true,
                        containerColor = LocalAuraColors.current.danger,
                        onClick = {
                            viewModel.deleteLecture(target)
                            lecturePendingRemoval = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "confirm_remove_lecture_slot_button"
                    )

                    AuraGlassButton(
                        text = "Delete Entire Course (${target.courseCode})",
                        icon = Icons.Default.Delete,
                        containerColor = LocalAuraColors.current.danger.copy(alpha = 0.25f),
                        textColor = LocalAuraColors.current.danger,
                        onClick = {
                            viewModel.deleteCourseByCodeFromTimetable(target.courseCode, deleteUploadedSlides = false)
                            lecturePendingRemoval = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "confirm_delete_course_from_timetable_button"
                    )

                    AuraGlassButton(
                        text = "Keep in Timetable",
                        onClick = { lecturePendingRemoval = null },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "cancel_delete_lecture_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableHorizontalHeaderCard(
    coursesCount: Int,
    ringingAlarmsCount: Int,
    totalLecturesCount: Int,
    isTimetableLocked: Boolean = false,
    onToggleLock: () -> Unit = {},
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenAiScanner: () -> Unit,
    onOpenAlarmStudio: () -> Unit,
    onOpenPhoto: () -> Unit,
    onOpenExport: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("timetable_header_card"),
        cornerRadius = 22.dp,
        accentGlow = auraColors.primaryAccent
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (auraColors.isDark) 0.26f else 0.10f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Full-width horizontal title + summary pill + Schedule Lock shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "University Timetable",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = auraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = auraColors.primaryAccent.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, auraColors.secondaryAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = auraColors.secondaryAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coursesCount Courses • $ringingAlarmsCount/$totalLecturesCount Alarms",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.secondaryAccent,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    AuraGlassIconButton(
                        icon = if (isTimetableLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isTimetableLocked) "Unlock Timetable Edits" else "Lock Timetable Against Accidental Deletion",
                        onClick = onToggleLock,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = if (isTimetableLocked) AuraMintAccent else auraColors.textSecondary,
                        testTag = "timetable_lock_toggle_button"
                    )
                }

                // Row 2: Dedicated horizontal action toolbar — fits cleanly from 320dp compact to Expanded tablets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuraGlassButton(
                        text = "Courses",
                        icon = Icons.Default.Edit,
                        isPrimary = true,
                        cornerRadius = 14.dp,
                        onClick = onOpenCourses,
                        testTag = "manage_courses_button"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AuraGlassIconButton(
                        icon = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search or Filter Timetable",
                        onClick = onToggleSearch,
                        size = 33.dp,
                        iconSize = 16.dp,
                        tint = if (isSearchActive) auraColors.warning else auraColors.textPrimary,
                        testTag = "timetable_search_toggle_button"
                    )

                    AuraGlassIconButton(
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "AI Scan Timetable or Syllabus",
                        onClick = onOpenAiScanner,
                        size = 33.dp,
                        iconSize = 16.dp,
                        tint = auraColors.aiAccent,
                        testTag = "ai_scan_timetable_button"
                    )

                    AuraGlassIconButton(
                        icon = Icons.Default.Alarm,
                        contentDescription = "Select Lecture Alarms & Sound",
                        onClick = onOpenAlarmStudio,
                        size = 33.dp,
                        iconSize = 16.dp,
                        tint = auraColors.warning,
                        testTag = "timetable_alarm_studio_button"
                    )

                    AuraGlassIconButton(
                        icon = Icons.Default.Image,
                        contentDescription = "Upload or View Timetable Picture",
                        onClick = onOpenPhoto,
                        size = 33.dp,
                        iconSize = 16.dp,
                        tint = auraColors.secondaryAccent,
                        testTag = "upload_timetable_photo_button"
                    )

                    AuraGlassIconButton(
                        icon = Icons.Default.PictureAsPdf,
                        contentDescription = "Export PDF or .ICS Calendar",
                        onClick = onOpenExport,
                        size = 33.dp,
                        iconSize = 16.dp,
                        tint = auraColors.secondaryAccent,
                        testTag = "timetable_export_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableSearchAndCourseFilterStrip(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    courseCodes: List<String>,
    selectedCourseFilter: String,
    onSelectCourseFilter: (String) -> Unit,
    onManageSelectedCourse: () -> Unit = {},
    onClear: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search course, venue, or lecturer...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = auraColors.secondaryAccent, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty() || selectedCourseFilter != "ALL") {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Filter", tint = auraColors.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timetable_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = auraColors.secondaryAccent,
                unfocusedBorderColor = Color(0x36FFFFFF),
                focusedContainerColor = Color(0x1EFFFFFF),
                unfocusedContainerColor = Color(0x14FFFFFF),
                focusedTextColor = auraColors.textPrimary,
                unfocusedTextColor = auraColors.textPrimary
            )
        )

        if (courseCodes.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val active = selectedCourseFilter == "ALL"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) auraColors.primaryAccent else Color(0x1EFFFFFF),
                        border = BorderStroke(1.dp, if (active) auraColors.secondaryAccent else Color(0x30FFFFFF)),
                        modifier = Modifier.clickable { onSelectCourseFilter("ALL") }
                    ) {
                        Text(
                            text = "All Courses",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else auraColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                if (selectedCourseFilter != "ALL") {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = auraColors.danger.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, auraColors.danger.copy(alpha = 0.60f)),
                            modifier = Modifier
                                .clickable(onClick = onManageSelectedCourse)
                                .testTag("filter_manage_selected_course_button")
                        ) {
                            Text(
                                text = "Edit / Delete $selectedCourseFilter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.danger,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                items(courseCodes) { code ->
                    val active = selectedCourseFilter.equals(code, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) auraColors.primaryAccent else Color(0x1EFFFFFF),
                        border = BorderStroke(1.dp, if (active) auraColors.secondaryAccent else Color(0x30FFFFFF)),
                        modifier = Modifier.clickable { onSelectCourseFilter(code) }
                    ) {
                        Text(
                            text = code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else auraColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableModeTabsBar(
    viewMode: TimetableViewMode,
    onSelectMode: (TimetableViewMode) -> Unit
) {
    val auraColors = LocalAuraColors.current
    ScrollableTabRow(
        selectedTabIndex = viewMode.ordinal,
        containerColor = if (auraColors.isDark) Color(0xCC0D172C) else Color(0xDDFFFFFF),
        contentColor = auraColors.secondaryAccent,
        edgePadding = 12.dp,
        divider = {},
        indicator = { tabPositions ->
            if (viewMode.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[viewMode.ordinal])
                        .clip(CircleShape),
                    height = 3.dp,
                    color = auraColors.secondaryAccent
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, Color(0x28FFFFFF))
            .testTag("timetable_view_tabs")
    ) {
        val tabDefs = listOf(
            Triple(TimetableViewMode.DAILY, "Daily", Icons.Default.CalendarViewDay) to "tab_daily",
            Triple(TimetableViewMode.WEEKLY, "Weekly", Icons.Default.CalendarViewWeek) to "tab_weekly",
            Triple(TimetableViewMode.MONTHLY, "Monthly", Icons.Default.CalendarMonth) to "tab_monthly",
            Triple(TimetableViewMode.AGENDA, "Agenda", Icons.AutoMirrored.Filled.FormatListBulleted) to "tab_agenda",
            Triple(TimetableViewMode.OFFICE_HOURS, "Office Hours", Icons.Default.RecordVoiceOver) to "tab_office_hours"
        )
        tabDefs.forEach { (triple, tag) ->
            val (mode, label, icon) = triple
            val selected = viewMode == mode
            Tab(
                selected = selected,
                onClick = { onSelectMode(mode) },
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag(tag),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickDaySelectorStrip(
    selectedDate: LocalDate,
    allLectures: List<LectureEntity>,
    onSelectDayOfWeek: (Int) -> Unit,
    onJumpToToday: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    val today = LocalDate.now()
    val dayShortLabels = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("timetable_day_quick_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedDate != today) {
            item(key = "jump_today") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AuraMintAccent.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, AuraMintAccent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onJumpToToday)
                        .testTag("jump_to_today_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Jump to Today",
                            tint = AuraMintAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraMintAccent,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        items(dayShortLabels, key = { it.first }) { (dow, label) ->
            val isSelected = selectedDate.dayOfWeek.value == dow
            val count = allLectures.count { it.dayOfWeek == dow }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) auraColors.primaryAccent else Color(0x18FFFFFF),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) auraColors.secondaryAccent else Color(0x2EFFFFFF)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectDayOfWeek(dow) }
                    .testTag("day_chip_$dow")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else auraColors.textPrimary,
                        maxLines = 1
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.25f)
                                    else auraColors.secondaryAccent.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                fontFamily = AuraMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else auraColors.secondaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeStudyWindowItemCard(
    gap: StudyGapWindow,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("free_study_window_card"),
        cornerRadius = 18.dp,
        borderColor = AuraMintAccent.copy(alpha = 0.55f),
        accentGlow = AuraMintAccent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AuraMintAccent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AuraMintAccent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Free Study Window • ${TimetableInsightsAndShareHelper.formatDurationLabel(gap.durationMinutes)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AuraMintAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTime12Hour(gap.startTime24)} – ${formatTime12Hour(gap.endTime24)} (${gap.afterCourseCode} → ${gap.beforeCourseCode})",
                    fontSize = 11.sp,
                    color = LocalAuraColors.current.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gap.recommendation,
                    fontSize = 10.sp,
                    color = LocalAuraColors.current.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuraMintAccent.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, AuraMintAccent.copy(alpha = 0.65f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onStartFocus)
                    .testTag("free_gap_start_focus_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus Session",
                        tint = AuraMintAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Focus",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraMintAccent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetablePhotoBannerCard(
    imagePath: String,
    onUploadOrReplace: () -> Unit,
    onOpenFullScreen: () -> Unit,
    onRemove: () -> Unit
) {
    val thumbBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = imagePath) {
        value = if (imagePath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                MediaStorageHelper.loadBitmapSafe(imagePath, maxDimension = 800)
            }
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("timetable_photo_card"),
        cornerRadius = 16.dp
    ) {
        if (imagePath.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Official Timetable Picture",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pin screenshot or printed timetable for instant zoom",
                        fontSize = 10.sp,
                        color = LocalAuraColors.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AuraGlassButton(
                    text = "Upload",
                    icon = Icons.Default.Image,
                    isPrimary = true,
                    cornerRadius = 12.dp,
                    onClick = onUploadOrReplace,
                    testTag = "upload_timetable_banner_button"
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x28FFFFFF))
                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenFullScreen),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = thumbBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Timetable Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Image, null, tint = LocalAuraColors.current.secondaryAccent)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenFullScreen)
                ) {
                    Text(
                        text = "My Uploaded Timetable",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap to open full-screen & pinch-to-zoom",
                        fontSize = 10.sp,
                        color = LocalAuraColors.current.secondaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AuraGlassIconButton(
                    icon = Icons.Default.ZoomIn,
                    contentDescription = "Zoom Timetable Photo",
                    onClick = onOpenFullScreen,
                    size = 40.dp,
                    iconSize = 17.dp,
                    tint = LocalAuraColors.current.secondaryAccent,
                    testTag = "open_timetable_viewer_button"
                )
                Spacer(modifier = Modifier.width(4.dp))
                AuraGlassIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Replace Timetable Photo",
                    onClick = onUploadOrReplace,
                    size = 40.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.textPrimary,
                    testTag = "replace_timetable_photo_button"
                )
                Spacer(modifier = Modifier.width(4.dp))
                AuraGlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Remove Timetable Photo",
                    onClick = onRemove,
                    size = 40.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.danger,
                    testTag = "remove_timetable_photo_button"
                )
            }
        }
    }
}

@Composable
private fun TimetableLectureCard(
    lecture: LectureEntity,
    defaultAlarmToneId: String = "AURORA_CHIME",
    isScheduleLocked: Boolean = false,
    isLiveNow: Boolean = false,
    liveProgress: Float = 0f,
    liveMinutesRemaining: Int = 0,
    isUpNextToday: Boolean = false,
    onToggleAlarm: () -> Unit = {},
    onOpenSlides: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auraColors = LocalAuraColors.current
    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lecture_card_${lecture.id}"),
        borderColor = when {
            isLiveNow -> AuraMintAccent.copy(alpha = 0.85f)
            isUpNextToday -> auraColors.secondaryAccent.copy(alpha = 0.65f)
            else -> auraColors.border
        },
        accentGlow = when {
            isLiveNow -> AuraMintAccent
            isUpNextToday -> auraColors.secondaryAccent
            else -> null
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isLiveNow || isUpNextToday) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AuraBadge(
                        text = if (isLiveNow) "LIVE IN PROGRESS • ${liveMinutesRemaining}m left" else "UP NEXT TODAY",
                        backgroundColor = if (isLiveNow) AuraMintAccent.copy(alpha = 0.22f) else auraColors.secondaryAccent.copy(alpha = 0.20f),
                        textColor = if (isLiveNow) AuraMintAccent else auraColors.secondaryAccent
                    )
                }
                if (isLiveNow) {
                    LinearProgressIndicator(
                        progress = { liveProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AuraMintAccent,
                        trackColor = Color(0x26FFFFFF)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(
                    text = lecture.courseCode,
                    backgroundColor = auraColors.primaryAccent.copy(alpha = 0.2f),
                    textColor = auraColors.secondaryAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatTime12Hour(lecture.startTime)} – ${formatTime12Hour(lecture.endTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = auraColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!isScheduleLocked) {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Schedule",
                            tint = auraColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_lecture_${lecture.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Lecture",
                            tint = auraColors.secondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("request_delete_lecture_${lecture.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Lecture or Course",
                            tint = auraColors.danger.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Schedule Locked",
                        tint = AuraMintAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = lecture.courseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = auraColors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lecture.venue,
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = auraColors.aiAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lecture.lecturer,
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Smart Leave-By Campus Walk Departure Badge + 1-Tap Open Course Slides Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1F10B981))
                        .border(1.dp, AuraMintAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = AuraMintAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Leave by ${computeLeaveByTime12Hour(lecture.startTime, lecture.walkingMinutes)} (${lecture.walkingMinutes}m walk)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AuraMintAccent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onOpenSlides != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = auraColors.primaryAccent.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, auraColors.secondaryAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenSlides)
                            .testTag("lecture_open_slides_${lecture.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = "Open Course Slides",
                                tint = auraColors.secondaryAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Slides",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.secondaryAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val toneName = AlarmToneOption.fromId(
                lecture.alarmToneId.ifBlank { defaultAlarmToneId }
            ).displayName

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (lecture.reminderEnabled) auraColors.warning.copy(alpha = 0.16f)
                        else Color(0x14FFFFFF)
                    )
                    .border(
                        1.dp,
                        if (lecture.reminderEnabled) auraColors.warning.copy(alpha = 0.50f)
                        else Color(0x28FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onToggleAlarm)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("toggle_lecture_alarm_${lecture.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (lecture.reminderEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                    contentDescription = if (lecture.reminderEnabled) "Mute Alarm for ${lecture.courseCode}" else "Enable Alarm for ${lecture.courseCode}",
                    tint = if (lecture.reminderEnabled) auraColors.warning else auraColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lecture.reminderEnabled) {
                        val whenStr = if (lecture.reminderMinutesBefore == 0) "at start" else "${lecture.reminderMinutesBefore}m early"
                        "Ringing $whenStr • $toneName (Tap to mute)"
                    } else {
                        "Alarm Muted for this lecture • Tap to enable"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lecture.reminderEnabled) auraColors.warning else auraColors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WeeklyGridView(
    lectures: List<LectureEntity>,
    onLectureClick: (LectureEntity) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val scrollState = rememberScrollState()
    val todayDow = remember { LocalDate.now().dayOfWeek.value }
    val nowTime = remember { LocalTime.now() }
    val nowFormatted = remember(nowTime) {
        nowTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }
    val nowHhMm = remember(nowTime) {
        String.format(Locale.US, "%02d:%02d", nowTime.hour, nowTime.minute)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(scrollState)
            .testTag("weekly_grid_view")
    ) {
        // Day Headers
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            days.forEachIndexed { idx, day ->
                val isToday = (idx + 1) == todayDow
                Box(
                    modifier = Modifier
                        .width(152.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isToday) AuraMintAccent.copy(alpha = 0.24f) else Color(0x28FFFFFF)
                        )
                        .border(
                            width = if (isToday) 1.5.dp else 1.dp,
                            color = if (isToday) AuraMintAccent else Color(0x45FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isToday) "$day • TODAY" else day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isToday) AuraMintAccent else LocalAuraColors.current.secondaryAccent
                    )
                }
            }
        }

        // Columns for Mon to Sat (scrolls vertically via parent LazyColumn!)
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dayNum in 1..6) {
                val isTodayCol = dayNum == todayDow
                val dayLectures = lectures.filter { it.dayOfWeek == dayNum }.sortedBy { it.startTime }
                Column(
                    modifier = Modifier
                        .width(152.dp)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTodayCol) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraMintAccent.copy(alpha = 0.18f))
                                .border(1.dp, AuraMintAccent.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("weekly_grid_live_now_indicator"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AuraMintAccent)
                            )
                            Text(
                                text = "NOW • $nowFormatted",
                                fontFamily = AuraMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraMintAccent,
                                maxLines = 1
                            )
                        }
                    }

                    if (dayLectures.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x12FFFFFF))
                                .border(1.dp, LocalAuraColors.current.border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Free",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textMuted
                            )
                        }
                    } else {
                        dayLectures.forEach { lecture ->
                            val isLiveClass = isTodayCol && nowHhMm >= lecture.startTime && nowHhMm <= lecture.endTime
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onLectureClick(lecture) },
                                color = if (isLiveClass) AuraMintAccent.copy(alpha = 0.18f) else LocalAuraColors.current.cardBg,
                                border = BorderStroke(
                                    if (isLiveClass) 1.6.dp else 1.dp,
                                    if (isLiveClass) AuraMintAccent else Color(0x5564D2FF)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isLiveClass) {
                                            "LIVE • ${formatTime12Hour(lecture.startTime)} - ${formatTime12Hour(lecture.endTime)}"
                                        } else {
                                            "${formatTime12Hour(lecture.startTime)} - ${formatTime12Hour(lecture.endTime)}"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLiveClass) AuraMintAccent else LocalAuraColors.current.secondaryAccent,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lecture.courseCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = lecture.courseName,
                                        fontSize = 10.sp,
                                        color = LocalAuraColors.current.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = lecture.venue,
                                        fontSize = 9.sp,
                                        color = LocalAuraColors.current.primaryAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Leave ${computeLeaveByTime12Hour(lecture.startTime, lecture.walkingMinutes)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuraMintAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onOpenDayView: (LocalDate) -> Unit,
    lectures: List<LectureEntity>
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1..7
    val selectedDayLectures = remember(lectures, selectedDate) {
        lectures.filter { it.dayOfWeek == selectedDate.dayOfWeek.value }.sortedBy { it.startTime }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("monthly_calendar_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraGlassIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        onClick = { currentMonth = currentMonth.minusMonths(1) },
                        size = 42.dp,
                        tint = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    AuraGlassIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        size = 42.dp,
                        tint = LocalAuraColors.current.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekday Headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Grid
                val totalCells = (firstDayOfWeek - 1) + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        for (col in 0..6) {
                            val dayNum = row * 7 + col - (firstDayOfWeek - 2)
                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val dayLectures = lectures.filter { it.dayOfWeek == date.dayOfWeek.value }
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> LocalAuraColors.current.primaryAccent
                                                isToday -> Color(0x28FFFFFF)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary
                                        )
                                        if (dayLectures.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White else LocalAuraColors.current.secondaryAccent
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Inline Schedule Preview
        AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)) + " (${selectedDayLectures.size} classes)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = LocalAuraColors.current.textPrimary
                    )
                    AuraGlassButton(
                        text = "Open Day",
                        cornerRadius = 12.dp,
                        isPrimary = true,
                        onClick = { onOpenDayView(selectedDate) }
                    )
                }
                if (selectedDayLectures.isEmpty()) {
                    Text(
                        text = "No classes scheduled on this day.",
                        fontSize = 12.sp,
                        color = LocalAuraColors.current.textSecondary
                    )
                } else {
                    selectedDayLectures.forEach { lec ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraBadge(text = lec.courseCode)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatTime12Hour(lec.startTime)} – ${formatTime12Hour(lec.endTime)} • ${lec.venue}",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaLectureCard(
    lecture: LectureEntity,
    isScheduleLocked: Boolean = false,
    onToggleAlarm: () -> Unit,
    onEditLecture: () -> Unit,
    onDeleteLecture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auraColors = LocalAuraColors.current
    val dayName = DayOfWeek.of(lecture.dayOfWeek).name.lowercase().replaceFirstChar { it.uppercase() }
    AuraCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top horizontal row: Day + Time + Action Icons (never crushes course title!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(
                    text = dayName,
                    backgroundColor = auraColors.primaryAccent.copy(alpha = 0.22f),
                    textColor = auraColors.secondaryAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatTime12Hour(lecture.startTime)} – ${formatTime12Hour(lecture.endTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = auraColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onToggleAlarm,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("agenda_toggle_alarm_${lecture.id}")
                ) {
                    Icon(
                        imageVector = if (lecture.reminderEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                        contentDescription = if (lecture.reminderEnabled) "Mute Alarm" else "Enable Alarm",
                        tint = if (lecture.reminderEnabled) auraColors.warning else auraColors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (!isScheduleLocked) {
                    IconButton(onClick = onEditLecture, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = auraColors.secondaryAccent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteLecture,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("agenda_request_delete_${lecture.id}")
                    ) {
                        Icon(Icons.Default.Delete, "Remove Lecture or Course", tint = auraColors.danger.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Full-width horizontal course title & details
            Text(
                text = "${lecture.courseCode} • ${lecture.courseName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = auraColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${lecture.venue} • ${lecture.lecturer}",
                style = MaterialTheme.typography.bodySmall,
                color = auraColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
