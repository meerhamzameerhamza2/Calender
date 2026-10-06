package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EventEntity
import com.example.model.Holiday
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ActiveScreen {
    MAIN,
    SEARCH,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHomeScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showGotoDialog by remember { mutableStateOf(false) }
    var isSwitcherExpanded by remember { mutableStateOf(false) }
    var showEventBottomSheet by remember { mutableStateOf(false) }
    var isAddingImportantDay by remember { mutableStateOf(false) }
    var selectedEventToEdit by remember { mutableStateOf<EventEntity?>(null) }
    var selectedEventForDetails by remember { mutableStateOf<EventEntity?>(null) }
    var selectedHolidayForDetails by remember { mutableStateOf<Holiday?>(null) }

    val viewMode by viewModel.viewMode.collectAsState()
    val viewYear by viewModel.viewYear.collectAsState()
    val viewMonth by viewModel.viewMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val days by viewModel.daysForViewMonth.collectAsState()
    val daysForSelectedWeek by viewModel.daysForSelectedWeek.collectAsState()
    val selectedSummary by viewModel.selectedDateSummary.collectAsState()
    val selectedEvents by viewModel.selectedDateEvents.collectAsState()
    val selectedHoliday by viewModel.selectedDateHoliday.collectAsState()
    val monthHolidays by viewModel.monthHolidays.collectAsState()
    val monthEvents by viewModel.monthEvents.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val importantDays by viewModel.importantDays.collectAsState()
    val settings by viewModel.settings.collectAsState()

    BackHandler(enabled = activeScreen != ActiveScreen.MAIN || viewMode != CalendarViewMode.MONTH) {
        if (activeScreen != ActiveScreen.MAIN) {
            activeScreen = ActiveScreen.MAIN
        } else {
            viewModel.setViewMode(CalendarViewMode.MONTH)
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { false }
    )

    when (activeScreen) {
        ActiveScreen.SEARCH -> {
            SearchScreen(
                events = allEvents,
                onBack = { activeScreen = ActiveScreen.MAIN },
                onEventClick = { ev ->
                    selectedEventForDetails = ev
                }
            )
        }
        ActiveScreen.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onBack = { activeScreen = ActiveScreen.MAIN }
            )
        }
        ActiveScreen.MAIN -> {
            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                containerColor = DarkBackground,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewMode == CalendarViewMode.IMPORTANT_DAYS) {
                            IconButton(onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Important Days",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // "Calendar" heading removed as requested! Spacer maintains right alignment
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Floating Action Capsule on top right (Frosted glass styling with opacity and blur depth)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFF222631).copy(alpha = 0.82f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            // Search Icon Button
                            IconButton(
                                onClick = { activeScreen = ActiveScreen.SEARCH },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("app_bar_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Calendar Icon Button: toggles collapse/expand of ViewSwitcherBar
                            IconButton(
                                onClick = { isSwitcherExpanded = !isSwitcherExpanded },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("app_bar_calendar_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Toggle View Options",
                                    tint = if (isSwitcherExpanded) ElectricBlue else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 3-Dots Overflow Menu
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("app_bar_overflow_menu")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E2129).copy(alpha = 0.94f))
                                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Go to", color = TextPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showGotoDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_goto")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Important Days", color = TextPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setViewMode(CalendarViewMode.IMPORTANT_DAYS)
                                        },
                                        modifier = Modifier.testTag("menu_important_days")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = TextPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            activeScreen = ActiveScreen.SETTINGS
                                        },
                                        modifier = Modifier.testTag("menu_settings")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Collapsible View Switcher Bar (Year, Month, Week, Agenda) toggled by Calendar icon
                    AnimatedVisibility(
                        visible = isSwitcherExpanded && viewMode != CalendarViewMode.IMPORTANT_DAYS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ViewSwitcherBar(
                            currentMode = viewMode,
                            onModeSelected = { viewModel.setViewMode(it) }
                        )
                    }

                    // Main View Content Switcher
                    when (viewMode) {
                        CalendarViewMode.MONTH -> {
                            MonthViewContent(
                                viewYear = viewYear,
                                viewMonth = viewMonth,
                                selectedYear = selectedYear,
                                selectedMonth = selectedMonth,
                                selectedDay = selectedDay,
                                days = days,
                                daysForSelectedWeek = daysForSelectedWeek,
                                selectedSummary = selectedSummary,
                                selectedEvents = selectedEvents,
                                selectedHoliday = selectedHoliday,
                                monthHolidays = monthHolidays,
                                monthEvents = monthEvents,
                                settings = settings,
                                onDayClick = { y, m, d -> viewModel.selectDate(y, m, d) },
                                onPrevMonth = { viewModel.prevMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onPrevWeek = { viewModel.prevWeek() },
                                onNextWeek = { viewModel.nextWeek() },
                                onGoToToday = { viewModel.goToToday() },
                                onAddEventClick = {
                                    selectedEventToEdit = null
                                    showEventBottomSheet = true
                                },
                                onEventClick = { ev ->
                                    selectedEventForDetails = ev
                                },
                                onHolidayClick = { h ->
                                    selectedHolidayForDetails = h
                                }
                            )
                        }
                        CalendarViewMode.YEAR -> {
                            YearViewContent(
                                currentYear = viewYear,
                                todayYear = viewModel.todayYear,
                                todayMonth = viewModel.todayMonth,
                                todayDay = viewModel.todayDay,
                                onYearChange = { newY -> viewModel.selectDate(newY, viewMonth, selectedDay) },
                                onMonthSelect = { m ->
                                    viewModel.selectDate(viewYear, m, 1)
                                    viewModel.setViewMode(CalendarViewMode.MONTH)
                                }
                            )
                        }
                        CalendarViewMode.WEEK -> {
                            WeekViewContent(
                                selectedYear = selectedYear,
                                selectedMonth = selectedMonth,
                                selectedDay = selectedDay,
                                todayYear = viewModel.todayYear,
                                todayMonth = viewModel.todayMonth,
                                todayDay = viewModel.todayDay,
                                events = allEvents,
                                onDaySelect = { y, m, d -> viewModel.selectDate(y, m, d) },
                                onPrevWeek = { viewModel.prevWeek() },
                                onNextWeek = { viewModel.nextWeek() },
                                onAddEventClick = {
                                    selectedEventToEdit = null
                                    showEventBottomSheet = true
                                },
                                onEventClick = { ev ->
                                    selectedEventForDetails = ev
                                }
                            )
                        }
                        CalendarViewMode.AGENDA -> {
                            AgendaViewContent(
                                events = allEvents,
                                onAddEventClick = {
                                    selectedEventToEdit = null
                                    showEventBottomSheet = true
                                },
                                onEventClick = { ev ->
                                    selectedEventForDetails = ev
                                },
                                onDeleteEventClick = { ev ->
                                    viewModel.deleteEvent(ev)
                                },
                                onHolidayClick = { h ->
                                    selectedHolidayForDetails = h
                                }
                            )
                        }
                        CalendarViewMode.IMPORTANT_DAYS -> {
                            ImportantDaysContent(
                                importantEvents = importantDays,
                                onAddImportantDayClick = {
                                    selectedEventToEdit = null
                                    isAddingImportantDay = true
                                    showEventBottomSheet = true
                                },
                                onEventClick = { ev ->
                                    selectedEventForDetails = ev
                                },
                                onHolidayClick = { h ->
                                    selectedHolidayForDetails = h
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Go to Date Dialog
    if (showGotoDialog) {
        GotoDateDialog(
            initialYear = viewYear,
            initialMonth = viewMonth,
            initialDay = selectedDay,
            onDismiss = { showGotoDialog = false },
            onDateSelected = { y, m, d ->
                viewModel.selectDate(y, m, d)
                viewModel.setViewMode(CalendarViewMode.MONTH)
            }
        )
    }

    // Event Details Popup Dialog with Delete, Edit, and Quick Important Day Toggle
    if (selectedEventForDetails != null) {
        EventDetailsDialog(
            event = selectedEventForDetails!!,
            onDismiss = { selectedEventForDetails = null },
            onDelete = { ev ->
                viewModel.deleteEvent(ev)
                selectedEventForDetails = null
            },
            onEdit = { ev ->
                val target = ev
                selectedEventForDetails = null
                selectedEventToEdit = target
                showEventBottomSheet = true
            },
            onToggleImportant = { ev ->
                val updated = ev.copy(isImportantDay = !ev.isImportantDay)
                viewModel.updateEvent(updated)
                selectedEventForDetails = updated
            }
        )
    }

    // Holiday Details Popup Dialog
    if (selectedHolidayForDetails != null) {
        HolidayDetailsDialog(
            holiday = selectedHolidayForDetails!!,
            onDismiss = { selectedHolidayForDetails = null }
        )
    }

    // New / Edit Event Bottom Sheet
    if (showEventBottomSheet) {
        NewEventBottomSheet(
            sheetState = sheetState,
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            initialDay = selectedDay,
            editingEvent = selectedEventToEdit,
            initialIsImportantDay = isAddingImportantDay,
            onDismiss = {
                showEventBottomSheet = false
                isAddingImportantDay = false
            },
            onSave = { ev ->
                if (ev.id == 0L) {
                    viewModel.addEvent(ev)
                } else {
                    viewModel.updateEvent(ev)
                }
                isAddingImportantDay = false
            },
            onDelete = { ev ->
                viewModel.deleteEvent(ev)
                isAddingImportantDay = false
            }
        )
    }
}
